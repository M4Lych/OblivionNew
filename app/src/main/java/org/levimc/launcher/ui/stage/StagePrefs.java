package org.levimc.launcher.ui.stage;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/** Persistent, per-model Character Stage settings. */
public final class StagePrefs {
    private static final String PREFS = "oblivion_stage";
    private static final String REVISION_PREFIX = "model_revision:";

    public static final class Settings {
        public final float cameraX;
        public final float cameraY;
        public final float cameraDistance;
        public final float cameraFov;
        public final float gravityScale;
        public final float stiffness;
        public final float damping;

        Settings(float cameraX, float cameraY, float cameraDistance, float cameraFov,
                 float gravityScale, float stiffness, float damping) {
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraDistance = cameraDistance;
            this.cameraFov = cameraFov;
            this.gravityScale = gravityScale;
            this.stiffness = stiffness;
            this.damping = damping;
        }
    }

    private StagePrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String prefix(String modelId) {
        String safe = modelId == null ? "default" : modelId.replaceAll("[^A-Za-z0-9_.-]", "_");
        return "model:" + safe + ":";
    }

    private static void bumpRevision(SharedPreferences preferences, String modelId,
                                     SharedPreferences.Editor editor) {
        String key = REVISION_PREFIX + (modelId == null ? "default" : modelId);
        editor.putInt(key, preferences.getInt(key, 0) + 1);
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
        return prefs(context).getInt("fps", 30) < 30 ? 24 : 30;
    }

    public static void setFps(Context context, int fps) {
        prefs(context).edit().putInt("fps", fps >= 30 ? 30 : 24).apply();
    }

    public static boolean isGlass(Context context) {
        return prefs(context).getBoolean("glass", true);
    }

    public static void setGlass(Context context, boolean value) {
        prefs(context).edit().putBoolean("glass", value).apply();
    }

    public static int revision(Context context, String modelId) {
        return prefs(context).getInt(REVISION_PREFIX + (modelId == null ? "default" : modelId), 0);
    }

    /** Read overrides for one model, falling back to its model.json values. */
    public static Settings settings(Context context, String modelId) {
        ModelPack defaults;
        try {
            defaults = ModelPack.open(context.getApplicationContext(), modelId == null ? "default" : modelId);
        } catch (Exception ignored) {
            defaults = new ModelPack();
        }
        SharedPreferences p = prefs(context);
        String key = prefix(modelId);
        return new Settings(
                p.getFloat(key + "camera_x", defaults.offsetX),
                p.getFloat(key + "camera_y", defaults.targetY),
                p.getFloat(key + "camera_distance", defaults.distance),
                p.getFloat(key + "camera_fov", defaults.fov),
                p.getFloat(key + "gravity", defaults.gravityScale),
                p.getFloat(key + "stiffness", defaults.springStiffness),
                p.getFloat(key + "damping", defaults.springDamping));
    }

    /** Apply persisted settings before the model renderer and animator are created. */
    static void apply(Context context, ModelPack pack) {
        Settings s = settings(context, pack.id);
        pack.offsetX = clamp(s.cameraX, -0.75f, 0.75f);
        pack.targetY = clamp(s.cameraY, 0.5f, 3.0f);
        pack.distance = clamp(s.cameraDistance, 1.0f, 6.0f);
        pack.fov = clamp(s.cameraFov, 10.0f, 32.0f);
        pack.gravityScale = clamp(s.gravityScale, 0.0f, 2.5f);
        pack.springStiffness = clamp(s.stiffness, 5.0f, 50.0f);
        pack.springDamping = clamp(s.damping, 1.0f, 20.0f);
    }

    public static void setCameraX(Context context, String modelId, float value) {
        saveModelFloat(context, modelId, "camera_x", clamp(value, -0.75f, 0.75f));
    }

    public static void setCameraY(Context context, String modelId, float value) {
        saveModelFloat(context, modelId, "camera_y", clamp(value, 0.5f, 3.0f));
    }

    public static void setCameraDistance(Context context, String modelId, float value) {
        saveModelFloat(context, modelId, "camera_distance", clamp(value, 1.0f, 6.0f));
    }

    public static void setCameraFov(Context context, String modelId, float value) {
        saveModelFloat(context, modelId, "camera_fov", clamp(value, 10.0f, 32.0f));
    }

    public static void setGravityScale(Context context, String modelId, float value) {
        saveModelFloat(context, modelId, "gravity", clamp(value, 0.0f, 2.5f));
    }

    public static void setSpringStiffness(Context context, String modelId, float value) {
        saveModelFloat(context, modelId, "stiffness", clamp(value, 5.0f, 50.0f));
    }

    public static void setSpringDamping(Context context, String modelId, float value) {
        saveModelFloat(context, modelId, "damping", clamp(value, 1.0f, 20.0f));
    }

    private static void saveModelFloat(Context context, String modelId, String setting, float value) {
        SharedPreferences p = prefs(context);
        SharedPreferences.Editor edit = p.edit().putFloat(prefix(modelId) + setting, value);
        bumpRevision(p, modelId, edit);
        edit.apply();
    }

    /** Remove only this model's overrides; model.json becomes active again. */
    public static void resetModel(Context context, String modelId) {
        SharedPreferences p = prefs(context);
        String prefix = prefix(modelId);
        SharedPreferences.Editor edit = p.edit();
        for (Map.Entry<String, ?> entry : p.getAll().entrySet()) {
            if (entry.getKey().startsWith(prefix)) edit.remove(entry.getKey());
        }
        bumpRevision(p, modelId, edit);
        edit.apply();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
