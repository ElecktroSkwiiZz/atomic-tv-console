/* ATOMIC TV Console — JB ATOMIC INDUSTRIES — https://jbatomic.com/
 * © 2026 JB Atomic Industries. Product branding and attribution.
 * User screenshots and recordings remain unwatermarked.
 */
package com.jbatomic.tvconsole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Grants temporary read-only access to exactly one verified cached update APK. */
public final class ApkProvider extends ContentProvider {
    private File apk(Uri uri) throws FileNotFoundException {
        if (!"/apk".equals(uri.getPath())) throw new FileNotFoundException("Chemin interdit");
        File candidate = new File(getContext().getCacheDir(), "ATOMIC-TV-Console-update.apk");
        if (!candidate.isFile()) throw new FileNotFoundException("APK absente");
        return candidate;
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Lecture seule");
        return ParcelFileDescriptor.open(apk(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        try {
            File file = apk(uri);
            String[] cols = projection == null ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            MatrixCursor cursor = new MatrixCursor(cols);
            Object[] values = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) values[i] = "ATOMIC-TV-Console-update.apk";
                else if (OpenableColumns.SIZE.equals(cols[i])) values[i] = file.length();
            }
            cursor.addRow(values);
            return cursor;
        } catch (FileNotFoundException ex) { return null; }
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
