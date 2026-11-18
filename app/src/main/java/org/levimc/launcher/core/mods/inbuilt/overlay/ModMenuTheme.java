package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.levimc.launcher.R;

final class ModMenuTheme {
    static final String[] PRESETS = {"Mujica", "Mask", "Oblivion", "Crucifix", "Requiem", "Silver", "Vesper", "Gilded", "Ashen"};
    static final String[] PRESET_IDS = {"mujica", "mask", "oblivion", "crucifix", "requiem", "silver", "vesper", "gilded", "ashen"};
    private static final int[] PRESET_NAMES = {R.string.mm_palette_graphite, R.string.mm_palette_midnight,
            R.string.mm_palette_ocean, R.string.mm_palette_forest, R.string.mm_palette_amethyst,
            R.string.mm_palette_rose, R.string.mm_palette_ember, R.string.mm_palette_sandstone,
            R.string.mm_palette_monokai};
    private static final int[][] DARK_COLORS = {
            {0xFF0D0B14, 0xFF08070E, 0xFF15121F, 0xFF211C32, 0xFF3A3256, 0xFFF1ECF8, 0xFFA59CBD, 0xFF9B7BE0},
            {0xFF0B0B0E, 0xFF060608, 0xFF141418, 0xFF1F1F26, 0xFF34343E, 0xFFF4F4F7, 0xFF9C9CA8, 0xFFD6D6E0},
            {0xFF0E0608, 0xFF090305, 0xFF1A0A0E, 0xFF2A1118, 0xFF4A1E2B, 0xFFFBEDEF, 0xFFB99AA1, 0xFFE0405F},
            {0xFF100E09, 0xFF0A0906, 0xFF1B1810, 0xFF2B2616, 0xFF4B4226, 0xFFFAF3E0, 0xFFB9AD8A, 0xFFD4AF5A},
            {0xFF12081A, 0xFF0B0511, 0xFF1F0F2B, 0xFF301846, 0xFF54307A, 0xFFF7EAFF, 0xFFBA9ACF, 0xFFC26BF0},
            {0xFF0F1114, 0xFF090B0D, 0xFF181B20, 0xFF242830, 0xFF3C424D, 0xFFEEF1F6, 0xFF9AA2B0, 0xFFB8C2D6},
            {0xFF0A0A18, 0xFF060610, 0xFF12122A, 0xFF1C1C40, 0xFF34346A, 0xFFEBEBFF, 0xFF9C9CCB, 0xFF7C84F2},
            {0xFF14100A, 0xFF0D0A06, 0xFF221A0E, 0xFF332815, 0xFF574526, 0xFFFFF5DD, 0xFFC4B28A, 0xFFE8B84A},
            {0xFF121212, 0xFF0A0A0A, 0xFF1C1C1C, 0xFF272727, 0xFF3F3F3F, 0xFFE9E9E9, 0xFF8F8F8F, 0xFFBDBDBD}
    };
    private static final int[][] LIGHT_COLORS = {
            {0xFFF3F0FA, 0xFFE9E4F5, 0xFFFFFFFF, 0xFFE4DDF5, 0xFFB9ADD6, 0xFF241A3A, 0xFF6F6488, 0xFF6C47C2},
            {0xFFF1F1F3, 0xFFE6E6EA, 0xFFFFFFFF, 0xFFE3E3E8, 0xFFB8B8C2, 0xFF1A1A20, 0xFF666672, 0xFF4B4B58},
            {0xFFFBF0F2, 0xFFF4E1E5, 0xFFFFFFFF, 0xFFF3D9DF, 0xFFD3A4B0, 0xFF3A0F1A, 0xFF8A5663, 0xFFB3173A},
            {0xFFFAF5E8, 0xFFF1E9D2, 0xFFFFFFFF, 0xFFEFE3C0, 0xFFCDB985, 0xFF3A2E10, 0xFF7E6E45, 0xFF8F6B10},
            {0xFFF8EFFC, 0xFFEEDFF7, 0xFFFFFFFF, 0xFFEBD6F7, 0xFFC7A1DC, 0xFF3A1250, 0xFF7D5A93, 0xFF8E34BC},
            {0xFFF1F3F6, 0xFFE4E8EE, 0xFFFFFFFF, 0xFFDFE4EC, 0xFFB2BBC8, 0xFF1C222C, 0xFF5E6879, 0xFF4A5C7D},
            {0xFFF0F0FC, 0xFFE2E2F6, 0xFFFFFFFF, 0xFFDADAF4, 0xFFA9A9D6, 0xFF1E1E48, 0xFF62629A, 0xFF4A52C8},
            {0xFFFBF6E6, 0xFFF2EACB, 0xFFFFFFFF, 0xFFF0E4B8, 0xFFD2BE7E, 0xFF3D3010, 0xFF7E6E3E, 0xFF9A7208},
            {0xFFF2F2F2, 0xFFE6E6E6, 0xFFFFFFFF, 0xFFE2E2E2, 0xFFB5B5B5, 0xFF1A1A1A, 0xFF666666, 0xFF555555}
    };
    final boolean dark;
    final int accent;
    final int primary;
    final int surface;
    final int sidebar;
    final int raised;
    final int selected;
    final int buttonColor;
    final int text;
    final int secondary;
    final int outline;
    final int radius;
    final int corner;
    private final Context context;

