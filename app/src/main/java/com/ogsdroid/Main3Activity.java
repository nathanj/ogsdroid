package com.ogsdroid;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.ogs.OGS;
import com.ogs.OGSGameConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main3Activity extends AppCompatActivity {
    private static final String TAG = "Main3Activity";

    private OGS ogs;
    private OGSGameConnection gameCon;
    private int currentGameId;
    private String phase = "play";

    private AppCompatActivity activity;
    private BoardView bv;
    private Board board;

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
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String accessToken = pref.getString("accessToken", "");

            // TODO - global ogs and global me
            OGS ogs = new OGS("82ff83f2631a55273c31", "cd42d95fd978348d57dc909a9aecd68d36b17bd2");
            ogs.setAccessToken(accessToken);
            JSONObject obj = ogs.me();
            Log.d(TAG, obj.toString());

            JSONObject gameDetails = ogs.getGameDetails(currentGameId);
            final GameDetails details = new GameDetails(gameDetails);
            board = new Board(details.height, details.width);
            bv.setBoard(board);

            bv.zoom = pref.getString("pref_zoom", "0");

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

                        Log.d("njclock", clock.toString());
                        final int whoseTurn = clock.getInt("current_player");

                        if (clock.get("white_time") instanceof Number) {
                            long now = System.currentTimeMillis();
                            long now_delta = now - clock.getLong("now");
                            long base_time = clock.getLong("last_move") + now_delta;
                            Log.d("njclock", "now = " + now);
                            Log.d("njclock", "now_delta = " + now_delta);
                            Log.d("njclock", "base_time = " + base_time);
                            Log.d("njclock", "black time = " + clock.getLong("black_time"));
                            Log.d("njclock", "delta = " + (clock.getLong("black_time") - System.currentTimeMillis()));
                            bv.clockWhite.setTime((int) (clock.getLong("white_time") - System.currentTimeMillis()) / 1000, 0, 0);
                            bv.clockBlack.setTime((int) (clock.getLong("black_time") - System.currentTimeMillis()) / 1000, 0, 0);
                        } else {
                            int thinkingTime = 0, periods = 0, periodTime = 0;
                            try {
                                JSONObject c = clock.getJSONObject("white_time");
                                Log.d("njclock", "white = " + c);
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
                                Log.d("njclock", "black = " + c);
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
                    if (p.equals("play")) {
                        bv.board.unmarkTerritory();
                        bv.board.unmarkRemoved();
                    } else
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (bv.zoomed) {
                bv.unZoom();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // {{{ options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "options menu phase = " + phase);
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
                Log.d(TAG, "user chose pass");

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
                Log.d(TAG, "user chose resign");
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
                Log.d(TAG, "user chose accept stones");
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
                Log.d(TAG, "user chose reject stones");
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
                Log.d(TAG, "gamedata = " + obj.toString());
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

    // }}}
}