package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONObject;

/** Visual tokens for Oblivion screens (dark gothic masquerade look). An OblivionDLC theme can override them. */
public final class OblivionTheme {
    private static final String PREFS = "oblivion_theme";
    private static final String KEY_DLC_JSON = "dlc_theme_json";

    public static int bg = 0xFF09060F;
    public static int surface = 0xFF141020;
    public static int surfaceAlt = 0xFF1D1630;
    public static int stroke = 0xFF3A2A5C;
    public static int primary = 0xFF8E24FF;
    public static int accent = 0xFFC2185B;
    public static int gold = 0xFFD4AF37;
    public static int text = 0xFFF3EEFF;
    public static int textDim = 0xFFA79BC2;
    public static String dlcName = null;

    private static boolean loaded;

    private OblivionTheme() {
    }

    public static synchronized void ensureLoaded(Context c) {
        if (loaded) return;
        loaded = true;
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY_DLC_JSON, null);
        if (json != null) applyJson(json);
    }

    public static synchronized boolean installDlcTheme(Context c, String json) {
        if (!applyJson(json)) return false;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DLC_JSON, json).apply();
        return true;
    }

    public static synchronized void resetToDefault(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_DLC_JSON).apply();
        bg = 0xFF09060F;
        surface = 0xFF141020;
        surfaceAlt = 0xFF1D1630;
        stroke = 0xFF3A2A5C;
        primary = 0xFF8E24FF;
        accent = 0xFFC2185B;
        gold = 0xFFD4AF37;
        text = 0xFFF3EEFF;
        textDim = 0xFFA79BC2;
        dlcName = null;
    }

    private static boolean applyJson(String json) {
        try {
            JSONObject o = new JSONObject(json);
            bg = color(o, "bg", bg);
            surface = color(o, "surface", surface);
            surfaceAlt = color(o, "surfaceAlt", surfaceAlt);
            stroke = color(o, "stroke", stroke);
            primary = color(o, "primary", primary);
            accent = color(o, "accent", accent);
            gold = color(o, "gold", gold);
            text = color(o, "text", text);
            textDim = color(o, "textDim", textDim);
            dlcName = o.optString("name", null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static int color(JSONObject o, String key, int fallback) {
        String s = o.optString(key, null);
        if (s == null || s.isEmpty()) return fallback;
        try {
            return Color.parseColor(s);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
