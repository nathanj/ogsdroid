package com.ogs;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SeekGraphConnection {
    private static final String TAG = "SeekGraphConnection";
    private final Socket socket;

    public interface SeekGraphConnectionCallbacks {
        void event(JSONArray events);
    }

    SeekGraphConnection(OGS ogs, Socket socket, final SeekGraphConnectionCallbacks callbacks) {
        this.socket = socket;
        socket.on("seekgraph/global", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONArray events = (JSONArray) args[0];
                //Log.d(TAG, "seekgraph calllback = " + events);
                callbacks.event(events);
            }
        });
        try {
            JSONObject args = new JSONObject();
            args.put("channel", "global");
            socket.emit("seek_graph/connect", args);
            Log.d(TAG, "opened seek graph");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        socket.off("seekgraph/global");
        try {
            JSONObject args = new JSONObject();
            args.put("channel", "global");
            socket.emit("seek_graph/disconnect", args);
            Log.d(TAG, "opened seek graph");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
