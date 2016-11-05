package com.ogs;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by njones on 11/4/16.
 */
public class SeekGraphConnection {
    private static final String TAG = "SeekGraphConnection";
    private final Socket socket;

    public interface SeekGraphConnectionCallbacks {
        void event(JSONArray events);
    }

    public void setCallbacks(SeekGraphConnectionCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    private SeekGraphConnectionCallbacks callbacks;

    SeekGraphConnection(OGS ogs, Socket socket) {
        this.socket = socket;
        socket.on("seekgraph/global", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONArray events = (JSONArray) args[0];
                callbacks.event(events);
            }
        });
        try {
            JSONObject args = new JSONObject();
            args.put("channel", "global");
            socket.emit("seek_graph/connect", args);
            Log.w(TAG, "opened seek graph");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
