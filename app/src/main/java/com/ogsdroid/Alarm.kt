package com.ogsdroid

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import org.json.JSONException
import java.io.IOException

class Alarm : BroadcastReceiver() {

    private fun isYourMove(): Int {
        var count = 0
        try {
            val ogs = Globals.getOGS()
            val notifications = ogs.notifications()
            Globals.putOGS()
            (0..notifications.length()).forEach { i ->
                val obj = notifications.getJSONObject(i)
                if (obj.getString("type") == "yourMove") {
                    count++
                }
            }
        } catch (ex: JSONException) {

        } catch (ex: IOException) {

        }
        return count
    }

    override fun onReceive(context: Context, intent: Intent) {
        //val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        //val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OGS")
        //wl.acquire()

        Toast.makeText(context, "Alarm running!", Toast.LENGTH_SHORT).show()
        println("OGS Alarm: running")

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ni = cm.activeNetworkInfo
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val notifyWifi = pref.getBoolean("pref_notify_wifi", false)

        if (notifyWifi && ni.type != ConnectivityManager.TYPE_WIFI) {
            println("OGS Alarm: active connection is not WiFi, not polling")
            return
        }

        val count = isYourMove()
        if (count > 0) {
            // send notification
            val intent = Intent(context, LoginActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.testnotification)
                    .setContentTitle("OGS")
                    .setContentText(if (count == 1) "It's your move!" else "It's your move in $count games!")
                    .setContentIntent(pi)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(1, builder.build())
        }

        //wl.release()
    }

    fun setAlarm(context: Context) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val notifyTime = pref.getString("pref_notify_time", "0").toLong()

        if (notifyTime > 0) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, Alarm::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, intent, 0)
            println("NJ OGS Alarm: setting alarm for $notifyTime")
            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + notifyTime, notifyTime, pi)
        }
    }

    fun cancelAlarm(context: Context) {
        val intent = Intent(context, Alarm::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, intent, 0)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        println("NJ OGS Alarm: canceling alarm")
        am.cancel(pi)
    }
}

