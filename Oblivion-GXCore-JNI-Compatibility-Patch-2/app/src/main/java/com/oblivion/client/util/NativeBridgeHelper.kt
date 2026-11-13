package com.oblivion.client.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.content.edit
import com.oblivion.client.R
import com.oblivion.client.core.minecraft.OblivionApplication

@Keep
object NativeBridgeHelper {
    @Volatile
    private var gxcoreLoaded = false

    @JvmStatic
    fun getAppContext(): Context {
        return OblivionApplication.context
    }

    @JvmStatic
    fun ensureGxCoreLoaded(): Boolean {
        if (gxcoreLoaded) return true
        return synchronized(this) {
            if (gxcoreLoaded) {
                true
            } else {
                try {
                    System.loadLibrary("gxcore")
                    gxcoreLoaded = true
                    true
                } catch (error: Throwable) {
                    Log.e("NativeBridgeHelper", "Failed to load libgxcore.so", error)
                    false
                }
            }
        }
    }

    @JvmStatic
    fun scanImage(inputPath: String): Boolean {
        if (!ensureGxCoreLoaded()) return false
        return org.levimc.launcher.util.NativeBridgeHelper.scanImage(inputPath)
    }

    @JvmStatic
    fun rewriteImage(inputPath: String, outputPath: String): Boolean {
        if (!ensureGxCoreLoaded()) return false
        return org.levimc.launcher.util.NativeBridgeHelper.rewriteImage(inputPath, outputPath)
    }

    @JvmStatic
    fun bootstrapGxCore(): Boolean {
        if (!ensureGxCoreLoaded()) return false
        return org.levimc.launcher.util.NativeBridgeHelper.bootstrapGxCore()
    }

    @JvmStatic
    fun loadMinecraftRuntime(libraryPath: String) {
        if (!ensureGxCoreLoaded()) {
            throw UnsatisfiedLinkError("Failed to load gxcore for the Minecraft runtime")
        }
        org.levimc.launcher.util.NativeBridgeHelper.loadMinecraftRuntime(libraryPath)?.let { error ->
            throw UnsatisfiedLinkError(error)
        }
    }

    @JvmStatic
    fun getPrefs(name: String): SharedPreferences {
        return getAppContext().getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getBoolean(prefsName: String, key: String, defaultValue: Boolean = false): Boolean {
        return try {
            getPrefs(prefsName).getBoolean(key, defaultValue)
        } catch (t: Throwable) {
            defaultValue
        }
    }

    @JvmStatic
    fun putBoolean(prefsName: String, key: String, value: Boolean) {
        try {
            getPrefs(prefsName).edit { putBoolean(key, value) }
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun getString(prefsName: String, key: String, defaultValue: String? = null): String? {
        return try {
            getPrefs(prefsName).getString(key, defaultValue)
        } catch (t: Throwable) {
            defaultValue
        }
    }

    @JvmStatic
    fun putString(prefsName: String, key: String, value: String) {
        try {
            getPrefs(prefsName).edit { putString(key, value) }
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun isLauncherManagedLoginEnabled(): Boolean = true

    @JvmStatic
    fun showInvalidLicenseOverlay() {
        val ctx = getAppContext()
        try {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.invalid_license_detected_toast),
                    Toast.LENGTH_LONG
                ).apply {
                    setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
                    show()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
