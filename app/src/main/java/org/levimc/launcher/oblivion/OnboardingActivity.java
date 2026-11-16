package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.levimc.launcher.BuildConfig;
import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.MainActivity;
import org.levimc.launcher.ui.activities.MsftLoginActivity;
import org.levimc.launcher.util.LanguageManager;

/**
 * First-run wizard: language, rules, license, Oblivion Modder, community, account links.
 * Every step can be skipped. It is shown again whenever the app version changes.
 */
public class OnboardingActivity extends OblivionBaseActivity {
    private static final String PREFS = "oblivion_onboarding";
    private static final String KEY_DONE_VERSION = "done_version";
    private static final String STATE_STEP = "step";
    private static final int STEPS = 6;

    private int step;

    public static boolean isNeeded(Context c) {
        String done = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DONE_VERSION, null);
        return !BuildConfig.VERSION_NAME.equals(done);
    }

    @Override
    protected int titleRes() {
        return R.string.oblivion_onboarding_title;
    }

    @Override
    protected void onCreate(Bundle state) {
        if (state != null) step = state.getInt(STATE_STEP, 0);
        super.onCreate(state);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_STEP, step);
    }

    @Override
    protected void buildContent(LinearLayout col) {
        TextView counter = OUi.body(this, getString(R.string.oblivion_step_of, step + 1, STEPS));
        counter.setPadding(0, 0, 0, OUi.dp(this, 8));
        col.addView(counter);
        LinearLayout card = OUi.card(this);
        switch (step) {
            case 0:
                stepLanguage(card);
                break;
            case 1:
                stepText(card, R.string.oblivion_rules_title, R.string.oblivion_rules_text);
                break;
            case 2:
                stepText(card, R.string.oblivion_license_title, R.string.oblivion_license_text);
                break;
            case 3:
                stepModder(card);
                break;
            case 4:
                stepCommunity(card);
                break;
            default:
                stepAccounts(card);
                break;
        }
        col.addView(card);

        boolean last = step == STEPS - 1;
        boolean acceptStep = step == 1 || step == 2;
        col.addView(OUi.button(this, getString(last ? R.string.oblivion_finish
                : acceptStep ? R.string.oblivion_accept : R.string.oblivion_next), true, v -> next()));
        col.addView(OUi.button(this, getString(R.string.oblivion_skip), false, v -> {
            if (last) finishOnboarding();
            else next();
        }));
        if (!last) {
            col.addView(OUi.button(this, getString(R.string.oblivion_skip_all), false, v -> finishOnboarding()));
        }
    }

    private void next() {
        if (step >= STEPS - 1) {
            finishOnboarding();
            return;
        }
        step++;
        rebuild();
    }

    private void finishOnboarding() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_DONE_VERSION, BuildConfig.VERSION_NAME).apply();
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    private void stepLanguage(LinearLayout card) {
        card.addView(OUi.title(this, getString(R.string.language)));
        String current = LanguageManager.getSavedLanguage(this);
        int cur = OblivionLanguages.indexOf(current);
        LanguageManager lm = new LanguageManager(this);
        for (int i = 0; i < OblivionLanguages.CODES.length; i++) {
            final int idx = i;
            String label = (i == cur ? "✓  " : "") + OblivionLanguages.NAMES[i];
            card.addView(OUi.button(this, label, i == cur, v -> {
                if (idx != cur) lm.setAppLanguage(OblivionLanguages.CODES[idx]);
            }));
        }
    }

    private void stepText(LinearLayout card, int titleRes, int textRes) {
        card.addView(OUi.title(this, getString(titleRes)));
        TextView t = OUi.body(this, getString(textRes));
        t.setPadding(0, OUi.dp(this, 8), 0, 0);
        card.addView(t);
    }

    private void stepModder(LinearLayout card) {
        card.addView(OUi.title(this, getString(R.string.oblivion_modder_title)));
        TextView t = OUi.body(this, getString(R.string.oblivion_modder_text, getString(R.string.oblivion_dev_name)));
        t.setPadding(0, OUi.dp(this, 8), 0, 0);
        card.addView(t);
        addLink(card, R.string.oblivion_dev_github, R.string.oblivion_open_github);
    }

    private void stepCommunity(LinearLayout card) {
        card.addView(OUi.title(this, getString(R.string.oblivion_community_title)));
        TextView t = OUi.body(this, getString(R.string.oblivion_community_text));
        t.setPadding(0, OUi.dp(this, 8), 0, 0);
        card.addView(t);
        addLink(card, R.string.oblivion_discord_invite, R.string.oblivion_discord_server);
        addLink(card, R.string.oblivion_social_youtube, R.string.oblivion_open_youtube);
        addLink(card, R.string.oblivion_social_instagram, R.string.oblivion_open_instagram);
    }

    private void stepAccounts(LinearLayout card) {
        card.addView(OUi.title(this, getString(R.string.oblivion_accounts_title)));
        TextView t = OUi.body(this, getString(R.string.oblivion_accounts_text));
        t.setPadding(0, OUi.dp(this, 8), 0, 0);
        card.addView(t);
        card.addView(OUi.button(this, getString(R.string.oblivion_connect_microsoft), true,
                v -> startActivity(new Intent(this, MsftLoginActivity.class))));
        if (OblivionConfig.isServerConfigured(this)) {
            card.addView(OUi.button(this, getString(R.string.oblivion_link_discord), true,
                    v -> OblivionSession.beginLogin(this)));
        }
    }

    private void addLink(LinearLayout card, int urlRes, int labelRes) {
        String url = getString(urlRes).trim();
        if (!url.startsWith("http")) return;
        card.addView(OUi.button(this, getString(labelRes), false, v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
            }
        }));
    }
}
