package com.soodalbbobgi.app

import android.app.Application
import com.soodalbbobgi.app.core.init.DebugInitializer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SoodalBbobgiApp : Application() {

    @Inject
    lateinit var debugInitializer: DebugInitializer

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            debugInitializer.initialize()
        }
    }
}
