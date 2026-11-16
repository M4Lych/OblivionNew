package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.SharedPreferences;

/** Oblivion client settings (also read by the in-game layer). */
public final class OblivionSettings {
    public static final String DUNGEON_HOME = "dungeon_home";
    public static final String AMBIENT_ANIM = "ambient_anim";
    public static final String LOADING_VIDEO = "loading_video";
    public static final String AUTO_APPLY_SKIN = "auto_apply_skin";
    public static final String SHOW_BADGE = "show_badge";
    public static final String INGAME_WATERMARK = "ingame_watermark";

    private OblivionSettings() {
    }

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("oblivion_settings", Context.MODE_PRIVATE);
    }

    public static boolean get(Context c, String key) {
        return p(c).getBoolean(key, !INGAME_WATERMARK.equals(key));
    }

    public static void set(Context c, String key, boolean value) {
        p(c).edit().putBoolean(key, value).apply();
    }
}
