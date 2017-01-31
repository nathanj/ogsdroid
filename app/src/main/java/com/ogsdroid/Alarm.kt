package com.ogsdroid

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import org.json.JSONException
import java.io.IOException

class Alarm : BroadcastReceiver() {

    private fun isYourMove(): Boolean {
        try {
            val notifications = Globals.ogs.notifications()
            (0..notifications.length()).forEach { i ->
                val obj = notifications.getJSONObject(i)
                if (obj.getString("type") == "yourMove") {
                    return true
                }
            }
        } catch (ex: JSONException) {

        } catch (ex: IOException) {

        }
        return false
    }

    override fun onReceive(context: Context, intent: Intent) {
        //val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        //val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OGS")
        //wl.acquire()

        if (isYourMove()) {
            // send notification

            val intent = Intent(context, LoginActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.stone_1)
                    .setContentTitle("OGS")
                    .setContentText("It's your move!")
                    .setContentIntent(pi)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(1, builder.build())
        }


        //wl.release()
    }

    private fun getTimeInterval(notifyTime: Int): Long {
        return when (notifyTime) {
            1 -> AlarmManager.INTERVAL_FIFTEEN_MINUTES
            2 -> AlarmManager.INTERVAL_HALF_HOUR
            3 -> AlarmManager.INTERVAL_HOUR
            4 -> AlarmManager.INTERVAL_HALF_DAY
            else -> AlarmManager.INTERVAL_DAY
        }
    }

    fun setAlarm(context: Context) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val notify = pref.getBoolean("pref_notify", false)
        val notifyTime = pref.getString("pref_notify_time", "5").toInt()

        if (notify) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, Alarm::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, intent, 0)
            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), getTimeInterval(notifyTime), pi)
        }
    }

    fun cancelAlarm(context: Context) {
        val intent = Intent(context, Alarm::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, intent, 0)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }
}

