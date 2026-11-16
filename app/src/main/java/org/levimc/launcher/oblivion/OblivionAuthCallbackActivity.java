package org.levimc.launcher.oblivion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.levimc.launcher.R;

/** Receives oblivion://auth?token=... after the Discord login finished in the browser. */
public class OblivionAuthCallbackActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        OblivionSession.handleCallback(this, getIntent().getData(), (account, error) -> {
            if (error != null) {
                Toast.makeText(this, getString(R.string.oblivion_login_failed, error), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.oblivion_login_ok, account.username), Toast.LENGTH_SHORT).show();
            }
            Intent hub = new Intent(this, OblivionHubActivity.class);
            hub.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(hub);
            finish();
        });
    }
}
