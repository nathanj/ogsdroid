package com.ogs

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class OGS(private val clientId: String, private val clientSecret: String) {

    @Throws(IOException::class)
    private fun getURL(url: String, method: String = "GET"): String {
        Log.d("myApp", "GET $url")
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
        Log.d("myApp", "POST $url - $body")
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

    @Throws(IOException::class)
    fun login(username: String, password: String) {
        val body = String.format("client_id=%s&client_secret=%s&grant_type=password&username=%s&password=%s",
                clientId, clientSecret, username, password) // TODO url encode
        try {
            val s = postURL("https://online-go.com/oauth2/access_token", body)
            val obj = JSONObject(s)
            accessToken = obj.getString("access_token")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        player = Player(me())
    }

    @Throws(JSONException::class, IOException::class)
    fun me(): JSONObject {
        val str = getURL("https://online-go.com/api/v1/me/?format=json")
        val obj = JSONObject(str)
        userId = obj.getInt("id")
        username = obj.getString("username")
        ranking = obj.getInt("ranking")
        return obj
    }

    @Throws(JSONException::class)
    fun listServerChallenges(): JSONObject? {
        try {
            val str = getURL("https://online-go.com/api/v1/challenges/?format=json")
            return JSONObject(str)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    @Throws(JSONException::class)
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

    @Throws(JSONException::class)
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

    @Throws(JSONException::class)
    fun getGameDetails(id: Int): JSONObject? {
        try {
            val str = getURL("https://online-go.com/api/v1/games/$id?format=json")
            return JSONObject(str)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

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

    fun deleteChallenge(challenge: Int) {
        deleteURL("https://online-go.com/api/v1/challenges/$challenge")
    }

    /**
     * Opens the real time api socket.
     */
    fun openSocket() {
        socket = IO.socket("https://ggs.online-go.com")
        socket!!.on(Socket.EVENT_CONNECT) {
            Log.d("myApp", "socket connect")
        }.on(Socket.EVENT_DISCONNECT) {
            Log.d("myApp", "socket disconnect")
        }
        socket!!.connect()
    }

    fun closeSocket() {
        socket?.disconnect()
        socket = null
    }

    fun openSeekGraph(callbacks: SeekGraphConnection.SeekGraphConnectionCallbacks): SeekGraphConnection {
        Log.d(TAG, "opening seek graph")
        return SeekGraphConnection(this, socket, callbacks)
    }

    /**
     * Uses the real time api to connect to a game.
     */
    fun openGameConnection(gameId: Int): OGSGameConnection {
        return OGSGameConnection(this, socket, gameId, userId)
    }

    var accessToken: String? = null
    private var username: String? = null
    private var userId: Int = 0
    private var ranking: Int = 0
    private var socket: Socket? = null

    lateinit var player: Player

    companion object {
        private val TAG = "OGS"
    }
}
