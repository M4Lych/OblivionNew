package org.levimc.launcher.oblivion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.core.news.NewsFeed;
import org.levimc.launcher.core.news.NewsItem;
import org.levimc.launcher.core.news.NewsRepository;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.ui.activities.GameDownloadActivity;
import org.levimc.launcher.ui.activities.NewsActivity;
import org.levimc.launcher.ui.activities.XboxFriendsActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * The Oblivion start screen. It replaces the Levi home page: logo (credits), Party, Newsworthy, Friends,
 * the player model, role + gamertag, version + LAUNCH, Resources and Settings. Every action is forwarded to
 * existing launcher functions through {@link Host}; nothing here touches the launch pipeline.
 */
public class OblivionHomeView extends FrameLayout {
    public interface Host {
        void launch();

        void pickInstalled(View anchor);

        void importApk();

        void exit();
    }

    private static WeakReference<OblivionHomeView> current = new WeakReference<>(null);

    private final Host host;
    private PlayerModelView modelView;
    private TextView roleView;
    private TextView nameView;
    private ImageView avatarView;
    private TextView versionView;
    private TextView newsView;
    private TextView newsTag;
    private View newsDot;
    private final List<NewsItem> news = new ArrayList<>();
    private int newsIndex;
    private final Runnable newsTicker = new Runnable() {
        @Override
        public void run() {
            showNext();
            postDelayed(this, 5000);
        }
    };

    public static void attach(Activity activity, Host host) {
        if (!OblivionSettings.get(activity, OblivionSettings.DUNGEON_HOME)) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        OblivionHomeView v = new OblivionHomeView(activity, host);
        content.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        current = new WeakReference<>(v);
    }

    public static void refreshInstance() {
        OblivionHomeView v = current.get();
        if (v != null) v.refresh();
    }

