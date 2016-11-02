package com.example.njones.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.ogs.OGS;
import com.ogs.OGSGameConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Main3Activity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener, View.OnTouchListener {

    private SurfaceView sv;
    private OGS ogs;
    private OGSGameConnection gameCon;
    private int currentGameId;
    private String phase = "play";

    private static final String TAG = "Main3Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);

        sv = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);

        sv.setOnClickListener(this);
        sv.setOnTouchListener(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        try {
            ogs = new OGS("ee20259490eabd6e8fba",
                    "31ce3312e5dd2b0a189c8249c3d66fd661834f32");
            ogs.setAccessToken("ec2ce38a81fbd2b708eb069cee764f907dbbe3e4");
            //ogs.login("nathanj439", "691c9d7a8986c29d80a0c13cb509d986");
            JSONObject obj = ogs.me();
            Log.w(TAG, obj.toString(2));
            //            obj = ogs.listServerChallenges();

            //            JSONArray results = obj.getJSONArray("results");
            //            Log.w("myApp", "challenge length = " + results.length());
            //            JSONObject obj2 = results.getJSONObject(0);
            //            Log.w("myApp", obj2.toString(2));

            //            JSONObject games = ogs.listGames();
            //            Log.w("myApp", "games length = " + games.length());
            //            JSONArray results = games.getJSONArray("results");
            //            JSONObject game = results.getJSONObject(0);
            //            currentGameId = game.getInt("id");
            currentGameId = 6772417;

            JSONObject gameDetails = ogs.getGameDetails(currentGameId);
            phase = gameDetails.getJSONObject("gamedata").getString("phase");
            phase = "stone removal";
            JSONArray moves = gameDetails.getJSONObject("gamedata").getJSONArray("moves");

            final String auth = gameDetails.getString("auth");
            Log.w(TAG, moves.toString(2));

            for (int i = 0; i < moves.length(); i++) {
                JSONArray move = moves.getJSONArray(i);
                int x = move.getInt(0);
                int y = move.getInt(1);
                if (x == -1)
                    ;
                else
                    board.addStone(move.getInt(0), move.getInt(1));
            }

            ogs.openSocket();
            gameCon = ogs.openGameConnection(currentGameId);

            gameCon.setCallbacks(new OGSGameConnection.OGSGameConnectionCallbacks() {
                @Override
                public void move(int x, int y) {
                    if (x == -1)
                        ; // pass
                    else
                        board.addStone(x, y);
                    surfaceCreated(sv.getHolder());
                }

                @Override
                public void clock(JSONObject clock) {
                    Log.w(TAG, clock.toString());
                }

                @Override
                public void phase(JSONObject phase2) {
                    phase = phase2.toString();
                }

                @Override
                public void removedStones(JSONObject obj) {
                    try {
                        String coords = obj.getString("stones");
                        boolean removed = obj.getBoolean("removed");
                        board.stoneRemoval(coords, removed);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void removedStonesAccepted(JSONObject obj) {

                }

                @Override
                public void error(JSONObject obj) {
                    Log.e(TAG, "got ogs error: " + obj.toString());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


        //*

        // */

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }

    private Board board = new Board();

    public void surfaceCreated(SurfaceHolder holder) {
        Canvas c = holder.lockCanvas();
        Log.w(TAG, "c = " + c);

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

    @Override
    public void onClick(View view) {
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        Log.w(TAG, event.getX() + " " + event.getY() + " " + event.getAction());

        if (phase.equals("play")) {
            if ((event.getAction() & MotionEvent.ACTION_POINTER_DOWN) > 0) {
                String moveStr = board.addStoneAtTouch(view.getWidth(),
                        view.getHeight(), event.getX(), event.getY());
                Log.w(TAG, "moveStr = " + moveStr);
                if (moveStr.length() > 0)
                    gameCon.makeMove(moveStr);
            }
        } else if (phase.equals("stone removal")) {
            if ((event.getAction() & MotionEvent.ACTION_POINTER_DOWN) > 0) {
                board.stoneRemovalAtTouch(view.getWidth(),
                        view.getHeight(), event.getX(), event.getY());
            }
        }

        surfaceCreated(sv.getHolder());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
//        tb.inflateMenu(R.menu.menu_main);
        return true;
    }
}
