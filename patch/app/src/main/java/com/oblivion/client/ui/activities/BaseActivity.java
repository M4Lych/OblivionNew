package com.oblivion.client.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.oblivion.client.R;
import com.oblivion.client.core.auth.MsftAccountStore;
import com.oblivion.client.ui.animation.DynamicAnim;
import com.oblivion.client.util.AccountTextUtils;
import com.oblivion.client.util.PersonalizationManager;
import com.oblivion.client.util.LauncherBackgroundController;
import com.oblivion.client.util.ThemeManager;
import com.oblivion.client.util.WallpaperFolder;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BaseActivity extends AppCompatActivity {
    private int appliedThemeGeneration = -1;
    private int appliedPersonalizationGeneration = -1;
    private int appliedBackgroundEffectGeneration = -1;
    private boolean navBarInjected = false;
    private final Handler scheduleHandler = new Handler(Looper.getMainLooper());
    private final Runnable scheduleCheck = new Runnable() {
        @Override
        public void run() {
            if (new ThemeManager(BaseActivity.this).refreshScheduledMode()) {
                recreate();
                return;
            }
            scheduleHandler.postDelayed(this, 60_000L);
        }
    };

    /** Page classes alive in the task, so breadcrumbs only jump to pages that can be restored. */
    private static final Set<Class<?>> sLivePages = new HashSet<>();
    private final OkHttpClient navAvatarClient = new OkHttpClient();
    private final ExecutorService navAccountExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> navAccountLoginLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale locale = Locale.ENGLISH;
        Locale.setDefault(locale);
        Resources res = newBase.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        Context localizedContext = newBase.createConfigurationContext(config);
        super.attachBaseContext(localizedContext);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager themeManager = new ThemeManager(this);
        themeManager.applyTheme();
        appliedThemeGeneration = ThemeManager.getThemeChangeGeneration();
        appliedPersonalizationGeneration = PersonalizationManager.getChangeGeneration();
        appliedBackgroundEffectGeneration = PersonalizationManager.getBackgroundEffectGeneration();
        super.onCreate(savedInstanceState);
        sLivePages.add(getClass());
        navAccountLoginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleNavAccountLoginResult(result.getResultCode(), result.getData()));
        hideSystemUI();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                visibility -> getWindow().getDecorView().post(this::hideSystemUI));
    }

    @Override
    public void setContentView(int layoutResID) {
        View contentView = LayoutInflater.from(this).inflate(layoutResID, null);
        wrapWithNavBar(contentView);
    }

    @Override
    public void setContentView(View view) {
        wrapWithNavBar(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        wrapWithNavBar(view);
    }

    private void wrapWithNavBar(View contentView) {
        if (shouldSkipNavBar()) {
            super.setContentView(contentView);
            if (avoidsCutoutWithoutShell()) com.oblivion.client.ui.widget.CutoutAvoider.attachToPage(contentView);
            applyPersonalization();
            return;
        }

        View shell = LayoutInflater.from(this).inflate(R.layout.shell_navbar, null, false);
        FrameLayout holder = shell.findViewById(R.id.shell_content);
        holder.addView(contentView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // The page fades in inside the static shell. Starting from partly visible avoids a blank
        // page area for the first frames, which reads as a flash when switching pages.
        contentView.setAlpha(0.4f);
        contentView.setTranslationY(6f * getResources().getDisplayMetrics().density);

        super.setContentView(shell);
        com.oblivion.client.ui.widget.CutoutAvoider.attachToShell(shell);
        navBarInjected = true;
        setupBaseNavBar();

        applyPersonalization();
        WallpaperFolder.rotateOnLaunch(this);

        contentView.post(() -> {
            DynamicAnim.springAlphaTo(contentView, 1f).start();
            DynamicAnim.springTranslationYTo(contentView, 0f).start();
        });
    }

    private void applyPersonalization() {
        PersonalizationManager pm = new PersonalizationManager(this);
        pm.applyToActivity(this);
    }

    protected boolean shouldSkipNavBar() {
        return false;
    }

    /** Full-screen pages without the shell (splash, game loading) draw edge to edge on purpose. */
    protected boolean avoidsCutoutWithoutShell() {
        return !(this instanceof SplashActivity);
    }

    private static final int[] NAV_TAB_IDS = {
            R.id.nav_tab_launch, R.id.nav_tab_download, R.id.nav_tab_instances,
            R.id.nav_tab_about, R.id.nav_tab_settings
    };

    private void setupBaseNavBar() {
        PersonalizationManager pm = new PersonalizationManager(this);

        // Over a wallpaper the chrome becomes translucent (Windows NAVBAR_BG) and the page area clear.
        if (pm.hasBackgroundImage()) {
            int chrome = pm.getChromeGlassColor(this);
            View root = findViewById(R.id.shell_root);
            View bar = findViewById(R.id.nav_bar_root);
            if (root != null) root.setBackgroundColor(Color.TRANSPARENT);
            if (bar != null) bar.setBackgroundColor(chrome);
            View divider = findViewById(R.id.nav_divider);
            if (divider != null) {
                divider.setBackgroundColor(pm.isDarkMode(this)
                        ? Color.argb(40, 255, 255, 255) : Color.argb(40, 0, 0, 0));
            }
        }

        for (int id : NAV_TAB_IDS) styleNavTab(findViewById(id), false);

        View backButton = findViewById(R.id.nav_back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
        refreshHistoryButtons();

        View themeToggle = findViewById(R.id.nav_theme_toggle);
        if (themeToggle instanceof ImageView) {
            ThemeManager tm = new ThemeManager(this);
            if (tm.canQuickToggle()) {
                boolean dark = tm.getCurrentMode() == ThemeManager.MODE_DARK;
                ((ImageView) themeToggle).setImageResource(dark ? R.drawable.ic_moon : R.drawable.ic_sun);
                themeToggle.setVisibility(View.VISIBLE);
                themeToggle.setOnClickListener(v -> new ThemeManager(this).setThemeMode(
                        dark ? ThemeManager.MODE_LIGHT : ThemeManager.MODE_DARK));
            } else {
                themeToggle.setVisibility(View.GONE);
            }
        }

        View signIn = findViewById(R.id.nav_sign_in_button);
        if (signIn != null) {
            signIn.setOnClickListener(v -> navAccountLoginLauncher.launch(new Intent(this, MsftLoginActivity.class)));
        }

        View avatarContainer = findViewById(R.id.nav_account_avatar_container);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> {
                if (!(this instanceof AccountsActivity)) startActivity(new Intent(this, AccountsActivity.class));
            });
            DynamicAnim.applyPressScale(avatarContainer);
        }

        refreshNavAccountUI();
    }

    /** Top-level pages are switched like tabs: reuse the page if it is open instead of stacking copies. */
    private void bindNavTab(int id, Class<? extends android.app.Activity> page) {
        View tab = findViewById(id);
        if (tab == null) return;
        tab.setOnClickListener(v -> {
            if (page.isInstance(this)) return;
            Intent intent = new Intent(this, page);
            if (page == MainActivity.class || sLivePages.contains(page)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            }
            startActivity(intent);
        });
    }

    private void refreshHistoryButtons() {
        View back = findViewById(R.id.nav_back_button);
        if (back != null) {
            boolean canBack = !isTaskRoot();
            back.setEnabled(canBack);
            back.setAlpha(canBack ? 1f : 0.35f);
        }
    }

    private void styleNavTab(@Nullable View view, boolean active) {
        if (!(view instanceof TextView)) return;
        TextView tab = (TextView) view;
        int iconSize = getResources().getDimensionPixelSize(R.dimen.oblivion_navbar_icon_size);
        android.graphics.drawable.Drawable[] icons = tab.getCompoundDrawablesRelative();
        for (android.graphics.drawable.Drawable icon : icons) {
            if (icon != null) icon.setBounds(0, 0, iconSize, iconSize);
        }
        tab.setCompoundDrawablesRelative(icons[0], icons[1], icons[2], icons[3]);
        // As in v1.6.1: no pill behind the active tab; it is bold in the primary text color and
        // the other tabs use the secondary text color.
        int text = ContextCompat.getColor(this, active ? R.color.text_primary : R.color.text_secondary);
        tab.setTextColor(text);
        tab.setTypeface(Typeface.create(tab.getTypeface(), active ? Typeface.BOLD : Typeface.NORMAL));
        TextViewCompat.setCompoundDrawableTintList(tab, ColorStateList.valueOf(text));
        android.util.TypedValue ripple = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        tab.setBackgroundResource(ripple.resourceId);
    }

    protected void onNewsChanged() {
    }

    /** The wallpaper was replaced in place on this page; do not recreate it for that change on resume. */
    public void markPersonalizationApplied() {
        appliedPersonalizationGeneration = PersonalizationManager.getChangeGeneration();
    }

    protected void refreshNavAccountUI() {
        if (!navBarInjected) return;
        java.util.List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(this);
        MsftAccountStore.MsftAccount active = null;
        for (MsftAccountStore.MsftAccount a : list) if (a.active) { active = a; break; }
        View signIn = findViewById(R.id.nav_sign_in_button);
        View avatarContainer = findViewById(R.id.nav_account_avatar_container);
        if (active == null) {
            if (signIn != null) signIn.setVisibility(View.VISIBLE);
            if (avatarContainer != null) avatarContainer.setVisibility(View.GONE);
            clearNavAvatar();
        } else {
            if (signIn != null) signIn.setVisibility(View.GONE);
            if (avatarContainer != null) avatarContainer.setVisibility(View.VISIBLE);
            loadNavXboxAvatar(active);
        }
    }

    private void clearNavAvatar() {
        com.microsoft.xbox.idp.toolkit.CircleImageView avatar = findViewById(R.id.nav_account_avatar);
        ProgressBar progress = findViewById(R.id.nav_avatar_progress);
        if (avatar != null) avatar.setImageResource(R.drawable.ic_minecraft_cube);
        if (progress != null) progress.setVisibility(View.GONE);
    }

    private void loadNavXboxAvatar(MsftAccountStore.MsftAccount active) {
        com.microsoft.xbox.idp.toolkit.CircleImageView avatar = findViewById(R.id.nav_account_avatar);
        ProgressBar progress = findViewById(R.id.nav_avatar_progress);
        if (avatar == null) return;

        String url = AccountTextUtils.sanitizeUrl(active != null ? active.xboxAvatarUrl : null);
        if (url == null) {
            avatar.setImageResource(R.drawable.ic_minecraft_cube);
            if (progress != null) progress.setVisibility(View.GONE);
            return;
        }

        Object currentUrl = avatar.getTag(R.id.nav_account_avatar);
        if (url.equals(currentUrl) && avatar.getDrawable() != null) {
            if (progress != null) progress.setVisibility(View.GONE);
            return;
        }

        Bitmap cached = AccountTextUtils.getCachedAvatar(url);
        if (cached != null) {
            avatar.setTag(R.id.nav_account_avatar, url);
            avatar.setImageBitmap(cached);
            if (progress != null) progress.setVisibility(View.GONE);
            return;
        }

        avatar.setTag(R.id.nav_account_avatar, url);
        avatar.setImageResource(R.drawable.ic_minecraft_cube);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        navAccountExecutor.execute(() -> {
            Bitmap bmp = null;
            try (Response imgResp = navAvatarClient.newCall(new Request.Builder().url(url).build()).execute()) {
                if (imgResp.isSuccessful() && imgResp.body() != null) {
                    bmp = android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream());
                }
            } catch (Exception ignored) {
            }

            final Bitmap loaded = bmp;
            runOnUiThread(() -> {
                if (!url.equals(avatar.getTag(R.id.nav_account_avatar))) return;
                if (loaded != null) {
                    AccountTextUtils.cacheAvatar(url, loaded);
                    avatar.setImageBitmap(loaded);
                }
                if (progress != null) progress.setVisibility(View.GONE);
            });
        });
    }

    private void handleNavAccountLoginResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null
                && data.getBooleanExtra(MsftLoginActivity.EXTRA_LOGIN_COMPLETED, false)) {
            String name = data.getStringExtra(MsftLoginActivity.EXTRA_LOGIN_NAME);
            String statusName = name != null ? name : getString(R.string.not_signed_in);
            Toast.makeText(this, getString(R.string.ms_login_success, statusName), Toast.LENGTH_SHORT).show();
            refreshNavAccountUI();
            onNavAccountChanged();
            return;
        }
        refreshNavAccountUI();
    }

    protected void onNavAccountChanged() {
    }

    protected void setActiveNavTab(int activeTabId) {
        if (!navBarInjected) return;
        for (int id : NAV_TAB_IDS) styleNavTab(findViewById(id), id == activeTabId);
        View activeTab = findViewById(activeTabId);
        if (activeTab != null) {
            androidx.core.view.OneShotPreDrawListener.add(activeTab, () -> activeTab.requestRectangleOnScreen(
                    new android.graphics.Rect(0, 0, activeTab.getWidth(), activeTab.getHeight()), true));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentGen = ThemeManager.getThemeChangeGeneration();
        int currentPGen = PersonalizationManager.getChangeGeneration();
        if (new ThemeManager(this).refreshScheduledMode()) currentGen = ThemeManager.getThemeChangeGeneration();
        if (appliedThemeGeneration != currentGen || appliedPersonalizationGeneration != currentPGen) {
            appliedThemeGeneration = currentGen;
            appliedPersonalizationGeneration = currentPGen;
            recreate();
            return;
        }
        refreshHistoryButtons();
        scheduleHandler.removeCallbacks(scheduleCheck);
        scheduleHandler.postDelayed(scheduleCheck, 60_000L);
        getDelegate().applyDayNight();
        hideSystemUI();
        int currentEffects = PersonalizationManager.getBackgroundEffectGeneration();
        if (appliedBackgroundEffectGeneration != currentEffects) {
            appliedBackgroundEffectGeneration = currentEffects;
            new PersonalizationManager(this).refreshBackgroundEffects(this);
        }
        LauncherBackgroundController.resume(this);
        refreshNavAccountUI();
    }

    @Override
    protected void onStop() {
        LauncherBackgroundController.stopped(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        scheduleHandler.removeCallbacks(scheduleCheck);
        hideSystemUI();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        navAccountExecutor.shutdownNow();
        if (isFinishing()) sLivePages.remove(getClass());
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().applyDayNight();
        hideSystemUI();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    protected void hideSystemUI() {
        View decorView = getWindow().getDecorView();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        }

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private boolean shouldSuppressTransition(Intent intent) {
        return intent != null && (intent.getFlags() & Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0;
    }

    private boolean hasLauncherBackground() {
        return getClass().getName().startsWith("com.oblivion.client.ui.activities.")
                && !(this instanceof SplashActivity)
                && !LauncherBackgroundController.isMinecraftBlocked()
                && new PersonalizationManager(this).hasBackgroundImage();
    }

    private boolean isLauncherNavigation(Intent intent) {
        return intent != null && intent.getComponent() != null
                && getPackageName().equals(intent.getComponent().getPackageName())
                && intent.getComponent().getClassName().startsWith("com.oblivion.client.ui.")
                && !SplashActivity.class.getName().equals(intent.getComponent().getClassName());
    }

    private void applyExitTransition(boolean keepBackground) {
        if (keepBackground) overridePendingTransition(0, 0);
        else overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void finish() {
        boolean keepBackground = !isTaskRoot() && (navBarInjected || hasLauncherBackground());
        if (!isTaskRoot()) LauncherBackgroundController.navigating(this);
        super.finish();
        applyExitTransition(keepBackground);
    }

    @Override
    public void startActivity(Intent intent) {
        prepareBackgroundNavigation(intent);
        try {
            super.startActivity(intent);
        } catch (RuntimeException e) {
            LauncherBackgroundController.navigationFailed(this);
            throw e;
        }
        if (!shouldSuppressTransition(intent)) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } else overridePendingTransition(0, 0);
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        prepareBackgroundNavigation(intent);
        try {
            super.startActivity(intent, options);
        } catch (RuntimeException e) {
            LauncherBackgroundController.navigationFailed(this);
            throw e;
        }
        if (!shouldSuppressTransition(intent)) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } else overridePendingTransition(0, 0);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        prepareBackgroundNavigation(intent);
        try {
            super.startActivityForResult(intent, requestCode, options);
        } catch (RuntimeException e) {
            LauncherBackgroundController.navigationFailed(this);
            throw e;
        }
        if (shouldSuppressTransition(intent)) overridePendingTransition(0, 0);
    }

    @Override
    public void finishAfterTransition() {
        boolean keepBackground = !isTaskRoot() && (navBarInjected || hasLauncherBackground());
        if (!isTaskRoot()) LauncherBackgroundController.navigating(this);
        super.finishAfterTransition();
        applyExitTransition(keepBackground);
    }

    @Override
    public void recreate() {
        LauncherBackgroundController.navigating(this);
        super.recreate();
    }

    private void prepareBackgroundNavigation(Intent intent) {
        if (isLauncherNavigation(intent)) {
            LauncherBackgroundController.navigating(this);
            if (navBarInjected || hasLauncherBackground()) intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
    }
}
