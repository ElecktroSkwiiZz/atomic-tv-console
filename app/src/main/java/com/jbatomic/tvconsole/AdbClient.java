/* Copyright 2026 JB Atomic Industries. Authenticated legacy ADB TCP, loopback only. */
package com.jbatomic.tvconsole;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class AdbClient {
  interface Result { void complete(boolean success, String output); }
  private final Context context;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final AtomicBoolean busy = new AtomicBoolean();
  private final AdbBase64 base64 = data -> Base64.encodeToString(data, Base64.NO_WRAP);
  AdbClient(Context context) { this.context=context.getApplicationContext(); }
  boolean busy(){return busy.get();}
  void run(int port, String command, Result callback) {
    if (!busy.compareAndSet(false,true)) { callback.complete(false,"Already running / Une commande est déjà en cours"); return; }
    executor.execute(() -> {
      boolean ok=false; String result;
      try { result=execute(port,command); ok=true; }
      catch(Exception e) { result=e.getClass().getSimpleName()+": "+(e.getMessage()==null?"ADB connection failed":e.getMessage()); }
      final boolean success=ok; final String output=result;
      main.post(() -> { busy.set(false);callback.complete(success,output); });
    });
  }
  private AdbCrypto crypto() throws Exception {
    File privateKey=new File(context.getFilesDir(),"adb-console-private.key");
    File publicKey=new File(context.getFilesDir(),"adb-console-public.key");
    if(privateKey.exists() && publicKey.exists()) return AdbCrypto.loadAdbKeyPair(base64,privateKey,publicKey);
    AdbCrypto key=AdbCrypto.generateAdbKeyPair(base64);
    key.saveAdbKeyPair(privateKey,publicKey);
    return key;
  }
  private String execute(int port,String command) throws Exception {
    if(port<1024 || port>65535)throw new IllegalArgumentException("TCP port must be between 1024 and 65535");
    if(command==null || command.length()>500 || command.contains("\n") || command.contains("\r"))
      throw new IllegalArgumentException("One line only, 500 characters maximum");
    try(Socket socket=new Socket()){
      socket.connect(new InetSocketAddress("127.0.0.1",port),4500);
      socket.setSoTimeout(20000);
      // The watchdog also unblocks a blocked AdbStream.read() if adbd stays silent.
      Thread watchdog=new Thread(() -> { try { Thread.sleep(19000);socket.close(); }catch(Exception ignored){} },"atomic-adb-watchdog");
      watchdog.setDaemon(true);watchdog.start();
      AdbConnection conn=AdbConnection.create(socket,crypto());
      try {
        if(!conn.connect(17000,TimeUnit.MILLISECONDS,false))
          throw new IOException("Authorize the RSA key on the TV / Autorisez la clé RSA sur la TV");
        try(AdbStream stream=conn.open("shell:"+command)){
          StringBuilder out=new StringBuilder();
          while(!stream.isClosed() && out.length()<32000) {
            try { byte[] bytes=stream.read(); if(bytes!=null)out.append(new String(bytes,StandardCharsets.UTF_8)); }
            catch(IOException e) { break; }
          }
          return out.length()==0?"(no output / aucune sortie)":out.toString();
        }
      }finally{conn.close();}
    }
  }
  void close(){executor.shutdownNow();}
}
