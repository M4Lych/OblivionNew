package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONObject;

/**
 * Visual tokens for Oblivion screens. Two built-in presets (Movie = bright, playful; Mujica = dark gothic)
 * and an OblivionDLC theme can override any token.
 */
public final class OblivionTheme {
    public static final String PRESET_MOVIE = "movie";
    public static final String PRESET_MUJICA = "mujica";

    private static final String PREFS = "oblivion_theme";
    private static final String KEY_DLC_JSON = "dlc_theme_json";
    private static final String KEY_PRESET = "preset";

    public static int bg, surface, surfaceAlt, stroke, primary, accent, gold, text, textDim;
    public static int glass, glassStroke, launch, launchLight, info, danger;
    public static String dlcName = null;

    private static boolean loaded;

    static {
        applyPreset(PRESET_MOVIE);
    }

    private OblivionTheme() {
    }

    private static void applyPreset(String preset) {
        if (PRESET_MUJICA.equals(preset)) {
            bg = 0xFF09060F; surface = 0xFF141020; surfaceAlt = 0xFF1D1630; stroke = 0xFF3A2A5C;
            primary = 0xFF8E24FF; accent = 0xFFC2185B; gold = 0xFFD4AF37; text = 0xFFF3EEFF; textDim = 0xFFA79BC2;
            glass = 0xCC140E22; glassStroke = 0x663A2A5C; launch = 0xFFC2185B; launchLight = 0xFFFF6FA3;
            info = 0xFF4A2A9A; danger = 0xFFE53935;
        } else {
            bg = 0xFF14161A; surface = 0xFF23262B; surfaceAlt = 0xFF2E3238; stroke = 0xFF444A52;
            primary = 0xFF1FD77F; accent = 0xFF2F63C7; gold = 0xFFFFC857; text = 0xFFFFFFFF; textDim = 0xFFC3C8D0;
            glass = 0xB3262626; glassStroke = 0x55FFFFFF; launch = 0xFF1FD77F; launchLight = 0xFF6CFFB0;
            info = 0xFF2F63C7; danger = 0xFFD9534F;
        }
        dlcName = null;
    }

    public static synchronized void ensureLoaded(Context c) {
        if (loaded) return;
        loaded = true;
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        applyPreset(p.getString(KEY_PRESET, PRESET_MOVIE));
        String json = p.getString(KEY_DLC_JSON, null);
        if (json != null) applyJson(json);
    }

    public static String preset(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PRESET, PRESET_MOVIE);
    }

    public static synchronized void setPreset(Context c, String preset) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PRESET, preset).apply();
        applyPreset(preset);
        String json = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DLC_JSON, null);
        if (json != null) applyJson(json);
    }

    public static synchronized boolean installDlcTheme(Context c, String json) {
        if (!applyJson(json)) return false;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DLC_JSON, json).apply();
        return true;
    }

    public static synchronized void resetToDefault(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_DLC_JSON).apply();
        applyPreset(preset(c));
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
            glass = color(o, "glass", glass);
            glassStroke = color(o, "glassStroke", glassStroke);
            launch = color(o, "launch", launch);
            launchLight = color(o, "launchLight", launchLight);
            info = color(o, "info", info);
            danger = color(o, "danger", danger);
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
