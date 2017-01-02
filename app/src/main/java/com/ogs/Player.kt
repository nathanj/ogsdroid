package com.ogs

import org.json.JSONObject

class Player(obj: JSONObject) {
    val username: String = obj.getString("username")
    val id: Int = obj.getInt("id")
    val ranking: Int = obj.getInt("ranking")
}
