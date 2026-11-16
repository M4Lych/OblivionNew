package org.levimc.launcher.oblivion;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypted app-private storage (AES-GCM, key in the Android Keystore). Protected packages are kept here
 * and are never written to shared/local storage by the launcher.
 */
public final class SecureVault {
    private static final String ALIAS = "oblivion_vault_key";
    private static final int IV_LEN = 12;

    private SecureVault() {
    }

    private static SecretKey key() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            kg.generateKey();
        }
        return (SecretKey) ks.getKey(ALIAS, null);
    }

    public static byte[] encrypt(byte[] plain) throws GeneralSecurityException, IOException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = c.getIV();
        byte[] ct = c.doFinal(plain);
        byte[] out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ct, 0, out, iv.length, ct.length);
        return out;
    }

    public static byte[] decrypt(byte[] blob) throws GeneralSecurityException, IOException {
        if (blob.length <= IV_LEN) throw new GeneralSecurityException("blob too short");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, blob, 0, IV_LEN));
        return c.doFinal(blob, IV_LEN, blob.length - IV_LEN);
    }

    public static String encryptString(String s) {
        try {
            return Base64.encodeToString(encrypt(s.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decryptString(String b64) {
        try {
            return new String(decrypt(Base64.decode(b64, Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "oblivion/vault");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    private static String safe(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public static void put(Context c, String name, byte[] data) throws IOException, GeneralSecurityException {
        byte[] blob = encrypt(data);
        File f = new File(dir(c), safe(name) + ".vault");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(blob);
        }
    }

    public static byte[] get(Context c, String name) {
        File f = new File(dir(c), safe(name) + ".vault");
        if (!f.isFile()) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return decrypt(bos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean has(Context c, String name) {
        return new File(dir(c), safe(name) + ".vault").isFile();
    }

    public static List<String> list(Context c) {
        List<String> out = new ArrayList<>();
        File[] fs = dir(c).listFiles();
        if (fs == null) return out;
        for (File f : fs) {
            String n = f.getName();
            if (n.endsWith(".vault")) out.add(n.substring(0, n.length() - 6));
        }
        return out;
    }
}
