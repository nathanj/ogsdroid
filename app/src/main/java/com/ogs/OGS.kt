package com.ogs

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class OGS(private val clientId: String, private val clientSecret: String) {

    @Throws(IOException::class)
    private fun getURL(url: String, method: String = "GET"): String {
        Log.d(TAG, "GET $url")
        val con = URL(url).openConnection() as HttpsURLConnection
        val token = accessToken
        if (token != null && token.isNotEmpty()) {
            con.setRequestProperty("Authorization", "Bearer " + token)
        }
        con.requestMethod = method
        return con.inputStream.bufferedReader().readText()
    }

    @Throws(IOException::class)
    private fun postURL(url: String, body: String, headers: Map<String, String>? = null, method: String = "POST"): String {
        Log.d(TAG, "POST $url - $body")
        val con = URL(url).openConnection() as HttpsURLConnection
        if (headers != null) {
            for ((k, v) in headers)
                con.setRequestProperty(k, v)
        }
        val token = accessToken
        if (token != null && token.isNotEmpty()) {
            con.setRequestProperty("Authorization", "Bearer " + token)
        }
        con.requestMethod = method
        con.doOutput = true

        val writer = con.outputStream.writer()
        writer.write(body)
        writer.close()

        return con.inputStream.bufferedReader().readText()
    }

    private fun deleteURL(url: String) = getURL(url, "DELETE")

    private fun urlencode(s: String) = URLEncoder.encode(s, "UTF-8")

    @Throws(IOException::class)
    fun login(username: String, password: String) {
        val body = "client_id=$clientId&client_secret=$clientSecret&grant_type=password&username=${urlencode(username)}&password=${urlencode(password)}"
        try {
            val s = postURL("https://online-go.com/oauth2/access_token", body)
            val obj = JSONObject(s)
            accessToken = obj.getString("access_token")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun me(): JSONObject {
        val obj = JSONObject(getURL("https://online-go.com/api/v1/me/?format=json"))
        player = Player(obj)
        return obj
    }
    @Throws(IOException::class)
    fun notifications(): JSONArray {
        val obj = JSONArray(getURL("https://online-go.com/api/v1/me/notifications/?format=json"))
        return obj
    }

    @Throws(IOException::class, JSONException::class)
    fun acceptChallenge(id: Int): Int {
        try {
            val str = postURL("https://online-go.com/api/v1/challenges/$id/accept?format=json", "")
            Log.d(TAG, "acceptChallenge resp=" + str)
            val obj = JSONObject(str)
            return obj.getInt("game")
        } catch (e: IOException) {
            e.printStackTrace()
            return 0
        }
    }

    @Throws(IOException::class, JSONException::class)
    fun listGames(): JSONObject? {
        try {
            val str = getURL("https://online-go.com/api/v1/me/games/?started__isnull=False&ended__isnull=True&format=json")
            //        Log.d("myApp", str);
            return JSONObject(str)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    @Throws(IOException::class, JSONException::class)
    fun getGameDetails(id: Int): JSONObject? {
        try {
            val str = getURL("https://online-go.com/api/v1/games/$id?format=json")
            return JSONObject(str)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    @Throws(IOException::class)
    fun createChallenge(name: String, ranked: Boolean, width: Int, height: Int,
                        mainTime: Int, periodTime: Int, periods: Int): JSONObject {
        val post = createJsonObject {
            put("challenger_color", "automatic")
            put("min_ranking", -1000)
            put("max_ranking", 1000)
            put("game", createJsonObject {
                put("name", name)
                put("rules", "japanese")
                put("ranked", ranked)
                put("handicap", 0)
                put("pause_on_weekends", false)
                put("width", width)
                put("height", height)
                put("disable_analysis", true)
                put("time_control", "byoyomi")
                put("time_control_parameters", createJsonObject {
                    put("time_control", "byoyomi")
                    put("main_time", mainTime)
                    put("period_time", periodTime)
                    put("periods", periods)
                })
            })
        }

        return JSONObject(
                postURL("https://online-go.com/api/v1/challenges/", post.toString(),
                        mapOf("Content-Type" to "application/json")
                ))
    }

    @Throws(IOException::class)
    fun deleteChallenge(challenge: Int) {
        deleteURL("https://online-go.com/api/v1/challenges/$challenge")
    }

    /**
     * Opens the real time api socket.
     */
    fun openSocket() {
        synchronized(this) {
            if (socket != null)
                return

            socket = IO.socket("https://ggs.online-go.com")
            socket!!.on(Socket.EVENT_CONNECT) {
                Log.d("myApp", "socket connect")
            }.on(Socket.EVENT_DISCONNECT) {
                Log.d("myApp", "socket disconnect")
            }.on(Socket.EVENT_CONNECT_ERROR) {
                Log.e("myApp", "socket connect error")
            }.on(Socket.EVENT_ERROR) {
                Log.e("myApp", "socket error")
            }.on(Socket.EVENT_CONNECT_TIMEOUT) {
                Log.e("myApp", "socket connect timeout")
            }.on(Socket.EVENT_RECONNECT) {
                Log.d("myApp", "socket reconnect")
            }
            socket!!.connect()
        }
    }

    fun closeSocket() {
        synchronized(this) {
            socket?.disconnect()
            socket = null
        }
    }

    fun openSeekGraph(callbacks: SeekGraphConnection.SeekGraphConnectionCallbacks): SeekGraphConnection? {
        synchronized(this) {
            Log.d(TAG, "trying opening seek graph socket:$socket")
            if (socket != null) {
                Log.d(TAG, "opening seek graph socket:$socket")
                return SeekGraphConnection(this, socket!!, callbacks)
            } else {
                return null
            }
        }
    }

    /**
     * Uses the real time api to connect to a game.
     */
    fun openGameConnection(gameId: Int): GameConnection? {
        synchronized(this) {
            Log.d(TAG, "socket:$socket player:$player")
            if (socket != null) {
                return GameConnection(this, socket!!, gameId, player!!.id)
            } else {
                return null
            }
        }
    }

    var accessToken: String? = null
    var player: Player? = null
        private set
    private var socket: Socket? = null

    companion object {
        private val TAG = "OGS"
    }
}
