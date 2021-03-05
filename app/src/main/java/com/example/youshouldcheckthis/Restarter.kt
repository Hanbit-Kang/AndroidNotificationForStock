package com.example.youshouldcheckthis

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.startForegroundService

class Restarter : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("Restarter", "Broadcast Listened")
        if (intent != null) {
            Log.e("Action Start",intent.action.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(Intent(context, CheckingService::class.java))
            } else {
                context?.startService(Intent(context, CheckingService::class.java))
            }
        }
    }
}