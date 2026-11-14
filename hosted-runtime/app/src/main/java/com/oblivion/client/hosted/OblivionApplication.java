package com.oblivion.client.hosted;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

/** Application entry point for the isolated hosted-game process. */
public final class OblivionApplication extends Application {
    private static final String TAG = "OblivionHosted";

    @Override
    public void onCreate() {
        super.onCreate();
        if (!isMinecraftProcess()) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IllegalStateException("Hosted runtime requires Android 10 or newer");
        }
        try {
            OblivionHostedProcess.initialize(this);
            registerActivityLifecycleCallbacks(new HostedActivityLifecycleCallbacks(this));
            Log.i(TAG, "Hosted process initialized");
        } catch (Throwable error) {
            Log.e(TAG, "Hosted process initialization failed", error);
            throw new IllegalStateException("Could not initialize hosted Minecraft runtime", error);
        }
    }

    private boolean isMinecraftProcess() {
        if (Build.VERSION.SDK_INT >= 28) {
            return getPackageName().concat(":minecraft").equals(Application.getProcessName());
        }
        return false;
    }
}
