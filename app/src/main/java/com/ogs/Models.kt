@file:Suppress("unused", "PropertyName")

package com.ogs

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.*


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
        var next: String? = null,
        var previous: String? = null,
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


class Time(

        //var data: JSONObject? = null
        var thinking_time: Long? = null,
        var skip_bonus: Boolean? = null

)

// Time can be either a float which is the thinking time, or it can be an object with a key of "thinking_time".
class TimeAdapter : JsonDeserializer<Time> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Time =
            when (json) {
                is JsonPrimitive -> Time(thinking_time = json.asLong)
                is JSONObject -> {
                    val lastMove = json.getLong("last_move")
                    val think = json.getDouble("thinking_time").toLong()
                    val now = System.currentTimeMillis()
                    Time(thinking_time = lastMove + think * 1000 - now)
                }
                else -> Time(thinking_time = 0)
            }
}

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
    var moves: List<List<Long>>? = null
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
    //var egf: Int? = null
    var rank: Int? = null
    var id: Int? = null

}


// -- UiConfig

class Bot {

    var username: String? = null
    var rating: Float? = null
    var ranking: Int? = null
    var country: String? = null
    var ui_class: String? = null
    var id: Int? = null
    var icon: String? = null

}


class Channel {

    var country: String? = null
    var id: String? = null
    var name: String? = null
    var rtl: Boolean? = null

}


class Ogs {

    var channels: List<Channel>? = null
    var preferences: Preferences? = null

}


class Preferences {

    var show_game_list_view: Boolean? = null

}

class UiConfig {

    var cdn_release: String? = null
    var lang: String? = null
    var csrf_token: String? = null
    var profanity_filter: Boolean? = null
    lateinit var chat_auth: String
    var server_name: String? = null
    var cdn: String? = null
    var notification_auth: String? = null
    var paypal_server: String? = null
    var incident_auth: String? = null
    var ggs_host: String? = null
    var braintree_cse: String? = null
    var cdn_host: String? = null
    var version: String? = null
    var ogs: Ogs? = null
    //public Ignores ignores;
    var release: String? = null
    var bots: List<Bot>? = null
    var aga_ratings_enabled: Boolean? = null
    var paypal_email: String? = null
    lateinit var user: User

}

class User {

    lateinit var username: String
    var ranking: Int = 0
    var ui_class: String? = null
    var is_tournament_moderator: Boolean? = null
    var can_create_tournaments: Boolean? = null
    var setup_rank_set: Boolean? = null
    var country: String? = null
    var pro: Boolean? = null
    var aga_valid: Any? = null
    var supporter: Boolean? = null
    var provisional: Int? = null
    var is_moderator: Boolean? = null
    var is_superuser: Boolean? = null
    var supporter_last_nagged: String? = null
    var anonymous: Boolean? = null
    var tournament_admin: Boolean? = null
    var auto_advance_after_submit: Boolean? = null
    var hide_recently_finished_games: Boolean? = null
    var id: Int = 0
    var icon: String? = null

}

data class ChallengeResp(
        val game: Int,
        val name: String
)

