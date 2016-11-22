package com.ogs;

/**
 * Created by njones on 10/30/16.
 */

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class OGSGameConnection {
    private static final String TAG = "OGSGameConnection";


    public static interface OGSGameConnectionCallbacks {
        public void move(int x, int y);

        public void clock(JSONObject clock);

        public void phase(String p);

        public void removedStones(JSONObject obj);

        public void removedStonesAccepted(JSONObject obj);

        public void error(String msg);

        public void gamedata(JSONObject obj);
    }

    public interface OGSGameConnectionResetCallback {
        void reset();
    }

    private Socket socket;
    private String gameAuth;
    private OGSGameConnectionCallbacks callbacks;
    private OGSGameConnectionResetCallback resetCallback;
    private int gameId;
    private int userId;

    public void setCallbacks(OGSGameConnectionCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setResetCallback(OGSGameConnectionResetCallback callback) {
        this.resetCallback = callback;
    }

    OGSGameConnection(OGS ogs, Socket socket, int gameId, int userId) {
        this.gameId = gameId;
        this.userId = userId;
        this.socket = socket;
        socket.on("game/" + gameId + "/clock", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                Log.d(TAG, "got clock = " + obj);
                clock(obj);
            }
        }).on("game/" + gameId + "/gamedata", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                Log.d(TAG, "got gamedata = " + obj);
                gamedata(obj);
            }
        }).on("game/" + gameId + "/move", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                Log.d(TAG, "got move = " + obj);
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
                String p = (String) args[0];
                phase(p);
            }
        }).on("game/" + gameId + "/reset", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                reset();
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

    public void disconnect() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("game_id", gameId);
            socket.emit("game/disconnect", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gamedata(JSONObject obj) {
        Log.d(TAG, "on gamedata: " + obj.toString());
        callbacks.gamedata(obj);
    }

    private void move(JSONObject obj) {
        try {
            if (callbacks != null) {
                JSONArray a = obj.getJSONArray("move");
                callbacks.move(a.getInt(0), a.getInt(1));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void reset() {
        if (resetCallback != null)
            resetCallback.reset();
    }

    private void clock(JSONObject obj) {
//        try {
//            Log.d(TAG, "on clock: " + obj.toString(2));
        if (callbacks != null) {
            callbacks.clock(obj);
        }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    private void phase(String p) {
        Log.d(TAG, "on phase: " + p);
        if (callbacks != null) {
            callbacks.phase(p);
        }
    }

    private void removedStones(JSONObject obj) {
        try {
            Log.d(TAG, "on removed_stones: " + obj.toString(2));
            if (callbacks != null) {
                callbacks.removedStones(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void removedStonesAccepted(JSONObject obj) {
        try {
            Log.d(TAG, "on removed_stones_accepted: " + obj.toString(2));
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
            Log.d(TAG, "sending move = " + obj.toString());
            socket.emit("game/move", obj);
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
            Log.d(TAG, "doing set removed stones: " + obj.toString());
            socket.emit("game/removed_stones/set", obj);
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
            Log.d(TAG, "doing accept removed stones " + obj.toString());
            socket.emit("game/removed_stones/accept", obj);
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
            Log.d(TAG, "doing reject removed stones: " + obj.toString());
            socket.emit("game/removed_stones/reject", obj);
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
            obj.put("move", "..");
            Log.d(TAG, "doing pass = " + obj);
            socket.emit("game/move", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void resign() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("auth", gameAuth);
            obj.put("game_id", gameId);
            obj.put("player_id", userId);
            Log.d(TAG, "doing resign = " + obj);
            socket.emit("game/resign", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void waitForStart() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("game_id", gameId);
            socket.emit("game/wait", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
