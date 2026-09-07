package com.flashbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val serviceIntent = Intent(context, FlashService::class.java)

        serviceIntent.action = intent.action

        ContextCompat.startForegroundService(
            context,
            serviceIntent
        )
    }
}
