package com.ogs

import android.util.Log
import com.ogsdroid.Globals
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.net.ssl.HttpsURLConnection


class Player2 {

    var id: Int? = null
    var username: String? = null
    var country: String? = null
    var icon: String? = null
    var ranking: Int? = null
    var ranking_blitz: Int? = null
    var ranking_live: Int? = null
    var ranking_correspondence: Int? = null
    var rating: String? = null
    var rating_blitz: String? = null
    var rating_live: String? = null
    var rating_correspondence: String? = null
    var professional: Boolean? = null
    var ui_class: String? = null
    var aga_valid: Any? = null

}

data class GameList(

        var count: Int? = null,
        var next: Any? = null,
        var previous: Any? = null,
        var results: List<Result>? = null

)

class Players {

    var white: Player2? = null
    var black: Player2? = null

}


class Related {

    var detail: String? = null

}

class Result {

    var related: Related? = null
    var players: Players? = null
    var id: Int? = null
    var name: String? = null
    var creator: Int? = null
    var mode: String? = null
    var source: String? = null
    var black: Int? = null
    var white: Int? = null
    var width: Int? = null
    var height: Int? = null
    var rules: String? = null
    var ranked: Boolean? = null
    var handicap: Int? = null
    var komi: String? = null
    var time_control: String? = null
    var black_player_rank: Int? = null
    var black_player_rating: String? = null
    var white_player_rank: Int? = null
    var white_player_rating: String? = null
    var time_per_move: Int? = null
    var time_control_parameters: String? = null
    var disable_analysis: Boolean? = null
    var tournament: Any? = null
    var tournament_round: Int? = null
    var ladder: Any? = null
    var pause_on_weekends: Boolean? = null
    var outcome: String? = null
    var black_lost: Boolean? = null
    var white_lost: Boolean? = null
    var annulled: Boolean? = null
    var started: String? = null
    var ended: Any? = null
    var sgf_filename: Any? = null

}

// {
//     "access_token": "izoMAnZm0P6ovjuM6qh9yTg3VAdY3H",
//     "expires_in": 36000,
//     "refresh_token": "NHyDWr2FSxxpROW3oNFFwczSzhG3an",
//     "scope": "read write groups",
//     "token_type": "Bearer"
// }
data class LoginInfo(
        val access_token: String,
        val refresh_token: String,
        val expires_in: Long
)

// {
//     "about": "",
//     "challenges": "/api/v1/mechallenges",
//     "friends": "/api/v1/mefriends",
//     "games": "/api/v1/megames",
//     "groups": "/api/v1/megroups",
//     "id": 413,
//     "mail": "/api/v1/memail",
//     "notifications": "/api/v1/menotifications",
//     "ranking": 15,
//     "ranking_blitz": 15,
//     "ranking_correspondence": 15,
//     "ranking_live": 15,
//     "rating": 650.0,
//     "rating_blitz": 650.0,
//     "rating_correspondence": 650.0,
//     "rating_live": 650.0,
//     "settings": "/api/v1/mesettings",
//     "tournaments": "/api/v1/metournaments",
//     "username": "nathanj439",
//     "vacation": "/api/v1/mevacation"
// }
data class Me(
        val id: Int,
        val ranking: Int,
        val username: String
)

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

interface OgsService {
    @GET("me/")
    fun me(): Observable<Me>

    @GET("megames/?started__isnull=False&ended__isnull=True")
    fun gameList(): Observable<GameList>
}

class OGS(private val clientId: String, private val clientSecret: String) {

    init {
        println("OGS init")
    }

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

    @Throws(IOException::class, JSONException::class)
    fun me() {
        println("calling me()")
        if (player != null)
            return
        val obj = JSONObject(getURL("https://online-go.com/api/v1/me/?format=json"))
        player = Player(obj)
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

            //socket!!.emit("authenticate", createJsonObject {
            //    put("player_id", 413)
            //    put("username", "nathanj439")
            //    put("auth", "27d72f2b1601ae3c47198ee8db8b22f0")
            //})
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
                put("player_id", Globals.me!!.id)
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
            Log.d(TAG, "socket:$socket player:$player")
            if (socket != null) {
                return GameConnection(this, socket!!, gameId, Globals.me!!.id, gamedata, callbacks)
            } else {
                return null
            }
        }
    }

    fun openNotificationConnection(auth: String, callbacks: NotificationConnection.NotificationConnectionCallbacks): NotificationConnection? {
        synchronized(this) {
            Log.d(TAG, "socket:$socket player:$player")
            if (socket != null) {
                return NotificationConnection(socket!!, Globals.me!!.id, auth, callbacks)
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


class Time(

        //var data: JSONObject? = null
        var thinking_time: Float? = null,
    var skip_bonus: Boolean? = null

)

//class TimeAdapter {
//    @FromJson fun fromJson(json: String): Time {
//        return Time(createJsonObject { put("asdf", "whee") })
//    }
//
//    @ToJson fun toJson(time: Time): String {
//        return "dontcare"
//    }
//}

class Clock {

    var game_id: Int? = null
    var current_player: Int? = null
    var black_player_id: Int? = null
    var white_player_id: Int? = null
    var title: String? = null
    var last_move: Long? = null
    var expiration: Long? = null
    var black_time: Time? = null
    var white_time: Time? = null

}

class Gamedata {

    var handicap: Int? = null
    var disable_analysis: Boolean? = null
    var _private: Boolean? = null
    var height: Int? = null
    var time_control: Time_control? = null
    var ranked: Boolean? = null
    //var meta_groups: List<Any>? = null
    var komi: Float? = null
    var game_id: Int? = null
    var width: Int? = null
    var rules: String? = null
    var black_player_id: Int? = null
    var pause_on_weekends: Boolean? = null
    var white_player_id: Int? = null
    var players: Players3? = null
    var game_name: String? = null
    var phase: String? = null
    //var history: List<Any>? = null
    var initial_player: String? = null
    var moves: List<List<Int>>? = null
    var allow_self_capture: Boolean? = null
    var automatic_stone_removal: Boolean? = null
    var free_handicap_placement: Boolean? = null
    var aga_handicap_scoring: Boolean? = null
    var allow_ko: Boolean? = null
    var allow_superko: Boolean? = null
    var superko_algorithm: String? = null
    var score_territory: Boolean? = null
    var score_territory_in_seki: Boolean? = null
    var score_stones: Boolean? = null
    var score_prisoners: Boolean? = null
    var score_passes: Boolean? = null
    var white_must_pass_last: Boolean? = null
    var opponent_plays_first_after_resume: Boolean? = null
    var strict_seki_mode: Boolean? = null
    var initial_state: Initial_state? = null
    var start_time: Int? = null
    var clock: Clock? = null
    var removed: String? = null
    var auth: String? = null
    var game_chat_auth: String? = null
    var winner: Int? = null
    var outcome: String? = null

}


class Initial_state {

    var black: String? = null
    var white: String? = null

}


class Players3 {

    var white: Player3? = null
    var black: Player3? = null

}

class Time_control {

    var system: String? = null
    var pause_on_weekends: Boolean? = null
    var time_control: String? = null
    var initial_time: Int? = null
    var max_time: Int? = null
    var time_increment: Int? = null
    var speed: String? = null

}


class Player3 {

    var username: String? = null
    var professional: Boolean? = null
    var egf: Int? = null
    var rank: Int? = null
    var id: Int? = null

}


