package org.levimc.launcher.oblivion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class SkinSpriteView extends View {
    private Bitmap sprite;
    private final Paint paint = new Paint();
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SkinSpriteView(Context c) {
        this(c, null);
    }

    public SkinSpriteView(Context c, AttributeSet a) {
        super(c, a);
        paint.setFilterBitmap(false);
        shadow.setColor(0x66000000);
    }

    public void setSkin(Bitmap skin, boolean slim) {
        sprite = skin == null ? null : SkinTools.sprite(skin, slim, 8);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        canvas.drawOval(w * 0.25f, h * 0.9f, w * 0.75f, h * 0.99f, shadow);
        if (sprite == null) return;
        float scale = Math.min(w / (float) sprite.getWidth(), (h * 0.92f) / sprite.getHeight());
        int dw = Math.round(sprite.getWidth() * scale);
        int dh = Math.round(sprite.getHeight() * scale);
        int left = (w - dw) / 2;
        int top = Math.round(h * 0.92f) - dh;
        canvas.drawBitmap(sprite, new Rect(0, 0, sprite.getWidth(), sprite.getHeight()),
                new Rect(left, top, left + dw, top + dh), paint);
    }
}