    ModMenuTheme(Context context) {
        this.context = context;
        ModMenuPreferences prefs = new ModMenuPreferences(context);
        int night = (prefs.mode() == 0 ? android.content.res.Resources.getSystem().getConfiguration()
                : context.getResources().getConfiguration()).uiMode & Configuration.UI_MODE_NIGHT_MASK;
        dark = prefs.mode() == 1 || prefs.mode() == 0 && night == Configuration.UI_MODE_NIGHT_YES;
        int index = prefs.palette();
        int[] palette = colors(index, dark);
        surface = palette[0];
        sidebar = palette[1];
        raised = palette[2];
        selected = palette[3];
        buttonColor = palette[3];
        outline = palette[4];
        text = palette[5];
        secondary = palette[6];
        int seed = prefs.accentSource() == 1 ? prefs.customAccent() : palette[7];
        primary = readable(seed, Color.WHITE, 4.5, Color.BLACK);
        accent = readable(seed, raised, 3.0, dark ? Color.WHITE : Color.BLACK);
        radius = prefs.radius();
        corner = prefs.elementRadius();
    }

    static Context wrap(Context source) {
        if (source instanceof MenuContext) return source;
        ModMenuPreferences prefs = new ModMenuPreferences(source);
        Configuration config = new Configuration(source.getResources().getConfiguration());
        int night = prefs.mode() == 0
                ? android.content.res.Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK
                : prefs.mode() == 1 ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        config.fontScale *= prefs.textScale() / 100f;
        return new MenuContext(source, config, prefs.textScale());
    }

    private static final class MenuContext extends ContextThemeWrapper {
        final int scale;
        MenuContext(Context source, Configuration configuration, int scale) {
            super(source, R.style.Theme_Levi_ModMenu);
            this.scale = scale;
            applyOverrideConfiguration(configuration);
        }
    }

    static float textRatio(Context context) {
        return new ModMenuPreferences(context).textScale() / (float) (context instanceof MenuContext ? ((MenuContext) context).scale : 100);
    }

