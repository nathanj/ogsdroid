package com.ogs

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.http.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.logging.Logger


interface OgsOauthService {
    @POST("oauth2/token/")
    fun login(@Query("username") username: String,
              @Query("password") password: String,
              @Query("client_id") client_id: String = "com.ogsdroid",
              @Query("client_secret") client_secret: String = "sai",
              @Query("grant_type") grant_type: String = "password"): Observable<LoginInfo>

    @POST("oauth2/token/")
    fun refreshToken(@Query("refresh_token") refresh_token: String,
                     @Query("client_id") client_id: String = "com.ogsdroid",
                     @Query("client_secret") client_secret: String = "sai",
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

data class OverviewGameJson(
        val moves: List<List<Long>>,
        val clock: Clock,
        val players: Players
)

data class OverviewGame(
        val height: Int,
        val width: Int,
        val id: Int,
        val json: OverviewGameJson
)

data class OverviewResp(
        val active_games: List<OverviewGame>
)

interface OgsService {
    @GET("me/")
    fun me(): Observable<Me>

    @GET("ui/config/")
    fun uiConfig(): Observable<UiConfig>

    @GET("ui/overview/")
    fun overview(): Observable<OverviewResp>

    @GET("megames/?started__isnull=False&ended__isnull=True")
    fun gameList(@Query("page") page: Int = 1): Observable<GameList>

    @POST("challenges/{id}/accept/")
    fun acceptChallenge(@Path("id") id: Int): Observable<ChallengeResp>

    @GET("menotifications/")
    fun notifications(): Observable<List<NotificationResp>>

    @POST("challenges/")
    fun createChallenge(@Body body: RequestBody): Observable<CreateChallengeResp>

    @DELETE("challenges/{id}")
    fun deleteChallenge(@Path("id") id: Int): Observable<DeleteChallengeResp>
}

object OgsSocket {
    private var socket: Socket? = null
    private val logger = Logger.getLogger(OgsSocket::class.java.name)

    var uiConfig: UiConfig? = null

    init {
        logger.fine("init")
    }

    fun openSocket() {
        uiConfig?.let { uiConfig ->
            if (socket != null)
                return

            val options = IO.Options()
            options.transports = arrayOf("websocket")
            socket = IO.socket("https://online-go.com/", options)

            socket!!.on(Socket.EVENT_CONNECT) {
                logger.fine("socket connect")
            }.on(Socket.EVENT_DISCONNECT) {
                logger.warning("socket disconnect")
            }.on(Socket.EVENT_CONNECT_ERROR) {
                logger.warning("socket connect error")
            }.on(Socket.EVENT_ERROR) {
                logger.warning("socket error")
            }.on(Socket.EVENT_CONNECT_TIMEOUT) {
                logger.warning("socket connect timeout")
            }.on(Socket.EVENT_RECONNECT) {
                logger.warning("socket reconnect")
            }
            socket!!.connect()
            socket!!.emit("authenticate", createJsonObject {
                put("player_id", uiConfig.user.id)
                put("username", uiConfig.user.username)
                put("auth", uiConfig.chat_auth)
            })
        }
    }

    fun closeSocket() {
        socket?.disconnect()
        socket = null
    }
}


class OGS(val uiConfig: UiConfig) {

    init {
        println("OGS init")
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
            socket = IO.socket("https://online-go.com/", options)

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
                put("player_id", uiConfig.user.id)
                put("username", uiConfig.user.username)
                put("auth", uiConfig.chat_auth)
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
        val latch = CountDownLatch(1)
        val list = ArrayList<JSONObject?>()
        socket?.let { socket ->
            println("NJ socket on game/$id/gamedata")
            socket.on("game/$id/gamedata", { objs ->
                socket.off("game/$id/gamedata")
                socket.emit("game/disconnect", createJsonObject {
                    put("game_id", id)
                })
                list.add(objs[0] as JSONObject)
                latch.countDown()
            })
            socket.emit("game/connect", createJsonObject {
                put("game_id", id)
                put("player_id", uiConfig.user.id)
                put("chat", 0)
            })
            try {
                latch.await()
            } catch (ex: InterruptedException) {
                list.add(null)
            }
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
                return GameConnection(this, socket!!, gameId, uiConfig.user.id, gamedata, callbacks)
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

    fun openNotificationConnection(callbacks: NotificationConnection.NotificationConnectionCallbacks): NotificationConnection? {
        synchronized(this) {
            Log.d(TAG, "socket:$socket")
            if (socket != null) {
                return NotificationConnection(socket!!, uiConfig.user.id, uiConfig.notification_auth!!, callbacks)
            } else {
                return null
            }
        }
    }

    private var socket: Socket? = null

    companion object {
        private val TAG = "OGS"
    }

    private fun sleepUntilInterrupted() {
        try {
            while (true) {
                Thread.sleep(Long.MAX_VALUE)
            }
        } catch (ex: InterruptedException) {
        }
    }

    fun listenForGameData(gameId: Int): Completable {
        return Completable.create { emitter ->
            socket!!.on("game/$gameId/data", {
                socket!!.off("game/$gameId/data")
                emitter.onComplete()
            })
            sleepUntilInterrupted()
            socket!!.off("game/$gameId/data")
        }.subscribeOn(Schedulers.io())
    }
}
