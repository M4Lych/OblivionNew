package org.levimc.launcher.oblivion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Renders a PlayerModel (skin + cape) as a slowly swaying 3D figure with plain Canvas maths:
 * every box face is a textured quad, faces are depth-sorted. Drag to rotate, tap for the cosmetics screen.
 */
public class PlayerModelView extends View {
    private static final class Face {
        float depth;
        float[] dst = new float[6];
        int u, v, w, h;
        int shade;
        Bitmap tex;
    }

    private static final float CP = (float) Math.cos(Math.toRadians(9));
    private static final float SP = (float) Math.sin(Math.toRadians(9));
    private PlayerModel model = PlayerModel.humanoid(false, false);
    private Bitmap skin;
    private Bitmap cape;
    private boolean animate = true;
    private float userYaw;
    private float lastX;
    private float downX, downY;
    private long downTime;
    private boolean dragging;
    private Runnable tapListener;
    private final Paint paint = new Paint();
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final LightingColorFilter[] shades = new LightingColorFilter[6];
    private final float[] src = new float[6];
    private final float[] cx = new float[8];
    private final float[] cy = new float[8];
    private final float[] cz = new float[8];

    public PlayerModelView(Context c) {
        this(c, null);
    }

    public PlayerModelView(Context c, AttributeSet a) {
        super(c, a);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        shadow.setColor(0x55000000);
        int[] lv = {255, 235, 205, 205, 175, 150};
        for (int i = 0; i < shades.length; i++) {
            int l = lv[i];
            shades[i] = new LightingColorFilter(0xFF000000 | (l << 16) | (l << 8) | l, 0);
        }
    }

    public void setContent(PlayerModel model, Bitmap skin, Bitmap cape) {
        this.model = model != null ? model : PlayerModel.humanoid(false, cape != null);
        this.skin = skin;
        this.cape = cape;
        invalidate();
    }

    public void setAnimated(boolean on) {
        animate = on;
        invalidate();
    }

    public void setOnModelTapListener(Runnable r) {
        tapListener = r;
        setClickable(r != null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = e.getY();
                downTime = e.getEventTime();
                dragging = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(e.getX() - downX) > 12) dragging = true;
                userYaw += (e.getX() - lastX) * 0.6f;
                lastX = e.getX();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging && e.getEventTime() - downTime < 350
                        && Math.abs(e.getY() - downY) < 12 && tapListener != null) {
                    tapListener.run();
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        double t = System.nanoTime() / 1e9;
        if (!dragging) userYaw *= 0.97f;
        float idle = animate ? (float) Math.sin(t * 0.7) * 16f : 0f;
        float yaw = (float) Math.toRadians(userYaw + idle);
        float swing = animate ? (float) Math.sin(t * 1.6) * 4f : 0f;
        float bob = animate ? (float) Math.sin(t * 3.2) * 0.35f : 0f;
        float scale = (h * 0.92f) / 36f;
        float cxs = w / 2f;
        float baseY = h * 0.93f;

        canvas.drawOval(cxs - 9 * scale, baseY - 1.2f * scale, cxs + 9 * scale, baseY + 1.6f * scale, shadow);
        if (skin == null) return;

        List<Face> faces = new ArrayList<>(96);
        for (PlayerModel.Box b : model.boxes) {
            if (b.cape && cape == null) continue;
            Bitmap tex = b.cape ? cape : (model.texture != null ? model.texture : skin);
            addBox(faces, b, tex, yaw, swing, bob, scale, cxs, baseY);
        }
        Collections.sort(faces, new Comparator<Face>() {
            @Override
            public int compare(Face a, Face b) {
                return Float.compare(a.depth, b.depth);
            }
        });
        for (Face f : faces) {
            src[0] = f.u; src[1] = f.v;
            src[2] = f.u + f.w; src[3] = f.v;
            src[4] = f.u; src[5] = f.v + f.h;
            matrix.setPolyToPoly(src, 0, f.dst, 0, 3);
            paint.setColorFilter(shades[f.shade]);
            canvas.save();
            canvas.concat(matrix);
            canvas.clipRect(f.u, f.v, f.u + f.w, f.v + f.h);
            canvas.drawBitmap(f.tex, 0, 0, paint);
            canvas.restore();
        }
        if (animate) postInvalidateOnAnimation();
    }

