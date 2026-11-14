package com.oblivion.client.hosted;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** Loads the installed game's Activity class instead of compiling a copied game Activity. */
public final class OblivionHostedAppComponentFactory extends AppComponentFactory {
    private static final String TAG = "OblivionHosted";

    @Override
    public Activity instantiateActivity(ClassLoader hostClassLoader, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (!OblivionHostedProcess.MINECRAFT_ACTIVITY.equals(className)) {
            return super.instantiateActivity(hostClassLoader, className, intent);
        }

        try {
            ClassLoader gameLoader = OblivionHostedProcess.requireMinecraftClassLoader();
            Thread.currentThread().setContextClassLoader(gameLoader);
            Class<?> type = Class.forName(className, true, gameLoader);
            if (!Activity.class.isAssignableFrom(type)) {
                throw new InstantiationException(className + " does not extend Activity");
            }
            @SuppressWarnings("unchecked")
            Constructor<? extends Activity> constructor =
                    ((Class<? extends Activity>) type).getDeclaredConstructor();
            constructor.setAccessible(true);
            Activity activity = constructor.newInstance();
            Log.i(TAG, "Created installed Minecraft Activity from game class loader");
            return activity;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException error) {
            throw error;
        } catch (InvocationTargetException error) {
            throw asInstantiationException(error.getCause());
        } catch (Throwable error) {
            throw asInstantiationException(error);
        }
    }

    private static InstantiationException asInstantiationException(Throwable cause) {
        InstantiationException exception = new InstantiationException(
                "Could not instantiate installed Minecraft MainActivity: " + cause);
        exception.initCause(cause);
        return exception;
    }
}
