package org.levimc.launcher.ui.stage;

import android.content.Context;
import android.content.SharedPreferences;

public final class StagePrefs {
    private static final String PREFS = "oblivion_stage";

    private StagePrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean("enabled", true);
    }

    public static void setEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("enabled", value).apply();
    }

    public static String packId(Context context) {
        return prefs(context).getString("pack", "default");
    }

    public static void setPackId(Context context, String id) {
        prefs(context).edit().putString("pack", id).apply();
    }

    public static int fps(Context context) {
        return prefs(context).getInt("fps", 30) >= 60 ? 60 : 30;
    }

    public static void setFps(Context context, int fps) {
        prefs(context).edit().putInt("fps", fps >= 60 ? 60 : 30).apply();
    }

    public static boolean isGlass(Context context) {
        return prefs(context).getBoolean("glass", true);
    }

    public static void setGlass(Context context, boolean value) {
        prefs(context).edit().putBoolean("glass", value).apply();
    }
}