    static void round(android.graphics.drawable.Drawable drawable, int radius) {
        if (drawable instanceof GradientDrawable) ((GradientDrawable) drawable).setCornerRadius(radius);
        else if (drawable instanceof LayerDrawable) {
            LayerDrawable layers = (LayerDrawable) drawable;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) round(layers.getDrawable(i), radius);
        }
    }

    static int mix(int from, int to, float amount) {
        return Color.rgb(Math.round(Color.red(from) * (1 - amount) + Color.red(to) * amount),
                Math.round(Color.green(from) * (1 - amount) + Color.green(to) * amount),
                Math.round(Color.blue(from) * (1 - amount) + Color.blue(to) * amount));
    }

    private static double luminance(int color) {
        double result = 0;
        double[] weights = {0.2126, 0.7152, 0.0722};
        int[] channels = {Color.red(color), Color.green(color), Color.blue(color)};
        for (int i = 0; i < 3; i++) {
            double value = channels[i] / 255.0;
            result += weights[i] * (value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4));
        }
        return result;
    }

    static int readable(int color, int against, double minimum, int toward) {
        int result = color;
        double base = luminance(against);
        for (int i = 0; i <= 100; i++) {
            double current = luminance(result);
            if ((Math.max(base, current) + 0.05) / (Math.min(base, current) + 0.05) >= minimum) break;
            result = mix(color, toward, i / 100f);
        }
        return result;
    }

    int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    GradientDrawable shape(int color, float radiusDp, boolean border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (border) drawable.setStroke(dp(1), outline);
        return drawable;
    }

    GradientDrawable panel(int opacity) {
        int alpha = Math.round(Math.max(0, Math.min(100, opacity)) * 2.55f);
        return shape((surface & 0xFFFFFF) | alpha << 24, radius, true);
    }

    static int[] colors(int index, boolean dark) {
        return (dark ? DARK_COLORS : LIGHT_COLORS)[ModMenuGeometry.clamp(index, 0, PRESET_IDS.length - 1)];
    }

    static String presetName(Context context, int index) {
        return context.getString(PRESET_NAMES[ModMenuGeometry.clamp(index, 0, PRESET_NAMES.length - 1)]);
    }

    RippleDrawable touch(int color, float radiusDp, boolean border) {
        return new RippleDrawable(ColorStateList.valueOf(dark ? 0x18FFFFFF : 0x13000000),
                shape(color, radiusDp, border), shape(Color.WHITE, radiusDp, false));
    }

    TextView label(CharSequence value, float size, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size * textRatio(context));
        view.setTextColor(bold ? text : secondary);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return view;
    }

    void button(TextView view, boolean filled) {
        view.setTextColor(filled ? Color.WHITE : text);
        view.setBackgroundTintList(null);
        view.setBackground(touch(filled ? primary : buttonColor, corner, !filled));
        view.setPadding(dp(10), dp(5), dp(10), dp(5));
        view.setMinHeight(dp(36));
        view.setMinimumHeight(dp(36));
        view.setTextSize(12 * textRatio(context));
        view.setIncludeFontPadding(false);
        if (view instanceof Button) {
            ((Button) view).setAllCaps(false);
            ((Button) view).setStateListAnimator(null);
            ((Button) view).setMinWidth(0);
            ((Button) view).setMinimumWidth(0);
        }
    }

    void input(EditText input) {
        input.setTextColor(text);
        input.setHintTextColor(secondary);
        input.setBackgroundTintList(null);
        input.setBackground(shape(buttonColor, corner, true));
        input.setPadding(dp(9), dp(5), dp(9), dp(5));
        input.setMinHeight(dp(38));
        input.setMinimumHeight(dp(38));
        input.setTextSize(12 * textRatio(context));
        input.setIncludeFontPadding(false);
    }

    void seek(SeekBar view) {
        view.setProgressTintList(null);
        view.setProgressBackgroundTintList(null);
        view.setThumbTintList(null);
        GradientDrawable track = shape(buttonColor, 3, false);
        GradientDrawable fill = shape(accent, 3, false);
        LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{track,
                new ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL)});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        layers.setLayerHeight(0, dp(3));
        layers.setLayerHeight(1, dp(3));
        layers.setLayerGravity(0, Gravity.CENTER_VERTICAL);
        layers.setLayerGravity(1, Gravity.CENTER_VERTICAL);
        view.setProgressDrawable(layers);
        int range = view.getMax() - view.getMin();
        layers.findDrawableByLayerId(android.R.id.progress).setLevel(range > 0
                ? Math.round((view.getProgress() - view.getMin()) * 10000f / range) : 0);
        GradientDrawable thumb = shape(dark ? 0xFFE1E1E7 : 0xFF555563, 8, false);
        thumb.setSize(dp(16), dp(16));
        view.setThumb(thumb);
        view.setThumbOffset(dp(8));
        view.setSplitTrack(false);
        view.setMinHeight(dp(36));
        view.setMinimumHeight(dp(36));
        view.setPadding(dp(12), 0, dp(12), 0);
    }

    ColorStateList checked(int on, int off) {
        return new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{on, off});
    }

    void toggle(CompoundButton view) {
        view.setTextColor(text);
        view.setTextSize(12 * textRatio(context));
        view.setMinHeight(dp(40));
        view.setMinimumHeight(dp(40));
        if (view instanceof ModMenuToggle) ((ModMenuToggle) view).setPalette(this);
        else view.setButtonTintList(checked(accent, secondary));
    }

    void controls(View view) {
        if (view instanceof EditText) input((EditText) view);
        else if (view instanceof Button && !(view instanceof CompoundButton)) button((TextView) view, false);
        else if (view instanceof CompoundButton) toggle((CompoundButton) view);
        else if (view instanceof SeekBar) seek((SeekBar) view);
        else if (view instanceof TextView) ((TextView) view).setTextColor(text);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) controls(group.getChildAt(i));
        }
    }

    static MaterialAlertDialogBuilder dialog(Context context) {
        return new MaterialAlertDialogBuilder(wrap(context));
    }
}
