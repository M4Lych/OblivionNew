# Oblivion Hosted Runtime — Prototype 0.1

Ini adalah proyek POC mandiri untuk menguji pendekatan **hosted-process** pada client Android. Source ini ditulis khusus untuk Oblivion dan tidak mengambil implementasi Java/C dari proyek referensi. Struktur dasarnya memakai API Android `AppComponentFactory`, `createPackageContext(..., CONTEXT_INCLUDE_CODE)`, class loader Minecraft yang terpasang, dan native Activity forwarder kecil.

> **Penting:** Ini belum menjadi pengganti penuh repository OblivionNew dan belum merupakan APK yang terbukti bisa menjalankan Minecraft. Ini adalah vertical slice untuk menguji jalur Activity + native startup secara terpisah sebelum dipindahkan ke client utama. Tidak ada file Minecraft yang disertakan atau dimodifikasi.

## Yang dibuat

- `OblivionHostedAppComponentFactory`: ketika Android membuat `com.mojang.minecraftpe.MainActivity`, class aktual diambil dari class loader aplikasi Minecraft yang terpasang.
- `OblivionHostedProcess`: membuat package context resmi, memeriksa class `MainActivity`, dan menemukan `libminecraftpe.so` dari `ApplicationInfo.nativeLibraryDir`.
- `HostedActivityLifecycleCallbacks`: memasang Context wrapper saat `onActivityPreCreated` (Android 10+) supaya Activity memperoleh assets/resources/class loader dari package Minecraft, tetapi file yang dapat ditulis tetap berada di sandbox Oblivion.
- `oblivion_minecraftpe_forwarder.c`: mengekspor `ANativeActivity_onCreate`, membuka library Minecraft berdasarkan path yang diberikan Java, lalu meneruskan native Activity startup ke entry point resmi. Tidak ada hook, patch memori, atau modifikasi binary Minecraft.
- Launcher demo memeriksa ABI ARM64 dan install source Google Play sebelum meminta Activity hosted dibuka.

## Batasan awal

1. Target ABI hanya `arm64-v8a`.
2. Minimum Android 10 (API 29), karena adapter Context dipasang melalui callback pre-create.
3. Belum mengintegrasikan Microsoft/Xbox sign-in Oblivion, mod loader, OblivionMod, worlds, RP/BP/skinpack manager, crash recovery, dan UI utama Oblivion.
4. Belum ada bukti runtime bahwa native entry point yang diekspor oleh versi Minecraft pada perangkat target dapat dipanggil seperti ini. Jika library tidak mengekspor `ANativeActivity_onCreate`, forwarder akan menghentikan launch dan menulis alasan ke logcat.
5. Referensi publik untuk pendekatan hosted-process yang serupa menyatakan dirinya POC dan secara eksplisit menguji Minecraft 1.21.51.02 ARM64 saja. Itu bukan jaminan dukungan versi terbaru. Karena itu, catat versi Minecraft perangkat pada setiap pengujian.
6. Reflection ke `ContextWrapper.mBase` adalah titik kompatibilitas Android yang perlu diuji di perangkat. Kode sengaja gagal secara eksplisit jika adaptasi gagal, bukan diam-diam melanjutkan dengan Context yang salah.

## Build

Prasyarat lokal: JDK 17, Android SDK Platform 35, Android NDK 27.0.12077973, CMake 3.22.1, dan Gradle 8.11.1 atau Android Studio yang mendukung AGP 8.9.2.

```bash
gradle :app:assembleDebug --no-daemon
```

APK debug akan berada di `app/build/outputs/apk/debug/`. Workflow GitHub Actions disertakan di `.github/workflows/android.yml`; workflow memasang Android SDK/NDK/CMake lalu mengunggah APK debug sebagai artifact.

Jalankan pemeriksaan source statis:

```bash
python3 scripts/verify_source.py
```

Pemeriksaan source tidak membuktikan bahwa APK bisa dibuat atau Minecraft berhasil diluncurkan.

## Tes perangkat pertama

1. Gunakan perangkat ARM64 Android 10+ dengan Minecraft resmi Google Play sudah terpasang.
2. Pasang APK debug.
3. Buka Oblivion Hosted Runtime dan pastikan versi Minecraft terdeteksi.
4. Tekan `Launch installed Minecraft`.
5. Bila launch gagal, ambil log:

```bash
adb logcat -d -v threadtime | grep -E 'OblivionHosted|OblivionNativeHost|AndroidRuntime|NativeActivity'
```

Hapus token, email, kode login, atau data akun sebelum membagikan log.

## Cara menggabungkan ke OblivionNew

Jangan langsung mengganti jalur launch produksi. Pindahkan paket Java di `app/src/main/java/com/oblivion/client/hosted/`, file C dan CMake ke module app, lalu gabungkan deklarasi AppComponentFactory dan Activity hosted ke `AndroidManifest.xml` repository utama. Setelah bisa melewati native startup di perangkat uji, barulah migrasikan login, storage, input/control overlay, mod loader, dan content manager secara bertahap.

Konfigurasi manifest perlu diperhatikan: AppComponentFactory adalah properti tingkat `<application>`, sedangkan process `:minecraft` dan `android.app.lib_name` adalah properti Activity hosted.

## Integrity

Basis pemeriksaan repository yang dianalisis saat paket ini disusun: `M4Lych/OblivionNew` commit `209e0cd2e576c3f5cf991465a7e444f96d9671fb`.

Tidak ada perubahan yang didorong ke GitHub. Source asli di repository pengguna tidak ditimpa.
