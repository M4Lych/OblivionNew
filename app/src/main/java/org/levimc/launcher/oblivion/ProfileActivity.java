package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONObject;
import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.ui.activities.AccountsActivity;

/** Profile: Microsoft/Xbox identity, Oblivion role + membership, achievements and claim codes. */
public class ProfileActivity extends OblivionBaseActivity {
    @Override
    protected int titleRes() {
        return R.string.oblivion_profile_title;
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
        buildIdentity(col);
        buildAccounts(col);
        buildAchievements(col);
        buildClaim(col);
        LinearLayout look = OUi.card(this);
        look.addView(OUi.title(this, getString(R.string.oblivion_cosmetics)));
        look.addView(OUi.button(this, getString(R.string.oblivion_cosmetics), true,
                v -> startActivity(new Intent(this, CosmeticsActivity.class))));
        look.addView(OUi.button(this, getString(R.string.oblivion_skin_changer), false,
                v -> startActivity(new Intent(this, SkinChangerActivity.class))));
        col.addView(look);
    }

    private void buildIdentity(LinearLayout col) {
        LinearLayout card = OUi.card(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackground(OUi.rounded(0x55FFFFFF, 0, 10, this));
        row.addView(avatar, new LinearLayout.LayoutParams(OUi.dp(this, 64), OUi.dp(this, 64)));
        String name = getString(R.string.oblivion_player_default);
        try {
            for (MsftAccountStore.MsftAccount a : MsftAccountStore.list(this)) {
                if (a.active) {
                    if (a.xboxGamertag != null && !a.xboxGamertag.isEmpty()) name = a.xboxGamertag;
                    else if (a.minecraftUsername != null) name = a.minecraftUsername;
                    if (a.xboxAvatarUrl != null && !a.xboxAvatarUrl.isEmpty()) {
                        Glide.with(getApplicationContext()).load(a.xboxAvatarUrl).into(avatar);
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(OUi.dp(this, 14), 0, 0, 0);
        TextView n = OUi.title(this, name);
        n.setTextSize(22);
        n.setTypeface(OUi.heavy());
        texts.addView(n);
        OblivionSession.Account acc = OblivionSession.current(this);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, OUi.dp(this, 6), 0, 0);
        if (acc != null) {
            int shown = 0;
            for (OblivionRole r : acc.roles) {
                if (shown++ >= 3) break;
                chips.addView(chip(r.label(this), r.color));
            }
        } else {
            chips.addView(chip(getString(R.string.oblivion_role_none), 0xFF7A7F87));
        }
        texts.addView(chips);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        card.addView(OUi.body(this, acc != null && acc.hasMembership()
                ? getString(R.string.oblivion_membership_active) : getString(R.string.oblivion_membership_inactive)));
        col.addView(card);
    }

    private TextView chip(String text, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(12);
        t.setTypeface(OUi.heavy());
        t.setBackground(OUi.rounded((color & 0x00FFFFFF) | 0xCC000000, 0, 12, this));
        t.setPadding(OUi.dp(this, 10), OUi.dp(this, 3), OUi.dp(this, 10), OUi.dp(this, 3));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, OUi.dp(this, 6), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private void buildAccounts(LinearLayout col) {
        LinearLayout card = OUi.card(this);
        card.addView(OUi.title(this, getString(R.string.oblivion_profile_accounts)));
        card.addView(OUi.button(this, getString(R.string.oblivion_profile_manage_accounts), true,
                v -> startActivity(new Intent(this, AccountsActivity.class))));
        if (!OblivionConfig.isServerConfigured(this)) {
            card.addView(OUi.body(this, getString(R.string.oblivion_server_missing)));
        } else if (!OblivionSession.isLinked(this)) {
            card.addView(OUi.body(this, getString(R.string.oblivion_link_desc)));
            card.addView(OUi.button(this, getString(R.string.oblivion_link_discord), true,
                    v -> OblivionSession.beginLogin(this)));
        } else {
            OblivionSession.Account a = OblivionSession.current(this);
            if (a != null) card.addView(OUi.body(this, getString(R.string.oblivion_signed_in_as, a.username)));
            if (a != null && !a.verified) card.addView(OUi.body(this, getString(R.string.oblivion_unverified)));
            card.addView(OUi.button(this, getString(R.string.oblivion_refresh), false, v ->
                    OblivionSession.refresh(this, (acc, e) -> {
                        if (e != null) Toast.makeText(this, e, Toast.LENGTH_SHORT).show();
                        rebuild();
                    })));
            card.addView(OUi.button(this, getString(R.string.oblivion_unlink), false, v -> {
                OblivionSession.logout(this);
                rebuild();
            }));
        }
        col.addView(card);
    }

    private void buildAchievements(LinearLayout col) {
        LinearLayout card = OUi.card(this);
        card.addView(OUi.title(this, getString(R.string.oblivion_profile_achievements)));
        for (Achievements.Def d : Achievements.ALL) {
            int have = Achievements.get(this, d.counter);
            boolean done = have >= d.goal;
            TextView t = OUi.body(this, (done ? "✔  " : "○  ") + getString(d.titleRes)
                    + (done ? "" : "   (" + Math.min(have, d.goal) + "/" + d.goal + ")"));
            t.setTextColor(done ? OblivionTheme.launchLight : OblivionTheme.textDim);
            t.setPadding(0, OUi.dp(this, 4), 0, OUi.dp(this, 4));
            card.addView(t);
        }
        card.addView(OUi.body(this, getString(R.string.oblivion_ach_note)));
        col.addView(card);
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
                    JSONObject p = OblivionApi.openEnvelope(ProfileActivity.this, json, null);
                    if (p == null) {
                        Toast.makeText(ProfileActivity.this, R.string.oblivion_bad_signature, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String pkg = p.optString("package_id", "");
                    String title = p.optString("title", code);
                    if (!pkg.isEmpty()) fetchPackage(pkg, title);
                    else claimed(title);
                }

                @Override
                public void fail(String message) {
                    Toast.makeText(ProfileActivity.this, getString(R.string.oblivion_claim_failed, message),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, String.valueOf(e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPackage(String packageId, String title) {
        OblivionApi.download(this, "/v1/packages/" + Uri.encode(packageId), OblivionSession.token(this),
                new OblivionApi.BytesCb() {
                    @Override
                    public void ok(byte[] data) {
                        try {
                            SecureVault.put(ProfileActivity.this, "pkg_" + packageId, data);
                            claimed(title);
                        } catch (Exception e) {
                            Toast.makeText(ProfileActivity.this, R.string.oblivion_vault_failed, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void fail(String message) {
                        Toast.makeText(ProfileActivity.this,
                                getString(R.string.oblivion_claim_failed, message), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void claimed(String title) {
        Achievements.increment(this, Achievements.CODES);
        Toast.makeText(this, getString(R.string.oblivion_claim_success, title), Toast.LENGTH_LONG).show();
        OblivionSession.refresh(this, (a, e) -> rebuild());
    }
}
