/* ATOMIC TV Console — JB ATOMIC INDUSTRIES — https://jbatomic.com/
 * © 2026 JB Atomic Industries. Product branding and attribution.
 * User screenshots and recordings remain unwatermarked.
 */
package com.jbatomic.tvconsole;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

/** Download an official GitHub Release APK and hand off to Android's installer (with user approval). */
public final class UpdateManager {
    public static final String DEFAULT_REPOSITORY = "ElecktroSkwiiZz/atomic-tv-console";
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean busy;
    private boolean en() { return "en".equals(activity.getSharedPreferences("tv_console", Activity.MODE_PRIVATE).getString("language","fr")); }
    private String tr(String fr,String english) { return en()?english:fr; }

    public UpdateManager(Activity activity) { this.activity = activity; }
    public static String repository(Activity activity) { return DEFAULT_REPOSITORY; }
    public static boolean validRepository(String name) {
        return name != null && name.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}") && !name.contains("..");
    }

    public void check() { checkInternal(false); }

    public void checkAutomaticallyIfDue() {
        android.content.SharedPreferences pref = activity.getSharedPreferences("tv_console", Activity.MODE_PRIVATE);
        if (!pref.getBoolean("auto_update_check", true)) return;
        long now = System.currentTimeMillis();
        if (now - pref.getLong("last_update_check", 0L) < 24L * 3600_000) return;
        pref.edit().putLong("last_update_check", now).apply();
        checkInternal(true);
    }

    private void checkInternal(boolean silent) {
        if (busy) { if (!silent) toast(tr("Une opération de mise à jour est déjà en cours","Another update operation is already running")); return; }
        String repo = repository(activity);
        if (!validRepository(repo)) { toast(tr("Indique d'abord un dépôt GitHub valide","A valid GitHub repository is required")); return; }
        busy = true;
        if (!silent) toast(tr("Recherche d'une version sur GitHub…","Checking for new versions on GitHub…"));
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + repo + "/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(12000); connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "ATOMIC-TV-Console/" + BuildConfig.VERSION_NAME);
                int status = connection.getResponseCode();
                if (status == 404) throw new Exception(tr("Aucune version publique n'a encore été publiée sur ","No stable release has been published on ") + repo);
                if (status != 200) throw new Exception(tr("GitHub répond HTTP ","GitHub HTTP status ") + status);
                String json;
                try (InputStream in = connection.getInputStream()) { json = new String(readLimit(in, 512_000), StandardCharsets.UTF_8); }
                JSONObject release = new JSONObject(json);
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) throw new Exception(tr("Aucune version stable publiée","No stable version has been published"));
                String tag = release.optString("tag_name", "");
                JSONArray assets = release.optJSONArray("assets");
                String assetUrl = null;
                String digest = "";
                if (assets != null) for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String name = asset.optString("name", "").toLowerCase(Locale.ROOT);
                    if (name.endsWith(".apk")) {
                        assetUrl = asset.optString("browser_download_url");
                        digest = asset.optString("digest", "");
                        break;
                    }
                }
                if (assetUrl == null) throw new Exception(tr("Aucune APK disponible dans cette publication GitHub","No APK attached to this GitHub release"));
                if (!assetUrl.startsWith("https://github.com/" + repo + "/releases/download/")) {
                    throw new Exception(tr("Adresse APK inattendue : publication refusée","Unexpected APK download URL: refusing update"));
                }
                if (compareVersion(tag, BuildConfig.VERSION_NAME) <= 0) {
                    if (!silent) onMain(() -> new AlertDialog.Builder(activity).setTitle(tr("Application à jour","Already up to date"))
                        .setMessage(tr("Version installée : ","Installed version: ") + BuildConfig.VERSION_NAME +
                            tr("\nDernière version GitHub : ","\nLatest GitHub version: ") + tag)
                        .setPositiveButton(tr("Fermer","Close"), null).show());
                    return;
                }
                final String download = assetUrl;
                final String sha = digest;
                onMain(() -> new AlertDialog.Builder(activity).setTitle(tr("Mise à jour disponible","Update available"))
                    .setMessage(tr("Version actuelle : ","Installed version: ") + BuildConfig.VERSION_NAME +
                        tr("\nNouvelle version : ","\nNew version: ") + tag +
                        tr("\n\nL'APK sera vérifiée avant l'installation. Android vous demandera de confirmer la mise à jour.",
                           "\n\nThe APK signature is checked before Android asks to install it."))
                    .setNegativeButton(tr("Plus tard","Later"), null)
                    .setPositiveButton(tr("Télécharger","Download"), (dialog, which) -> download(download, sha)).show());
            } catch (Exception error) { if (!silent) onMain(() -> new AlertDialog.Builder(activity)
                .setTitle(tr("Vérification impossible","Update check failed")).setMessage(error.getMessage())
                .setPositiveButton(tr("Fermer","Close"), null).show()); }
            finally { busy = false; }
        }, "tv-console-update-check").start();
    }

    private void download(String url, String digest) {
        if (busy) { toast(tr("Téléchargement déjà en cours","An APK download is already running")); return; }
        busy = true; toast(tr("Téléchargement de l'APK depuis GitHub…","Downloading APK from GitHub…"));
        new Thread(() -> {
            try {
                File tmp = new File(activity.getCacheDir(), "ATOMIC-TV-Console-update.part");
                File finalApk = new File(activity.getCacheDir(), "ATOMIC-TV-Console-update.apk");
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000); conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "ATOMIC-TV-Console/" + BuildConfig.VERSION_NAME);
                if (conn.getResponseCode() != 200) throw new Exception(tr("Téléchargement HTTP ","Download HTTP status ") + conn.getResponseCode());
                long size = conn.getContentLengthLong();
                if (size > 200_000_000) throw new Exception(tr("APK trop volumineuse","APK exceeds the size limit"));
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                long received = 0;
                try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[65536]; int n;
                    while ((n = in.read(buf)) != -1) {
                        received += n;
                        if (received > 200_000_000) throw new Exception(tr("APK trop volumineuse","APK exceeds the size limit"));
                        sha.update(buf, 0, n); out.write(buf, 0, n);
                    }
                }
                if (received < 1000) throw new Exception(tr("APK vide ou incomplète","The downloaded APK is empty or incomplete"));
                if (digest.startsWith("sha256:")) {
                    String actual = hex(sha.digest());
                    if (!actual.equalsIgnoreCase(digest.substring(7))) throw new Exception(tr("Empreinte SHA-256 incorrecte","SHA-256 checksum mismatch"));
                }
                if (!verifySameSigningCertificate(tmp)) throw new Exception(
                    tr("Signature différente de l'application installée. Une mise à jour directe est interdite par Android. ","The signing certificate differs from the installed application. Android will block the update. ") +
                    tr("Conservez la même clé de signature pour toutes les versions.","Use the same signing key for all versions."));
                PackageInfo info = activity.getPackageManager().getPackageArchiveInfo(tmp.getAbsolutePath(), 0);
                if (info == null || info.getLongVersionCode() <= BuildConfig.VERSION_CODE) throw new Exception(tr("L'APK n'est pas une version plus récente","The downloaded APK is not newer"));
                if (finalApk.exists() && !finalApk.delete()) throw new Exception(tr("Ancien fichier APK verrouillé","The previous APK file is locked"));
                if (!tmp.renameTo(finalApk)) throw new Exception(tr("Impossible de préparer l'APK","Unable to prepare downloaded APK"));
                onMain(() -> install(finalApk));
            } catch (Exception error) { onMain(() -> new AlertDialog.Builder(activity)
                .setTitle(tr("Mise à jour non installée","Update not installed")).setMessage(error.getMessage())
                .setPositiveButton(tr("Fermer","Close"), null).show()); }
            finally { busy = false; }
        }, "tv-console-apk-download").start();
    }

    private boolean verifySameSigningCertificate(File candidate) throws Exception {
        PackageManager pm = activity.getPackageManager();
        PackageInfo installed = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
        PackageInfo downloaded = pm.getPackageArchiveInfo(candidate.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
        if (downloaded == null || !activity.getPackageName().equals(downloaded.packageName)) return false;
        if (installed.signingInfo == null || downloaded.signingInfo == null) return false;
        Signature[] left = installed.signingInfo.getApkContentsSigners();
        Signature[] right = downloaded.signingInfo.getApkContentsSigners();
        if (left.length != right.length || left.length == 0) return false;
        for (Signature signer : left) {
            boolean found = false;
            for (Signature other : right) if (Arrays.equals(signer.toByteArray(), other.toByteArray())) found = true;
            if (!found) return false;
        }
        return true;
    }

    public void resumePendingInstallation() {
        android.content.SharedPreferences pref = activity.getSharedPreferences("tv_console", Activity.MODE_PRIVATE);
        if (!pref.getBoolean("pending_update_install", false) || !activity.getPackageManager().canRequestPackageInstalls()) return;
        pref.edit().putBoolean("pending_update_install", false).apply();
        File file = new File(activity.getCacheDir(), "ATOMIC-TV-Console-update.apk");
        if (file.isFile()) new AlertDialog.Builder(activity)
            .setTitle(tr("Mise à jour prête","Update ready")).setMessage(tr("Voulez-vous installer l'APK signée déjà téléchargée ?","Install the signed APK already downloaded?"))
            .setNegativeButton(tr("Plus tard","Later"), null)
            .setPositiveButton(tr("Installer","Install"), (dialog, n) -> install(file)).show();
    }

    private void install(File apk) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            activity.getSharedPreferences("tv_console", Activity.MODE_PRIVATE).edit()
                .putBoolean("pending_update_install", true).apply();
            new AlertDialog.Builder(activity).setTitle(tr("Autorisation Android requise","Android permission required"))
                .setMessage(tr("Autorisez ATOMIC TV Console à installer des APK, puis relancez la vérification de mise à jour.","Allow ATOMIC TV Console to install APK files, then retry the update."))
                .setNegativeButton(tr("Annuler","Cancel"), null)
                .setPositiveButton(tr("Paramètres","Settings"), (d, x) -> {
                    Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()));
                    try { activity.startActivity(i); } catch (Exception e) { toast(tr("Ouvrez les paramètres de sécurité Android","Open Android security settings")); }
                }).show();
            return;
        }
        Uri uri = Uri.parse("content://" + activity.getPackageName() + ".updates/apk");
        Intent intent = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("ATOMIC TV Console APK", uri));
        try { activity.startActivity(intent); }
        catch (Exception e) { toast(tr("Impossible d'ouvrir l'installeur Android : ","Could not open Android APK installer: ") + e.getClass().getSimpleName()); }
    }

    private byte[] readLimit(InputStream input, int limit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = input.read(buf)) != -1) {
            if (out.size() + n > limit) throw new Exception(tr("Réponse GitHub trop volumineuse","GitHub response exceeds the size limit"));
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
    private static String hex(byte[] data) {
        StringBuilder str = new StringBuilder(data.length * 2);
        for (byte b : data) str.append(String.format(Locale.US, "%02x", b & 255));
        return str.toString();
    }
    private static int compareVersion(String tag, String current) {
        String[] a = tag.replaceFirst("^[vV]", "").split("\\.");
        String[] b = current.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < 3; i++) {
            int x = part(a, i), y = part(b, i);
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }
    private static int part(String[] arr, int i) {
        if (i >= arr.length) return 0;
        try { return Integer.parseInt(arr[i].replaceAll("[^0-9].*$", "")); }
        catch (Exception ignored) { return 0; }
    }
    private void onMain(Runnable callback) { main.post(callback); }
    private void toast(String message) { onMain(() -> Toast.makeText(activity, message, Toast.LENGTH_LONG).show()); }
}
