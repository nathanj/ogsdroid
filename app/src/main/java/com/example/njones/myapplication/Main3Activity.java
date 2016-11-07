package com.example.njones.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class Main3Activity extends AppCompatActivity {

    private OGS ogs;
    private OGSGameConnection gameCon;
    private int currentGameId;
    private String phase = "play";

    private static final String TAG = "Main3Activity";
    private AppCompatActivity activity;
    BoardView bv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bv = (BoardView) findViewById(R.id.boardview);

        Intent intent = getIntent();
        currentGameId = intent.getIntExtra("id", 0);
        Log.w("AAAAAAAAAAA", "the id was " + currentGameId);

        getSupportActionBar().setHomeButtonEnabled(true);

        //sv = (SurfaceView) findViewById(R.id.surfaceView);
        //SurfaceHolder sh = sv.getHolder();
        //sh.addCallback(this);
        activity = this;

        //sv.setOnClickListener(this);
        //sv.setOnTouchListener(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //*
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
//            currentGameId = 6790027;

            JSONObject gameDetails = ogs.getGameDetails(currentGameId);
            int height = gameDetails.getJSONObject("gamedata").getInt("height");
            int width = gameDetails.getJSONObject("gamedata").getInt("width");
            bv.initBoard(height, width);
            phase = gameDetails.getJSONObject("gamedata").getString("phase");
            JSONArray moves = gameDetails.getJSONObject("gamedata").getJSONArray("moves");
            final String whitePlayer = gameDetails.getJSONObject("players").getJSONObject("white").getString("username");
            int whiteId = gameDetails.getJSONObject("players").getJSONObject("white").getInt("id");
            final String blackPlayer = gameDetails.getJSONObject("players").getJSONObject("black").getString("username");
            final int blackId = gameDetails.getJSONObject("players").getJSONObject("black").getInt("id");
            int whoseTurn = gameDetails.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");

            if (phase.equals("play")) {
                if (whoseTurn == blackId)
                    setTitle(String.format("%s vs %s - Black to play", whitePlayer, blackPlayer));
                else
                    setTitle(String.format("%s vs %s - White to play", whitePlayer, blackPlayer));
            } else if (phase.equals("finished")) {
                int winner = gameDetails.getJSONObject("gamedata").getInt("winner");
                if (winner == blackId)
                    setTitle(String.format("%s vs %s - Black won", whitePlayer, blackPlayer));
                else
                    setTitle(String.format("%s vs %s - White won", whitePlayer, blackPlayer));
            } else if (phase.equals("stone removal")) {
                setTitle(String.format("%s vs %s - Stone removal", whitePlayer, blackPlayer));
            }

            final String auth = gameDetails.getString("auth");
            Log.w(TAG, moves.toString(2));

            for (int i = 0; i < moves.length(); i++) {
                JSONArray move = moves.getJSONArray(i);
                int x = move.getInt(0);
                int y = move.getInt(1);
                if (x == -1)
                    ;
                else
                    bv.board.addStone(move.getInt(0), move.getInt(1));
            }

            ogs.openSocket();
            gameCon = ogs.openGameConnection(currentGameId);

            bv.gameConnection = gameCon;
            bv.phase = phase;

            gameCon.setCallbacks(new OGSGameConnection.OGSGameConnectionCallbacks() {
                @Override
                public void move(int x, int y) {
                    if (x == -1)
                        ; // pass
                    else
                        bv.board.addStone(x, y);
                    bv.postInvalidate();
                    //surfaceCreated(sv.getHolder());
                }

                @Override
                public void clock(JSONObject clock) {
                    try {

                        Log.w(TAG, clock.toString());
                        final int whoseTurn = clock.getInt("current_player");

                        Object whiteTime = clock.get("white_time");
                        Object blackTime = clock.get("black_time");
                        if (clock.get("white_time") instanceof Number) {
                            bv.whiteClock.setTime(clock.getInt("white_time"), 0, 0);
                            bv.blackClock.setTime(clock.getInt("black_time"), 0, 0);
                        } else {
                            int thinkingTime = 0, periods = 0, periodTime = 0;
                            try {
                                JSONObject c = clock.getJSONObject("white_time");
                                thinkingTime = c.getInt("thinking_time");
                                periods = c.getInt("periods");
                                periodTime = c.getInt("period_time");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            bv.whiteClock.setTime(thinkingTime, periods, periodTime);

                            thinkingTime = 0; periods = 0; periodTime = 0;
                            try {
                                JSONObject c = clock.getJSONObject("black_time");
                                thinkingTime = c.getInt("thinking_time");
                                periods = c.getInt("periods");
                                periodTime = c.getInt("period_time");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            bv.blackClock.setTime(thinkingTime, periods, periodTime);
                        }

                        bv.blacksMove = whoseTurn == blackId;

                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                if (whoseTurn == blackId)
                                    setTitle(String.format("%s vs %s - Black to play", whitePlayer, blackPlayer));
                                else
                                    setTitle(String.format("%s vs %s - White to play", whitePlayer, blackPlayer));

                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }

                @Override
                public void phase(String p) {
                    bv.phase = p;
                    invalidateOptionsMenu();
                }

                @Override
                public void removedStones(JSONObject obj) {
                    try {
                        String coords = obj.getString("stones");
                        boolean removed = obj.getBoolean("removed");
                        bv.board.stoneRemoval(coords, removed);
                        bv.postInvalidate();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void removedStonesAccepted(JSONObject obj) {

                }

                @Override
                public void error(String msg) {
                    Log.e(TAG, "got ogs error: " + msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.pass:
                case R.id.resign:
                    item.setVisible(phase.equals("playing"));
                    break;
                case R.id.accept_stones:
                case R.id.reject_stones:
                    item.setVisible(phase.equals("stone removal"));
                    break;
                default:
                    item.setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pass:
                Log.w(TAG, "user chose pass");

                new AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to pass?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                gameCon.pass();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            case R.id.resign:
                Log.w(TAG, "user chose resign");
                new AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to resign?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            gameCon.resign();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

                return true;
            case R.id.accept_stones:
                Log.w(TAG, "user chose accept stones");
                new AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to accept stones?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            gameCon.acceptStones(bv.getBoard().getRemovedCoords());
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

                return true;
            case R.id.reject_stones:
                Log.w(TAG, "user chose reject stones");
                new AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to reject stones?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                gameCon.rejectStones();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
