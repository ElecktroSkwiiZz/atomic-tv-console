/* Copyright 2026 JB Atomic Industries. Readable catalog; no root required. */
package com.jbatomic.tvconsole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CommandCatalog {
  static final String[] GROUPS = {"system","display","network","audio","apps","storage","input","performance","settings","diagnostic"};
  static final String[] TITLES_FR = {"Système","Écran et affichage","Réseau","Audio","Applications","Stockage","Télécommande et saisie","Performances","Paramètres","Diagnostic"};
  static final String[] TITLES_EN = {"System","Screen and display","Network","Audio","Applications","Storage","Remote and input","Performance","Settings","Diagnostics"};
  static final class Entry {
    final String group,fr,en,descFr,descEn,command;
    final boolean sensitive;
    Entry(String g,String f,String e,String df,String de,String c,boolean s){group=g;fr=f;en=e;descFr=df;descEn=de;command=c;sensitive=s;}
    String name(boolean english){return english?en:fr;}
    String description(boolean english){return english?descEn:descFr;}
  }
  static final List<Entry> ALL;
  static {
    List<Entry> a=new ArrayList<>();
    add(a,"system","Modèle du téléviseur","TV model","Fabricant et modèle","Manufacturer and model","getprop ro.product.model");
    add(a,"system","Fabricant","Manufacturer","Marque du matériel","Device manufacturer","getprop ro.product.manufacturer");
    add(a,"system","Version Android","Android version","Version Android installée","Installed Android version","getprop ro.build.version.release");
    add(a,"system","Niveau API","Android API level","Niveau du SDK Android","Android SDK level","getprop ro.build.version.sdk");
    add(a,"system","Version du système","System build","Empreinte de la ROM","Firmware build fingerprint","getprop ro.build.fingerprint");
    add(a,"system","Architecture CPU","CPU architecture","ABI principale de l'appareil","Primary device ABI","getprop ro.product.cpu.abi");
    add(a,"system","Durée de fonctionnement","Uptime","Temps écoulé depuis le démarrage","Time since last boot","uptime");
    add(a,"system","Heure du système","System time","Date et heure du téléviseur","Device date and time","date");
    add(a,"system","Informations système","System information","Informations du noyau Linux","Linux kernel information","uname -a");
    add(a,"system","Propriétés Android","Android properties","Liste des propriétés système","List Android system properties","getprop");
    add(a,"system","État de la batterie","Battery state","Selon les capteurs disponibles","If the TV reports battery information","dumpsys battery");
    add(a,"system","Redémarrer la TV","Reboot TV","Redémarre le téléviseur","Restart the TV","reboot",true);
    add(a,"display","Résolution actuelle","Current resolution","Taille d'affichage Android","Android logical display size","wm size");
    add(a,"display","Densité d'écran","Screen density","Densité logique Android","Android screen density","wm density");
    add(a,"display","État de l'écran","Display state","Informations sur les écrans reconnus","Information about detected displays","dumpsys display");
    add(a,"display","Luminosité système","System brightness","Valeur du réglage de luminosité","Current brightness setting","settings get system screen_brightness");
    add(a,"display","Taille des polices","Font size","Facteur d'échelle des polices","Font scale factor","settings get system font_scale");
    add(a,"display","Rotation automatique","Auto rotation","État de la rotation automatique","Auto rotate setting","settings get system accelerometer_rotation");
    add(a,"display","Animations fenêtre","Window animation scale","Facteur d'animation des fenêtres","Window animation scale","settings get global window_animation_scale");
    add(a,"display","Animations transitions","Transition animation scale","Facteur d'animation des transitions","Transition animation scale","settings get global transition_animation_scale");
    add(a,"display","Animations système","Animator scale","Facteur de durée des animations","Animator duration scale","settings get global animator_duration_scale");
    add(a,"display","Désactiver les animations","Disable animations","Peut rendre l'interface plus directe","Can make UI transitions feel faster","settings put global window_animation_scale 0; settings put global transition_animation_scale 0; settings put global animator_duration_scale 0",true);
    add(a,"display","Rétablir les animations","Restore animations","Règle les animations système sur 1x","Set system animations back to 1x","settings put global window_animation_scale 1; settings put global transition_animation_scale 1; settings put global animator_duration_scale 1",true);
    add(a,"network","Adresses réseau","Network interfaces","Liste des interfaces et adresses IP","List network interfaces and IP addresses","ip addr");
    add(a,"network","Itinéraires réseau","Network routes","Table de routage de la TV","TV routing table","ip route");
    add(a,"network","État du Wi-Fi","Wi-Fi state","Informations Wi-Fi selon Android","Wi-Fi status if available","dumpsys wifi");
    add(a,"network","État de la connectivité","Connectivity state","Connexions réseau suivies par Android","Android network connectivity","dumpsys connectivity");
    add(a,"network","DNS configurés","Configured DNS","DNS selon les propriétés système","DNS properties if exposed","getprop | grep dns");
    add(a,"network","Test Internet","Internet test","Test TCP sans commande ping longue","Quick network connectivity test","ping -c 2 -W 2 1.1.1.1");
    add(a,"network","Test DNS","DNS test","Résolution du domaine Google","Resolve the Google domain","ping -c 2 -W 2 google.com");
    add(a,"network","Connexion Ethernet","Ethernet status","État du réseau filaire","Wired network status","dumpsys ethernet");
    add(a,"audio","Périphériques audio","Audio devices","Sorties et entrées disponibles","Available audio outputs and inputs","dumpsys audio");
    add(a,"audio","Volume multimédia","Media volume","Réglages et volumes des flux audio","Media stream levels and settings","dumpsys audio | grep -A 20 STREAM_MUSIC");
    add(a,"audio","Politique audio","Audio policy","Routage et formats audios","Audio routes and formats","dumpsys media.audio_policy");
    add(a,"audio","État HDMI CEC","HDMI CEC state","État CEC selon la ROM Android","CEC status if supported by firmware","dumpsys hdmi_control");
    add(a,"audio","Volume plus","Volume up","Envoie l'événement VOLUME_UP","Send VOLUME_UP key event","input keyevent 24");
    add(a,"audio","Volume moins","Volume down","Envoie l'événement VOLUME_DOWN","Send VOLUME_DOWN key event","input keyevent 25");
    add(a,"audio","Couper/rétablir le son","Toggle mute","Envoie la commande de mise en sourdine","Send mute key event","input keyevent 164");
    add(a,"apps","Applications installées","Installed packages","Liste de toutes les applications","List all installed packages","pm list packages");
    add(a,"apps","Applications tierces","Third-party packages","Applications non système","User-installed packages","pm list packages -3");
    add(a,"apps","Applications système","System packages","Applications préinstallées","Preinstalled packages","pm list packages -s");
    add(a,"apps","Applications désactivées","Disabled packages","Paquets actuellement désactivés","Disabled application packages","pm list packages -d");
    add(a,"apps","Applications actives","Running activities","Activités et tâches Android","Android activities and tasks","dumpsys activity activities");
    add(a,"apps","Processus","Processes","Processus et identifiants","Processes and PIDs","ps -A");
    add(a,"apps","Processus principaux","Top processes","Principales consommations CPU","Processes using the most CPU","top -n 1 -m 12");
    add(a,"apps","Version de TV Capture","TV Capture version","Détails du paquet JB Atomic","JB Atomic package metadata","dumpsys package com.jbatomic.tvcapture | grep -E 'versionCode|versionName'");
    add(a,"storage","Espace de stockage","Disk usage","Espace utilisé et disponible","Used and available disk space","df -h");
    add(a,"storage","Montages","Mounted filesystems","Points de montage Android","Android mount points","mount");
    add(a,"storage","Mémoire disponible","Memory information","État et consommation de la RAM","RAM usage and status","cat /proc/meminfo");
    add(a,"storage","Stockage Android","Android storage","Informations du gestionnaire de stockage","Android storage management","dumpsys mount");
    add(a,"storage","Liste des téléchargements","Downloads directory","Fichiers visibles dans Download","Files visible in Download","ls -la /sdcard/Download");
    add(a,"input","Bouton Accueil","HOME button","Retour à l'accueil Android","Return to Android home","input keyevent 3",true);
    add(a,"input","Bouton Retour","BACK button","Événement Android Retour","Send Android Back event","input keyevent 4");
    add(a,"input","Bouton Menu","MENU button","Événement Android Menu","Send Android Menu event","input keyevent 82");
    add(a,"input","Bouton OK","OK button","Événement Android Entrée","Send Android Enter event","input keyevent 66");
    add(a,"input","Touche lecture/pause","Play/pause","Contrôle des contenus multimédias","Media play/pause event","input keyevent 85");
    add(a,"input","Touches configurées","Input devices","Périphériques d'entrée détectés","Detected input devices","dumpsys input");
    add(a,"performance","Utilisation CPU","CPU usage","Échantillon des processus","One CPU process sample","top -n 1 -m 10");
    add(a,"performance","Fréquences CPU","CPU frequencies","Fréquences si exposées par le noyau","CPU frequencies if exposed by kernel","cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
    add(a,"performance","Mémoire RAM","RAM status","Résumé de la mémoire Android","Android memory summary","dumpsys meminfo");
    add(a,"performance","Mémoire de la TV Capture","TV Capture memory","Consommation RAM de ATOMIC TV Capture","ATOMIC TV Capture memory usage","dumpsys meminfo com.jbatomic.tvcapture");
    add(a,"performance","Processus les plus lourds","Top memory/CPU processes","Liste de performance CPU","CPU performance list","top -n 1 -m 20");
    add(a,"settings","Veille écran","Screen timeout","Durée avant la veille en millisecondes","Screen sleep delay in milliseconds","settings get system screen_off_timeout");
    add(a,"settings","ADB activé","ADB enabled","Indicateur du débogage USB","USB debugging enabled indicator","settings get global adb_enabled");
    add(a,"settings","Mode développement","Development settings","Débogage et options de développement","Developer options enabled","settings get global development_settings_enabled");
    add(a,"settings","CEC HDMI activé","HDMI CEC enabled","État du contrôle HDMI","HDMI control setting","settings get global hdmi_control_enabled");
    add(a,"settings","eARC activé","eARC enabled","État du canal audio amélioré","Enhanced Audio Return Channel setting","settings get global earc_control_enabled");
    add(a,"settings","Liste globale des paramètres","All global settings","Liste des valeurs Android globales","List Android global settings","settings list global");
    add(a,"settings","Liste des paramètres système","All system settings","Liste des valeurs système","List Android system settings","settings list system");
    add(a,"settings","Liste des paramètres sécurisés","All secure settings","Liste des valeurs sécurisées lisibles","Readable secure settings","settings list secure");
    add(a,"diagnostic","État des services","Service list","Services système disponibles","Available Android system services","service list");
    add(a,"diagnostic","Version de SurfaceFlinger","Graphics compositor","État du compositeur graphique","Graphics compositor status","dumpsys SurfaceFlinger");
    add(a,"diagnostic","Résumé des fenêtres","Window state","Fenêtres et surface actuellement actives","Current windows and surfaces","dumpsys window");
    add(a,"diagnostic","Derniers logs système","System log tail","Dernières lignes de logcat","Last logcat entries","logcat -d -t 80");
    add(a,"diagnostic","Thermique","Thermal state","Informations de températures disponibles","Available temperature readings","dumpsys thermalservice");
    add(a,"diagnostic","État des capteurs","Sensors","Capteurs exposés par le matériel","Sensors exposed by the hardware","dumpsys sensorservice");
    ALL=Collections.unmodifiableList(a);
  }
  private static void add(List<Entry> a,String g,String fr,String en,String df,String de,String command){add(a,g,fr,en,df,de,command,false);}
  private static void add(List<Entry> a,String g,String fr,String en,String df,String de,String command,boolean sensitive){
    a.add(new Entry(g,fr,en,df,de,command,sensitive));
  }
  static String title(String group,boolean english){
    for(int i=0;i<GROUPS.length;i++)if(GROUPS[i].equals(group))return english?TITLES_EN[i]:TITLES_FR[i];
    return english?"All commands":"Toutes les commandes";
  }
}
