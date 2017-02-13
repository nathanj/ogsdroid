package com.ogs

import android.util.Log
import com.ogsdroid.Globals
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.http.*
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.net.ssl.HttpsURLConnection


interface OgsOauthService {
    @POST("oauth2/token/")
    fun login(@Query("username") username: String,
              @Query("password") password: String,
              @Query("client_id") client_id: String = "nathanj439_client",
              @Query("client_secret") client_secret: String = "sosecret",
              @Query("grant_type") grant_type: String = "password"): Observable<LoginInfo>

    @POST("oauth2/token/")
    fun refreshToken(@Query("refresh_token") refresh_token: String,
                     @Query("client_id") client_id: String = "nathanj439_client",
                     @Query("client_secret") client_secret: String = "sosecret",
                     @Query("grant_type") grant_type: String = "refresh_token"): Observable<LoginInfo>

}

data class NotificationResp(
        val type: String
)

data class CreateChallengeResp(
        val status: String,
        val game: Int,
        val challenge: Int
)

data class DeleteChallengeResp(
        val success: String
)

interface OgsService {
    @GET("me/")
    fun me(): Observable<Me>

    @GET("ui/config/")
    fun uiConfig(): Observable<UiConfig>

    @GET("megames/?started__isnull=False&ended__isnull=True")
    fun gameList(): Observable<GameList>

    @POST("challenges/{id}/accept/")
    fun acceptChallenge(@Path("id") id: Int): Observable<ChallengeResp>

    @GET("notifications/")
    fun notifications(): Observable<List<NotificationResp>>

    @POST("challenges/")
    fun createChallenge(@Body body: RequestBody): Observable<CreateChallengeResp>

    @DELETE("challenges/{id}")
    fun deleteChallenge(@Path("id") id: Int): Observable<DeleteChallengeResp>
}

class OGS(private val clientId: String, private val clientSecret: String) {

    init {
        println("OGS init")
    }

    @Throws(IOException::class)
    private fun getURL(url: String, method: String = "GET"): String {
        Log.d(TAG, "GET $url")
        val con = URL(url).openConnection() as HttpsURLConnection
        val token = ""
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
        val token = ""
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

    @Throws(IOException::class, JSONException::class)
    fun me() {
        val obj = JSONObject(getURL("https://online-go.com/api/v1/me/?format=json"))
    }

    fun meObservable(): Observable<Unit> {
        return Observable.fromCallable { me() }.subscribeOn(Schedulers.io())
    }

    @Throws(IOException::class, JSONException::class)
    fun notifications(): JSONArray {
        return JSONArray(getURL("https://online-go.com/api/v1/me/notifications/?format=json"))
    }

    fun notificationsObservable(): Observable<JSONArray> {
        return Observable.fromCallable { notifications() }.subscribeOn(Schedulers.io())
    }

    @Throws(IOException::class, JSONException::class)
    fun uiConfig(): JSONObject {
        val obj = JSONObject(getURL("https://online-go.com/api/v1/ui/config/?format=json"))
        return obj
    }

    fun acceptChallenge(id: Int): Int {
        try {
            val str = postURL("https://online-go.com/api/v1/challenges/$id/accept?format=json", "")
            Log.d(TAG, "acceptChallenge resp=" + str)
            val obj = JSONObject(str)
            return obj.getInt("game")
        } catch (e: JSONException) {
            e.printStackTrace()
            return 0
        } catch (e: IOException) {
            e.printStackTrace()
            return 0
        }
    }

    fun acceptChallengeObservable(id: Int): Observable<Int> {
        return Observable.fromCallable { acceptChallenge(id) }.subscribeOn(Schedulers.io())
    }

    @Throws(IOException::class, JSONException::class)
    fun listGames(): JSONObject {
        return JSONObject(getURL("https://online-go.com/api/v1/me/games/?started__isnull=False&ended__isnull=True&format=json"))
    }

    fun listGamesObservable(): Observable<JSONObject> {
        return Observable.fromCallable { listGames() }.subscribeOn(Schedulers.io())
    }

    @Throws(IOException::class, JSONException::class)
    fun getGameDetails(id: Int): JSONObject {
        return JSONObject(getURL("https://online-go.com/api/v1/games/$id?format=json"))
    }

    fun getGameDetailsObservable(id: Int): Observable<JSONObject> {
        return Observable.fromCallable { getGameDetails(id) }.subscribeOn(Schedulers.io())
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

            //Logger.getLogger(io.socket.client.Manager::class.java.name)

            val options = IO.Options()
            options.transports = arrayOf("websocket")
            socket = IO.socket("https://beta.online-go.com/", options)

            socket!!.on(Socket.EVENT_CONNECT) {
                Log.d("myApp", "socket connect")
            }.on(Socket.EVENT_DISCONNECT) {
                Log.d("myApp", "socket disconnect")
            }.on(Socket.EVENT_CONNECT_ERROR) {
                Thread.dumpStack()
                Log.e("myApp", "socket connect error")
            }.on(Socket.EVENT_ERROR) {
                Log.e("myApp", "socket error")
            }.on(Socket.EVENT_CONNECT_TIMEOUT) {
                Log.e("myApp", "socket connect timeout")
            }.on(Socket.EVENT_RECONNECT) {
                Log.d("myApp", "socket reconnect")
            }
            socket!!.connect()

            //socket!!.emit("notification/connect", createJsonObject {
            //    put("player_id", 458)
            //    put("auth", "ac3dd2f3a1fd8062535dcdfe5b4c8a79")
            //})

            socket!!.emit("authenticate", createJsonObject {
                put("player_id", Globals.uiConfig!!.user.id)
                put("username", Globals.uiConfig!!.user.username)
                put("auth", Globals.uiConfig!!.chat_auth)
            })
        }
    }

