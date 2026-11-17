package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.ModsFullscreenActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * OblivionMod: native mods (the Levi mod loader) plus OblivionDLC packages that restyle the client.
 * DLC states: In use, Installed, Owned (shop, not installed yet) and Locked.
 */
public class OblivionModActivity extends OblivionBaseActivity {
    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) installFrom(uri);
            });
    private JSONArray shop;

    @Override
    protected int titleRes() {
        return R.string.oblivion_mod_title;
    }

    @Override
    protected void buildContent(LinearLayout col) {
        col.addView(OUi.tile(this, getString(R.string.oblivion_res_native_mods),
                getString(R.string.oblivion_res_native_mods_sub),
                v -> startActivity(new Intent(this, ModsFullscreenActivity.class))));

        LinearLayout card = OUi.card(this);
        card.addView(OUi.title(this, "OblivionDLC"));
        card.addView(OUi.body(this, getString(R.string.oblivion_dlc_desc)));
        List<OblivionDlc.Info> installed = OblivionDlc.list(this);
        if (installed.isEmpty()) card.addView(OUi.body(this, getString(R.string.oblivion_dlc_none)));
        for (OblivionDlc.Info i : installed) {
            LinearLayout row = OUi.card(this);
            row.addView(OUi.title(this, i.name));
            row.addView(OUi.body(this, i.version + (i.author.isEmpty() ? "" : "  ·  " + i.author)));
            TextView state = OUi.body(this, getString(i.active ? R.string.oblivion_dlc_state_active : R.string.oblivion_dlc_state_installed));
            state.setTextColor(i.active ? OblivionTheme.launchLight : OblivionTheme.textDim);
            row.addView(state);
            row.addView(OUi.button(this, getString(i.active ? R.string.oblivion_dlc_stop : R.string.oblivion_dlc_use),
                    !i.active, v -> {
                        OblivionDlc.setActive(this, i.active ? "" : i.id);
                        rebuild();
                        org.levimc.launcher.oblivion.OblivionHomeView.refreshInstance();
                        Toast.makeText(this, R.string.oblivion_dlc_restart, Toast.LENGTH_SHORT).show();
                    }));
            row.addView(OUi.button(this, getString(R.string.oblivion_dlc_remove), false, v -> {
                OblivionDlc.remove(this, i.id);
                rebuild();
            }));
            card.addView(row);
        }
        card.addView(OUi.button(this, getString(R.string.oblivion_dlc_install), true, v -> picker.launch("*/*")));
        col.addView(card);

        LinearLayout store = OUi.card(this);
        store.addView(OUi.title(this, getString(R.string.oblivion_shop)));
        if (!OblivionConfig.isServerConfigured(this) || !OblivionSession.isLinked(this)) {
            store.addView(OUi.body(this, getString(R.string.oblivion_shop_needs_server)));
        } else if (shop == null) {
            store.addView(OUi.button(this, getString(R.string.oblivion_shop_load), true, v -> loadShop()));
        } else {
            OblivionSession.Account acc = OblivionSession.current(this);
            for (int k = 0; k < shop.length(); k++) {
                JSONObject o = shop.optJSONObject(k);
                if (o == null) continue;
                String pkg = o.optString("package_id");
                boolean owned = acc != null && acc.owns("pkg:" + pkg);
                LinearLayout row = OUi.card(this);
                row.addView(OUi.title(this, o.optString("name", pkg)));
                row.addView(OUi.body(this, o.optString("description", "")));
                TextView state = OUi.body(this, getString(owned ? R.string.oblivion_dlc_state_owned : R.string.oblivion_dlc_state_locked));
                state.setTextColor(owned ? OblivionTheme.launchLight : OblivionTheme.gold);
                row.addView(state);
                if (owned) {
                    row.addView(OUi.button(this, getString(R.string.oblivion_dlc_get), true, v -> download(pkg)));
                }
                store.addView(row);
            }
        }
        col.addView(store);
    }

    private void loadShop() {
        OblivionApi.get(this, "/v1/dlc", OblivionSession.token(this), new OblivionApi.JsonCb() {
            @Override
            public void ok(JSONObject json) {
                shop = json.optJSONArray("items");
                if (shop == null) shop = new JSONArray();
                rebuild();
            }

            @Override
            public void fail(String message) {
                Toast.makeText(OblivionModActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void download(String pkg) {
        OblivionApi.download(this, "/v1/packages/" + Uri.encode(pkg), OblivionSession.token(this),
                new OblivionApi.BytesCb() {
                    @Override
                    public void ok(byte[] data) {
                        finishInstall(OblivionDlc.install(OblivionModActivity.this, data));
                    }

                    @Override
                    public void fail(String message) {
                        Toast.makeText(OblivionModActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void installFrom(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            finishInstall(OblivionDlc.install(this, bos.toByteArray()));
        } catch (Exception e) {
            Toast.makeText(this, String.valueOf(e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void finishInstall(String error) {
        if (error != null) {
            Toast.makeText(this, getString(R.string.oblivion_dlc_invalid) + " (" + error + ")", Toast.LENGTH_LONG).show();
            return;
        }
        Achievements.increment(this, Achievements.DLC);
        Toast.makeText(this, R.string.oblivion_dlc_installed, Toast.LENGTH_SHORT).show();
        org.levimc.launcher.oblivion.OblivionHomeView.refreshInstance();
        rebuild();
    }
}
