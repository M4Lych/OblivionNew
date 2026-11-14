package com.oblivion.client.hosted;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;

/**
 * Exposes Minecraft's package resources and code loader while retaining Oblivion's app sandbox
 * for writable files. This avoids attempting to read Minecraft's private data directory.
 */
final class HostedMinecraftContext extends ContextWrapper {
    private final Context gameContext;
    private final ApplicationInfo hostedApplicationInfo;

    HostedMinecraftContext(Context hostBase, Context gameContext) {
        super(hostBase);
        this.gameContext = gameContext;
        ApplicationInfo copy = new ApplicationInfo(gameContext.getApplicationInfo());
        ApplicationInfo host = hostBase.getApplicationInfo();
        copy.dataDir = host.dataDir;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            copy.deviceProtectedDataDir = host.deviceProtectedDataDir;
        }
        this.hostedApplicationInfo = copy;
    }

    @Override public ClassLoader getClassLoader() { return gameContext.getClassLoader(); }
    @Override public AssetManager getAssets() { return gameContext.getAssets(); }
    @Override public Resources getResources() { return gameContext.getResources(); }
    @Override public String getPackageName() { return HostedMinecraftProcessName.PACKAGE; }
    @Override public ApplicationInfo getApplicationInfo() { return hostedApplicationInfo; }
    @Override public String getPackageCodePath() { return gameContext.getPackageCodePath(); }
    @Override public String getPackageResourcePath() { return gameContext.getPackageResourcePath(); }
    @Override public Context getApplicationContext() { return super.getApplicationContext(); }

    private static final class HostedMinecraftProcessName {
        static final String PACKAGE = OblivionHostedProcess.MINECRAFT_PACKAGE;
    }
}
