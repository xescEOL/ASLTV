package com.xesc.asltv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.xesc.asltv.utils.ApkInstaller
import com.xesc.asltv.worker.ListSyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AceStreamTVApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Limpiar todos los APKs descargados al iniciar
        ApkInstaller.cleanOldApks(this, maxAgeDays = 0)
        // Programar sync periódico
        ListSyncWorker.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}