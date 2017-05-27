package com.ogsdroid

import android.media.MediaPlayer
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import com.ogs.ChatMessage
import com.ogs.GameConnection
import com.ogs.Gamedata
import com.ogs.OGS
import com.squareup.moshi.Moshi
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class Main3Activity : AppCompatActivity() {

    private var gameCon: GameConnection? = null
    private var phase = "play"

    private var bv: BoardView? = null
    private var board: Board? = null
    private var prefix = ""
    private var gamedata: GameDetails? = null
    private var clickSound: MediaPlayer? = null
    private var passSound: MediaPlayer? = null

    private var currentGameId: Int = 0
    private var ogs: OGS? = null

    override fun onPostResume() {
        super.onPostResume()
        Log.d(TAG, "onPostResume")

        //*
        try {
            val pref = PreferenceManager.getDefaultSharedPreferences(this)

            ogs = OGS(Globals.uiConfig!!)

            val gameDetails = ogs!!.getGameDetailsViaSocketBlocking(currentGameId)

            val moshi = Moshi.Builder()
                    //.add(TimeAdapter())
                    .build()
            val adapter = moshi.adapter(Gamedata::class.java)
            val gameData = adapter.fromJson(gameDetails!!.toString())

            val details = GameDetails(gameData)
            board = Board(details.handicap, details.height, details.width)
            bv!!.setBoard(board)

            bv!!.zoom = pref.getString("pref_zoom", "3")

            for (move in details.moves) {
                val x = move[0].toInt()
                val y = move[1].toInt()
                if (x == -1)
                    bv!!.board.pass()
                else
                    bv!!.board.addStone(x, y)
            }

            if (details.removed != null) {
                bv!!.board.stoneRemoval(details.removed, true)
            }

            bv!!.blackPlayer = details.blackPlayer
            bv!!.whitePlayer = details.whitePlayer

            ogs!!.closeSocket()
            ogs!!.openSocket()

            gameCon = ogs!!.openGameConnection(currentGameId, gameData,
                    object : GameConnection.OGSGameConnectionCallbacks {
                        override fun chat(msg: ChatMessage) {
                            Log.d(TAG, "got chat messague: " + msg.toString())

                            this@Main3Activity.runOnUiThread {
                                // TODO: no toast for now, all chat messages are sent when connecting to the game.
                                /*
                                    if (msg.username != Globals.uiConfig!!.user.username) {
                                        val toast = Toast.makeText(this@Main3Activity, msg.toString(), Toast.LENGTH_SHORT)
                                        toast.show()
                                    }
                                */

                                val tv = findViewById(R.id.chat_text_view) as TextView
                                tv.text = msg.toString() + "\n" + tv.text
                            }
                        }

                        override fun move(x: Int, y: Int) {
                            if (x == -1) {
                                bv!!.board.pass()
                                passSound!!.start()
                            } else {
                                bv!!.board.addStone(x, y)
                                clickSound!!.start()
                            }
                            bv!!.postInvalidate()
                        }

                        override fun clock(clock: JSONObject) {
                            try {

                                Log.d("njclock", clock.toString())
                                val whoseTurn = clock.getInt("current_player")

                                if (clock.get("white_time") is Number) {
                                    val now = System.currentTimeMillis()
                                    val now_delta = now - clock.getLong("now")
                                    val base_time = clock.getLong("last_move") + now_delta
                                    Log.d("njclock", "now = " + now)
                                    Log.d("njclock", "now_delta = " + now_delta)
                                    Log.d("njclock", "base_time = " + base_time)
                                    Log.d("njclock", "black time = " + clock.getLong("black_time"))
                                    Log.d("njclock", "delta = " + (clock.getLong("black_time") - System.currentTimeMillis()))
                                    bv!!.clockWhite.setTime((clock.getLong("white_time") - System.currentTimeMillis()).toInt() / 1000, 0, 0)
                                    bv!!.clockBlack.setTime((clock.getLong("black_time") - System.currentTimeMillis()).toInt() / 1000, 0, 0)
                                } else {
                                    var thinkingTime = 0
                                    var periods = 0
                                    var periodTime = 0
                                    try {
                                        val c = clock.getJSONObject("white_time")
                                        Log.d("njclock", "white = " + c)
                                        thinkingTime = c.getInt("thinking_time")
                                        periods = c.getInt("periods")
                                        periodTime = c.getInt("period_time")
                                    } catch (e: JSONException) {
                                        //e.printStackTrace();
                                    }

                                    bv!!.clockWhite.setTime(thinkingTime, periods, periodTime)

                                    thinkingTime = 0
                                    periods = 0
                                    periodTime = 0
                                    try {
                                        val c = clock.getJSONObject("black_time")
                                        Log.d("njclock", "black = " + c)
                                        thinkingTime = c.getInt("thinking_time")
                                        periods = c.getInt("periods")
                                        periodTime = c.getInt("period_time")
                                    } catch (e: JSONException) {
                                    }

                                    bv!!.clockBlack.setTime(thinkingTime, periods, periodTime)
                                }

                                if (gamedata != null) {
                                    gamedata!!.whoseTurn = whoseTurn
                                    changeTitle()
                                }
                                bv!!.blacksMove = whoseTurn == details.blackId
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }

                        }

                        override fun phase(p: String) {
                            if (phase != p) {
                                phase = p
                                bv!!.phase = p
                                invalidateOptionsMenu()
                            }
                            if (p == "play") {
                                bv!!.board.unmarkTerritory()
                                bv!!.board.unmarkRemoved()
                            } else
                                bv!!.board.markTerritory()

                            changeTitle()
                        }

                        internal fun changeTitle() {
                            if (gamedata == null)
                                return
                            this@Main3Activity.runOnUiThread {
                                if (phase == "play") {
                                    if (gamedata!!.whoseTurn == gamedata!!.blackId)
                                        title = String.format("Black to play - %s", prefix)
                                    else
                                        title = String.format("White to play - %s", prefix)
                                } else if (phase == "finished") {
                                    if (gamedata!!.winner == gamedata!!.blackId)
                                        title = String.format("Black wins by %s - %s", gamedata!!.outcome, prefix)
                                    else
                                        title = String.format("White wins by %s - %s", gamedata!!.outcome, prefix)
                                } else if (phase == "stone removal") {
                                    title = String.format("Stone removal - %s", prefix)
                                }
                            }
                        }

                        override fun gamedata(obj: JSONObject) {
                            val gameData = try {
                                adapter.fromJson(obj.toString())
                            } catch (ex: IOException) {
                                throw RuntimeException(ex)
                            }

                            gamedata = GameDetails(gameData)

                            try {
                                val tv = findViewById(R.id.chat_text_view) as TextView
                                val chats = obj.getJSONArray("chat_log")
                                this@Main3Activity.runOnUiThread { tv.text = "" }
                                for (i in 0..chats.length() - 1) {
                                    val c = chats.getJSONObject(i)
                                    val msg = ChatMessage(c.getString("username"), c.getString("body"), c.getLong("date"))
                                    this@Main3Activity.runOnUiThread { tv.text = msg.toString() + "\n" + tv.text }
                                }
                            } catch (e: JSONException) {
                            }

                            if (phase != gamedata!!.phase) {
                                phase = gamedata!!.phase
                                bv!!.phase = gamedata!!.phase
                                invalidateOptionsMenu()
                            }

                            if (phase == "play")
                                bv!!.board.unmarkTerritory()
                            else
                                bv!!.board.markTerritory()

                            prefix = String.format("%s vs %s", gamedata!!.whitePlayer, gamedata!!.blackPlayer)
                            changeTitle()
                        }

                        override fun removedStones(obj: JSONObject) {
                            try {
                                val coords = obj.getString("stones")
                                val removed = obj.getBoolean("removed")
                                bv!!.board.stoneRemoval(coords, removed)
                                bv!!.postInvalidate()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }

                        }

                        override fun removedStonesAccepted(obj: JSONObject) {

                        }

                        override fun error(msg: String) {
                            Log.e(TAG, "got ogs error: " + msg)
                            this@Main3Activity.runOnUiThread {
                                AlertDialog.Builder(this@Main3Activity)
                                        .setMessage("OGS error: " + msg)
                                        .setCancelable(true)
                                        .setPositiveButton("Ok") { dialog, id -> }
                                        .show()
                            }
                        }
                    }
            )

            bv!!.gameConnection = gameCon
            bv!!.phase = phase

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        clickSound!!.release()
        passSound!!.release()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        if (gameCon != null) {
            gameCon!!.disconnect()
            gameCon = null
        }
        ogs?.closeSocket()
        ogs = null
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        bv = findViewById(R.id.boardview) as BoardView

        findViewById(R.id.chat_text_view).visibility = View.GONE
        val editText = findViewById(R.id.chat_edit_text) as EditText
        editText.visibility = View.GONE
        findViewById(R.id.chat_scroll_view).visibility = View.GONE

        editText.setOnEditorActionListener { textView, i, keyEvent ->
            println("textView = [${textView}], i = [${i}], keyEvent = [${keyEvent}]")
            if (i == EditorInfo.IME_ACTION_DONE ||
                    (i == EditorInfo.IME_NULL &&
                            keyEvent.action == KeyEvent.ACTION_UP &&
                            keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                println("gameCon = ${gameCon}")
                gameCon!!.sendChatMessage(
                        editText.text.toString(), bv!!.board.moveNumber)
                editText.setText("")
            }

            true
        }

        val intent = intent
        currentGameId = intent.getIntExtra("id", 0)

        supportActionBar!!.setHomeButtonEnabled(true)

        clickSound = MediaPlayer.create(this, R.raw.click)
        passSound = MediaPlayer.create(this, R.raw.pass)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            if (bv!!.zoomed) {
                bv!!.unZoom()
                return true
            }
        }
        val v = findViewById(R.id.chat_text_view) as TextView
        // If the chat is visible then go back to the game
        if (v.visibility == View.VISIBLE) {
            val v2 = findViewById(R.id.chat_edit_text) as EditText
            val sv = findViewById(R.id.chat_scroll_view) as ScrollView
            sv.visibility = View.GONE
            v.visibility = View.GONE
            v2.visibility = View.GONE
            bv!!.visibility = View.VISIBLE
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // {{{ options
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "options menu phase = " + phase)
        menuInflater.inflate(R.menu.menu_main, menu)
        for (i in 0..menu.size() - 1) {
            val item = menu.getItem(i)
            when (item.itemId) {
                R.id.pass, R.id.resign -> item.isVisible = phase == "play"
                R.id.accept_stones, R.id.reject_stones -> item.isVisible = phase == "stone removal"
                else -> item.isVisible = true
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat -> {
                val v = findViewById(R.id.chat_text_view) as TextView
                val v2 = findViewById(R.id.chat_edit_text) as EditText
                val sv = findViewById(R.id.chat_scroll_view) as ScrollView


                if (v.visibility == View.GONE) {
                    sv.visibility = View.VISIBLE
                    v.visibility = View.VISIBLE
                    v2.visibility = View.VISIBLE

                    sv.fullScroll(View.FOCUS_UP)

                    //                    v2.setImeActionLabel("Send", KeyEvent.KEYCODE_ENTER);


                    bv!!.visibility = View.GONE
                } else {
                    sv.visibility = View.GONE
                    v.visibility = View.GONE
                    v2.visibility = View.GONE
                    bv!!.visibility = View.VISIBLE
                }


                return true
            }
            R.id.pass -> {
                Log.d(TAG, "user chose pass")

                AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to pass?")
                        .setCancelable(true)
                        .setPositiveButton("Yes") { dialog, id -> gameCon!!.pass() }
                        .setNegativeButton("No", null)
                        .show()

                return true
            }
            R.id.resign -> {
                Log.d(TAG, "user chose resign")
                AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to resign?")
                        .setCancelable(true)
                        .setPositiveButton("Yes") { dialog, id -> gameCon!!.resign() }
                        .setNegativeButton("No", null)
                        .show()

                return true
            }
            R.id.accept_stones -> {
                Log.d(TAG, "user chose accept stones")
                AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to accept stones?")
                        .setCancelable(true)
                        .setPositiveButton("Yes") { dialog, id -> gameCon!!.acceptStones(board!!.removedCoords) }
                        .setNegativeButton("No", null)
                        .show()

                return true
            }
            R.id.reject_stones -> {
                Log.d(TAG, "user chose reject stones")
                AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to reject stones?")
                        .setCancelable(true)
                        .setPositiveButton("Yes") { dialog, id -> gameCon!!.rejectStones() }
                        .setNegativeButton("No", null)
                        .show()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class GameDetails internal constructor(gamedata: Gamedata) {
        var height: Int = 0
        var width: Int = 0
        var handicap: Int = 0
        var phase: String
        var moves: List<List<Long>>
        var whitePlayer: String
        var blackPlayer: String
        var whiteId: Int = 0
        var blackId: Int = 0
        var whoseTurn: Int = 0
        var gameAuth: String? = null
        var removed: String? = null
        var winner: Int? = null
        var outcome: String? = null

        init {
            height = gamedata.height!!
            width = gamedata.width!!
            phase = gamedata.phase!!
            moves = gamedata.moves!!
            handicap = gamedata.handicap!!
            removed = gamedata.removed

            whitePlayer = gamedata.players!!.white!!.username!!
            whiteId = gamedata.players!!.white!!.id!!
            blackPlayer = gamedata.players!!.black!!.username!!
            blackId = gamedata.players!!.black!!.id!!
            whoseTurn = gamedata.clock!!.current_player!!

            gameAuth = gamedata.auth
            winner = gamedata.winner
            outcome = gamedata.outcome
        }
    }

    companion object {
        private val TAG = "Main3Activity"
    }

    // }}}
}
