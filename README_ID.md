# Oblivion untuk Android

**Client Minecraft Bedrock ARM64 yang menjalankan runtime Minecraft resmi di dalam aplikasi Oblivion.**

Oblivion adalah aplikasi Android turunan dari proyek open-source LeviLauncher Android. Identitas launcher, namespace aplikasi Android, layar utama, dan fitur yang ditampilkan telah disesuaikan untuk proyek ini. Game Minecraft tidak disertakan di dalam APK: Oblivion menggunakan instalasi Minecraft Bedrock berlisensi yang sudah terpasang dari Google Play.

> Oblivion adalah proyek komunitas independen, bukan proyek yang berafiliasi, didukung, atau disponsori Mojang Studios maupun Microsoft.

## Ruang lingkup fitur

Client hanya menampilkan area berikut:

- **Launch** — menjalankan Minecraft melalui `MinecraftActivity` milik Oblivion dalam proses aplikasi yang sama, bukan membuka launcher Minecraft terpisah.
- **OblivionMod** — menelusuri dan mengelola native mod package yang didukung.
- **Content** — impor, ekspor, melihat, dan menghapus world, resource pack, behavior pack, serta skin pack.
- **Microsoft account** — login dan mengelola akun Microsoft/Xbox untuk Minecraft. Akun Minecraft yang aktif wajib tersedia sebelum Launch dapat digunakan.

Oblivion menolak berjalan apabila package Minecraft bukan `com.mojang.minecraftpe` atau Android tidak melaporkan bahwa aplikasi Minecraft berasal dari Google Play. Minecraft sideload/pirated tidak didukung. APK ini tidak mengunduh, mendistribusikan, ataupun menyertakan file game Minecraft.

## Persyaratan

- Android 9.0 (API 28) atau lebih baru.
- Perangkat ARM64 (`arm64-v8a`).
- Minecraft Bedrock Edition resmi dari Google Play sudah terpasang.
- Akun Microsoft yang memiliki akses Minecraft/Xbox dan sudah login melalui Oblivion.
- Ruang penyimpanan yang cukup untuk Minecraft, world, dan pack.

Performa runtime native tetap perlu diuji langsung pada perangkat dan versi Minecraft yang digunakan.

## Build APK melalui GitHub Actions

Workflow `.github/workflows/android.yml` menyiapkan Java 21, Android SDK, Android NDK r28c, dan CMake, mengambil dependensi native pada commit yang dipatok, menjalankan unit test Gradle, lalu membuat APK ARM64 debug dan release ter-minify. CI juga memeriksa package ID, ABI ARM64 saja, alignment segmen native ELF 16 KB beserta kesesuaian offset/alamat, dan checksum SHA-256. APK release yang dihasilkan masih unsigned.

1. Buat repository GitHub baru untuk Oblivion dan unggah isi folder proyek ini ke root repository. Jangan unggah folder `build/`, `.gradle/`, `.cxx/`, `local.properties`, atau `google-services.json` pribadi.
2. Buka tab **Actions**, lalu pilih **Oblivion Android CI**.
3. Tekan **Run workflow**. Workflow juga berjalan otomatis saat ada push atau pull request ke `main`, `master`, atau `develop`.
4. Setelah build berhasil, unduh artifact **Oblivion-Android-ARM64-Debug** untuk pengujian perangkat. Artifact **Oblivion-Android-ARM64-Release-Unsigned** hanya untuk memeriksa build release ter-minify, bukan untuk distribusi produksi.
5. Pasang APK debug pada perangkat Android ARM64 yang sudah memiliki Minecraft resmi dari Google Play. Login Microsoft di Oblivion, lalu jalankan skenario pada `DEVICE_TEST_MATRIX.md`.

### Mengedit dari Android

Ekstrak ZIP dan buka folder proyek melalui Acode. Gunakan GitHub web atau Git dari Termux untuk mengunggah/mendorong perubahan. GitHub Actions melakukan proses build Android di cloud sehingga Android SDK dan NDK tidak perlu dipasang di HP.

## Catatan teknis

- Integrasi runtime Minecraft berada di `app/src/main/java/com/oblivion/client/core/minecraft/`. `MinecraftActivity` mewarisi `com.mojang.minecraftpe.MainActivity`, yang menjadi mekanisme utama untuk menjalankan runtime di dalam aplikasi.
- Pemeriksaan instalasi Google Play dilakukan pada launcher dan `MinecraftLauncher` sebelum game Activity dimulai.
- Microsoft/Xbox authentication juga dipersiapkan saat runtime diinisialisasi.
- Impor content menggunakan Android Storage Access Framework. Ekspor menggunakan pemilih dokumen/folder Android.
- Resource aplikasi dan locale aplikasi diatur menggunakan bahasa Inggris.
- Beberapa ekstensi file internal turunan upstream mungkin tetap dipertahankan sebagai format data lama agar kompatibilitas format tidak rusak. Itu bukan identitas aplikasi atau package Android.

## Status pengujian

Phase 3 menambahkan pengecualian backup untuk data akun, pembatasan Activity utama agar tidak dapat dibuka aplikasi eksternal, batas streaming dan arsip bertingkat saat impor, unit test untuk pembatasan byte, serta verifikasi APK di CI untuk package ID, ABI ARM64, alignment segmen native ELF 16 KB, serta kesesuaian offset/alamat. Pemeriksaan source offline dan self-test APK verifier berhasil.

Build Android Gradle dan runtime perangkat fisik belum terverifikasi di lingkungan ini karena Gradle wrapper gagal mengunduh distribusinya. Jalankan workflow GitHub Actions, lalu selesaikan `DEVICE_TEST_MATRIX.md`. Build yang sukses belum otomatis membuktikan kompatibilitas gameplay maupun import/export untuk semua versi Minecraft.

Lihat `PHASE_3_REPORT.md` untuk status pemeriksaan lengkap dan `RELEASE_CHECKLIST.md` sebelum menerbitkan release bertanda tangan.

## Source dan lisensi upstream

Source aplikasi Android diturunkan dari [LiteLDev/LeviLauncher-Android](https://github.com/LiteLDev/LeviLauncher-Android) yang menggunakan Apache License 2.0. Lisensi dan notice upstream dipertahankan. Modul kontrol Android berisi adaptasi source PojavLauncher dengan lisensi LGPL-3.0-or-later; lisensi dan noticenya ada di folder `oblivion_controls/`. Komponen pihak ketiga utama dicatat dalam `UPSTREAM_NOTICES.md`.
