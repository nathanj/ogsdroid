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
    var timeControlParameters: JSONObject? = null

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
            timeControlParameters = obj.getJSONObject("time_control_parameters")
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
        val handicapStr = if (handicap == -1)
            "Auto Handicap"
        else if (handicap == 0)
            "No Handicap"
        else
            String.format("%d Stones", handicap)

        return String.format("%s - %dx%d - %s (%s) - %s - %s - %s",
                name, width, height, username, rankToString(rank),
                if (ranked) "Ranked" else "Casual",
                handicapStr,
                formatTime())
    }

    fun prettyTime(time: Int): String {
        if (time > 24 * 3600)
            return (time / 24 / 3600).toString() + "d"
        else if (time > 3600)
            return (time / 3600).toString() + "h"
        else if (time > 60)
            return (time / 60).toString() + "m"
        return time.toString() + "s"
    }

    fun formatTime(): String {
        try {
            val tcp = timeControlParameters
            if (tcp != null) {
                val control = tcp.getString("time_control")
                if (control == "byoyomi") {
                    val periodTime = tcp.getInt("period_time")
                    val mainTime = tcp.getInt("main_time")
                    val periods = tcp.getInt("periods")
                    return "${prettyTime(mainTime)} + ${prettyTime(periodTime)} x $periods"
                } else if (control == "fischer") {
                    val initialTime = tcp.getInt("initial_time")
                    val maxTime = tcp.getInt("max_time")
                    val increment = tcp.getInt("time_increment")
                    return "${prettyTime(initialTime)} + ${prettyTime(increment)} up to ${prettyTime(maxTime)}"
                } else if (control == "canadian") {
                    val periodTime = tcp.getInt("period_time")
                    val mainTime = tcp.getInt("main_time")
                    val stonesPerPeriod = tcp.getInt("stones_per_period")
                    return "${prettyTime(mainTime)} + ${prettyTime(periodTime)} per $stonesPerPeriod stones"
                } else if (control == "absolute") {
                    val totalTime = tcp.getInt("total_time")
                    return prettyTime(totalTime)
                } else if (control == "simple") {
                    val perMove = tcp.getInt("per_move")
                    return prettyTime(perMove)
                } else {
                    System.err.println("error: control = $control  tcp=$tcp")
                }
            }
        } catch (ex: JSONException) {
            // nothing, move along
        }
        return prettyTime(timePerMove) + " / move"
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
