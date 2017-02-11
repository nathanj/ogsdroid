package com.ogs

import android.util.Log
import io.socket.client.Socket
import org.json.JSONObject

data class ChatMessage(val username: String, val msg: String, val date: Long) {
    override fun toString(): String {
        return "$username: $msg"
    }
}

class GameConnection internal constructor(ogs: OGS, private val socket: Socket, private val gameId: Int, private var userId: Int, private val gamedata: Gamedata, private var callbacks: OGSGameConnectionCallbacks? = null) {
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

        println("NJ socket on game/$gameId/gamedata")

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
        }.on("game/$gameId/conditional_moves") {
            Log.d(TAG, "conditional_moves $it")
        }.on("game/$gameId/chat-reset") {
            Log.d(TAG, "chat-reset $it")
        }.on("active_game") { args ->
            val obj = args[0] as JSONObject
            Log.d(TAG, "active_game obj=$obj")
        }.on("game/$gameId/chat", {
            val obj = it[0] as JSONObject
            Log.d(TAG, "chat obj=$obj")
            val line = obj.getJSONObject("line")
            val username = line.getString("username")
            val msg = line.getString("body")
            val date = line.getLong("date")
            callbacks?.chat(ChatMessage(username, msg, date))
        })

        gameAuth = gamedata.auth
        chatAuth = gamedata.game_chat_auth

        Log.d(TAG, "socket = $socket")
        //emit("ui-pushes/subscribe", createJsonObject {
        //    put("channel", "announcements")
        //})
        //emit("authenticate", createJsonObject {
        //    put("auth", "d0e89279f1fd3a6e086f369fc7e66e8a")
        //    put("player_id", 458)
        //    put("username", "nathanj4398fae9954a5344145")
        //})
        //emit("chat/connect", createJsonObject {
        //    put("auth", "d0e89279f1fd3a6e086f369fc7e66e8a")
        //    put("player_id", 458)
        //    put("username", "nathanj4398fae9954a5344145")
        //    put("ui_class", "provisional")
        //})
        emit("ui-pushes/subscribe", createJsonObject {
            put("channel", "game-$gameId")
        })
        emit("chat/join", createJsonObject {
            put("channel", "game-$gameId")
        })
        emit("game/connect", createJsonObject {
            put("game_id", gameId)
            put("player_id", userId)
            put("chat", true)
        })
    }

    fun disconnect() {

        println("NJ socket off game/$gameId/gamedata")


        socket.off("game/$gameId/clock")
                .off("game/$gameId/gamedata")
                .off("game/$gameId/move")
                .off("game/$gameId/removed_stones")
                .off("game/$gameId/removed_stones_accepted")
                .off("game/$gameId/error")
                .off("game/$gameId/phase")
                .off("game/$gameId/reset")
                .off("game/$gameId/chat")
                .off("active_game")

        emit("ui-pushes/unsubscribe", createJsonObject {
            put("channel", "game-$gameId")
        })
        emit("chat/part", createJsonObject {
            put("channel", "game-$gameId")
        })
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

    fun sendChatMessage(msg: String, moveNumber: Int) {
        emit("game/chat", createJsonObject {
            put("body", msg)
            put("game_id", gameId)
            put("type", "main")
            put("move_number", moveNumber)
        })
    }

    companion object {
        private val TAG = "GameConnection"
    }
}
