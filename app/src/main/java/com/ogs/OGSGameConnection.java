package com.ogs;

/**
 * Created by njones on 10/30/16.
 */

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class OGSGameConnection {
    private Socket socket;

    public void setMoveListener(MoveListener moveListener) {
        this.moveListener = moveListener;
    }

    public static interface MoveListener {

        public void call(int x, int y);
    }

    private MoveListener moveListener;


    OGSGameConnection(Socket socket, int gameId, int userId) {
        socket.on("game/" + gameId + "/clock", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                clock(obj);
            }
        });
        socket.on("game/" + gameId + "/gamedata", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                gamedata(obj);
            }
        });
        socket.on("game/" + gameId + "/move", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                move(obj);
            }
        });
        try {
            JSONObject obj = new JSONObject();
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            obj.put("chat", false);
            socket.emit("game/connect", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gamedata(JSONObject obj) {
        try {
            Log.w("OGSGameConnection", "on gamedata: " + obj.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void move(JSONObject obj) {
        try {
            Log.w("OGSGameConnection", "on move: " + obj.toString(2));
            if (moveListener != null) {
                JSONArray a = obj.getJSONArray("move");
                moveListener.call(a.getInt(0), a.getInt(1));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void clock(JSONObject obj) {
        try {
            Log.w("OGSGameConnection", "on clock: " + obj.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
