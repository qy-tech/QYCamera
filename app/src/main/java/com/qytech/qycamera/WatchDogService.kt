package com.qytech.qycamera

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

class WatchDogService : Service() {
    private val binder = WatchDogBinder()

    private lateinit var context: Context

    inner class WatchDogBinder : Binder() {
        fun getService(): WatchDogService = this@WatchDogService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        context = QYCameraApplication.instance().applicationContext
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand message:  ")
        if (intent?.hasExtra(DpinPlugReceiver.STATE) == true) {
            when (intent.getStringExtra(DpinPlugReceiver.STATE)) {
                DpinPlugReceiver.STATE_DPIN_DETACHED -> {
                    MainActivity.instance()?.finish()
                }
                DpinPlugReceiver.STATE_DPIN_ATTACHED -> {
                    MainActivity.start(context)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

}