    private void addBox(List<Face> out, PlayerModel.Box b, Bitmap tex, float yaw, float swing, float bob,
                        float scale, float cxs, float baseY) {
        float i = b.inflate;
        float x0 = b.x - i, x1 = b.x + b.w + i;
        float y0 = b.y - i, y1 = b.y + b.h + i;
        float z0 = b.z - i, z1 = b.z + b.d + i;
        float angle = 0f;
        float pivotY = 0f;
        switch (b.group) {
            case "rarm": angle = -swing; pivotY = 22f; break;
            case "larm": angle = swing; pivotY = 22f; break;
            case "rleg": angle = swing; pivotY = 12f; break;
            case "lleg": angle = -swing; pivotY = 12f; break;
            case "cape": angle = 7f + swing * 0.6f; pivotY = 24f; break;
            default: break;
        }
        double a = Math.toRadians(angle);
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        float cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw);
        float[] px = {x0, x1, x0, x1, x0, x1, x0, x1};
        float[] py = {y0, y0, y1, y1, y0, y0, y1, y1};
        float[] pz = {z0, z0, z0, z0, z1, z1, z1, z1};
        for (int k = 0; k < 8; k++) {
            float y = py[k] - pivotY;
            float z = pz[k];
            float ny = y * ca - z * sa + pivotY;
            float nz = y * sa + z * ca;
            float x = px[k];
            float wx = x * cyw + nz * syw;
            float wz = -x * syw + nz * cyw;
            float py2 = ny * CP - wz * SP;
            float pz2 = ny * SP + wz * CP;
            cx[k] = cxs + wx * scale;
            cy[k] = baseY - (py2 + bob) * scale;
            cz[k] = pz2;
        }
        int u = b.u, v = b.v;
        int w = Math.round(b.w), h = Math.round(b.h), d = Math.round(b.d);
        // corner indexes: bit0 = x1, bit1 = y1, bit2 = z1
        // FRONT (+z)
        if (b.cape) {
            face(out, tex, new int[]{6, 7, 4, 5}, 12, 1, 10, 16, 2);
            face(out, tex, new int[]{3, 2, 1, 0}, 1, 1, 10, 16, 3);
            face(out, tex, new int[]{2, 6, 0, 4}, 0, 1, 1, 16, 4);
            face(out, tex, new int[]{7, 3, 5, 1}, 11, 1, 1, 16, 4);
            return;
        }
        face(out, tex, new int[]{6, 7, 4, 5}, u + d, v + d, w, h, 1);
        face(out, tex, new int[]{3, 2, 1, 0}, u + 2 * d + w, v + d, w, h, 4);
        face(out, tex, new int[]{2, 6, 0, 4}, u, v + d, d, h, 2);
        face(out, tex, new int[]{7, 3, 5, 1}, u + d + w, v + d, d, h, 3);
        face(out, tex, new int[]{2, 3, 6, 7}, u + d, v, w, d, 0);
        face(out, tex, new int[]{4, 5, 0, 1}, u + d + w, v, w, d, 5);
    }

    /** c = {TL, TR, BL, BR} corner indexes as seen from outside the face. */
    private void face(List<Face> out, Bitmap tex, int[] c, int u, int v, int w, int h, int shade) {
        if (w <= 0 || h <= 0) return;
        float tlx = cx[c[0]], tly = cy[c[0]], trx = cx[c[1]], try_ = cy[c[1]];
        float blx = cx[c[2]], bly = cy[c[2]];
        float cross = (trx - tlx) * (bly - tly) - (try_ - tly) * (blx - tlx);
        if (cross <= 0) return;
        float brx = cx[c[3]], bry = cy[c[3]];
        float mx = (tlx + trx + blx + brx) / 4f, my = (tly + try_ + bly + bry) / 4f;
        Face f = new Face();
        f.tex = tex;
        f.u = u; f.v = v; f.w = w; f.h = h;
        f.shade = shade;
        f.depth = (cz[c[0]] + cz[c[1]] + cz[c[2]] + cz[c[3]]) / 4f;
        f.dst[0] = grow(tlx, mx); f.dst[1] = grow(tly, my);
        f.dst[2] = grow(trx, mx); f.dst[3] = grow(try_, my);
        f.dst[4] = grow(blx, mx); f.dst[5] = grow(bly, my);
        out.add(f);
    }

    private static float grow(float p, float centre) {
        return p + (p >= centre ? 0.6f : -0.6f);
    }

    public Bitmap snapshotSkin() {
        return skin;
    }
}
