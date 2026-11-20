/* © 2026 JB Atomic Industries — ATOMIC TV Console.
 * Android TV remote-first user interface; French / English; explicit ADB confirmation.
 */
package com.jbatomic.tvconsole;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class MainActivity extends Activity {
  private static final int BG=0xff091221,CARD=0xff13253b,TEXT=0xfff1f8ff,MUTED=0xffacc6da,CYAN=0xff6cdeeb;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private AdbClient client;
  private LinearLayout page;
  private boolean english;
  private boolean firstResume=true;
  private int currentScreen=0; // 0: categories, 1: browser, 2: result
  private String group=null;
  private int port=5555;
  private LinearLayout results;
  private String lastCommand="",lastOutput="";
  private boolean lastOk;

  private String t(String fr,String en){return english?en:fr;}
  private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    client=new AdbClient(this);
    english="en".equals(getSharedPreferences("tv_console",MODE_PRIVATE).getString("language",
      Locale.getDefault().getLanguage().equals("en")?"en":"fr"));
    port=getSharedPreferences("tv_console",MODE_PRIVATE).getInt("adb_port",5555);
    showHome();
    ui.postDelayed(()->new UpdateManager(this).checkAutomaticallyIfDue(),1600);
  }
  @Override public void onResume() {
    super.onResume();
    if(firstResume){firstResume=false;return;}
    ui.postDelayed(()->new UpdateManager(this).resumePendingInstallation(),450);
  }
  @Override public void onDestroy(){client.close();super.onDestroy();}
  @Override public void onBackPressed(){
    if(currentScreen==2){currentScreen=1;showBrowser(group);}
    else if(currentScreen==1)showHome();
    else super.onBackPressed();
  }
  private GradientDrawable shape(int color,int border,int radius){
    GradientDrawable d=new GradientDrawable();
    d.setColor(color); d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);
    return d;
  }
  private StateListDrawable focusBg(boolean primary){
    StateListDrawable states=new StateListDrawable();
    states.addState(new int[]{android.R.attr.state_focused},shape(0xff267f9b,0xffa2efff,11));
    states.addState(new int[]{android.R.attr.state_pressed},shape(0xff2a829b,0xffa2efff,11));
    states.addState(new int[]{},shape(primary?0xff237a96:0xff20374f,primary?0xff5de2f4:0xff345771,11));
    return states;
  }
  private void initialize(){
    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setSmoothScrollingEnabled(true);
    page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(24),dp(15),dp(24),dp(20));
    scroll.addView(page);setContentView(scroll);
    header(); space(page,13);
  }
  private LinearLayout column(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x;}
  private LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
  private TextView text(String s,int size,int color,boolean bold){
    TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);
    if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }
  private void space(LinearLayout parent,int height){
    View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));
  }
  private Button button(String label,boolean primary,Runnable click){
    Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);
    b.setTextColor(TEXT);b.setBackground(focusBg(primary));b.setMinHeight(dp(43));
    b.setPadding(dp(13),dp(8),dp(13),dp(8));
    b.setOnClickListener(v->click.run());b.setFocusable(true);
    return b;
  }
  private void action(LinearLayout into,String label,boolean primary,Runnable click) {
    Button b=button(label,primary,click);
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(7);
    into.addView(b,p);
  }
  private LinearLayout panel(LinearLayout parent){
    LinearLayout v=column();v.setPadding(dp(16),dp(14),dp(16),dp(14));
    v.setBackground(shape(CARD,0xff2b485e,14));
    parent.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;
  }
  private void header(){
    LinearLayout bar=row();page.addView(bar,new LinearLayout.LayoutParams(-1,-2));
    ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.console_icon);
    logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
    logo.setContentDescription("JB Atomic Industries — ATOMIC TV Console");
    bar.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));
    LinearLayout title=column(); title.setPadding(dp(10),0,0,0);
    bar.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
    title.addView(text("ATOMIC TV CONSOLE",22,TEXT,true));
    title.addView(text("JB ATOMIC INDUSTRIES  ·  v"+BuildConfig.VERSION_NAME,11,CYAN,false));
    Button language=button(english?"FR":"EN",false,()->{
      english=!english;
      getSharedPreferences("tv_console",MODE_PRIVATE).edit().putString("language",english?"en":"fr").apply();
      if(currentScreen==1)showBrowser(group);else showHome();
    });
    bar.addView(language,new LinearLayout.LayoutParams(dp(67),dp(48)));
  }
  private void quickBar() {
    LinearLayout bar=row();bar.setPadding(0,0,0,0);page.addView(bar);
    Button home=button(t("⌂  Accueil","⌂  Home"),false,this::showHome);
    bar.addView(home,new LinearLayout.LayoutParams(0,dp(45),1f));
    View sep=new View(this);bar.addView(sep,new LinearLayout.LayoutParams(dp(8),1));
    Button all=button(t("⌕  Rechercher","⌕  Search"),true,()->showBrowser(null));
    bar.addView(all,new LinearLayout.LayoutParams(0,dp(45),1f));
  }
  private void showHome() {
    currentScreen=0;group=null; initialize();
    page.addView(text(t("Exécutez des commandes ADB sans clavier ni PC.",
      "Run ADB commands without a keyboard or PC."),15,MUTED,false));
    page.addView(text(t("Débogage réseau requis : connexion locale sur 127.0.0.1:"+port+".",
      "Network debugging required: local connection to 127.0.0.1:"+port+"."),12,CYAN,false));
    space(page,12);
    LinearLayout top=row();page.addView(top);
    Button check=button(t("●  Tester la connexion","●  Test connection"),true,()->runCommand("getprop ro.product.model",t("Test ADB","ADB connection test"),false));
    top.addView(check,new LinearLayout.LayoutParams(0,dp(45),1f));
    View gap=new View(this);top.addView(gap,new LinearLayout.LayoutParams(dp(8),1));
    Button search=button(t("⌕  Chercher une commande","⌕  Find a command"),false,()->showBrowser(null));
    top.addView(search,new LinearLayout.LayoutParams(0,dp(45),1f));
    space(page,16);
    page.addView(text(t("CATÉGORIES","CATEGORIES")+"  ·  "+CommandCatalog.ALL.size()+" "+t("commandes","commands"),12,CYAN,true));
    space(page,8);
    LinearLayout list=column();page.addView(list);
    for(int n=0;n<CommandCatalog.GROUPS.length;n+=2){
      LinearLayout r=row();list.addView(r); r.setPadding(0,0,0,dp(8));
      for(int j=n;j<Math.min(n+2,CommandCatalog.GROUPS.length);j++){
        final String item=CommandCatalog.GROUPS[j];
        final int total=(int)CommandCatalog.ALL.stream().filter(c->item.equals(c.group)).count();
        Button b=button(CommandCatalog.title(item,english)+"  ·  "+total,false,()->showBrowser(item));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(53),1f);
        if(j%2==0)p.rightMargin=dp(7);
        r.addView(b,p);
      }
    }
    space(page,6);
    LinearLayout tools=panel(page);
    tools.addView(text(t("OUTILS","TOOLS"),12,CYAN,true));space(tools,7);
    action(tools,t("⌘  Commande personnalisée","⌘  Custom command"),false,this::customCommand);
    action(tools,t("⚙  Connexion et port ADB","⚙  ADB connection and port"),false,this::settings);
    action(tools,t("↻  Rechercher une mise à jour","↻  Check for updates"),false,()->new UpdateManager(this).check());
    space(page,11);
    page.addView(text(t("Les commandes système requièrent une connexion ADB déjà autorisée. Les actions sensibles sont confirmées avant exécution.",
      "System commands require an authorized ADB connection. Sensitive actions require confirmation."),11,MUTED,false));
  }
  private void showBrowser(String target){
    currentScreen=1;group=target;initialize();quickBar();space(page,12);
    page.addView(text(target==null?t("Toutes les commandes","All commands"):CommandCatalog.title(target,english),
      21,TEXT,true));space(page,8);
    EditText search=new EditText(this);
    search.setSingleLine(true);
    search.setTextSize(14);search.setTextColor(TEXT);search.setHintTextColor(MUTED);
    search.setHint(t("Rechercher un nom, une commande, un mot-clé…","Search by name, command, keyword…"));
    search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
    search.setBackground(shape(0xff15283c,0xff446a80,10));search.setPadding(dp(13),dp(9),dp(13),dp(9));
    page.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));space(page,9);
    results=column();page.addView(results);
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int st,int c,int a){}
      public void onTextChanged(CharSequence s,int st,int before,int count){refreshResults(s.toString());}
      public void afterTextChanged(Editable e){}
    });
    refreshResults("");
  }
  private void refreshResults(String query){
    if(results==null)return;
    results.removeAllViews();
    String q=query.toLowerCase(Locale.ROOT).trim();
    int found=0;
    for(CommandCatalog.Entry c:CommandCatalog.ALL){
      if(group!=null&&!group.equals(c.group))continue;
      String hay=(c.fr+" "+c.en+" "+c.command+" "+c.descFr+" "+c.descEn+" "+
        CommandCatalog.title(c.group,false)+" "+CommandCatalog.title(c.group,true)).toLowerCase(Locale.ROOT);
      if(!hay.contains(q))continue;
      found++;
      Button b=button(c.name(english)+(c.sensitive?"  ⚠":"")+"   ›",false,()->preview(c));
      b.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
      LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(47));bp.bottomMargin=dp(5);
      results.addView(b,bp);
      TextView detail=text(c.description(english)+"  ·  "+c.command,11,MUTED,false);
      detail.setMaxLines(2);detail.setPadding(dp(7),0,0,dp(9));
      results.addView(detail);
    }
    if(found==0)results.addView(text(t("Aucune commande trouvée.","No command found."),14,MUTED,false));
    TextView counter=text(found+" "+t("résultats","results"),11,CYAN,false);
    results.addView(counter);
  }
  private void preview(CommandCatalog.Entry entry){
    new AlertDialog.Builder(this).setTitle(entry.name(english))
      .setMessage(entry.description(english)+"\n\n$ adb shell "+entry.command+
        (entry.sensitive?"\n\n⚠ "+t("Cette commande modifie le système ou la navigation.",
          "This command changes system behavior or navigation."):""))
      .setNegativeButton(t("Annuler","Cancel"),null)
      .setPositiveButton(t("Exécuter","Run"),(d,w)->runCommand(entry.command,entry.name(english),entry.sensitive)).show();
  }
  private void customCommand(){
    EditText input=new EditText(this);input.setTextSize(16);
    input.setSingleLine(true);input.setHint("getprop ro.product.model");
    input.setSelectAllOnFocus(true);
    LinearLayout holder=column();holder.setPadding(dp(21),0,dp(21),0);
    holder.addView(text(t("Saisissez uniquement la partie après « adb shell ». Les commandes personnalisées peuvent modifier ou effacer des données.",
      "Enter the part after 'adb shell' only. Custom commands may modify or delete data."),13,MUTED,false));
    holder.addView(input,new LinearLayout.LayoutParams(-1,dp(55)));
    new AlertDialog.Builder(this).setTitle(t("Commande avancée","Advanced command"))
      .setView(holder).setNegativeButton(t("Annuler","Cancel"),null)
      .setPositiveButton(t("Prévisualiser","Preview"),(d,w)->{
        String command=input.getText().toString().trim();
        if(command.isEmpty())return;
        if(command.contains("\n")||command.length()>500){toast(t("Une seule ligne, 500 caractères maximum","One line, max 500 characters"));return;}
        new AlertDialog.Builder(this).setTitle(t("Confirmer la commande","Confirm command"))
          .setMessage("adb shell "+command+"\n\n⚠ "+t("Exécution avec les droits ADB. Vérifiez la commande avant de continuer.",
            "Runs with ADB privileges. Check the command before proceeding."))
          .setNegativeButton(t("Annuler","Cancel"),null)
          .setPositiveButton(t("Lancer","Run"),(a,b)->runCommand(command,t("Commande personnalisée","Custom command"),true)).show();
      }).show();
  }
  private void runCommand(String command,String title,boolean sensitive){
    if(client.busy()){toast(t("Une commande est déjà en cours","A command is already running"));return;}
    currentScreen=2;lastCommand=command;
    initialize();quickBar();space(page,15);
    page.addView(text(t("Exécution en cours…","Running…"),18,CYAN,true));
    page.addView(text("$ adb shell "+command,12,MUTED,false));
    client.run(port,command,(ok,output)->{
      lastOk=ok;lastOutput=output;showOutput(title);
    });
  }
  private void showOutput(String title){
    currentScreen=2;initialize();quickBar();space(page,14);
    page.addView(text(title,21,TEXT,true));space(page,5);
    page.addView(text(lastOk?t("✓ Commande terminée","✓ Command finished"):t("⚠ Vérification ADB","⚠ ADB connection issue"),12,lastOk?0xff7cddb3:0xffffb780,true));
    space(page,10);
    LinearLayout p=panel(page);
    TextView cmd=text("$ adb shell "+lastCommand,12,CYAN,false);
    cmd.setTextIsSelectable(true);p.addView(cmd);space(p,12);
    TextView output=text(lastOutput,12,TEXT,false);
    output.setTypeface(Typeface.MONOSPACE);output.setTextIsSelectable(true);
    p.addView(output);
    space(page,15);
    action(page,t("Nouvelle commande","Another command"),true,()->showBrowser(group));
  }
  private void settings(){
    EditText portInput=new EditText(this);portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    portInput.setText(String.valueOf(port));portInput.setSingleLine(true);portInput.setSelectAllOnFocus(true);
    LinearLayout holder=column();holder.setPadding(dp(20),0,dp(20),0);
    holder.addView(text(t("La connexion ADB utilisée est locale (127.0.0.1). Activez le débogage réseau/TCP sur la TV, puis acceptez la clé RSA lors du premier test. Le port 5555 n'est pas actif sur toutes les TV.",
      "ADB connects to this TV only (127.0.0.1). Enable network/TCP debugging in your TV settings and approve the RSA key on first connection. Port 5555 is not enabled on all TVs."),13,MUTED,false));
    space(holder,10);
    holder.addView(text(t("Port ADB TCP","ADB TCP port"),13,CYAN,true));
    holder.addView(portInput,new LinearLayout.LayoutParams(-1,dp(51)));
    new AlertDialog.Builder(this).setTitle(t("Connexion ADB","ADB connection")).setView(holder)
      .setNeutralButton(t("Options développeur","Developer options"),(d,w)->{
        try{startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));}
        catch(Exception ignored){toast(t("Ouvrez les options développeur manuellement","Open Developer options manually"));}
      })
      .setNegativeButton(t("Annuler","Cancel"),null)
      .setPositiveButton(t("Enregistrer","Save"),(d,w)->{
        try{
          int candidate=Integer.parseInt(portInput.getText().toString());
          if(candidate<1024||candidate>65535)throw new NumberFormatException();
          port=candidate;getSharedPreferences("tv_console",MODE_PRIVATE).edit().putInt("adb_port",port).apply();
          showHome();
        }catch(NumberFormatException ignored){toast(t("Port invalide (1024–65535)","Invalid port (1024–65535)"));}
      }).show();
  }
  private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
}
