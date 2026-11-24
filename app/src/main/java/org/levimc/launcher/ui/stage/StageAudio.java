package org.levimc.launcher.ui.stage;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Optional per-model sound effects. No audio is loaded unless sounds.json exists. */
final class StageAudio {
    private static final String TAG = "OblivionStage";
    private static final int MAX_SOUND_BYTES = 12 * 1024 * 1024;

    private final SoundPool soundPool;
    private final Map<String, Integer> samples = new HashMap<>();
    private final Set<Integer> readySamples = new HashSet<>();
    private final Map<Integer, File> pendingFiles = new HashMap<>();
    private final float volume;
    private boolean released;

    static StageAudio createIfConfigured(Context context, ModelPack pack) {
        try (InputStream in = pack.source.open("sounds.json")) {
            byte[] bytes = ObmModel.readAll(in);
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            JSONObject sounds = root.optJSONObject("sounds");
            if (sounds == null || sounds.length() == 0) return null;
            return new StageAudio(context, pack, root, sounds);
        } catch (Exception e) {
            // A missing optional manifest is normal and should not stop rendering.
            if (!(e instanceof IOException)) Log.w(TAG, "invalid optional sounds.json", e);
            return null;
        }
    }

    private StageAudio(Context context, ModelPack pack, JSONObject root, JSONObject sounds) {
        volume = clamp((float) root.optDouble("volume", 0.72), 0f, 1f);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(attributes)
                .build();
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            File temp;
            synchronized (StageAudio.this) {
                temp = pendingFiles.remove(sampleId);
                if (!released && status == 0) readySamples.add(sampleId);
            }
            if (temp != null) temp.delete();
        });

        File cacheDir = new File(context.getCacheDir(), "oblivion-stage-audio");
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            Log.w(TAG, "unable to create stage audio cache directory");
            return;
        }
        Iterator<String> keys = sounds.keys();
        while (keys.hasNext()) {
            String event = keys.next();
            String path = sounds.optString(event, "").trim();
            if (path.isEmpty() || !safeRelativePath(path)) continue;
            File temp = null;
            try (InputStream input = pack.source.open(path)) {
                String suffix = suffixOf(path);
                temp = File.createTempFile("stage_sfx_", suffix, cacheDir);
                copyLimited(input, temp);
                int sampleId;
                synchronized (this) {
                    if (released) {
                        temp.delete();
                        return;
                    }
                    // Hold the same monitor as onLoadComplete so an extremely
                    // fast decode cannot race the pending-file registration.
                    sampleId = soundPool.load(temp.getAbsolutePath(), 1);
                    if (sampleId != 0) {
                        samples.put(event, sampleId);
                        pendingFiles.put(sampleId, temp);
                    }
                }
                if (sampleId == 0) temp.delete();
            } catch (Exception e) {
                if (temp != null) temp.delete();
                Log.w(TAG, "optional stage sound could not be loaded: " + event, e);
            }
        }
        if (samples.isEmpty()) {
            soundPool.release();
            released = true;
            Log.i(TAG, "sounds.json found no loadable sound files");
        } else {
            Log.i(TAG, "optional character sounds loading: " + samples.size() + " event(s)");
        }
    }

    void play(String event) {
        if (event == null || event.isEmpty()) return;
        Integer sampleId;
        synchronized (this) {
            if (released) return;
            sampleId = samples.get(event);
            if (sampleId == null || !readySamples.contains(sampleId)) return;
        }
        soundPool.play(sampleId, volume, volume, 1, 0, 1f);
    }

    void release() {
        Map<Integer, File> files;
        synchronized (this) {
            if (released) return;
            released = true;
            files = new HashMap<>(pendingFiles);
            pendingFiles.clear();
            readySamples.clear();
            samples.clear();
        }
        soundPool.release();
        for (File file : files.values()) file.delete();
    }

    private static void copyLimited(InputStream input, File target) throws IOException {
        try (FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_SOUND_BYTES) throw new IOException("sound file exceeds 12 MiB limit");
                output.write(buffer, 0, read);
            }
        }
    }

    private static boolean safeRelativePath(String path) {
        if (path.startsWith("/") || path.indexOf('\\') >= 0) return false;
        String[] parts = path.split("/");
        for (String part : parts) if (part.isEmpty() || "..".equals(part) || ".".equals(part)) return false;
        return true;
    }

    private static String suffixOf(String path) {
        int dot = path.lastIndexOf('.');
        if (dot >= 0 && path.length() - dot <= 6) {
            String suffix = path.substring(dot).toLowerCase();
            if (".ogg".equals(suffix) || ".wav".equals(suffix) || ".mp3".equals(suffix)
                    || ".m4a".equals(suffix)) return suffix;
        }
        return ".audio";
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
