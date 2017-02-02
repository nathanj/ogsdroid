package com.ogs

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.ogsdroid.Globals
import com.ogsdroid.LoginActivity
import com.ogsdroid.R
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread

class NotificationService : Service() {
    var ogs: OGS = Globals.getOGS()
    var notificationConnection: NotificationConnection? = null
    val moves = ArrayList<String>()

    init {
        println("NotificationService init")
    }

    override fun onBind(p0: Intent?) = null

    override fun onCreate() {
        println("NotificationService.onCreate()")
        Toast.makeText(this, "NotificationService onCreate", Toast.LENGTH_SHORT).show()

        val t = thread(start = true) {

            try {
                val pref = PreferenceManager.getDefaultSharedPreferences(this)
                ogs.accessToken = pref.getString("accessToken", "")

                println("NJ NotificationService getting me")
                ogs.me()

                println("NJ NotificationService getting ui config")
                val config = ogs.uiConfig()
                val auth = config.getString("notification_auth")

                println("NJ NotificationService opening socket")
                ogs.openSocket()
                println("NJ NotificationService opening notification connection")
                notificationConnection = ogs.openNotificationConnection(auth,
                        object : NotificationConnection.NotificationConnectionCallbacks {
                            override fun notification(obj: JSONObject) {
                                try {
                                    println("NJ obj = ${obj}")

                                    if (obj.getString("type") == "delete")
                                        moves.remove(obj.getString("id"))

                                    if (obj.getString("type") == "yourMove")
                                        moves.add(obj.getString("id"))

                                    println("NJ moves.size = ${moves.size}")

                                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    if (moves.size == 0) {
                                        nm.cancel(1)
                                    } else {
                                        val intent = Intent(this@NotificationService, LoginActivity::class.java)
                                        val pi = PendingIntent.getActivity(this@NotificationService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

                                        val builder = NotificationCompat.Builder(this@NotificationService)
                                                .setSmallIcon(R.drawable.testnotification)
                                                .setContentTitle("OGS")
                                                .setContentIntent(pi)

                                        if (moves.size == 1)
                                            builder.setContentText("It's your move!")
                                        else
                                            builder.setContentText("It's your move in ${moves.size} games!")

                                        nm.notify(1, builder.build())

                                        println("NJ in service notification = $obj")
                                    }
                                } catch (ex: JSONException) {
                                    println("bad json obj $obj")
                                    ex.printStackTrace()
                                }
                            }
                        })

                println("NJ NotificationService created and waiting!")

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("NotificationService.onStartCommand(intent=$intent, flags=$flags, startId=$startId)")
        Toast.makeText(this, "NotificationService onStartCommand", Toast.LENGTH_SHORT).show()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        notificationConnection?.disconnect()
        Globals.putOGS()
        println("NotificationService.onDestroy()")
        Toast.makeText(this, "NotificationService onDestroy", Toast.LENGTH_SHORT).show()
    }
}
