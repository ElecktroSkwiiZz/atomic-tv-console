/*
 * © 2026 JB Atomic Industries — ATOMIC TV Console.
 * Opt-in, PIN-protected LAN portal. Never expose this HTTP server to the public Internet.
 */
package com.jbatomic.tvconsole;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class ConsoleWebServer {
  static final int HTTP_PORT=8767;
  private static final int BODY_LIMIT=4096;
  private static final int OUTPUT_LIMIT=125000;
  private static final long SESSION_TTL=45L*60*1000;
  private final Context app;
  private final AdbClient adb;
  private final java.util.function.IntSupplier adbPort;
  private final SecureRandom random=new SecureRandom();
  private final ExecutorService requests=new ThreadPoolExecutor(3,5,30L,TimeUnit.SECONDS,
    new ArrayBlockingQueue<Runnable>(32));
  private final ExecutorService execution=Executors.newSingleThreadExecutor();
  private final Handler main=new Handler(Looper.getMainLooper());
  private final Map<String,Session> sessions=new ConcurrentHashMap<>();
  private final Map<String,Job> jobs=new ConcurrentHashMap<>();
  private final List<Job> history=Collections.synchronizedList(new ArrayList<>());
  private final AtomicBoolean runningJob=new AtomicBoolean();
  private volatile ServerSocket listener;
  private volatile boolean started;
  private volatile String pin;
  private int loginAttempts;
  private long lockedUntil;
  private int counter=0;
  private static final class Session {
    final String csrf;
    volatile long expires;
    Session(String csrf,long expires){this.csrf=csrf;this.expires=expires;}
  }
  private static final class Job {
    String id,title,command,output="";
    volatile String status="running";
    volatile boolean ok;
    final long created=System.currentTimeMillis();
    synchronized JSONObject json()throws Exception{
      JSONObject j=new JSONObject();
      j.put("id",id);j.put("title",title);j.put("command",command);
      j.put("output",output);j.put("status",status);j.put("ok",ok);j.put("created",created);
      return j;
    }
  }
  ConsoleWebServer(Context context,AdbClient adb,java.util.function.IntSupplier adbPort){
    this.app=context.getApplicationContext();this.adb=adb;this.adbPort=adbPort;
    regeneratePin();
  }
  String getPin(){return pin;}
  boolean active(){return started;}
  String address(){
    try{
      for(Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();en.hasMoreElements();){
        NetworkInterface n=en.nextElement();
        if(!n.isUp()||n.isLoopback())continue;
        for(Enumeration<InetAddress> addrs=n.getInetAddresses();addrs.hasMoreElements();){
          InetAddress addr=addrs.nextElement();
          if(addr instanceof Inet4Address&&addr.isSiteLocalAddress())return "http://"+addr.getHostAddress()+":"+HTTP_PORT;
        }
      }
    }catch(Exception ignored){}
    return "http://IP-DE-LA-TV:"+HTTP_PORT;
  }
  synchronized void regeneratePin(){
    pin=String.format(Locale.ROOT,"%08d",random.nextInt(100000000));
    sessions.clear();loginAttempts=0;lockedUntil=0L;
  }
  synchronized boolean start(){
    if(started)return true;
    try{
      listener=new ServerSocket();
      listener.setReuseAddress(true);
      listener.bind(new InetSocketAddress(HTTP_PORT),12);
      started=true;
      Thread accept=new Thread(()->{
        while(started){
          try{Socket s=listener.accept();
            s.setSoTimeout(16000);
            try{requests.execute(()->handle(s));}
            catch(RejectedExecutionException queueFull){try{s.close();}catch(IOException ignored){}}
          }catch(IOException | RejectedExecutionException e){if(started)android.util.Log.w("AtomicWeb","LAN server accept failed",e);}
        }
      },"atomic-console-lan");
      accept.setDaemon(true);accept.start();
      return true;
    }catch(IOException e){android.util.Log.e("AtomicWeb","Unable to bind LAN port",e);return false;}
  }
  synchronized void stop(){
    started=false;
    sessions.clear();
    try{if(listener!=null)listener.close();}catch(IOException ignored){}
  }
  void close(){stop();requests.shutdownNow();execution.shutdownNow();}
  private static String token(SecureRandom rnd){
    byte[] b=new byte[24];rnd.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }
  private static String decode(String s){
    try{return URLDecoder.decode(s,"UTF-8");}catch(Exception e){return "";}
  }
  private static Map<String,String> form(String text){
    Map<String,String> p=new HashMap<>();
    for(String piece:text.split("&")){
      String[] kv=piece.split("=",2);
      if(kv.length==2)p.put(decode(kv[0]),decode(kv[1]));
    }
    return p;
  }
  private static String header(Map<String,String> h,String key){return h.getOrDefault(key.toLowerCase(Locale.ROOT),"");}
  private boolean validHost(Map<String,String> h){
    String host=header(h,"host");
    // Reject arbitrary DNS names to reduce DNS rebinding exposure.
    return host.matches("(?:\\d{1,3}\\.){3}\\d{1,3}:"+HTTP_PORT)
      || host.equals("localhost:"+HTTP_PORT) || host.equals("127.0.0.1:"+HTTP_PORT);
  }
  private boolean sameOrigin(Map<String,String> h){
    String origin=header(h,"origin"),host=header(h,"host");
    if(origin.isEmpty())return false;
    if(host.isEmpty()||host.length()>80||host.indexOf('/')>=0||host.indexOf('@')>=0)return false;
    return origin.equalsIgnoreCase("http://"+host);
  }
  private Session session(Map<String,String> h){
    String cookies=header(h,"cookie");
    for(String c:cookies.split(";")){
      String s=c.trim();
      if(!s.startsWith("atomic_session="))continue;
      String id=s.substring("atomic_session=".length());
      Session session=sessions.get(id);
      if(session==null)return null;
      if(System.currentTimeMillis()>session.expires){sessions.remove(id);return null;}
      session.expires=System.currentTimeMillis()+SESSION_TTL;
      return session;
    }
    return null;
  }
  private void handle(Socket socket){
    try(Socket s=socket){
      InetAddress remote=s.getInetAddress();
      if(!(remote.isLoopbackAddress()||remote.isSiteLocalAddress()))return;
      InputStream input=s.getInputStream();
      BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8));
      String request=reader.readLine();
      if(request==null||request.length()>2048){respond(s,400,"text/plain","Bad request",null);return;}
      String[] first=request.split(" ");
      if(first.length!=3||!first[2].startsWith("HTTP/")){respond(s,400,"text/plain","Bad request",null);return;}
      String method=first[0],target=first[1];
      if(!method.equals("GET")&&!method.equals("POST")){respond(s,405,"text/plain","Method not allowed",null);return;}
      if(target.length()>1024){respond(s,414,"text/plain","URI too long",null);return;}
      String path=target.split("\\?",2)[0];
      Map<String,String> headers=new HashMap<>();
      int total=0;
      for(int i=0;i<42;i++){
        String line=reader.readLine();if(line==null){respond(s,400,"text/plain","Bad request",null);return;}
        if(line.isEmpty())break;
        total+=line.length();if(total>10000){respond(s,431,"text/plain","Headers too long",null);return;}
        int colon=line.indexOf(':');if(colon>0)headers.put(line.substring(0,colon).trim().toLowerCase(Locale.ROOT),line.substring(colon+1).trim());
      }
      if(!validHost(headers)){respond(s,403,"text/plain","Invalid host",null);return;}
      String body="";
      if(method.equals("POST")){
        if(!sameOrigin(headers)){respond(s,403,"application/json",error("Origin rejected"),null);return;}
        String contentType=header(headers,"content-type");
        if(!contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")){respond(s,415,"application/json",error("Invalid content type"),null);return;}
        int n;
        try{n=Integer.parseInt(header(headers,"content-length"));}catch(Exception e){n=-1;}
        if(n<0||n>BODY_LIMIT){respond(s,413,"application/json",error("Invalid request body"),null);return;}
        char[] chars=new char[n];int read=0;
        while(read<n){int z=reader.read(chars,read,n-read);if(z<0)break;read+=z;}
        if(read!=n){respond(s,400,"application/json",error("Truncated request"),null);return;}
        body=new String(chars);
      }
      Map<String,String> data=form(body);
      if(method.equals("GET")&&(path.equals("/")||path.equals("/console.css")||path.equals("/console.js"))){
        String asset=path.equals("/")?"console.html":path.substring(1);
        String type=asset.endsWith(".css")?"text/css":asset.endsWith(".js")?"application/javascript":"text/html";
        respond(s,200,type,loadAsset(asset),null);return;
      }
      if(path.equals("/api/login")&&method.equals("POST")){
        synchronized(this){
          if(System.currentTimeMillis()<lockedUntil){respond(s,429,"application/json",error("Locked for 10 minutes"),null);return;}
          if(!pin.equals(data.getOrDefault("pin",""))){
            loginAttempts++;
            if(loginAttempts>=6){lockedUntil=System.currentTimeMillis()+600000;loginAttempts=0;}
            respond(s,401,"application/json",error("Invalid access code"),null);return;
          }
          loginAttempts=0;lockedUntil=0;
        }
        String id=token(random),csrf=token(random);
        sessions.put(id,new Session(csrf,System.currentTimeMillis()+SESSION_TTL));
        JSONObject ok=new JSONObject();ok.put("ok",true);ok.put("csrf",csrf);
        respond(s,200,"application/json",ok.toString(),"atomic_session="+id+"; HttpOnly; SameSite=Strict; Path=/; Max-Age=2700");
        return;
      }
      Session sess=session(headers);
      if(sess==null){respond(s,401,"application/json",error("Authentication required"),null);return;}
      if(method.equals("POST")&&!sess.csrf.equals(header(headers,"x-atomic-csrf"))){
        respond(s,403,"application/json",error("CSRF token mismatch"),null);return;
      }
      if(path.equals("/api/session")&&method.equals("GET")){
        JSONObject ok=new JSONObject();ok.put("ok",true);ok.put("csrf",sess.csrf);
        ok.put("version",BuildConfig.VERSION_NAME);ok.put("port",HTTP_PORT);
        respond(s,200,"application/json",ok.toString(),null);return;
      }
      if(path.equals("/api/catalog")&&method.equals("GET")){
        JSONArray all=new JSONArray();for(int i=0;i<CommandCatalog.ALL.size();i++){
          CommandCatalog.Entry entry=CommandCatalog.ALL.get(i);
          JSONObject item=new JSONObject();item.put("id",i);item.put("group",entry.group);
          item.put("fr",entry.fr);item.put("en",entry.en);
          item.put("descFr",entry.descFr);item.put("descEn",entry.descEn);
          item.put("command",entry.command);item.put("sensitive",entry.sensitive);all.put(item);
        }
        respond(s,200,"application/json",all.toString(),null);return;
      }
      if(path.equals("/api/tasks")&&method.equals("GET")){
        JSONArray all=new JSONArray();for(int i=0;i<MaintenanceTasks.ALL.size();i++){
          MaintenanceTasks.Task task=MaintenanceTasks.ALL.get(i);
          JSONObject item=new JSONObject();item.put("id",i);item.put("fr",task.titleFr);
          item.put("en",task.titleEn);item.put("descFr",task.descriptionFr);
          item.put("descEn",task.descriptionEn);item.put("sensitive",task.changesSettings);
          item.put("commands",new JSONArray(Arrays.asList(task.commands)));all.put(item);
        }
        respond(s,200,"application/json",all.toString(),null);return;
      }
      if(path.equals("/api/run")&&method.equals("POST")){
        if(!runningJob.compareAndSet(false,true)){respond(s,409,"application/json",error("Command already running"),null);return;}
        try{
          String mode=data.getOrDefault("mode","");
          String name="",cmd="";
          String[] commands;
          boolean sensitive=false;
          if(mode.equals("catalog")){
            int id=Integer.parseInt(data.getOrDefault("id","-1"));
            if(id<0||id>=CommandCatalog.ALL.size())throw new IllegalArgumentException("Unknown command");
            CommandCatalog.Entry entry=CommandCatalog.ALL.get(id);
            commands=new String[]{entry.command};name=entry.fr;sensitive=entry.sensitive;
          }else if(mode.equals("workflow")){
            int id=Integer.parseInt(data.getOrDefault("id","-1"));
            if(id<0||id>=MaintenanceTasks.ALL.size())throw new IllegalArgumentException("Unknown workflow");
            MaintenanceTasks.Task task=MaintenanceTasks.ALL.get(id);
            commands=task.commands;name=task.titleFr;sensitive=task.changesSettings;
          }else if(mode.equals("terminal")){
            cmd=data.getOrDefault("command","").trim();
            if(cmd.length()<1||cmd.length()>450||cmd.contains("\r")||cmd.contains("\n"))throw new IllegalArgumentException("Invalid command");
            commands=new String[]{cmd};name="Terminal";sensitive=true;
          }else throw new IllegalArgumentException("Unknown mode");
          if(sensitive&&!"yes".equals(data.get("confirm")))throw new IllegalArgumentException("Explicit confirmation required");
          if(commands.length==0||commands.length>25)throw new IllegalArgumentException("Invalid sequence");
          String id="r"+(++counter);
          Job job=new Job();job.id=id;job.title=name;job.command=String.join("\n",commands);
          jobs.put(id,job);
          execution.execute(()->execute(job,commands));
          JSONObject ok=new JSONObject();ok.put("id",id);ok.put("status","running");
          respond(s,202,"application/json",ok.toString(),null);
        }catch(Exception e){
          runningJob.set(false);respond(s,400,"application/json",error(e.getMessage()),null);
        }
        return;
      }
      if(path.equals("/api/job")&&method.equals("GET")){
        String id=form(target.contains("?")?target.substring(target.indexOf('?')+1):"").getOrDefault("id","");
        Job job=jobs.get(id);
        if(job==null){respond(s,404,"application/json",error("Job not found"),null);return;}
        respond(s,200,"application/json",job.json().toString(),null);return;
      }
      if(path.equals("/api/history")&&method.equals("GET")){
        JSONArray all=new JSONArray();
        synchronized(history){
          for(int i=history.size()-1;i>=0;i--){
            Job job=history.get(i);
            JSONObject item=new JSONObject();
            item.put("id",job.id);item.put("title",job.title);
            item.put("ok",job.ok);item.put("status",job.status);
            item.put("created",job.created);
            all.put(item);
          }
        }
        respond(s,200,"application/json",all.toString(),null);return;
      }
      if(path.equals("/api/export")&&method.equals("GET")){
        String id=form(target.contains("?")?target.substring(target.indexOf('?')+1):"").getOrDefault("id","");
        Job job=jobs.get(id);
        if(job==null||!"done".equals(job.status)){respond(s,404,"text/plain","No completed report",null);return;}
        String report="ATOMIC TV Console — JB Atomic Industries\n"+new Date(job.created)+"\n"+job.title+"\n$ adb shell "+job.command+"\n\n"+job.output;
        respond(s,200,"text/plain; charset=utf-8",report,null,"attachment; filename=\"atomic-console-"+id+".txt\"");
        return;
      }
      if(path.equals("/api/logout")&&method.equals("POST")){
        for(String cookie:header(headers,"cookie").split(";")){
          String t=cookie.trim();if(t.startsWith("atomic_session="))sessions.remove(t.substring("atomic_session=".length()));
        }
        respond(s,200,"application/json","{\"ok\":true}","atomic_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");return;
      }
      respond(s,404,"application/json",error("Not found"),null);
    }catch(Exception e){android.util.Log.w("AtomicWeb","Request failed",e);}
  }
  void recordLocal(String title,String command,String output,boolean ok){
    Job job=new Job();
    synchronized(this){job.id="tv"+(++counter);}
    job.title=title==null?"TV command":title;
    job.command=command==null?"":command;
    job.output=output==null?"":output.substring(0,Math.min(output.length(),OUTPUT_LIMIT));
    job.ok=ok;job.status="done";
    jobs.put(job.id,job);
    remember(job);
  }
  private void remember(Job job){
    synchronized(history){
      history.add(job);
      while(history.size()>20){
        Job old=history.remove(0);
        jobs.remove(old.id);
      }
    }
  }
  private void execute(Job job,String[] commands){
    boolean success=true;StringBuilder report=new StringBuilder();
    try{
      for(int i=0;i<commands.length;i++){
        String command=commands[i];CountDownLatch latch=new CountDownLatch(1);
        final boolean[] ok={false};final String[] result={""};
        adb.run(adbPort.getAsInt(),command+"; echo ATOMIC_EXIT_CODE:$?",(passed,output)->{
          ok[0]=passed;result[0]=output;latch.countDown();
        });
        if(!latch.await(32,TimeUnit.SECONDS)){success=false;report.append("\nADB timeout\n");break;}
        boolean passed=ok[0]&&result[0].matches("(?s).*ATOMIC_EXIT_CODE:0\\s*.*");
        String clean=result[0].replaceAll("ATOMIC_EXIT_CODE:[0-9]+","").trim();
        report.append("\n[").append(i+1).append('/').append(commands.length).append("] $ adb shell ").append(command)
          .append('\n').append(clean).append('\n');
        if(report.length()>OUTPUT_LIMIT)report.delete(0,report.length()-OUTPUT_LIMIT);
        if(!passed){success=false;break;}
      }
    }catch(Exception e){success=false;report.append("\n").append(e.getMessage());}
    finally{
      job.ok=success;job.output=report.toString();job.status="done";
      remember(job);
      runningJob.set(false);
    }
  }
  private String loadAsset(String filename)throws IOException{
    try(InputStream in=app.getAssets().open(filename);ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
      return out.toString("UTF-8");
    }
  }
  private static String error(String s){
    try{return new JSONObject().put("error",s==null?"Error":s).toString();}catch(Exception e){return "{\"error\":\"Error\"}";}
  }
  private void respond(Socket s,int status,String type,String body,String cookie)throws IOException{
    respond(s,status,type,body,cookie,null);
  }
  private void respond(Socket s,int status,String type,String body,String cookie,String disposition)throws IOException{
    String reason=status==200?"OK":status==202?"Accepted":status==400?"Bad Request":status==401?"Unauthorized":
      status==403?"Forbidden":status==404?"Not Found":status==409?"Conflict":status==429?"Too Many Requests":"Error";
    byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
    StringBuilder head=new StringBuilder("HTTP/1.1 "+status+" "+reason+"\r\n");
    head.append("Content-Type: ").append(type).append(type.contains("charset")?"":"; charset=utf-8").append("\r\n");
    head.append("Content-Length: ").append(bytes.length).append("\r\n");
    head.append("Cache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n");
    head.append("X-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\n");
    head.append("Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self' data:; base-uri 'none'; frame-ancestors 'none'; form-action 'self'\r\n");
    if(cookie!=null)head.append("Set-Cookie: ").append(cookie).append("\r\n");
    if(disposition!=null)head.append("Content-Disposition: ").append(disposition).append("\r\n");
    head.append("Connection: close\r\n\r\n");
    OutputStream out=s.getOutputStream();
    out.write(head.toString().getBytes(StandardCharsets.US_ASCII));
    out.write(bytes);out.flush();
  }
}
