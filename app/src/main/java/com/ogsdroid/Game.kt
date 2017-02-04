package com.ogsdroid

import com.ogs.OGS
import io.reactivex.Observable
import org.json.JSONObject
import java.util.*

internal class Game : Comparable<Game> {
    var board: Board? = null
    var id: Int = 0
    var name: String? = null
    var myturn: Boolean = false

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
            g.id = details.getJSONObject("gamedata").getInt("game_id")
            val moves = details.getJSONObject("gamedata").getJSONArray("moves")
            g.board = Board(0, details.getInt("height"), details.getInt("width"))
            for (m in 0..moves.length() - 1) {
                val x = moves.getJSONArray(m).getInt(0)
                val y = moves.getJSONArray(m).getInt(1)
                if (x != -1)
                    g.board!!.addStone(x, y)
            }
            val white = details.getJSONObject("players").getJSONObject("white").getString("username")
            val black = details.getJSONObject("players").getJSONObject("black").getString("username")
            val currentPlayer = details.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player")
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

        fun getGamesList(ogs: OGS): Observable<Game> {
            return ogs.listGamesObservable()
                    .flatMap { gameListObj ->
                        val results = gameListObj.getJSONArray("results")
                        val arr = ArrayList<Int>(results.length())
                        for (i in 0..results.length() - 1) {
                            arr.add(results.getJSONObject(i).getInt("id"))
                        }
                        Observable.fromIterable(arr)
                    }
                    .flatMap { gameId -> ogs.getGameDetailsObservable(gameId) }
                    .map { gameDetails -> Game.fromJson(ogs.player!!.id, gameDetails) }
        }
    }
}