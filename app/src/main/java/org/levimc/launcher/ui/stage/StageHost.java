package org.levimc.launcher.ui.stage;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;

import org.levimc.launcher.R;

public final class StageHost {
    private final Context context;
    private final FrameLayout holder;
    private final StageView stage;
    private View content;
    private ImageView toggle;
    private boolean cardsHidden;

    private StageHost(Context context, FrameLayout holder) {
        this.context = context;
        this.holder = holder;
        this.stage = new StageView(context);
    }

    public static StageHost install(Context context, FrameLayout holder) {
        StageHost host = new StageHost(context, holder);
        holder.addView(host.stage, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        host.stage.configure(StagePrefs.packId(context), StagePrefs.fps(context));
        host.stage.setVisibility(StagePrefs.isEnabled(context) ? View.VISIBLE : View.GONE);
        return host;
    }

    public void finish(View contentView) {
        content = contentView;
        float d = context.getResources().getDisplayMetrics().density;
        toggle = new ImageView(context);
        toggle.setImageResource(R.drawable.ic_stage_eye);
        toggle.setBackgroundResource(R.drawable.bg_stage_toggle);
        int pad = Math.round(9 * d);
        toggle.setPadding(pad, pad, pad, pad);
        toggle.setContentDescription(context.getString(R.string.stage_toggle_cards));
        toggle.setOnClickListener(v -> setCardsHidden(!cardsHidden));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.round(40 * d), Math.round(40 * d));
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.setMargins(0, 0, Math.round(14 * d), Math.round(14 * d));
        holder.addView(toggle, lp);
        refresh();
    }

    private void setCardsHidden(boolean hidden) {
        cardsHidden = hidden;
        if (content == null) return;
        if (hidden) {
            content.animate().alpha(0f).setDuration(260).withEndAction(() -> content.setVisibility(View.INVISIBLE)).start();
        } else {
            content.setVisibility(View.VISIBLE);
            content.animate().alpha(1f).setDuration(260).start();
        }
    }

    public void refresh() {
        boolean enabled = StagePrefs.isEnabled(context);
        stage.configure(StagePrefs.packId(context), StagePrefs.fps(context));
        stage.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (toggle != null) toggle.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled && cardsHidden) setCardsHidden(false);
        if (content != null) applyGlass(content, enabled && StagePrefs.isGlass(context));
    }

    private void applyGlass(View view, boolean glass) {
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            Object tag = card.getTag(R.id.stage_card_original_color);
            if (tag == null) {
                tag = card.getCardBackgroundColor().getDefaultColor();
                card.setTag(R.id.stage_card_original_color, tag);
            }
            int original = (Integer) tag;
            int color = glass ? ColorUtils.setAlphaComponent(original, 168) : original;
            card.setCardBackgroundColor(ColorStateList.valueOf(color));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyGlass(group.getChildAt(i), glass);
        }
    }
}
