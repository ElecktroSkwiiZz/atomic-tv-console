/* © 2026 JB Atomic Industries — curated workflows for Android TV.
 * Never silently removes or disables packages. User-selected packages only.
 */
package com.jbatomic.tvconsole;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

final class MaintenanceTasks {
  static final class Task {
    final String titleFr,titleEn,descriptionFr,descriptionEn;
    final String[] commands;
    final boolean changesSettings;
    Task(String fr,String en,String descFr,String descEn,boolean changes,String... commands){
      this.titleFr=fr;this.titleEn=en;this.descriptionFr=descFr;this.descriptionEn=descEn;
      this.commands=commands;this.changesSettings=changes;
    }
    String title(boolean english){return english?titleEn:titleFr;}
    String description(boolean english){return english?descriptionEn:descriptionFr;}
  }
  static final List<Task> ALL=Collections.unmodifiableList(Arrays.asList(
    new Task("Diagnostic complet TV","Full TV diagnosis",
      "Récupère version Android, CPU, mémoire, stockage et état thermique. Aucun changement.",
      "Read Android version, CPU, memory, storage and thermal state. Read-only.",false,
      "getprop ro.product.model","getprop ro.build.version.release","cat /proc/cpuinfo","cat /proc/meminfo","df -h","dumpsys thermalservice"),
    new Task("Diagnostic fluidité","Performance diagnosis",
      "Vérifie les processus, la RAM et les animations. Ne modifie aucun réglage.",
      "Inspect processes, RAM and animation settings. No changes.",false,
      "top -n 1 -m 12","dumpsys meminfo","settings get global window_animation_scale","settings get global transition_animation_scale","settings get global animator_duration_scale"),
    new Task("Diagnostic HDMI / audio","HDMI and audio diagnosis",
      "Réunit les états CEC, eARC et les périphériques audio. Lecture seule.",
      "Check CEC, eARC and audio routing. Read-only.",false,
      "settings get global hdmi_control_enabled","settings get global earc_control_enabled","dumpsys hdmi_control","dumpsys audio"),
    new Task("Diagnostic réseau","Network diagnosis",
      "Analyse les interfaces, adresses IP, routes et DNS. Lecture seule.",
      "Inspect IP interfaces, routing and DNS. Read-only.",false,
      "ip -4 addr show","ip route","getprop | grep dns","dumpsys connectivity"),
    new Task("Diagnostic stockage","Storage diagnosis",
      "Vérifie l'espace, les volumes et la mémoire sans supprimer de données.",
      "Review disk space and volumes without deleting user data.",false,
      "df -h","dumpsys diskstats","dumpsys mount","cat /proc/meminfo"),
    new Task("Diminuer les animations à 0,5×","Set animations to 0.5×",
      "Trois réglages Android. Rend les transitions plus courtes, sans augmenter les FPS réels.",
      "Three Android settings. Shorter transitions; does not increase actual video or game FPS.",true,
      "settings put global window_animation_scale 0.5",
      "settings put global transition_animation_scale 0.5",
      "settings put global animator_duration_scale 0.5"),
    new Task("Désactiver les animations","Disable animations",
      "Trois réglages Android. Transitions immédiates ; certaines applications peuvent moins bien s'afficher.",
      "Three Android settings. Instant UI transitions; some apps may not animate correctly.",true,
      "settings put global window_animation_scale 0",
      "settings put global transition_animation_scale 0",
      "settings put global animator_duration_scale 0"),
    new Task("Rétablir les animations 1×","Restore animations to 1×",
      "Rétablit les trois facteurs d'animation par défaut.",
      "Restore all three default animation scales.",true,
      "settings put global window_animation_scale 1",
      "settings put global transition_animation_scale 1",
      "settings put global animator_duration_scale 1"),
    new Task("Réduire le délai de veille à 10 min","Set sleep timeout to 10 min",
      "Règle le délai de mise en veille Android à 600000 ms. Peut être ignoré par le firmware.",
      "Set Android screen timeout to 600000 ms; firmware may override it.",true,
      "settings put system screen_off_timeout 600000"),
    new Task("Réduire les caches temporaires","Trim temporary caches",
      "Demande à Android de libérer des fichiers cache. Ne supprime pas les données personnelles ; des caches se recréent.",
      "Ask Android to trim temporary cached files. Does not erase app data; caches may be regenerated.",true,
      "pm trim-caches 1G"),
    new Task("Rapport sur les applications","Applications audit",
      "Liste les applications utilisateur, système et désactivées. Aucune suppression.",
      "List installed, system and disabled packages without modifying them.",false,
      "pm list packages -3","pm list packages -s","pm list packages -d")
  ));

  // Protect launchers, settings, update services, input, package management and connectivity.
  // This is deliberately conservative: a user can still inspect every installed package.
  private static final String[] PROTECTED_PREFIXES={
    "android","com.android.","com.google.android.gms","com.google.android.gsf",
    "com.google.android.tvlauncher","com.google.android.apps.tv.launcher",
    "com.google.android.permissioncontroller","com.google.android.packageinstaller",
    "com.google.android.ext.services","com.google.android.webview",
    "com.jbatomic.tvconsole"
  };
  private static final String[] PROTECTED_PARTS={
    "launcher","systemui","settings","permission","packageinstaller",
    "networkstack","connectivity","inputmethod","webview","keyguard",
    "updateservice","otaupdate","firmware","bootloader","framework",
    "bluetooth","wifi.service","adbd","setupwizard","devicepolicy",
    "gms","googlequicksearchbox"
  };
  static boolean protectedPackage(String packageName){
    if(packageName==null || !packageName.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))return true;
    String name=packageName.toLowerCase(Locale.ROOT);
    for(String p:PROTECTED_PREFIXES)if(name.equals(p)||name.startsWith(p))return true;
    for(String part:PROTECTED_PARTS)if(name.contains(part))return true;
    return false;
  }
  static List<String> parsePackages(String shellOutput){
    List<String> result=new ArrayList<>();
    if(shellOutput==null)return result;
    for(String line:shellOutput.split("\\r?\\n")){
      String value=line.trim();
      if(!value.startsWith("package:"))continue;
      String name=value.substring(8).trim();
      if(!protectedPackage(name) && !result.contains(name))result.add(name);
    }
    Collections.sort(result);
    return result;
  }
}
