package com.ogs

import io.socket.client.Socket
import org.json.JSONObject


data class ChatMessage(val username: String, val msg: String)

class GameChatConnection(val ogs: OGS, val socket: Socket, val gameId: Int, val auth: String) {
    interface GameChatConnectionCallback {
        fun chat(msg: ChatMessage)
    }

    var callback: GameChatConnectionCallback? = null

    init {
        socket.emit("chat/join", createJsonObject {
            put("channel", "game-$gameId")
        }.toString())

        socket.on("game/$gameId/chat", {
            val obj = it[0] as JSONObject
            val message = obj.getJSONObject("message")
            val username = message.getString("username")
            val msg = message.getString("body")
            callback?.chat(ChatMessage(username, msg))
        })
    }

    fun sendMessage(player: Player, msg: String) {
        val json = createJsonObject {
            put("auth", auth)
            put("player_id", player.id)
            put("username", player.username)
            put("ranking", player.ranking)
            put("body", msg)
            put("type", "discussion")
            put("game_id", gameId)
            put("is_player", 1)
            put("move_number", 1) // TODO
        }

        socket.emit("game/chat", json.toString())
    }

    fun disconnect() {
        socket.emit("chat/part", createJsonObject {
            put("channel", "game-$gameId")
        }.toString())
    }
}