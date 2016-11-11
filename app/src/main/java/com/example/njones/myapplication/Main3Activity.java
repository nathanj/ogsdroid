package com.example.njones.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
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
    private static final String TAG = "Main3Activity";

    private OGS ogs;
    private OGSGameConnection gameCon;
    private int currentGameId;
    private String phase = "play";

    private AppCompatActivity activity;
    private BoardView bv;
    private Board board;

    private class GameDetails {
        public int height, width;
        public String phase;
        public JSONArray moves;
        public String whitePlayer, blackPlayer;
        public int whiteId, blackId;
        public int whoseTurn;
        public String gameAuth;

        GameDetails(JSONObject gameDetails) {
            try {
                height = gameDetails.getJSONObject("gamedata").getInt("height");
                width = gameDetails.getJSONObject("gamedata").getInt("width");
                phase = gameDetails.getJSONObject("gamedata").getString("phase");
                moves = gameDetails.getJSONObject("gamedata").getJSONArray("moves");

                whitePlayer = gameDetails.getJSONObject("players").getJSONObject("white").getString("username");
                whiteId = gameDetails.getJSONObject("players").getJSONObject("white").getInt("id");
                blackPlayer = gameDetails.getJSONObject("players").getJSONObject("black").getString("username");
                blackId = gameDetails.getJSONObject("players").getJSONObject("black").getInt("id");
                whoseTurn = gameDetails.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");

                gameAuth = gameDetails.getString("auth");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class GameData {
        public String phase;
        public String whitePlayer, blackPlayer;
        public int whiteId, blackId;
        public int whoseTurn;
        public int winner;

        GameData(JSONObject obj) {
            try {
                Log.w(TAG, "gamedata = " + obj.toString());
                phase = obj.getString("phase");

                whitePlayer = obj.getJSONObject("players").getJSONObject("white").getString("username");
                whiteId = obj.getJSONObject("players").getJSONObject("white").getInt("id");
                blackPlayer = obj.getJSONObject("players").getJSONObject("black").getString("username");
                blackId = obj.getJSONObject("players").getJSONObject("black").getInt("id");
                try {
                    whoseTurn = obj.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");
                } catch (JSONException e) {
                    whoseTurn = 1;
                }
                try {
                    winner = obj.getJSONObject("gamedata").getInt("winner");
                } catch (JSONException e) {
                    winner = 1;
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bv = (BoardView) findViewById(R.id.boardview);

        Intent intent = getIntent();
        currentGameId = intent.getIntExtra("id", 0);

        getSupportActionBar().setHomeButtonEnabled(true);

        activity = this;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //*
        try {
            // TODO - global ogs and global me
            ogs = new OGS("ee20259490eabd6e8fba",
                    "31ce3312e5dd2b0a189c8249c3d66fd661834f32");
            ogs.setAccessToken("ec2ce38a81fbd2b708eb069cee764f907dbbe3e4");
            JSONObject obj = ogs.me();
            Log.w(TAG, obj.toString());

            JSONObject gameDetails = ogs.getGameDetails(currentGameId);
            final GameDetails details = new GameDetails(gameDetails);
            board = new Board(details.height, details.width);
            bv.setBoard(board);

            for (int i = 0; i < details.moves.length(); i++) {
                JSONArray move = details.moves.getJSONArray(i);
                int x = move.getInt(0);
                int y = move.getInt(1);
                if (x == -1)
                    bv.board.pass();
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
                        bv.board.pass();
                    else
                        bv.board.addStone(x, y);
                    bv.postInvalidate();
                }

                @Override
                public void clock(JSONObject clock) {
                    try {

                        Log.w("njclock", clock.toString());
                        final int whoseTurn = clock.getInt("current_player");

                        if (clock.get("white_time") instanceof Number) {
                            long now = System.currentTimeMillis();
                            long now_delta = now - clock.getLong("now");
                            long base_time = clock.getLong("last_move") + now_delta;
                            Log.w("njclock", "now = " + now);
                            Log.w("njclock", "now_delta = " + now_delta);
                            Log.w("njclock", "base_time = " + base_time);
                            Log.w("njclock", "black time = " + clock.getLong("black_time"));
                            Log.w("njclock", "delta = " + (clock.getLong("black_time") - System.currentTimeMillis()));
                            bv.clockWhite.setTime((int) (clock.getLong("white_time") - System.currentTimeMillis()) / 1000, 0, 0);
                            bv.clockBlack.setTime((int) (clock.getLong("black_time") - System.currentTimeMillis()) / 1000, 0, 0);
                        } else {
                            int thinkingTime = 0, periods = 0, periodTime = 0;
                            try {
                                JSONObject c = clock.getJSONObject("white_time");
                                Log.w("njclock", "white = " + c);
                                thinkingTime = c.getInt("thinking_time");
                                periods = c.getInt("periods");
                                periodTime = c.getInt("period_time");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            bv.clockWhite.setTime(thinkingTime, periods, periodTime);

                            thinkingTime = 0;
                            periods = 0;
                            periodTime = 0;
                            try {
                                JSONObject c = clock.getJSONObject("black_time");
                                Log.w("njclock", "black = " + c);
                                thinkingTime = c.getInt("thinking_time");
                                periods = c.getInt("periods");
                                periodTime = c.getInt("period_time");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            bv.clockBlack.setTime(thinkingTime, periods, periodTime);
                        }

                        bv.blacksMove = whoseTurn == details.blackId;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void phase(String p) {
                    if (!phase.equals(p)) {
                        phase = p;
                        bv.phase = p;
                        invalidateOptionsMenu();
                    }
                    if (p.equals("play"))
                        bv.board.unmarkTerritory();
                    else
                        bv.board.markTerritory();
                }

                @Override
                public void gamedata(JSONObject obj) {
                    final GameData gamedata = new GameData(obj);
                    if (!phase.equals(gamedata.phase)) {
                        phase = gamedata.phase;
                        bv.phase = gamedata.phase;
                        invalidateOptionsMenu();
                    }

                    if (phase.equals("play"))
                        bv.board.unmarkTerritory();
                    else
                        bv.board.markTerritory();

                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            if (phase.equals("play")) {
                                if (gamedata.whoseTurn == gamedata.blackId)
                                    setTitle(String.format("%s vs %s - Black to play", gamedata.whitePlayer, gamedata.blackPlayer));
                                else
                                    setTitle(String.format("%s vs %s - White to play", gamedata.whitePlayer, gamedata.blackPlayer));
                            } else if (phase.equals("finished")) {
                                if (gamedata.winner == gamedata.blackId)
                                    setTitle(String.format("%s vs %s - Black won", gamedata.whitePlayer, gamedata.blackPlayer));
                                else
                                    setTitle(String.format("%s vs %s - White won", gamedata.whitePlayer, gamedata.blackPlayer));
                            } else if (phase.equals("stone removal")) {
                                setTitle(String.format("%s vs %s - Stone removal", gamedata.whitePlayer, gamedata.blackPlayer));
                            }
                        }
                    });
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

    // {{{ options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.w(TAG, "options menu phase = " + phase);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.pass:
                case R.id.resign:
                    item.setVisible(phase.equals("play"));
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
                                gameCon.acceptStones(board.getRemovedCoords());
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

    // }}}
}
