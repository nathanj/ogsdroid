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
    public static interface OGSGameConnectionCallbacks {
        public void move(int x, int y);
        public void clock(JSONObject clock);
        public void phase(JSONObject phase);
    }

    private Socket socket;
    private String gameAuth;
    private OGSGameConnectionCallbacks callbacks;
    private int gameId;
    private int userId;

    public void setCallbacks(OGSGameConnectionCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    OGSGameConnection(OGS ogs, Socket socket, int gameId, int userId) {
        this.gameId = gameId;
        this.userId = userId;
        this.socket = socket;
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
        socket.on("game/" + gameId + "/phase", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                phase(obj);
            }
        });
        try {
            JSONObject gameDetails = ogs.getGameDetails(gameId);
            gameAuth = gameDetails.getString("auth");

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
//        try {
//            Log.w("OGSGameConnection", "on gamedata: " + obj.toString(2));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    private void move(JSONObject obj) {
        try {
            Log.w("OGSGameConnection", "on move: " + obj.toString(2));
            if (callbacks != null) {
                JSONArray a = obj.getJSONArray("move");
                callbacks.move(a.getInt(0), a.getInt(1));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void clock(JSONObject obj) {
//        try {
//            Log.w("OGSGameConnection", "on clock: " + obj.toString(2));
            if (callbacks != null) {
                callbacks.clock(obj);
            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    private void phase(JSONObject obj) {
        try {
            Log.w("OGSGameConnection", "on phase: " + obj.toString(2));
            if (callbacks != null) {
                callbacks.phase(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void makeMove(String coord) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("auth", gameAuth);
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            obj.put("move", coord);
            Log.w("myApp", "json object: " + obj.toString());
            Log.w("myApp", "socket = " + socket);
            socket.emit("game/move", obj);
            Log.w("myApp", "emitted game connect");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
