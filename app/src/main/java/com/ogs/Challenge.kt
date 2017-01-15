package com.ogs

import org.json.JSONException
import org.json.JSONObject

class Challenge : Comparable<Challenge> {
    var challengeId: Int = 0
    var name: String = ""
    var username: String = ""
    var ranked: Boolean = false
    var rank: Int = 0
    var minRank: Int = 0
    var maxRank: Int = 0
    var handicap: Int = 0
    var timePerMove: Int = 0
    var width: Int = 0
    var height: Int = 0

    constructor(id: Int) {
        challengeId = id
    }

    constructor(obj: JSONObject) {
        try {
            challengeId = obj.getInt("challenge_id")
            username = obj.getString("username")
            name = obj.getString("name")
            timePerMove = obj.getInt("time_per_move")
            ranked = obj.getBoolean("ranked")
            rank = obj.getInt("rank")
            minRank = obj.getInt("min_rank")
            maxRank = obj.getInt("max_rank")
            handicap = obj.getInt("handicap")
            width = obj.getInt("width")
            height = obj.getInt("height")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun rankToString(rank: Int): String {
        if (rank < 30)
            return String.format("%d Kyu", 30 - rank)
        else
            return String.format("%d Dan", rank - 30 + 1)
    }

    override fun toString(): String {
        var handicapStr = if (handicap == -1) "Auto Handicap" else if (handicap == 0) "No Handicap" else ""
        if (handicap > 0)
            handicapStr = String.format("%d Stones", handicap)

        return String.format("%s - %dx%d - %s (%s) - %s - %s - %ds / move",
                name, width, height, username, rankToString(rank),
                if (ranked) "Ranked" else "Casual",
                handicapStr,
                timePerMove)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Challenge)
            return other.challengeId == challengeId
        else
            return false
    }

    fun canAccept(myRanking: Int): Boolean {
        return myRanking >= minRank && myRanking <= maxRank && (!ranked || Math.abs(myRanking - rank) <= 9)
    }

    override fun compareTo(other: Challenge): Int {
        if (other.timePerMove == timePerMove)
            return rank - other.rank
        return timePerMove - other.timePerMove
    }
}
