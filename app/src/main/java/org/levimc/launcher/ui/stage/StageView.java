package org.levimc.launcher.ui.stage;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;

public final class StageView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "OblivionStage";

    private SurfaceTexture surface;
    private int surfaceWidth;
    private int surfaceHeight;
    private RenderThread thread;
    private String packId = "default";
    private int fps = 30;
    private float downX;
    private float downY;
    private float lastX;
    private boolean dragging;
    private boolean activityResumed;

    public StageView(Context context) {
        super(context);
        setOpaque(false);
        setClickable(true);
        setSurfaceTextureListener(this);
    }

    public void configure(String id, int frameRate) {
        boolean restart;
        synchronized (this) {
            restart = !id.equals(packId);
            packId = id;
            // The stage is intentionally capped at 30 FPS. A 24 FPS mode is
            // available for lower-power devices; avoid an unrestricted 60 FPS
            // GLES loop on phones with limited thermal headroom.
            fps = frameRate >= 30 ? 30 : 24;
            if (!restart && thread != null) thread.setFps(fps);
        }
        if (restart) stopThread();
        updateRenderState();
    }

    public void setHostActive(boolean active) {
        synchronized (this) {
            activityResumed = active;
        }
        updateRenderState();
    }

    public void shutdown() {
        synchronized (this) {
            activityResumed = false;
        }
        stopThread();
    }

    private void updateRenderState() {
        boolean eligible;
        synchronized (this) {
            eligible = activityResumed
                    && surface != null
                    && surfaceWidth > 0
                    && surfaceHeight > 0
                    && getVisibility() == VISIBLE
                    && getWindowVisibility() == VISIBLE
                    && isAttachedToWindow()
                    && isShown();
        }
        if (eligible) startThread();
        else stopThread();
    }

    private synchronized void startThread() {
        if (!activityResumed || surface == null || surfaceWidth <= 0 || surfaceHeight <= 0
                || getVisibility() != VISIBLE || getWindowVisibility() != VISIBLE
                || !isAttachedToWindow() || !isShown()) return;
        if (thread != null) {
            if (thread.isAlive()) return;
            thread = null;
        }
        thread = new RenderThread(this, getContext().getApplicationContext(), surface, packId, fps);
        thread.setSize(surfaceWidth, surfaceHeight);
        thread.start();
        Log.i(TAG, "stage renderer started at " + fps + " FPS");
    }

    private RenderThread requestThreadStop() {
        RenderThread t;
        synchronized (this) {
            t = thread;
            if (t != null) {
                t.finish();
                t.interrupt();
            }
        }
        if (t != null) Log.i(TAG, "stage renderer shutdown requested");
        return t;
    }

    private void stopThread() {
        // Do not block Activity.onPause/onStop waiting for EGL teardown. The
        // render thread releases its GL objects and notifies this view in finally.
        requestThreadStop();
    }

    private void stopThreadAndWait() {
        // SurfaceTexture destruction must not race EGL surface teardown. This
        // callback is the one lifecycle boundary where a short join is required.
        RenderThread t = requestThreadStop();
        if (t == null) return;
        try {
            t.join(1800);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (t.isAlive()) {
            Log.w(TAG, "EGL thread did not finish before SurfaceTexture destruction");
        }
    }

    private void onRenderThreadStopped(RenderThread stopped) {
        synchronized (this) {
            if (thread == stopped) thread = null;
        }
        // Only restart after an explicit lifecycle/configuration stop. A renderer
        // that failed on its own must not enter an automatic crash/restart loop.
        if (stopped.wasRequestedToStop() && isAttachedToWindow()) {
            post(this::updateRenderState);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateRenderState();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopThreadAndWait();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(android.view.View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateRenderState();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
        synchronized (this) {
            surface = st;
            surfaceWidth = width;
            surfaceHeight = height;
        }
        updateRenderState();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
        synchronized (this) {
            surfaceWidth = width;
            surfaceHeight = height;
            if (thread != null) thread.setSize(width, height);
        }
        updateRenderState();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        stopThreadAndWait();
        synchronized (this) {
            surface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture st) {
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateRenderState();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int w = getWidth();
        int h = getHeight();
        RenderThread t = thread;
        if (w == 0 || h == 0 || t == null) return false;

        float x = e.getX();
        float y = e.getY();
        float nx = x / w * 2f - 1f;
        float ny = -(y / h * 2f - 1f);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = x;
                downY = y;
                dragging = false;
                t.look(nx, ny);
                return true;
            case MotionEvent.ACTION_MOVE:
                float totalX = x - downX;
                float totalY = y - downY;
                if (!dragging && (Math.abs(totalX) > 8f || Math.abs(totalY) > 8f)) {
                    dragging = true;
                }
                if (dragging) {
                    // Horizontal swipes rotate the character. Rotation is retained
                    // after release and softened by the animator's spring response.
                    t.rotateBy((x - lastX) * 0.16f);
                    t.look(0f, 0f);
                } else {
                    t.look(nx, ny);
                }
                lastX = x;
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging && Math.abs(x - downX) < 24f && Math.abs(y - downY) < 24f) {
                    t.react();
                }
                t.look(0f, 0f);
                dragging = false;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                t.look(0f, 0f);
                dragging = false;
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent e) {
        RenderThread t = thread;
        int w = getWidth();
        int h = getHeight();
        if (t != null && w > 0 && h > 0 && e.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
            t.look(e.getX() / w * 2f - 1f, -(e.getY() / h * 2f - 1f));
            return true;
        }
        return super.onGenericMotionEvent(e);
    }

    private static final class RenderThread extends Thread {
        private final Context context;
        private final SurfaceTexture texture;
        private final String packId;
        private volatile boolean running = true;
        private volatile int width;
        private volatile int height;
        private volatile int frameRate;
        private volatile StageAnimator animator;
        private volatile float pendingX;
        private volatile float pendingY;
        private volatile boolean pendingReact;

        private final StageView owner;

        RenderThread(StageView owner, Context context, SurfaceTexture texture, String packId, int frameRate) {
            super("OblivionStage");
            this.owner = owner;
            this.context = context;
            this.texture = texture;
            this.packId = packId;
            this.frameRate = frameRate;
        }

        void setSize(int w, int h) {
            width = w;
            height = h;
        }

        void setFps(int fps) {
            frameRate = fps;
        }

        void rotateBy(float degrees) {
            StageAnimator a = animator;
            if (a != null) a.rotateBy(degrees);
        }

        void look(float x, float y) {
            pendingX = x;
            pendingY = y;
            StageAnimator a = animator;
            if (a != null) a.setLook(x, y);
        }

        void react() {
            pendingReact = true;
            StageAnimator a = animator;
            if (a != null) a.react();
        }

        void finish() {
            running = false;
        }

        boolean wasRequestedToStop() {
            return !running;
        }

        private static EGLConfig choose(EGLDisplay display, int samples) {
            int[] attrs = new int[]{
                    EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_SAMPLES, samples,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];
            if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) || count[0] <= 0) return null;
            return configs[0];
        }

        @Override
        public void run() {
            EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
            EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
            StageRenderer renderer = null;
            try {
                if (display == EGL14.EGL_NO_DISPLAY) return;
                int[] version = new int[2];
                if (!EGL14.eglInitialize(display, version, 0, version, 1)) return;
                // StageRenderer supersamples into an offscreen RGBA8 framebuffer
                // and resolves it to this window surface. Keep this surface single-sample
                // so the GLES blit has matching sample counts on all ES 3 devices.
                EGLConfig config = choose(display, 0);
                if (config != null) {
                    int[] samples = new int[1];
                    EGL14.eglGetConfigAttrib(display, config, EGL14.EGL_SAMPLES, samples, 0);
                    Log.i(TAG, "stage EGL samples=" + samples[0] + ", surface=" + width + "x" + height);
                }
                if (config == null) return;
                eglContext = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
                        new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE}, 0);
                if (eglContext == EGL14.EGL_NO_CONTEXT) return;
                eglSurface = EGL14.eglCreateWindowSurface(display, config, texture, new int[]{EGL14.EGL_NONE}, 0);
                if (eglSurface == EGL14.EGL_NO_SURFACE) return;
                if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) return;

                if (!running) return;
                ModelPack pack = ModelPack.open(context, packId);
                if (!running) return;
                ObmModel model = ObmModel.parse(ObmModel.readAll(pack.source.open(pack.meshFile)));
                if (!running) return;
                renderer = new StageRenderer(model, pack);
                if (!running) return;
                renderer.init();
                if (!running) return;
                StageAnimator anim = new StageAnimator(model, pack);
                anim.setLook(pendingX, pendingY);
                if (pendingReact) anim.react();
                animator = anim;

                long last = System.nanoTime();
                while (running) {
                    int frameWidth = width;
                    int frameHeight = height;
                    if (frameWidth <= 0 || frameHeight <= 0) {
                        Thread.sleep(100);
                        last = System.nanoTime();
                        continue;
                    }
                    long frameStart = System.nanoTime();
                    float dt = Math.min(0.05f, Math.max(0.001f, (frameStart - last) / 1.0e9f));
                    last = frameStart;
                    anim.update(dt);
                    renderer.draw(anim, frameWidth, frameHeight);
                    if (!EGL14.eglSwapBuffers(display, eglSurface)) break;

                    // Always yield, including on slow frames. The old loop could
                    // run continuously when rendering exceeded its target period,
                    // increasing CPU/GPU load exactly when the device struggled.
                    long framePeriodNs = 1_000_000_000L / Math.max(1, Math.min(30, frameRate));
                    long remainingNs = framePeriodNs - (System.nanoTime() - frameStart);
                    if (remainingNs > 0L) {
                        long sleepMs = remainingNs / 1_000_000L;
                        int sleepNs = (int) (remainingNs % 1_000_000L);
                        Thread.sleep(sleepMs, sleepNs);
                    } else {
                        Thread.sleep(3L);
                    }
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                Log.w(TAG, "stage renderer stopped: " + t);
            } finally {
                animator = null;
                try {
                    if (renderer != null) renderer.release();
                } catch (Throwable ignored) {
                }
                if (display != EGL14.EGL_NO_DISPLAY) {
                    try {
                        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                        if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface);
                        if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext);
                        EGL14.eglTerminate(display);
                    } catch (Throwable ignored) {
                    }
                }
                Log.i(TAG, "stage EGL thread released");
                owner.onRenderThreadStopped(this);
            }
        }
    }
}
