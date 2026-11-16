package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONObject;
import org.levimc.launcher.R;
import org.levimc.launcher.core.news.ChangelogManager;
import org.levimc.launcher.ui.activities.NewsActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Oblivion hub: account (Discord roles / membership), claim codes, skins, cosmetics, DLC themes, client settings. */
public class OblivionHubActivity extends OblivionBaseActivity {
    private final ActivityResultLauncher<String> dlcPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) installDlc(uri);
            });

    private final ActivityResultLauncher<String> videoPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) importLoadingVideo(uri);
            });

    @Override
    protected int titleRes() {
        return R.string.oblivion_hub;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OblivionSession.isLinked(this)) {
            OblivionSession.refresh(this, (a, e) -> {
                if (a != null && !isFinishing()) rebuild();
            });
        }
    }

    @Override
    protected void buildContent(LinearLayout col) {
        buildAccount(col);
        buildClaim(col);

        LinearLayout tools = OUi.card(this);
        tools.addView(OUi.title(this, getString(R.string.oblivion_tools)));
        tools.addView(OUi.button(this, getString(R.string.oblivion_skin_changer), true,
                v -> startActivity(new Intent(this, SkinChangerActivity.class))));
        tools.addView(OUi.button(this, getString(R.string.oblivion_cosmetics), false,
                v -> startActivity(new Intent(this, CosmeticsActivity.class))));
        tools.addView(OUi.button(this, getString(R.string.oblivion_news), false,
                v -> startActivity(new Intent(this, NewsActivity.class))));
        tools.addView(OUi.button(this, getString(R.string.oblivion_whats_new), false, v -> {
            getSharedPreferences("launcher_changelog", MODE_PRIVATE).edit()
                    .putLong("last_seen_version_code", 0L).apply();
            ChangelogManager.showIfNeeded(this, () -> {
            });
        }));
        tools.addView(OUi.button(this, getString(R.string.oblivion_discord_server), false, v -> openInvite()));
        tools.addView(OUi.button(this, getString(R.string.oblivion_loading_video_pick), false, v -> videoPicker.launch("video/*")));
        col.addView(tools);

        LinearLayout dlc = OUi.card(this);
        dlc.addView(OUi.title(this, getString(R.string.oblivion_dlc)));
        dlc.addView(OUi.body(this, OblivionTheme.dlcName != null
                ? getString(R.string.oblivion_dlc_active, OblivionTheme.dlcName)
                : getString(R.string.oblivion_dlc_desc)));
        dlc.addView(OUi.button(this, getString(R.string.oblivion_dlc_install), true, v -> dlcPicker.launch("*/*")));
        dlc.addView(OUi.button(this, getString(R.string.oblivion_dlc_reset), false, v -> {
            OblivionTheme.resetToDefault(this);
            Toast.makeText(this, R.string.oblivion_dlc_reset_done, Toast.LENGTH_SHORT).show();
            recreate();
        }));
        col.addView(dlc);

        LinearLayout settings = OUi.card(this);
        settings.addView(OUi.title(this, getString(R.string.oblivion_client_settings)));
        addSwitch(settings, R.string.oblivion_set_dungeon_home, OblivionSettings.DUNGEON_HOME);
        addSwitch(settings, R.string.oblivion_set_ambient, OblivionSettings.AMBIENT_ANIM);
        addSwitch(settings, R.string.oblivion_set_loading_video, OblivionSettings.LOADING_VIDEO);
        addSwitch(settings, R.string.oblivion_set_auto_skin, OblivionSettings.AUTO_APPLY_SKIN);
        addSwitch(settings, R.string.oblivion_set_badge, OblivionSettings.SHOW_BADGE);
        addSwitch(settings, R.string.oblivion_set_watermark, OblivionSettings.INGAME_WATERMARK);
        col.addView(settings);
    }

    private void addSwitch(LinearLayout parent, int label, String key) {
        androidx.appcompat.widget.SwitchCompat sw = new androidx.appcompat.widget.SwitchCompat(this);
        sw.setText(label);
        sw.setTextColor(OblivionTheme.text);
        sw.setTypeface(OUi.font(this));
        sw.setChecked(OblivionSettings.get(this, key));
        sw.setPadding(0, OUi.dp(this, 8), 0, OUi.dp(this, 8));
        sw.setOnCheckedChangeListener((b, on) -> OblivionSettings.set(this, key, on));
        parent.addView(sw);
    }

    private void buildAccount(LinearLayout col) {
        LinearLayout card = OUi.card(this);
        card.addView(OUi.title(this, getString(R.string.oblivion_account)));
        if (!OblivionConfig.isServerConfigured(this)) {
            card.addView(OUi.body(this, getString(R.string.oblivion_server_missing)));
            col.addView(card);
            return;
        }
        OblivionSession.Account a = OblivionSession.current(this);
        if (!OblivionSession.isLinked(this) || a == null) {
            card.addView(OUi.body(this, getString(R.string.oblivion_link_desc)));
            card.addView(OUi.button(this, getString(R.string.oblivion_link_discord), true,
                    v -> OblivionSession.beginLogin(this)));
            col.addView(card);
            return;
        }
        card.addView(OUi.body(this, getString(R.string.oblivion_signed_in_as, a.username)));
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.START);
        chips.setPadding(0, OUi.dp(this, 8), 0, OUi.dp(this, 4));
        int shown = 0;
        for (OblivionRole r : a.roles) {
            if (shown++ >= 3) break;
            chips.addView(chip(r.label(this), r.color));
        }
        card.addView(chips);
        card.addView(OUi.body(this, a.hasMembership()
                ? getString(R.string.oblivion_membership_active)
                : getString(R.string.oblivion_membership_inactive)));
        if (!a.verified) {
            card.addView(OUi.body(this, getString(R.string.oblivion_unverified)));
        }
        card.addView(OUi.button(this, getString(R.string.oblivion_refresh), false, v ->
                OblivionSession.refresh(this, (acc, e) -> {
                    if (e != null) Toast.makeText(this, e, Toast.LENGTH_SHORT).show();
                    rebuild();
                })));
        card.addView(OUi.button(this, getString(R.string.oblivion_unlink), false, v -> {
            OblivionSession.logout(this);
            rebuild();
        }));
        col.addView(card);
    }

    private TextView chip(String text, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(12);
        t.setTypeface(OUi.font(this), android.graphics.Typeface.BOLD);
        t.setBackground(OUi.rounded((color & 0x00FFFFFF) | 0xCC000000, 0, 12, this));
        t.setPadding(OUi.dp(this, 10), OUi.dp(this, 4), OUi.dp(this, 10), OUi.dp(this, 4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, OUi.dp(this, 8), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private void buildClaim(LinearLayout col) {
        LinearLayout card = OUi.card(this);
        card.addView(OUi.title(this, getString(R.string.oblivion_claim)));
        card.addView(OUi.body(this, getString(R.string.oblivion_claim_desc)));
        EditText input = new EditText(this);
        input.setHint(R.string.oblivion_claim_hint);
        input.setHintTextColor(OblivionTheme.textDim);
        input.setTextColor(OblivionTheme.text);
        input.setTypeface(OUi.font(this));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setSingleLine(true);
        card.addView(input);
        card.addView(OUi.button(this, getString(R.string.oblivion_claim_redeem), true,
                v -> redeem(input.getText().toString().trim())));
        col.addView(card);
    }

    private void redeem(String code) {
        if (code.isEmpty()) return;
        String token = OblivionSession.token(this);
        if (token == null || !OblivionConfig.isServerConfigured(this)) {
            Toast.makeText(this, R.string.oblivion_claim_need_link, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            JSONObject body = new JSONObject().put("code", code);
            OblivionApi.postJson(this, "/v1/claim", token, body, new OblivionApi.JsonCb() {
                @Override
                public void ok(JSONObject json) {
                    JSONObject p = OblivionApi.openEnvelope(OblivionHubActivity.this, json, null);
                    if (p == null) {
                        Toast.makeText(OblivionHubActivity.this, R.string.oblivion_bad_signature, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String pkg = p.optString("package_id", "");
                    String title = p.optString("title", code);
                    if (!pkg.isEmpty()) {
                        fetchPackage(pkg, title);
                    } else {
                        claimed(title);
                    }
                }

                @Override
                public void fail(String message) {
                    Toast.makeText(OblivionHubActivity.this, getString(R.string.oblivion_claim_failed, message),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPackage(String packageId, String title) {
        OblivionApi.download(this, "/v1/packages/" + Uri.encode(packageId), OblivionSession.token(this),
                new OblivionApi.BytesCb() {
                    @Override
                    public void ok(byte[] data) {
                        try {
                            SecureVault.put(OblivionHubActivity.this, "pkg_" + packageId, data);
                            claimed(title);
                        } catch (Exception e) {
                            Toast.makeText(OblivionHubActivity.this, R.string.oblivion_vault_failed, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void fail(String message) {
                        Toast.makeText(OblivionHubActivity.this,
                                getString(R.string.oblivion_claim_failed, message), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void claimed(String title) {
        Toast.makeText(this, getString(R.string.oblivion_claim_success, title), Toast.LENGTH_LONG).show();
        OblivionSession.refresh(this, (a, e) -> rebuild());
    }

    private void openInvite() {
        String url = OblivionConfig.discordInvite(this);
        if (url.isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    private void importLoadingVideo(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             java.io.FileOutputStream out = new java.io.FileOutputStream(LoadingBackground.userVideo(this))) {
            if (in == null) return;
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            Toast.makeText(this, R.string.oblivion_loading_video_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() == null ? "error" : e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void installDlc(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            byte[] data = bos.toByteArray();
            String json = null;
            if (data.length > 2 && data[0] == 'P' && data[1] == 'K') {
                try (ZipInputStream zin = new ZipInputStream(new java.io.ByteArrayInputStream(data))) {
                    ZipEntry e;
                    while ((e = zin.getNextEntry()) != null) {
                        if (e.getName().endsWith("theme.json")) {
                            ByteArrayOutputStream tb = new ByteArrayOutputStream();
                            while ((n = zin.read(buf)) > 0) tb.write(buf, 0, n);
                            json = tb.toString("UTF-8");
                            break;
                        }
                    }
                }
            } else {
                json = new String(data, StandardCharsets.UTF_8);
            }
            if (json != null && OblivionTheme.installDlcTheme(this, json)) {
                Toast.makeText(this, R.string.oblivion_dlc_installed, Toast.LENGTH_SHORT).show();
                recreate();
            } else {
                Toast.makeText(this, R.string.oblivion_dlc_invalid, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.oblivion_dlc_invalid, Toast.LENGTH_LONG).show();
        }
    }
}
