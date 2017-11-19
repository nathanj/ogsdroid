package com.ogsdroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlarmStarter : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent) {
        println("Autostart running, action=${intent.action}")
        if (intent.action != "android.intent.action.BOOT_COMPLETED")
            return

        println("Starting alarm!")
        val al = Alarm()
        al.setAlarm(context)
    }
}