    private OblivionHomeView(Context c, Host host) {
        super(c);
        this.host = host;
        OblivionTheme.ensureLoaded(c);
        setClickable(true);
        build(c);
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void open(Class<?> cls) {
        getContext().startActivity(new Intent(getContext(), cls));
    }

    private void build(Context c) {
        MovieBackgroundView bg = new MovieBackgroundView(c);
        bg.setAnimated(OblivionSettings.get(c, OblivionSettings.AMBIENT_ANIM));
        addView(bg, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        int m = OUi.dp(c, 16);

        // centre: model
        modelView = new PlayerModelView(c);
        modelView.setAnimated(OblivionSettings.get(c, OblivionSettings.AMBIENT_ANIM));
        modelView.setOnModelTapListener(() -> open(CosmeticsActivity.class));
        LayoutParams mp = new LayoutParams(OUi.dp(c, 300), LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
        mp.setMargins(0, OUi.dp(c, 58), 0, OUi.dp(c, 96));
        addView(modelView, mp);

        // centre bottom: role + gamertag
        LinearLayout plate = new LinearLayout(c);
        plate.setOrientation(LinearLayout.VERTICAL);
        roleView = new TextView(c);
        roleView.setGravity(Gravity.CENTER);
        roleView.setTextSize(15);
        roleView.setTypeface(OUi.heavy());
        roleView.setTextColor(0xFFFFFFFF);
        roleView.setPadding(0, OUi.dp(c, 3), 0, OUi.dp(c, 3));
        plate.addView(roleView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(OUi.rounded(OblivionTheme.glass, OblivionTheme.glassStroke, 6, c));
        avatarView = new ImageView(c);
        avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarView.setBackground(OUi.rounded(0x55FFFFFF, 0, 5, c));
        bar.addView(avatarView, new LinearLayout.LayoutParams(OUi.dp(c, 38), OUi.dp(c, 38)));
        nameView = new TextView(c);
        nameView.setGravity(Gravity.CENTER);
        nameView.setTextSize(19);
        nameView.setTypeface(OUi.heavy());
        nameView.setTextColor(OblivionTheme.text);
        nameView.setSingleLine(true);
        bar.addView(nameView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 0, 0, 0);
        bar.setPadding(OUi.dp(c, 4), OUi.dp(c, 4), OUi.dp(c, 12), OUi.dp(c, 4));
        plate.addView(bar, bp);
        OUi.pressable(plate);
        plate.setOnClickListener(v -> open(ProfileActivity.class));
        LayoutParams pp = new LayoutParams(OUi.dp(c, 250), LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        pp.setMargins(0, 0, 0, m);
        addView(plate, pp);

        // top-left: logo + name
        LinearLayout brand = new LinearLayout(c);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(c);
        logo.setBackground(OUi.rounded(0xFF000000, 0, 10, c));
        logo.setPadding(OUi.dp(c, 6), OUi.dp(c, 6), OUi.dp(c, 6), OUi.dp(c, 6));
        applyLogo(c, logo);
        brand.addView(logo, new LinearLayout.LayoutParams(OUi.dp(c, 50), OUi.dp(c, 50)));
        TextView title = new TextView(c);
        title.setText("Oblivion");
        title.setTextSize(30);
        title.setTypeface(OUi.heavy());
        title.setTextColor(0xFFFFFFFF);
        title.setShadowLayer(OUi.dp(c, 3), 0, OUi.dp(c, 2), 0xAA000000);
        title.setPadding(OUi.dp(c, 10), 0, 0, 0);
        brand.addView(title);
        OUi.pressable(brand);
        brand.setOnClickListener(v -> open(CreditsActivity.class));
        LayoutParams brp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        brp.setMargins(m, m, 0, 0);
        addView(brand, brp);

        // top-right: Party
        TextView party = OUi.glassButton(c, c.getString(R.string.oblivion_party), 20, v -> open(PartyActivity.class));
        party.setBackground(OUi.rounded(0xCC1FD77F, 0xFF6CFFB0, 7, c));
        LayoutParams ptp = new LayoutParams(OUi.dp(c, 150), LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        ptp.setMargins(0, m, m, 0);
        addView(party, ptp);

        // right column: Newsworthy + Friends
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        FrameLayout newsCard = new FrameLayout(c);
        newsCard.setBackground(OUi.rounded(OblivionTheme.glass, OblivionTheme.glassStroke, 7, c));
        newsTag = new TextView(c);
        newsTag.setTextSize(10);
        newsTag.setTypeface(OUi.heavy());
        newsTag.setTextColor(OblivionTheme.launchLight);
        newsTag.setAllCaps(true);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        tp.setMargins(OUi.dp(c, 10), OUi.dp(c, 6), 0, 0);
        newsCard.addView(newsTag, tp);
        newsView = new TextView(c);
        newsView.setGravity(Gravity.CENTER);
        newsView.setTextSize(15);
        newsView.setTypeface(OUi.heavy());
        newsView.setTextColor(OblivionTheme.text);
        newsView.setMaxLines(2);
        newsView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        newsView.setText(R.string.oblivion_newsworthy);
        newsView.setPadding(OUi.dp(c, 10), OUi.dp(c, 18), OUi.dp(c, 10), OUi.dp(c, 8));
        newsCard.addView(newsView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newsDot = new View(c);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(0xFFFF5252);
        newsDot.setBackground(dot);
        newsDot.setVisibility(GONE);
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(OUi.dp(c, 10), OUi.dp(c, 10), Gravity.TOP | Gravity.END);
        dp.setMargins(0, OUi.dp(c, 8), OUi.dp(c, 8), 0);
        newsCard.addView(newsDot, dp);
        OUi.pressable(newsCard);
        newsCard.setOnClickListener(v -> openNews());
        right.addView(newsCard, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, OUi.dp(c, 70)));
        TextView friends = OUi.glassButton(c, c.getString(R.string.oblivion_friends), 20, v -> open(XboxFriendsActivity.class));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.setMargins(0, OUi.dp(c, 8), 0, 0);
        right.addView(friends, fp);
        LayoutParams rp = new LayoutParams(OUi.dp(c, 190), LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        rp.setMargins(0, m + OUi.dp(c, 56), m, 0);
        addView(right, rp);

        // bottom-right: Resources + Settings
        LinearLayout br = new LinearLayout(c);
        br.setOrientation(LinearLayout.HORIZONTAL);
        TextView res = OUi.glassButton(c, c.getString(R.string.oblivion_resources), 19, v -> open(ResourcesActivity.class));
        TextView set = OUi.glassButton(c, c.getString(R.string.settings), 19, v -> open(SettingsHubActivity.class));
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(OUi.dp(c, 130), LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(OUi.dp(c, 120), LinearLayout.LayoutParams.WRAP_CONTENT);
        b2.setMargins(OUi.dp(c, 8), 0, 0, 0);
        br.addView(res, b1);
        br.addView(set, b2);
        LayoutParams brr = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        brr.setMargins(0, 0, m, m);
        addView(br, brr);

        // bottom-left: version + LAUNCH
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        versionView = new TextView(c);
        versionView.setGravity(Gravity.CENTER);
        versionView.setTextSize(15);
        versionView.setTypeface(OUi.heavy());
        versionView.setTextColor(0xFFFFFFFF);
        versionView.setSingleLine(true);
        GradientDrawable vb = OUi.rounded(OblivionTheme.info, 0, 6, c);
        versionView.setBackground(vb);
        versionView.setPadding(OUi.dp(c, 10), OUi.dp(c, 3), OUi.dp(c, 10), OUi.dp(c, 3));
        left.addView(versionView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView launch = new TextView(c);
        launch.setText(R.string.oblivion_launch);
        launch.setGravity(Gravity.CENTER);
        launch.setTextSize(30);
        launch.setTypeface(OUi.heavy());
        launch.setTextColor(0xFFFFFFFF);
        launch.setShadowLayer(OUi.dp(c, 2), 0, OUi.dp(c, 2), 0x66000000);
        launch.setBackground(OUi.rounded(OblivionTheme.launch, 0, 6, c));
        launch.setClickable(true);
        OUi.pressable(launch);
        launch.setOnClickListener(v -> {
            if (OblivionSettings.get(getContext(), OblivionSettings.AUTO_APPLY_SKIN)) {
                try {
                    SkinApply.apply(getContext());
                } catch (Exception ignored) {
                }
            }
            Achievements.increment(getContext(), Achievements.LAUNCHES);
            host.launch();
        });
        row.addView(launch, new LinearLayout.LayoutParams(0, OUi.dp(c, 58), 1f));
        TextView arrow = new TextView(c);
        arrow.setText("▼");
        arrow.setGravity(Gravity.CENTER);
        arrow.setTextSize(22);
        arrow.setTextColor(0xFFFFFFFF);
        arrow.setBackground(OUi.rounded(OblivionTheme.launchLight, 0, 6, c));
        arrow.setClickable(true);
        OUi.pressable(arrow);
        arrow.setOnClickListener(v -> showVersionMenu(v));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(OUi.dp(c, 58), OUi.dp(c, 58));
        ap.setMargins(OUi.dp(c, 3), 0, 0, 0);
        row.addView(arrow, ap);
        left.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LayoutParams lp = new LayoutParams(OUi.dp(c, 260), LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        lp.setMargins(m, 0, 0, m);
        addView(left, lp);

        refresh();
        loadNews(c);
    }

    private void showVersionMenu(View anchor) {
        Context c = getContext();
        PopupMenu menu = new PopupMenu(c, anchor);
        menu.getMenu().add(0, 1, 0, R.string.oblivion_menu_installed);
        menu.getMenu().add(0, 2, 1, R.string.oblivion_menu_download);
        menu.getMenu().add(0, 3, 2, R.string.oblivion_menu_import);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    host.pickInstalled(anchor);
                    break;
                case 2:
                    open(GameDownloadActivity.class);
                    break;
                default:
                    host.importApk();
                    break;
            }
            return true;
        });
        menu.show();
    }

    private void applyLogo(Context c, ImageView logo) {
        File f = OblivionDlc.asset(c, "logo");
        Bitmap b = f == null ? null : BitmapFactory.decodeFile(f.getAbsolutePath());
        if (b != null) logo.setImageBitmap(b);
        else logo.setImageResource(R.drawable.ic_oblivion_logo);
    }

    private void loadNews(Context c) {
        NewsRepository.loadCached(c, (feed, error) -> setNews(feed));
        NewsRepository.refreshIfStale(c, (feed, error) -> setNews(feed));
    }

    private void setNews(NewsFeed feed) {
        news.clear();
        if (feed != null && feed.items != null) news.addAll(feed.items);
        newsIndex = -1;
        int unread = org.levimc.launcher.core.news.NewsState.getUnreadCount(getContext(), feed);
        newsDot.setVisibility(unread > 0 ? VISIBLE : GONE);
        showNext();
    }

    private void showNext() {
        if (news.isEmpty()) {
            newsTag.setText("");
            newsView.setText(R.string.oblivion_newsworthy);
            return;
        }
        newsIndex = (newsIndex + 1) % news.size();
        NewsItem it = news.get(newsIndex);
        newsView.animate().alpha(0f).translationY(-8f).setDuration(180).withEndAction(() -> {
            newsTag.setText(it.category == null ? "" : it.category);
            newsView.setText(it.title);
            newsView.setTranslationY(8f);
            newsView.animate().alpha(1f).translationY(0f).setDuration(220).start();
        }).start();
    }

    private void openNews() {
        Intent i = new Intent(getContext(), NewsActivity.class);
        if (!news.isEmpty() && newsIndex >= 0 && newsIndex < news.size()) {
            i.putExtra("news_id", news.get(newsIndex).id);
        }
        getContext().startActivity(i);
    }

    public void refresh() {
        Context c = getContext();
        String name = c.getString(R.string.oblivion_player_default);
        String avatar = null;
        try {
            for (MsftAccountStore.MsftAccount a : MsftAccountStore.list(c)) {
                if (a.active) {
                    if (a.xboxGamertag != null && !a.xboxGamertag.isEmpty()) name = a.xboxGamertag;
                    else if (a.minecraftUsername != null && !a.minecraftUsername.isEmpty()) name = a.minecraftUsername;
                    avatar = a.xboxAvatarUrl;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        nameView.setText(name);
        if (avatar != null && !avatar.isEmpty()) {
            Glide.with(c.getApplicationContext()).load(avatar).into(avatarView);
        } else {
            avatarView.setImageDrawable(null);
        }
        OblivionSession.Account acc = OblivionSession.current(c);
        OblivionRole top = acc == null ? null : acc.highestRole();
        String roleText = top == null ? c.getString(R.string.oblivion_role_none) : "@" + top.label(c);
        roleView.setText(roleText);
        int roleColor = top == null ? 0xFF7A7F87 : (top.color & 0x00FFFFFF) | 0xE6000000;
        roleView.setBackground(OUi.rounded(roleColor, 0, 6, c));
        GameVersion v = SkinApply.selectedVersion(c);
        String ver = v == null ? null : (v.versionCode != null && !v.versionCode.isEmpty() ? v.versionCode : v.displayName);
        versionView.setText(ver == null ? c.getString(R.string.oblivion_no_version) : "Minecraft v" + ver);
        Bitmap skin = SkinApply.composed(c);
        if (skin == null) skin = SkinApply.defaultSkin(c);
        boolean slim = SkinTools.isSlim(c);
        PlayerModel custom = OblivionDlc.model(c);
        Bitmap cape = SkinTools.capeOf(c, SkinApply.wornCosmetics(c));
        modelView.setContent(custom != null ? custom : PlayerModel.humanoid(slim, cape != null), skin, cape);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postDelayed(newsTicker, 5000);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(newsTicker);
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) refresh();
    }
}
