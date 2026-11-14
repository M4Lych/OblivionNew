package com.oblivion.client.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.oblivion.client.core.minecraft.MinecraftActivity;
import com.oblivion.client.core.minecraft.MinecraftActivityState;
import com.oblivion.client.core.minecraft.MinecraftImportIntents;
import com.oblivion.client.util.MinecraftUriHandler;

public class IntentHandler extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent originalIntent) {
        Uri data = originalIntent.getData();

        if (data != null && MinecraftUriHandler.isMinecraftUri(data)) {
            handleMinecraftUri(originalIntent, data);
            return;
        }

        if (MinecraftImportIntents.isMinecraftResourceIntent(this, originalIntent)) {
            if (MinecraftImportIntents.forwardToRunningMinecraft(this, originalIntent)) {
                finish();
                return;
            }

            Intent launcherIntent = new Intent(originalIntent);
            launcherIntent.setClass(this, MainActivity.class);
            startActivity(launcherIntent);
            finish();
            return;
        }

        Intent newIntent = new Intent(originalIntent);
        newIntent.setClass(this, MainActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(newIntent);
        finish();
    }

    private void handleMinecraftUri(Intent originalIntent, Uri uri) {
        MinecraftUriHandler.MinecraftUri parsedUri = MinecraftUriHandler.parse(uri);
        Intent newIntent = new Intent(originalIntent);
        newIntent.putExtra("MINECRAFT_URI", uri.toString());
        newIntent.putExtra("MINECRAFT_URI_ACTION", parsedUri != null ? parsedUri.action : "");
        if (isMinecraftActivityRunning()) {
            newIntent.setClass(this, MinecraftActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(newIntent);
        } else {
            newIntent.setClass(this, MainActivity.class);
            newIntent.putExtra("LAUNCH_WITH_URI", true);
            startActivity(newIntent);
        }
        
        finish();
    }

    private boolean isMinecraftActivityRunning() {
        return MinecraftActivityState.isRunning(this);
    }

}
