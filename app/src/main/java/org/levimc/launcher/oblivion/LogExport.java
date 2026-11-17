package org.levimc.launcher.oblivion;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import org.levimc.launcher.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Saves a launch log as a text file in Downloads (Android 10+) or the app's external files folder. */
public final class LogExport {
    private LogExport() {
    }

    public static void save(Context c, String text) {
        String name = "oblivion-launch-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        try {
            String where;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Oblivion");
                Uri uri = c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new IllegalStateException("insert failed");
                try (OutputStream out = c.getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IllegalStateException("no stream");
                    out.write(data);
                }
                where = "Downloads/Oblivion/" + name;
            } else {
                File dir = c.getExternalFilesDir("logs");
                if (dir == null) dir = new File(c.getFilesDir(), "logs");
                dir.mkdirs();
                File f = new File(dir, name);
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(data);
                }
                where = f.getAbsolutePath();
            }
            Toast.makeText(c, c.getString(R.string.oblivion_log_saved, where), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(c, String.valueOf(e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
}
