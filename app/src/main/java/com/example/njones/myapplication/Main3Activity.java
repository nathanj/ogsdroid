package com.example.njones.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Main3Activity extends AppCompatActivity implements SurfaceHolder.Callback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);


        final Socket socket;
        try {
            socket = IO.socket("https://ggs.online-go.com");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("game_id", 6726727);
                            obj.put("player_id", 212470);
                            obj.put("chat", true);
                            Log.w("myApp", "json object: " + obj.toString());
                            socket.emit("game/connect", obj);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }

                    JSONObject obj = new JSONObject();
                    try

                    {
                        obj.put("auth", "ee71d495f4500328d86c0ccba756d250");
                        obj.put("game_id", 6726727);
                        obj.put("player_id", 212470);
                        obj.put("move", "ad");
                        Log.w("myApp", "json object: " + obj.toString());
                        socket.emit("game/move", obj);
                        Log.w("myApp", "emitted game connect");
                    } catch (
                            JSONException e
                            )

                    {
                        e.printStackTrace();
                    }

                    //socket.disconnect();
                }

            }).on("event", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];
                    Log.w("myApp", "on event: " + obj.toString());
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.w("myApp", "socket disconnect");
                }

            }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];
                    Log.w("myApp", "on message: " + obj.toString());
                }
            }).on("game/6726727/gamedata", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];
                    try {
                        Log.w("myApp", "on gamedata: " + obj.toString(2));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            socket.connect();
        } catch (
                URISyntaxException e
                )

        {
            e.printStackTrace();
        }

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

    }

    public void surfaceCreated(SurfaceHolder holder) {
        Board board = new Board();

        Canvas c = holder.lockCanvas();

        Bitmap bmpIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.board);

        Paint p = new Paint();
        p.setARGB(128, 128, 128, 128);
        c.drawLine(10, 10, 80, 80, p);
        //c.drawBitmap(bmpIcon, 50, 50, null);
        Rect r = new Rect();
        r.set(0, 0, bmpIcon.getWidth(), bmpIcon.getHeight());
        Rect r2 = new Rect();
        r2.set(0, 0, c.getWidth(), c.getHeight());
        c.drawBitmap(bmpIcon, null, r2, null);

        board.draw(c);

        holder.unlockCanvasAndPost(c);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
