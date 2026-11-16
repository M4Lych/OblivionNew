package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.util.LauncherStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Skin helpers: normalise, apply cosmetics, draw a front preview and write a Bedrock skin pack for the instance. */
public final class SkinTools {
    private static final String PREFS = "oblivion_skin";
    private static final String KEY_SLIM = "slim";
    private static final String KEY_PACK_COUNTER = "pack_counter";
    private static final String KEY_COSMETICS = "applied_cosmetics";
    private static final String PACK_DIR = "oblivion_skins";

    private SkinTools() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static File skinFile(Context c) {
        File d = new File(c.getFilesDir(), "oblivion");
        if (!d.isDirectory()) d.mkdirs();
        return new File(d, "skin.png");
    }

    public static boolean isSlim(Context c) {
        return prefs(c).getBoolean(KEY_SLIM, false);
    }

    public static void setSlim(Context c, boolean slim) {
        prefs(c).edit().putBoolean(KEY_SLIM, slim).apply();
    }

    public static List<String> appliedCosmetics(Context c) {
        List<String> out = new ArrayList<>();
        String s = prefs(c).getString(KEY_COSMETICS, "");
        for (String p : s.split(",")) if (!p.isEmpty()) out.add(p);
        return out;
    }

    public static void setAppliedCosmetics(Context c, List<String> ids) {
        prefs(c).edit().putString(KEY_COSMETICS, String.join(",", ids)).apply();
    }

    public static Bitmap loadBaseSkin(Context c) {
        File f = skinFile(c);
        if (!f.isFile()) return null;
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
        return b == null ? null : normalize(b);
    }

