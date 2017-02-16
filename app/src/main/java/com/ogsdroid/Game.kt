package com.ogsdroid

import com.ogs.Gamedata
import com.ogs.OGS
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.util.*

class Game : Comparable<Game> {
    internal var board: Board? = null
    internal var id: Int = 0
    internal var name: String? = null
    internal var myturn: Boolean = false

    override fun toString(): String {
        return name!!
    }

    override fun compareTo(other: Game): Int {
        if (myturn && !other.myturn)
            return -1
        if (!myturn && other.myturn)
            return 1
        return 0
    }

    companion object {
        fun fromJson(playerId: Int, details: JSONObject): Game {
            val g = Game()
            //println(details.toString(2))
            g.id = details.getInt("game_id")
            val moves = details.getJSONArray("moves")
            g.board = Board(0, details.getInt("height"), details.getInt("width"))
            for (m in 0..moves.length() - 1) {
                val x = moves.getJSONArray(m).getInt(0)
                val y = moves.getJSONArray(m).getInt(1)
                if (x != -1)
                    g.board!!.addStone(x, y)
            }
            val white = details.getJSONObject("players").getJSONObject("white").getString("username")
            val black = details.getJSONObject("players").getJSONObject("black").getString("username")
            val currentPlayer = details.getJSONObject("clock").getInt("current_player")
            if (playerId == currentPlayer) {
                g.myturn = true
                g.name = String.format("%s vs %s", white, black)
            } else {
                g.myturn = false
                g.name = String.format("%s vs %s", white, black)
            }
            println("returning game=$g")
            return g
        }

        fun fromGamedata(playerId: Int, gamedata: Gamedata): Game {
            val g = Game()
            g.id = gamedata.game_id!!
            g.board = Board(0, gamedata.height!!, gamedata.width!!)
            gamedata.moves?.forEach {
                val x = it[0]
                val y = it[1]
                if (x != -1)
                    g.board!!.addStone(x, y)
            }
            val white = gamedata.players?.white?.username
            val black = gamedata.players?.black?.username
            val currentPlayer = gamedata.clock?.current_player
            if (playerId == currentPlayer) {
                g.myturn = true
                g.name = String.format("%s vs %s", white, black)
            } else {
                g.myturn = false
                g.name = String.format("%s vs %s", white, black)
            }
            println("returning game=$g")
            return g
        }
    }
}