    fun closeSocket() {
        synchronized(this) {
            socket?.disconnect()
            socket = null
        }
    }

    fun getGameDetailsViaSocketBlocking(id: Int): JSONObject? {
        openSocket()
        val lock = Object()
        val list = ArrayList<JSONObject>()
        socket?.let { socket ->
            println("NJ socket on game/$id/gamedata")
            socket.on("game/$id/gamedata", { objs ->
                synchronized(lock) {
                    list.add(objs[0] as JSONObject)
                    lock.notify()
                }
            })
            socket.emit("game/connect", createJsonObject {
                put("game_id", id)
                put("player_id", Globals.uiConfig!!.user.id)
                put("chat", 0)
            })
            synchronized(lock) {
                while (list.isEmpty()) {
                    lock.wait()
                }
            }
            println("NJ socket off game/$id/gamedata")
            socket.off("game/$id/gamedata")
        }
        if (list.isEmpty())
            return null
        else
            return list[0]
    }

    fun getGameDetailsViaSocketObservable(id: Int): Observable<JSONObject?> {
        return Observable.fromCallable { getGameDetailsViaSocketBlocking(id) }.subscribeOn(Schedulers.io())
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
    fun openGameConnection(gameId: Int, gamedata: Gamedata, callbacks: GameConnection.OGSGameConnectionCallbacks? = null): GameConnection? {
        synchronized(this) {
            Log.d(TAG, "socket:$socket")
            if (socket != null) {
                return GameConnection(this, socket!!, gameId, Globals.uiConfig!!.user.id, gamedata, callbacks)
            } else {
                return null
            }
        }
    }

    fun openGameSocket(gameId: Int) {
        val json = createJsonObject {
            put("game_id", gameId)
        }
        println("emitting game/connect with json=$json")
        socket!!.on("game/$gameId/data") {

        }
        socket!!.emit("game/connect", json)
    }

    fun closeGameSocket(gameId: Int) {
        val json = createJsonObject {
            put("game_id", gameId)
        }
        println("emitting game/disconnect with json=$json")
        socket!!.emit("game/disconnect", json)
    }

    fun challengeKeepalive(challengeId: Int, gameId: Int) {
        val json = createJsonObject {
            put("challenge_id", challengeId)
            put("game_id", gameId)
        }
        println("emitting challenge/keepalive with json=$json")
        socket!!.emit("challenge/keepalive", json)
    }

    fun openNotificationConnection(auth: String, callbacks: NotificationConnection.NotificationConnectionCallbacks): NotificationConnection? {
        synchronized(this) {
            Log.d(TAG, "socket:$socket")
            if (socket != null) {
                return NotificationConnection(socket!!, Globals.uiConfig!!.user.id, auth, callbacks)
            } else {
                return null
            }
        }
    }

    private var socket: Socket? = null

    companion object {
        private val TAG = "OGS"
    }

    fun waitForGameDataBlocking(gameId: Int) {
        val lock = Object()
        var gotData = false
        socket!!.on("game/$gameId/data", {
            synchronized(lock) {
                gotData = true
                lock.notify()
            }
        })
        synchronized(lock) {
            while (!gotData) {
                try {
                    lock.wait()
                } catch (ex: InterruptedException) {
                    gotData = true
                }
            }
        }
        socket!!.off("game/$gameId/data")
    }

    fun waitForGameData(gameId: Int): Observable<Unit> {
        return Observable.fromCallable { waitForGameDataBlocking(gameId) }.subscribeOn(Schedulers.io())
    }
}
