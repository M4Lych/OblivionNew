package org.levimc.launcher.ui.stage;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModelPack {
    public interface Source {
        InputStream open(String path) throws IOException;
    }

    public static final class Info {
        public final String id;
        public final String name;

        Info(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static final class Material {
        String name;
        String diffuse;
        String shade;
        String emissive;
        boolean alpha;
        boolean outline;
    }

    static final String ASSET_ROOT = "oblivion/models";

    String id;
    String name;
    String meshFile = "model.obm";
    Source source;
    Material[] materials = new Material[0];
    float fov = 28f;
    float targetY = 1.05f;
    float distance = 4.2f;
    float offsetX = 0f;
    float breath = 1f;
    float sway = 1f;
    float springs = 1f;
    float lookAt = 1f;
    boolean blink = true;
    String hipBone = "Hip";
    String neckBone = "Neck";
    String headBone = "Head";
    String springPrefix = "Sp_";
    String[] spineBones = new String[0];
    String blinkMorph = "blink";
    String smileMorph = "smile";

    private static final class AssetSource implements Source {
        private final AssetManager assets;
        private final String base;

        AssetSource(AssetManager assets, String base) {
            this.assets = assets;
            this.base = base;
        }

        @Override
        public InputStream open(String path) throws IOException {
            return assets.open(base + "/" + path);
        }
    }

    private static final class FileSource implements Source {
        private final File dir;

        FileSource(File dir) {
            this.dir = dir;
        }

        @Override
        public InputStream open(String path) throws IOException {
            File f = new File(dir, path);
            if (!f.getCanonicalPath().startsWith(dir.getCanonicalPath())) throw new IOException("path escapes pack");
            return new FileInputStream(f);
        }
    }

    private static File[] roots(Context context) {
        File external = context.getExternalFilesDir(null);
        List<File> out = new ArrayList<>();
        if (external != null) out.add(new File(external, ASSET_ROOT));
        out.add(new File(context.getFilesDir(), ASSET_ROOT));
        return out.toArray(new File[0]);
    }

    private static String str(JSONObject o, String key) {
        return o == null || o.isNull(key) ? null : o.optString(key, null);
    }

    static ModelPack load(Source source, String fallbackId) throws IOException, JSONException {
        JSONObject j = new JSONObject(new String(ObmModel.readAll(source.open("model.json")), StandardCharsets.UTF_8));
        ModelPack p = new ModelPack();
        p.source = source;
        p.id = j.optString("id", fallbackId);
        p.name = j.optString("name", p.id);
        p.meshFile = j.optString("mesh", "model.obm");
        JSONArray mats = j.optJSONArray("materials");
        if (mats != null) {
            p.materials = new Material[mats.length()];
            for (int i = 0; i < mats.length(); i++) {
                JSONObject mo = mats.getJSONObject(i);
                Material m = new Material();
                m.name = mo.optString("name", "m" + i);
                m.diffuse = str(mo, "diffuse");
                m.shade = str(mo, "shade");
                m.emissive = str(mo, "emissive");
                m.alpha = mo.optBoolean("alpha", false);
                m.outline = mo.optBoolean("outline", true);
                p.materials[i] = m;
            }
        }
        JSONObject cam = j.optJSONObject("camera");
        if (cam != null) {
            p.fov = (float) cam.optDouble("fov", p.fov);
            p.targetY = (float) cam.optDouble("targetY", p.targetY);
            p.distance = (float) cam.optDouble("distance", p.distance);
            p.offsetX = (float) cam.optDouble("offsetX", p.offsetX);
        }
        JSONObject an = j.optJSONObject("animation");
        if (an != null) {
            p.breath = (float) an.optDouble("breath", 1.0);
            p.sway = (float) an.optDouble("sway", 1.0);
            p.springs = (float) an.optDouble("springs", 1.0);
            p.lookAt = (float) an.optDouble("lookAt", 1.0);
            p.blink = an.optBoolean("blink", true);
        }
        JSONObject bones = j.optJSONObject("bones");
        if (bones != null) {
            p.hipBone = bones.optString("hip", p.hipBone);
            p.neckBone = bones.optString("neck", p.neckBone);
            p.headBone = bones.optString("head", p.headBone);
            p.springPrefix = bones.optString("springPrefix", p.springPrefix);
            JSONArray spine = bones.optJSONArray("spine");
            if (spine != null) {
                p.spineBones = new String[spine.length()];
                for (int i = 0; i < spine.length(); i++) p.spineBones[i] = spine.getString(i);
            }
        }
        JSONObject morphs = j.optJSONObject("morphs");
        if (morphs != null) {
            p.blinkMorph = morphs.optString("blink", p.blinkMorph);
            p.smileMorph = morphs.optString("smile", p.smileMorph);
        }
        return p;
    }

    public static List<Info> discover(Context context) {
        List<Info> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (File root : roots(context)) {
            File[] dirs = root.listFiles();
            if (dirs == null) continue;
            for (File d : dirs) {
                if (!d.isDirectory() || seen.contains(d.getName()) || !new File(d, "model.json").isFile()) continue;
                try {
                    ModelPack p = load(new FileSource(d), d.getName());
                    out.add(new Info(d.getName(), p.name));
                    seen.add(d.getName());
                } catch (Exception ignored) {
                }
            }
        }
        try {
            String[] ids = context.getAssets().list(ASSET_ROOT);
            if (ids != null) {
                for (String id : ids) {
                    if (seen.contains(id)) continue;
                    try {
                        ModelPack p = load(new AssetSource(context.getAssets(), ASSET_ROOT + "/" + id), id);
                        out.add(new Info(id, p.name));
                        seen.add(id);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    static ModelPack open(Context context, String id) throws IOException, JSONException {
        for (File root : roots(context)) {
            File dir = new File(root, id);
            if (new File(dir, "model.json").isFile()) return load(new FileSource(dir), id);
        }
        return load(new AssetSource(context.getAssets(), ASSET_ROOT + "/" + id), id);
    }
}
