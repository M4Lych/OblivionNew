package org.levimc.launcher.oblivion;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Client for the Oblivion server (Discord login, roles, claim codes, packages). See /server in the repo. */
public final class OblivionApi {
    public interface JsonCb {
        void ok(JSONObject json);

        void fail(String message);
    }

    public interface BytesCb {
        void ok(byte[] data);

        void fail(String message);
    }

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build();
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OblivionApi() {
    }

    public static void get(Context c, String path, String token, JsonCb cb) {
        Context app = c.getApplicationContext();
        IO.execute(() -> {
            try {
                Request.Builder rb = new Request.Builder().url(OblivionConfig.apiBase(app) + path);
                if (token != null) rb.header("Authorization", "Bearer " + token);
                deliverJson(HTTP.newCall(rb.build()).execute(), cb);
            } catch (Exception e) {
                postFail(cb, e.getMessage());
            }
        });
    }

    public static void postJson(Context c, String path, String token, JSONObject body, JsonCb cb) {
        Context app = c.getApplicationContext();
        IO.execute(() -> {
            try {
                Request.Builder rb = new Request.Builder().url(OblivionConfig.apiBase(app) + path)
                        .post(RequestBody.create(body.toString(), JSON));
                if (token != null) rb.header("Authorization", "Bearer " + token);
                deliverJson(HTTP.newCall(rb.build()).execute(), cb);
            } catch (Exception e) {
                postFail(cb, e.getMessage());
            }
        });
    }

    public static void download(Context c, String path, String token, BytesCb cb) {
        Context app = c.getApplicationContext();
        IO.execute(() -> {
            try {
                Request.Builder rb = new Request.Builder().url(OblivionConfig.apiBase(app) + path);
                if (token != null) rb.header("Authorization", "Bearer " + token);
                try (Response r = HTTP.newCall(rb.build()).execute()) {
                    if (!r.isSuccessful() || r.body() == null) {
                        MAIN.post(() -> cb.fail("HTTP " + r.code()));
                        return;
                    }
                    byte[] data = r.body().bytes();
                    MAIN.post(() -> cb.ok(data));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.fail(String.valueOf(e.getMessage())));
            }
        });
    }

    private static void deliverJson(Response r, JsonCb cb) throws IOException {
        try (Response resp = r) {
            String text = resp.body() != null ? resp.body().string() : "";
            JSONObject o;
            try {
                o = new JSONObject(text.isEmpty() ? "{}" : text);
            } catch (Exception e) {
                postFail(cb, "HTTP " + resp.code());
                return;
            }
            if (!resp.isSuccessful()) {
                postFail(cb, o.optString("error", "HTTP " + resp.code()));
                return;
            }
            MAIN.post(() -> cb.ok(o));
        }
    }

    private static void postFail(JsonCb cb, String message) {
        MAIN.post(() -> cb.fail(message == null ? "error" : message));
    }

    /**
     * Server responses are {"payload":"<json text>","sig":"<base64 ECDSA-SHA256>"}.
     * Returns the payload JSON, or null if the signature is invalid. If no public key is configured the
     * payload is returned but flagged unverified through {@code verifiedOut[0]=false}.
     */
    public static JSONObject openEnvelope(Context c, JSONObject env, boolean[] verifiedOut) {
        try {
            String payload = env.getString("payload");
            String sig = env.optString("sig", "");
            String pub = OblivionConfig.serverPublicKey(c);
            boolean verified = false;
            if (!pub.isEmpty()) {
                PublicKey key = KeyFactory.getInstance("EC")
                        .generatePublic(new X509EncodedKeySpec(Base64.decode(pub, Base64.DEFAULT)));
                Signature s = Signature.getInstance("SHA256withECDSA");
                s.initVerify(key);
                s.update(payload.getBytes(StandardCharsets.UTF_8));
                verified = s.verify(Base64.decode(sig, Base64.DEFAULT));
                if (!verified) return null;
            }
            if (verifiedOut != null && verifiedOut.length > 0) verifiedOut[0] = verified;
            return new JSONObject(payload);
        } catch (Exception e) {
            return null;
        }
    }
}
