package org.levimc.launcher.oblivion;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A player model made of textured boxes using Minecraft's box UV layout. The default is the classic/slim
 * humanoid; an OblivionDLC package can provide its own model.json (and optionally its own texture).
 * Coordinates are in pixels, y up, origin at the feet, the model faces +z (towards the viewer).
 */
public final class PlayerModel {
    public static final class Box {
        public String name = "";
        public String group = "";
        public float x, y, z, w, h, d;
        public int u, v;
        public float inflate;
        public boolean cape;
    }

    public String name = "Default";
    public final List<Box> boxes = new ArrayList<>();
    /** Optional own texture (model.png from a DLC); null = use the player's skin. */
    public Bitmap texture;

    private static Box box(String name, String group, float x, float y, float z, float w, float h, float d,
                           int u, int v, float inflate) {
        Box b = new Box();
        b.name = name;
        b.group = group;
        b.x = x; b.y = y; b.z = z; b.w = w; b.h = h; b.d = d;
        b.u = u; b.v = v;
        b.inflate = inflate;
        return b;
    }

    public static PlayerModel humanoid(boolean slim, boolean withCape) {
        PlayerModel m = new PlayerModel();
        m.name = slim ? "Slim" : "Classic";
        float aw = slim ? 3 : 4;
        float rax = slim ? -7 : -8;
        if (withCape) {
            Box c = box("cape", "cape", -5, 8, -3.1f, 10, 16, 1, 0, 0, 0);
            c.cape = true;
            m.boxes.add(c);
        }
        m.boxes.add(box("rleg", "rleg", -4, 0, -2, 4, 12, 4, 0, 16, 0));
        m.boxes.add(box("lleg", "lleg", 0, 0, -2, 4, 12, 4, 16, 48, 0));
        m.boxes.add(box("body", "body", -4, 12, -2, 8, 12, 4, 16, 16, 0));
        m.boxes.add(box("rarm", "rarm", rax, 12, -2, aw, 12, 4, 40, 16, 0));
        m.boxes.add(box("larm", "larm", 4, 12, -2, aw, 12, 4, 32, 48, 0));
        m.boxes.add(box("head", "head", -4, 24, -4, 8, 8, 8, 0, 0, 0));
        m.boxes.add(box("rleg2", "rleg", -4, 0, -2, 4, 12, 4, 0, 32, 0.25f));
        m.boxes.add(box("lleg2", "lleg", 0, 0, -2, 4, 12, 4, 0, 48, 0.25f));
        m.boxes.add(box("body2", "body", -4, 12, -2, 8, 12, 4, 16, 32, 0.25f));
        m.boxes.add(box("rarm2", "rarm", rax, 12, -2, aw, 12, 4, 40, 32, 0.25f));
        m.boxes.add(box("larm2", "larm", 4, 12, -2, aw, 12, 4, 48, 48, 0.25f));
        m.boxes.add(box("head2", "head", -4, 24, -4, 8, 8, 8, 32, 0, 0.5f));
        return m;
    }

    /** Parses {"name":..,"parts":[{"name","group","origin":[x,y,z],"size":[w,h,d],"uv":[u,v],"inflate"}]}. */
    public static PlayerModel fromJson(String json) {
        try {
            JSONObject o = new JSONObject(json);
            PlayerModel m = new PlayerModel();
            m.name = o.optString("name", "Custom");
            JSONArray parts = o.getJSONArray("parts");
            for (int i = 0; i < parts.length() && i < 128; i++) {
                JSONObject p = parts.getJSONObject(i);
                JSONArray origin = p.getJSONArray("origin");
                JSONArray size = p.getJSONArray("size");
                JSONArray uv = p.optJSONArray("uv");
                Box b = box(p.optString("name", "part" + i), p.optString("group", ""),
                        (float) origin.getDouble(0), (float) origin.getDouble(1), (float) origin.getDouble(2),
                        (float) size.getDouble(0), (float) size.getDouble(1), (float) size.getDouble(2),
                        uv == null ? 0 : uv.getInt(0), uv == null ? 0 : uv.getInt(1),
                        (float) p.optDouble("inflate", 0));
                m.boxes.add(b);
            }
            return m.boxes.isEmpty() ? null : m;
        } catch (Exception e) {
            return null;
        }
    }
}
