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
    private static final String TAG = "OGSGameConnection";


    public static interface OGSGameConnectionCallbacks {
        public void move(int x, int y);
        public void clock(JSONObject clock);
        public void phase(JSONObject phase);
        public void removedStones(JSONObject obj);
        public void removedStonesAccepted(JSONObject obj);
        public void error(String msg);
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
        }).on("game/" + gameId + "/gamedata", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                gamedata(obj);
            }
        }).on("game/" + gameId + "/move", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                move(obj);
            }
        }).on("game/" + gameId + "/removed_stones", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                removedStones(obj);
            }
        }).on("game/" + gameId + "/removed_stones_accepted", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                removedStonesAccepted(obj);
            }
        }).on("game/" + gameId + "/error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String msg = (String) args[0];
                error(msg);
            }
        }).on("game/" + gameId + "/phase", new Emitter.Listener() {
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
//            Log.w(TAG, "on gamedata: " + obj.toString(2));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    private void move(JSONObject obj) {
        try {
            Log.w(TAG, "on move: " + obj.toString(2));
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
//            Log.w(TAG, "on clock: " + obj.toString(2));
            if (callbacks != null) {
                callbacks.clock(obj);
            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    private void phase(JSONObject obj) {
        try {
            Log.w(TAG, "on phase: " + obj.toString(2));
            if (callbacks != null) {
                callbacks.phase(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void removedStones(JSONObject obj) {
        try {
            Log.w(TAG, "on removed_stones: " + obj.toString(2));
            if (callbacks != null) {
                callbacks.removedStones(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void removedStonesAccepted(JSONObject obj) {
        try {
            Log.w(TAG, "on removed_stones_accepted: " + obj.toString(2));
            if (callbacks != null) {
                callbacks.removedStonesAccepted(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void error(String msg) {
            if (callbacks != null) {
                callbacks.error(msg);
            }
    }

    public void makeMove(String coord) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("auth", gameAuth);
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            obj.put("move", coord);
            Log.w(TAG, "json object: " + obj.toString());
            Log.w(TAG, "socket = " + socket);
            socket.emit("game/move", obj);
            Log.w(TAG, "emitted game connect");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void removeStones(String coords, boolean removed) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("auth", gameAuth);
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            obj.put("stones", coords);
            obj.put("removed", removed);
            Log.w(TAG, "json object: " + obj.toString());
            Log.w(TAG, "socket = " + socket);
            socket.emit("game/removed_stones/set", obj);
            Log.w(TAG, "emitted game removed_stones/set");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void acceptStones(String coords) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("auth", gameAuth);
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            obj.put("stones", coords);
            obj.put("strict_seki_mode", false);
            Log.w(TAG, "json object: " + obj.toString());
            Log.w(TAG, "socket = " + socket);
            socket.emit("game/removed_stones/accept", obj);
            Log.w(TAG, "emitted game removed_stones/accept");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void rejectStones() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("auth", gameAuth);
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            Log.w(TAG, "json object: " + obj.toString());
            Log.w(TAG, "socket = " + socket);
            socket.emit("game/removed_stones/reject", obj);
            Log.w(TAG, "emitted game removed_stones/reject");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void pass() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("auth", gameAuth);
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            obj.put("pass", true);
            socket.emit("game/pass", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
