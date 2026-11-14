package com.oblivion.client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Minimal launcher UI used to validate the independent hosted-process runtime. */
public final class MainActivity extends Activity {
    private static final String MINECRAFT_PACKAGE = "com.mojang.minecraftpe";
    private static final String MINECRAFT_ACTIVITY = "com.mojang.minecraftpe.MainActivity";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28, 28, 28, 28);

        TextView title = new TextView(this);
        title.setText("OBLIVION — HOSTED RUNTIME POC");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView status = new TextView(this);
        status.setText(getMinecraftStatus());
        status.setTextSize(14);
        status.setPadding(0, 20, 0, 20);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        Button launch = new Button(this);
        launch.setText(R.string.launch_minecraft);
        launch.setOnClickListener(view -> launchMinecraft());
        root.addView(launch, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private String getMinecraftStatus() {
        if (!isArm64()) return "Unsupported ABI: this prototype is arm64-v8a only.";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(MINECRAFT_PACKAGE, 0);
            return "Minecraft detected: " + info.versionName + "\nABI: arm64-v8a\nInstall source: " + getInstaller();
        } catch (PackageManager.NameNotFoundException error) {
            return "Minecraft is not installed. Install the official Google Play version first.";
        }
    }

    private void launchMinecraft() {
        if (!isArm64()) {
            toast("This prototype only supports arm64-v8a.");
            return;
        }
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(MINECRAFT_PACKAGE, 0);
            String installer;
            if (Build.VERSION.SDK_INT >= 30) {
                installer = getPackageManager().getInstallSourceInfo(MINECRAFT_PACKAGE)
                        .getInstallingPackageName();
            } else {
                @SuppressWarnings("deprecation")
                String legacyInstaller = getPackageManager().getInstallerPackageName(MINECRAFT_PACKAGE);
                installer = legacyInstaller;
            }
            if (!"com.android.vending".equals(installer)) {
                toast("Install-source check failed. Use the official Google Play installation.");
                return;
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(getPackageName(), MINECRAFT_ACTIVITY));
            intent.putExtra("oblivion.minecraft.nativeLibraryDir", info.nativeLibraryDir);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (PackageManager.NameNotFoundException error) {
            toast("Minecraft is not installed.");
        } catch (Throwable error) {
            toast("Launch request failed: " + error.getClass().getSimpleName());
        }
    }

    private String getInstaller() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                return String.valueOf(getPackageManager().getInstallSourceInfo(MINECRAFT_PACKAGE)
                        .getInstallingPackageName());
            }
            @SuppressWarnings("deprecation")
            String installer = getPackageManager().getInstallerPackageName(MINECRAFT_PACKAGE);
            return String.valueOf(installer);
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static boolean isArm64() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return true;
        }
        return false;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
