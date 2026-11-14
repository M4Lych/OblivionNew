package com.oblivion.client.core.minecraft

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.oblivion.client.core.crash.CrashReporter
import com.oblivion.client.core.packages.PackageService
import com.oblivion.client.settings.FeatureSettings

class OblivionApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        FeatureSettings.init(applicationContext)
        CrashReporter.init(this)
        val processName = Application.getProcessName()
        if (processName.endsWith(":crash")) return

        PackageService.init(this)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    companion object {
        @JvmStatic
        lateinit var context: Context
            private set

        @JvmStatic
        lateinit var preferences: SharedPreferences
            private set
    }
}
