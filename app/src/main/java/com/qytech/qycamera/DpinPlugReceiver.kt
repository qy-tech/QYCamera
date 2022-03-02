package com.qytech.qycamera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class DpinPlugReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DPIN_PLUG = "android.intent.action.DPIN_PLUG"
        const val STATE = "state"
        const val STATE_DPIN_DETACHED = "0"
        const val STATE_DPIN_ATTACHED = "1"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive dpin broadCast:  ${intent.action}")

        when {
            Intent.ACTION_BOOT_COMPLETED == intent.action -> {
                val watchDogIntent = Intent(context, WatchDogService::class.java)
                context.startService(watchDogIntent)
            }
            ACTION_DPIN_PLUG == intent.action -> {
                Timber.d("dpin state:  ${intent.getStringExtra(STATE)}")
                val watchDogIntent = Intent(context, WatchDogService::class.java)
                watchDogIntent.putExtra(STATE, intent.getStringExtra(STATE))
                context.startService(watchDogIntent)
            }
        }
    }
}