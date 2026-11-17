package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * OblivionDLC: declarative packages (a zip with dlc.json) that restyle the client. A DLC can provide
 * theme colours, logo, home background, intro/loading video, a custom player model and extra cosmetics.
 * No code is executed from a DLC - everything is data - so a DLC cannot harm the launch pipeline.
 */
public final class OblivionDlc {
    public static final class Info {
        public String id = "";
        public String name = "";
        public String version = "";
        public String author = "";
        public File dir;
        public JSONObject manifest;
        public boolean active;
    }

    private static final String PREFS = "oblivion_dlc";
    private static final String KEY_ACTIVE = "active";
    private static final long MAX_TOTAL = 96L * 1024 * 1024;
    private static final int MAX_FILES = 300;

    private OblivionDlc() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static File root(Context c) {
        File d = new File(c.getFilesDir(), "oblivion/dlc");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    public static List<Info> list(Context c) {
        List<Info> out = new ArrayList<>();
        String active = prefs(c).getString(KEY_ACTIVE, "");
        File[] dirs = root(c).listFiles();
        if (dirs == null) return out;
        for (File d : dirs) {
            Info i = read(d);
            if (i != null) {
                i.active = i.id.equals(active);
                out.add(i);
            }
        }
        return out;
    }

    private static Info read(File dir) {
        File mf = new File(dir, "dlc.json");
        if (!mf.isFile()) return null;
        try {
            byte[] data = java.nio.file.Files.readAllBytes(mf.toPath());
            JSONObject o = new JSONObject(new String(data, StandardCharsets.UTF_8));
            Info i = new Info();
            i.id = o.getString("id");
            i.name = o.optString("name", i.id);
            i.version = o.optString("version", "");
            i.author = o.optString("author", "");
            i.dir = dir;
            i.manifest = o;
            return i;
        } catch (Exception e) {
            return null;
        }
    }

    public static Info active(Context c) {
        for (Info i : list(c)) if (i.active) return i;
        return null;
    }

    /** Installs a DLC zip (or a bare theme json) and activates it. Returns null on success or an error text. */
    public static String install(Context c, byte[] data) {
        try {
            if (data.length > 2 && data[0] == 'P' && data[1] == 'K') return installZip(c, data);
            JSONObject theme = new JSONObject(new String(data, StandardCharsets.UTF_8));
            String id = "theme-" + Integer.toHexString(theme.toString().hashCode());
            File dir = new File(root(c), id);
            dir.mkdirs();
            JSONObject mf = new JSONObject().put("id", id).put("name", theme.optString("name", id)).put("theme", theme);
            write(new File(dir, "dlc.json"), mf.toString().getBytes(StandardCharsets.UTF_8));
            setActive(c, id);
            return null;
        } catch (Exception e) {
            return String.valueOf(e.getMessage());
        }
    }

    private static String installZip(Context c, byte[] data) throws Exception {
        File tmp = new File(c.getCacheDir(), "dlc_extract_" + System.nanoTime());
        tmp.mkdirs();
        long total = 0;
        int files = 0;
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry e;
            byte[] buf = new byte[16384];
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                if (++files > MAX_FILES) return "too many files";
                File out = new File(tmp, e.getName());
                if (!out.getCanonicalPath().startsWith(tmp.getCanonicalPath() + File.separator)) {
                    return "unsafe path";
                }
                out.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    int n;
                    while ((n = zin.read(buf)) > 0) {
                        total += n;
                        if (total > MAX_TOTAL) return "package too large";
                        fos.write(buf, 0, n);
                    }
                }
            }
        }
        Info info = read(tmp);
        if (info == null) {
            deleteRecursive(tmp);
            return "dlc.json missing or invalid";
        }
        File dest = new File(root(c), info.id.replaceAll("[^A-Za-z0-9._-]", "_"));
        deleteRecursive(dest);
        if (!tmp.renameTo(dest)) return "could not install";
        setActive(c, info.id);
        return null;
    }

    public static void setActive(Context c, String id) {
        prefs(c).edit().putString(KEY_ACTIVE, id == null ? "" : id).apply();
        applyActive(c);
    }

    public static void remove(Context c, String id) {
        for (Info i : list(c)) {
            if (i.id.equals(id)) deleteRecursive(i.dir);
        }
        if (id.equals(prefs(c).getString(KEY_ACTIVE, ""))) setActive(c, "");
    }

    /** Pushes the active DLC's theme and timings into the live tokens. */
    public static void applyActive(Context c) {
        Info a = active(c);
        OblivionTheme.resetToDefault(c);
        OblivionTiming.INTRO_MS = 5000L;
        OblivionTiming.LOADING_MS = 10000L;
        if (a == null) return;
        JSONObject theme = a.manifest.optJSONObject("theme");
        if (theme != null) OblivionTheme.installDlcTheme(c, theme.toString());
        long intro = a.manifest.optLong("intro_ms", 0);
        long loading = a.manifest.optLong("loading_ms", 0);
        if (intro >= 1000 && intro <= 60000) OblivionTiming.INTRO_MS = intro;
        if (loading >= 1000 && loading <= 120000) OblivionTiming.LOADING_MS = loading;
    }

    /** File of an asset slot ("logo", "home_background", "intro_video", "loading_video") of the active DLC. */
    public static File asset(Context c, String slot) {
        Info a = active(c);
        if (a == null) return null;
        JSONObject assets = a.manifest.optJSONObject("assets");
        if (assets == null) return null;
        String rel = assets.optString(slot, "");
        if (rel.isEmpty()) return null;
        try {
            File f = new File(a.dir, rel);
            if (f.isFile() && f.getCanonicalPath().startsWith(a.dir.getCanonicalPath() + File.separator)) return f;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static PlayerModel model(Context c) {
        Info a = active(c);
        if (a == null) return null;
        String rel = a.manifest.optString("model", "");
        if (rel.isEmpty()) return null;
        try {
            File f = new File(a.dir, rel);
            if (!f.isFile() || !f.getCanonicalPath().startsWith(a.dir.getCanonicalPath() + File.separator)) return null;
            PlayerModel m = PlayerModel.fromJson(new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            String tex = a.manifest.optString("model_texture", "");
            if (m != null && !tex.isEmpty()) {
                File tf = new File(a.dir, tex);
                if (tf.isFile()) m.texture = BitmapFactory.decodeFile(tf.getAbsolutePath());
            }
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Cosmetic> cosmetics(Context c) {
        List<Cosmetic> out = new ArrayList<>();
        Info a = active(c);
        if (a == null) return out;
        JSONArray arr = a.manifest.optJSONArray("cosmetics");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                File f = new File(a.dir, o.getString("asset"));
                if (!f.isFile() || !f.getCanonicalPath().startsWith(a.dir.getCanonicalPath() + File.separator)) continue;
                Cosmetic cos = new Cosmetic();
                cos.id = a.id + ":" + o.getString("id");
                cos.name = o.optString("name", cos.id);
                cos.description = o.optString("description", "");
                cos.type = "cape".equalsIgnoreCase(o.optString("type")) ? Cosmetic.Type.CAPE : Cosmetic.Type.OVERLAY;
                cos.file = f.getAbsolutePath();
                cos.claimOnly = o.optBoolean("claim", false);
                cos.membershipOnly = o.optBoolean("membership", false);
                cos.price = o.optInt("price", 0);
                JSONArray roles = o.optJSONArray("roles");
                if (roles != null) {
                    List<String> keys = new ArrayList<>();
                    for (int r = 0; r < roles.length(); r++) keys.add(roles.optString(r));
                    cos.roles = OblivionRole.parse(keys);
                }
                out.add(cos);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static void write(File f, byte[] data) throws java.io.IOException {
        try (FileOutputStream o = new FileOutputStream(f)) {
            o.write(data);
        }
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursive(k);
        f.delete();
    }
}
