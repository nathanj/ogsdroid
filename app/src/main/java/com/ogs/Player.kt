package com.ogs

import org.json.JSONObject

class Player {
    val username: String
    val id: Int
    val ranking: Int

    constructor(obj: JSONObject) {
        username = obj.getString("username")
        ranking = obj.getInt("ranking")
        id = obj.getInt("id")
    }
}
