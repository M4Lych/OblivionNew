package org.levimc.launcher.core.worldgen;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class OblivionWorldGen {
    private static final String PREFS = "oblivion_worldgen";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_NETHER = "nether";
    private static final String KEY_END = "end";
    private static final String KEY_HEIGHT = "height_index";
    private static final String KEY_STYLE = "style_index";
    private static final String KEY_RIVER = "river_index";
    private static final String KEY_PROBE = "probe";

    public static final int[] HEIGHTS = {320, 512, 768, 1000};
    public static final double[] STYLE_SMOOTHNESS = {0.75, 0.95, 0.40};
    public static final double[] RIVER_WIDTH = {1.0, 1.6, 0.5};

    private static boolean loaded;
    private static boolean loadFailed;
    private static boolean configured;

    private OblivionWorldGen() {
    }

    private static native boolean nativeConfigure(String text);

    private static native String nativeVersion();

    private static native double nativeSurfaceHeight(double x, double z);

    private static native boolean nativeProbeStart();

    private static native String nativeProbeStatus();

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static synchronized boolean ensureLoaded() {
        if (loaded) return true;
        if (loadFailed) return false;
        try {
            System.loadLibrary("oblivion_worldgen");
            loaded = true;
        } catch (Throwable t) {
            loadFailed = true;
        }
        return loaded;
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply();
        apply(context);
    }

    public static boolean isProbeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_PROBE, false);
    }

    public static void setProbeEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_PROBE, value).apply();
        apply(context);
    }

    public static String probeStatus() {
        if (!ensureLoaded()) return "unavailable";
        try {
            return nativeProbeStatus();
        } catch (Throwable t) {
            return "unavailable";
        }
    }

    public static boolean isNether(Context context) {
        return prefs(context).getBoolean(KEY_NETHER, true);
    }

    public static void setNether(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_NETHER, value).apply();
        apply(context);
    }

    public static boolean isEnd(Context context) {
        return prefs(context).getBoolean(KEY_END, true);
    }

    public static void setEnd(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_END, value).apply();
        apply(context);
    }

    public static int heightIndex(Context context) {
        return clampIndex(prefs(context).getInt(KEY_HEIGHT, 3), HEIGHTS.length);
    }

    public static int styleIndex(Context context) {
        return clampIndex(prefs(context).getInt(KEY_STYLE, 0), STYLE_SMOOTHNESS.length);
    }

    public static int riverIndex(Context context) {
        return clampIndex(prefs(context).getInt(KEY_RIVER, 0), RIVER_WIDTH.length);
    }

    public static int cycleHeight(Context context) {
        int next = (heightIndex(context) + 1) % HEIGHTS.length;
        prefs(context).edit().putInt(KEY_HEIGHT, next).apply();
        apply(context);
        return next;
    }

    public static int cycleStyle(Context context) {
        int next = (styleIndex(context) + 1) % STYLE_SMOOTHNESS.length;
        prefs(context).edit().putInt(KEY_STYLE, next).apply();
        apply(context);
        return next;
    }

    public static int cycleRiver(Context context) {
        int next = (riverIndex(context) + 1) % RIVER_WIDTH.length;
        prefs(context).edit().putInt(KEY_RIVER, next).apply();
        apply(context);
        return next;
    }

    private static int clampIndex(int value, int size) {
        return value < 0 || value >= size ? 0 : value;
    }

    public static String buildConfig(Context context) {
        return String.format(Locale.US,
                "overworld=%d;nether=%d;end=%d;maxY=%d;smoothness=%.2f;riverWidth=%.2f",
                isEnabled(context) ? 1 : 0,
                isNether(context) ? 1 : 0,
                isEnd(context) ? 1 : 0,
                HEIGHTS[heightIndex(context)],
                STYLE_SMOOTHNESS[styleIndex(context)],
                RIVER_WIDTH[riverIndex(context)]);
    }

    public static synchronized void apply(Context context) {
        if (!ensureLoaded()) return;
        try {
            configured = nativeConfigure(buildConfig(context));
            if (isProbeEnabled(context)) nativeProbeStart();
        } catch (Throwable t) {
            configured = false;
        }
    }

    public static boolean isLibraryAvailable() {
        return ensureLoaded();
    }

    public static String versionLabel() {
        if (!ensureLoaded()) return "unavailable";
        try {
            return nativeVersion();
        } catch (Throwable t) {
            return "unavailable";
        }
    }

    public static boolean isConfigured() {
        return configured;
    }

    public static double sampleSurfaceHeight(double x, double z) {
        if (!ensureLoaded() || !configured) return Double.NaN;
        try {
            return nativeSurfaceHeight(x, z);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }
}
