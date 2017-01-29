package com.ogs

import android.util.Log
import io.socket.client.Socket
import org.json.JSONObject
import java.util.*

data class ChatMessage(val username: String, val msg: String, val date: Long) {
    override fun toString(): String {
        return "$username: $msg"
    }
}

class GameConnection internal constructor(ogs: OGS, private val socket: Socket, private val gameId: Int, private val userId: Int) {
    interface OGSGameConnectionCallbacks {
        fun move(x: Int, y: Int)
        fun clock(clock: JSONObject)
        fun phase(p: String)
        fun removedStones(obj: JSONObject)
        fun removedStonesAccepted(obj: JSONObject)
        fun error(msg: String)
        fun gamedata(obj: JSONObject)
        fun chat(msg: ChatMessage)
    }

    interface OGSGameConnectionResetCallback {
        fun reset()
    }

    private var gameAuth: String? = null
    private var chatAuth: String? = null
    private var callbacks: OGSGameConnectionCallbacks? = null
    private var resetCallback: OGSGameConnectionResetCallback? = null

    fun setCallbacks(callbacks: OGSGameConnectionCallbacks) {
        this.callbacks = callbacks
    }

    fun setResetCallback(callback: OGSGameConnectionResetCallback) {
        this.resetCallback = callback
    }

    private fun emit(event: String, msg: Any) {
        Log.d(TAG, "emit event:$event msg:$msg")
        socket.emit(event, msg)
    }

    init {
        socket.on("game/$gameId/clock") { args ->
            val obj = args[0] as JSONObject
            Log.d(TAG, "got clock = " + obj)
            clock(obj)
        }.on("game/$gameId/gamedata") { args ->
            val obj = args[0] as JSONObject
            Log.d(TAG, "got gamedata = " + obj)
            gamedata(obj)
        }.on("game/$gameId/move") { args ->
            val obj = args[0] as JSONObject
            Log.d(TAG, "got move = " + obj)
            move(obj)
        }.on("game/$gameId/removed_stones") { args ->
            val obj = args[0] as JSONObject
            removedStones(obj)
        }.on("game/$gameId/removed_stones_accepted") { args ->
            val obj = args[0] as JSONObject
            removedStonesAccepted(obj)
        }.on("game/$gameId/error") { args ->
            val msg = args[0] as String
            error(msg)
        }.on("game/$gameId/phase") { args ->
            val p = args[0] as String
            phase(p)
        }.on("game/$gameId/reset") {
            reset()
        }.on("game/$gameId/chat", {
            val obj = it[0] as JSONObject
            val message = obj.getJSONObject("message")
            val username = message.getString("username")
            val msg = message.getString("body")
            val date = message.getLong("date")
            callbacks?.chat(ChatMessage(username, msg, date))
        })

        val gameDetails = ogs.getGameDetails(gameId)
        gameAuth = gameDetails!!.getString("auth")
        chatAuth = gameDetails.getString("game_chat_auth")

        Log.d(TAG, "socket = $socket")
        emit("game/connect", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
            put("game_type", "game")
            put("chat", 1)
        })
        emit("game/clear_delayed_resign", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
        })
        emit("chat/join", createJsonObject {
            put("channel", "game-$gameId")
        })
    }

    fun disconnect() {
        socket.off("game/$gameId/clock")
                .off("game/$gameId/gamedata")
                .off("game/$gameId/move")
                .off("game/$gameId/removed_stones")
                .off("game/$gameId/removed_stones_accepted")
                .off("game/$gameId/error")
                .off("game/$gameId/phase")
                .off("game/$gameId/reset")
                .off("game/$gameId/chat")

        emit("chat/part", createJsonObject {
            put("channel", "game-$gameId")
        }.toString())
        emit("game/disconnect", createJsonObject {
            put("game_id", gameId)
        })
    }

    private fun gamedata(obj: JSONObject) {
        Log.d(TAG, "on gamedata: " + obj.toString())
        callbacks?.gamedata(obj)
    }

    private fun move(obj: JSONObject) {
        val a = obj.getJSONArray("move")
        callbacks?.move(a.getInt(0), a.getInt(1))
    }

    private fun reset() {
        resetCallback?.reset()
    }

    private fun clock(obj: JSONObject) {
        callbacks?.clock(obj)
    }

    private fun phase(p: String) {
        callbacks?.phase(p)
    }

    private fun removedStones(obj: JSONObject) {
        callbacks?.removedStones(obj)
    }

    private fun removedStonesAccepted(obj: JSONObject) {
        callbacks?.removedStonesAccepted(obj)
    }

    private fun error(msg: String) {
        callbacks?.error(msg)
    }

    fun makeMove(coord: String) {
        val obj = createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
            put("move", coord)
        }
        Log.d(TAG, "sending move = " + obj.toString())
        emit("game/move", obj)
    }

    fun removeStones(coords: String, removed: Boolean) {
        val obj = createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
            put("stones", coords)
            put("removed", removed)
        }
        Log.d(TAG, "doing set removed stones: " + obj.toString())
        emit("game/removed_stones/set", obj)
    }

    fun acceptStones(coords: String) {
        val obj = createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
            put("stones", coords)
            put("strict_seki_mode", false)
        }
        Log.d(TAG, "doing accept removed stones " + obj.toString())
        emit("game/removed_stones/accept", obj)
    }

    fun rejectStones() {
        val obj = createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
        }
        Log.d(TAG, "doing reject removed stones: " + obj.toString())
        emit("game/removed_stones/reject", obj)
    }

    fun pass() {
        val obj = createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
            put("move", "..")
        }
        Log.d(TAG, "doing pass = " + obj)
        emit("game/move", obj)
    }

    fun resign() {
        val obj = createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", userId)
        }
        Log.d(TAG, "doing resign = " + obj)
        emit("game/resign", obj)
    }

    fun waitForStart() {
        val obj = createJsonObject {
            put("game_id", gameId)
        }
        emit("game/wait", obj)
    }

    fun sendChatMessage(player: Player, msg: String, moveNumber: Int) {
        val json = createJsonObject {
            put("auth", chatAuth)
            put("player_id", player.id)
            put("username", player.username)
            put("ranking", player.ranking)
            put("body", msg)
            put("type", "discussion")
            put("game_id", gameId)
            put("is_player", 1)
            put("move_number", moveNumber) // TODO
        }

        Log.d(TAG, "Sending chat message using $socket: $json")
        emit("game/chat", json)
    }

    companion object {
        private val TAG = "GameConnection"
    }
}