    public static void saveBaseSkin(Context c, Bitmap skin) throws IOException {
        try (FileOutputStream out = new FileOutputStream(skinFile(c))) {
            normalize(skin).compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    /** Converts a 64x32 legacy skin to 64x64 and rejects other sizes. */
    public static Bitmap normalize(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w == 64 && h == 64) return src.copy(Bitmap.Config.ARGB_8888, true);
        if (w == 64 && h == 32) {
            Bitmap out = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(out);
            cv.drawBitmap(src, 0, 0, null);
            copyLimbFlipped(out, 0, 16, 16, 48);
            copyLimbFlipped(out, 40, 16, 32, 48);
            return out;
        }
        return null;
    }

    private static void copyLimbFlipped(Bitmap bmp, int ox, int oy, int nx, int ny) {
        flip(bmp, ox + 4, oy, 4, 4, nx + 4, ny);
        flip(bmp, ox + 8, oy, 4, 4, nx + 8, ny);
        flip(bmp, ox, oy + 4, 4, 12, nx + 8, ny + 4);
        flip(bmp, ox + 4, oy + 4, 4, 12, nx + 4, ny + 4);
        flip(bmp, ox + 8, oy + 4, 4, 12, nx, ny + 4);
        flip(bmp, ox + 12, oy + 4, 4, 12, nx + 12, ny + 4);
    }

    private static void flip(Bitmap bmp, int sx, int sy, int w, int h, int dx, int dy) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bmp.setPixel(dx + (w - 1 - x), dy + y, bmp.getPixel(sx + x, sy + y));
            }
        }
    }

    /** Draws every cosmetic overlay (64x64 PNGs, alpha-blended) on top of the base skin. */
    public static Bitmap withCosmetics(Context c, Bitmap base, List<Cosmetic> cosmetics) {
        Bitmap out = base.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(out);
        for (Cosmetic cos : cosmetics) {
            if (cos.type != Cosmetic.Type.OVERLAY) continue;
            Bitmap ov = CosmeticCatalog.loadBitmap(c, cos);
            if (ov != null) cv.drawBitmap(ov, 0, 0, null);
        }
        return out;
    }

    public static Bitmap capeOf(Context c, List<Cosmetic> cosmetics) {
        for (Cosmetic cos : cosmetics) {
            if (cos.type == Cosmetic.Type.CAPE) {
                Bitmap b = CosmeticCatalog.loadBitmap(c, cos);
                if (b != null) return b;
            }
        }
        return null;
    }

    /** Front-facing preview, scaled with nearest-neighbour. */
    public static Bitmap sprite(Bitmap skin, boolean slim, int scale) {
        Bitmap raw = Bitmap.createBitmap(16, 32, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(raw);
        int aw = slim ? 3 : 4;
        int rightArmX = slim ? 1 : 0;
        int leftArmX = slim ? 12 : 12;
        part(cv, skin, 4, 20, 4, 12, 4, 20);
        part(cv, skin, 20, 52, 4, 12, 8, 20);
        part(cv, skin, 4, 36, 4, 12, 4, 20);
        part(cv, skin, 4, 52, 4, 12, 8, 20);
        part(cv, skin, 20, 20, 8, 12, 4, 8);
        part(cv, skin, 20, 36, 8, 12, 4, 8);
        part(cv, skin, 44, 20, aw, 12, rightArmX, 8);
        part(cv, skin, 44, 36, aw, 12, rightArmX, 8);
        part(cv, skin, 36, 52, aw, 12, leftArmX, 8);
        part(cv, skin, 52, 52, aw, 12, leftArmX, 8);
        part(cv, skin, 8, 8, 8, 8, 4, 0);
        part(cv, skin, 40, 8, 8, 8, 4, 0);
        return Bitmap.createScaledBitmap(raw, 16 * scale, 32 * scale, false);
    }

    private static void part(Canvas cv, Bitmap skin, int sx, int sy, int w, int h, int dx, int dy) {
        Paint p = new Paint();
        p.setFilterBitmap(false);
        cv.drawBitmap(skin, new Rect(sx, sy, sx + w, sy + h), new Rect(dx, dy, dx + w, dy + h), p);
    }

    public static List<File> skinPackDirs(Context c, GameVersion v) {
        List<File> out = new ArrayList<>();
        if (v == null) return out;
        FeatureSettings.StorageType[] types = {FeatureSettings.StorageType.INTERNAL, FeatureSettings.StorageType.EXTERNAL};
        for (FeatureSettings.StorageType t : types) {
            try {
                FeatureSettings.StorageType n = LauncherStorage.normalizeContentStorageType(t, v.versionIsolation);
                File data = LauncherStorage.getContentGameDataDir(c, v.getStorageProfileId(), n);
                if (data == null) continue;
                File dir = new File(new File(data, "skin_packs"), PACK_DIR);
                if (!out.contains(dir)) out.add(dir);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    /** Writes (or rewrites) the Oblivion skin pack so the skin shows up in Minecraft's Dressing Room. */
    public static int writePack(Context c, GameVersion version, String skinName, Bitmap skin, boolean slim, Bitmap cape) {
        int written = 0;
        int counter = prefs(c).getInt(KEY_PACK_COUNTER, 0) + 1;
        prefs(c).edit().putInt(KEY_PACK_COUNTER, counter).apply();
        String name = skinName == null || skinName.trim().isEmpty() ? "Oblivion Skin" : skinName.trim();
        for (File dir : skinPackDirs(c, version)) {
            try {
                if (!dir.isDirectory() && !dir.mkdirs()) continue;
                new File(dir, "texts").mkdirs();
                try (FileOutputStream o = new FileOutputStream(new File(dir, "skin.png"))) {
                    skin.compress(Bitmap.CompressFormat.PNG, 100, o);
                }
                File capeFile = new File(dir, "cape.png");
                if (cape != null) {
                    try (FileOutputStream o = new FileOutputStream(capeFile)) {
                        cape.compress(Bitmap.CompressFormat.PNG, 100, o);
                    }
                } else if (capeFile.exists()) {
                    capeFile.delete();
                }
                JSONObject manifest = new JSONObject();
                manifest.put("format_version", 1);
                manifest.put("header", new JSONObject()
                        .put("name", "Oblivion Skins")
                        .put("uuid", "7b1f6a4e-9c52-4f0a-8b7e-3d6a2c9e1f10")
                        .put("version", new JSONArray().put(1).put(0).put(counter)));
                manifest.put("modules", new JSONArray().put(new JSONObject()
                        .put("type", "skin_pack")
                        .put("uuid", "c3d7e1a9-5b24-4e6d-9a18-0f2b7c8d4e55")
                        .put("version", new JSONArray().put(1).put(0).put(counter))));
                write(new File(dir, "manifest.json"), manifest.toString(2));
                JSONObject entry = new JSONObject()
                        .put("localization_name", "oblivion_skin")
                        .put("geometry", slim ? "geometry.humanoid.customSlim" : "geometry.humanoid.custom")
                        .put("texture", "skin.png")
                        .put("type", "free");
                if (cape != null) entry.put("cape", "cape.png");
                JSONObject skins = new JSONObject()
                        .put("skins", new JSONArray().put(entry))
                        .put("serialize_name", "Oblivion")
                        .put("localization_name", "Oblivion");
                write(new File(dir, "skins.json"), skins.toString(2));
                write(new File(dir, "texts/languages.json"), "[\"en_US\"]");
                write(new File(dir, "texts/en_US.lang"),
                        "skinpack.Oblivion=Oblivion Skins\nskin.Oblivion.oblivion_skin=" + name.replace("\n", " ") + "\n");
                written++;
            } catch (Exception ignored) {
            }
        }
        return written;
    }

    private static void write(File f, String text) throws IOException {
        try (FileOutputStream o = new FileOutputStream(f)) {
            o.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
