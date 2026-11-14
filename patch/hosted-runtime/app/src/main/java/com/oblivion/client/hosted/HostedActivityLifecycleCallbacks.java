package com.oblivion.client.hosted;

import android.app.Activity;
import android.app.Application;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Field;

/** Adapts the Activity's base Context before Minecraft's onCreate runs (Android 10+). */
final class HostedActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "OblivionHosted";
    private final Application application;

    HostedActivityLifecycleCallbacks(Application application) {
        this.application = application;
    }

    @Override
    public void onActivityPreCreated(Activity activity, Bundle state) {
        if (!OblivionHostedProcess.isMinecraftActivity(activity)) return;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw new IllegalStateException("Activity pre-create callback requires Android 10+");
            }
            int theme = OblivionHostedProcess.getMinecraftActivityTheme(application);

            Field baseField = ContextWrapper.class.getDeclaredField("mBase");
            baseField.setAccessible(true);
            Object oldBase = baseField.get(activity);
            if (!(oldBase instanceof HostedMinecraftContext)) {
                baseField.set(activity, new HostedMinecraftContext(
                        (android.content.Context) oldBase,
                        OblivionHostedProcess.requireMinecraftContext()));
            }
            if (theme != 0) activity.setTheme(theme);
            Log.i(TAG, "Minecraft Activity Context adapted before onCreate");
        } catch (Throwable error) {
            Log.e(TAG, "Could not adapt Minecraft Activity Context", error);
            throw new IllegalStateException("Minecraft host Context adaptation failed", error);
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
