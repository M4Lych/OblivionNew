package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.levimc.launcher.BuildConfig;
import org.levimc.launcher.R;
import org.levimc.launcher.core.crash.CrashReporter;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.dialogs.LogcatOverlayManager;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.ThemeManager;

import java.util.Locale;

/** Oblivion settings: language, appearance, game options and credits. */
public class SettingsActivity extends BaseActivity {

    private static final int[] THEME_MODES = {
            ThemeManager.MODE_FOLLOW_SYSTEM, ThemeManager.MODE_LIGHT, ThemeManager.MODE_DARK};

    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));
        setActiveNavTab(R.id.nav_tab_settings);

        themeManager = new ThemeManager(this);
        setupLanguage();
        setupTheme();
        setupGameOptions();

        TextView version = findViewById(R.id.about_version);
        version.setText(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
    }

    private void setupLanguage() {
        LanguageManager languageManager = new LanguageManager(this);
        String[] names = {
                getString(R.string.english), getString(R.string.chinese), getString(R.string.french),
                getString(R.string.hindi), getString(R.string.indonesian), getString(R.string.japanese),
                getString(R.string.portuguese), getString(R.string.russian), getString(R.string.spanish),
                getString(R.string.turkish), getString(R.string.vietnamese)
        };
        String[] codes = {"en", "zh-CN", "fr", "hi", "id", "ja", "pt", "ru", "es", "tr", "vi"};
        String current = Locale.forLanguageTag(languageManager.getCurrentLanguage()).getLanguage();
        int idx = 0;
        for (int i = 0; i < codes.length; i++) {
            String c = Locale.forLanguageTag(codes[i]).getLanguage();
            if (c.equals(current) || (c.equals("id") && current.equals("in"))) {
                idx = i;
                break;
            }
        }
        TextView currentView = findViewById(R.id.language_current);
        currentView.setText(names[idx]);
        findViewById(R.id.language_change).setOnClickListener(v -> new CustomAlertDialog(this)
                .setTitleText(getString(R.string.language))
                .setItems(names, (dialog, which) -> {
                    currentView.setText(names[which]);
                    if (!codes[which].equals(languageManager.getCurrentLanguage())) {
                        languageManager.setAppLanguage(codes[which]);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show());
    }

    private void setupTheme() {
        String[] labels = {
                getString(R.string.oblivion_theme_system),
                getString(R.string.oblivion_theme_light),
                getString(R.string.oblivion_theme_dark)
        };
        TextView currentView = findViewById(R.id.theme_current);
        int mode = themeManager.getCurrentMode();
        int idx = 0;
        for (int i = 0; i < THEME_MODES.length; i++) {
            if (THEME_MODES[i] == mode) idx = i;
        }
        currentView.setText(labels[idx]);
        findViewById(R.id.theme_change).setOnClickListener(v -> new CustomAlertDialog(this)
                .setTitleText(getString(R.string.oblivion_theme))
                .setItems(labels, (dialog, which) -> {
                    currentView.setText(labels[which]);
                    themeManager.setThemeMode(THEME_MODES[which]);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show());
    }

    private void setupGameOptions() {
        FeatureSettings fs = FeatureSettings.getInstance();

        SwitchMaterial login = findViewById(R.id.switch_ms_login);
        login.setChecked(fs.isLauncherManagedMcLoginEnabled());
        login.setOnCheckedChangeListener((btn, checked) -> fs.setLauncherManagedMcLoginEnabled(checked));

        SwitchMaterial logcat = findViewById(R.id.switch_logcat);
        logcat.setChecked(fs.isLogcatOverlayEnabled());
        logcat.setOnCheckedChangeListener((btn, checked) -> {
            fs.setLogcatOverlayEnabled(checked);
            try {
                LogcatOverlayManager mgr = LogcatOverlayManager.getInstance();
                if (mgr != null) mgr.refreshVisibility();
            } catch (Throwable ignored) {
            }
        });

        SwitchMaterial crash = findViewById(R.id.switch_crash_upload);
        crash.setChecked(fs.isCrashUploadEnabled());
        crash.setOnCheckedChangeListener((btn, checked) -> {
            fs.setCrashUploadEnabled(checked);
            CrashReporter.refreshCrashlyticsCollection(this);
        });
    }
}
