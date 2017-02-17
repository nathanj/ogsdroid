package com.ogs

import android.util.Log
import io.socket.client.Socket
import org.json.JSONObject

class NotificationConnection(
        private val socket: Socket,
        private val userId: Int,
        private val auth: String,
        private val callbacks: NotificationConnectionCallbacks
) {
    interface NotificationConnectionCallbacks {
        fun notification(obj: JSONObject)
    }

    init {
        socket.on("active_game") { args ->
            val obj = args[0] as JSONObject
            Log.d(TAG, "got notification = " + obj)
            callbacks.notification(obj)
        }

        socket.emit("notification/connect", createJsonObject {
            put("player_id", userId)
            put("auth", auth)
        })
    }

    fun disconnect() {
        socket.emit("notification/disconnect")
        socket.off("active_game")
    }

    companion object {
        private val TAG = "NotificationConnection"
    }
}
