package com.ogsdroid;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ogs.ChatMessage;
import com.ogs.GameConnection;
import com.ogs.OGS;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main3Activity extends AppCompatActivity {
    private static final String TAG = "Main3Activity";

    private GameConnection gameCon;
    private String phase = "play";

    private BoardView bv;
    private Board board;
    private String prefix = "";
    private GameData gamedata = null;
    private MediaPlayer clickSound;
    private MediaPlayer passSound;

    private int currentGameId;
    private OGS ogs;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(TAG, "onPostResume");

        //*
        try
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

            ogs = Globals.INSTANCE.getOGS();

            JSONObject gameDetails = ogs.getGameDetails(currentGameId);
            final GameDetails details = new GameDetails(gameDetails);
            board = new Board(details.handicap, details.height, details.width);
            bv.setBoard(board);

            bv.zoom = pref.getString("pref_zoom", "0");

            for (int i = 0; i < details.moves.length(); i++) {
                JSONArray move = details.moves.getJSONArray(i);
                int x = move.getInt(0);
                int y = move.getInt(1);
                if (x == -1)
                    bv.board.pass();
                else
                    bv.board.addStone(x, y);
            }

            if (details.removed != null) {
                bv.board.stoneRemoval(details.removed, true);
            }

            bv.blackPlayer = details.blackPlayer;
            bv.whitePlayer = details.whitePlayer;

            ogs.openSocket();
            gameCon = ogs.openGameConnection(currentGameId);

            bv.gameConnection = gameCon;
            bv.phase = phase;

            gameCon.setCallbacks(new GameConnection.OGSGameConnectionCallbacks() {
                @Override
                public void chat(@NotNull final ChatMessage msg) {
                    Log.d(TAG, msg.toString());

                    Main3Activity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!msg.getUsername().equals(ogs.getPlayer().getUsername())) {
                                Toast toast = Toast.makeText(Main3Activity.this, msg.toString(), Toast.LENGTH_LONG);
                                toast.show();
                            }

                            TextView tv = (TextView) findViewById(R.id.chat_text_view);
                            tv.setText(msg.toString() + "\n" + tv.getText());
                        }
                    });
                }

                @Override
                public void move(int x, int y) {
                    if (x == -1) {
                        bv.board.pass();
                        passSound.start();
                    } else {
                        bv.board.addStone(x, y);
                        clickSound.start();
                    }
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
                                //e.printStackTrace();
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
                            }
                            bv.clockBlack.setTime(thinkingTime, periods, periodTime);
                        }

                        if (gamedata != null) {
                            gamedata.whoseTurn = whoseTurn;
                            changeTitle();
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

                    changeTitle();
                }

                void changeTitle() {
                    if (gamedata == null)
                        return;
                    Main3Activity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if (phase.equals("play")) {
                                if (gamedata.whoseTurn == gamedata.blackId)
                                    setTitle(String.format("Black to play - %s", prefix));
                                else
                                    setTitle(String.format("White to play - %s", prefix));
                            } else if (phase.equals("finished")) {
                                if (gamedata.winner == gamedata.blackId)
                                    setTitle(String.format("Black wins by %s - %s", gamedata.outcome, prefix));
                                else
                                    setTitle(String.format("White wins by %s - %s", gamedata.outcome, prefix));
                            } else if (phase.equals("stone removal")) {
                                setTitle(String.format("Stone removal - %s", prefix));
                            }
                        }
                    });
                }

                @Override
                public void gamedata(JSONObject obj) {
                    gamedata = new GameData(obj);

                    try {
                        final TextView tv = (TextView) findViewById(R.id.chat_text_view);
                        JSONArray chats = obj.getJSONArray("chat_log");
                        Main3Activity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText("");
                            }
                        });
                        for (int i = 0; i < chats.length(); i++) {
                            JSONObject c = chats.getJSONObject(i);
                            final ChatMessage msg = new ChatMessage(c.getString("username"), c.getString("body"), c.getLong("date"));
                            Main3Activity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv.setText(msg.toString() + "\n" + tv.getText());
                                }
                            });
                        }
                    } catch (JSONException e) {
                    }

                    if (!phase.equals(gamedata.phase)) {
                        phase = gamedata.phase;
                        bv.phase = gamedata.phase;
                        invalidateOptionsMenu();
                    }

                    if (phase.equals("play"))
                        bv.board.unmarkTerritory();
                    else
                        bv.board.markTerritory();

                    prefix = String.format("%s vs %s", gamedata.whitePlayer, gamedata.blackPlayer);
                    changeTitle();
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
                public void error(final String msg) {
                    Log.e(TAG, "got ogs error: " + msg);
                    Main3Activity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(Main3Activity.this)
                                    .setMessage("OGS error: " + msg)
                                    .setCancelable(true)
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    })
                                    .show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        clickSound.release();
        passSound.release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (gameCon != null) {
            gameCon.disconnect();
            gameCon = null;
        }
        Globals.INSTANCE.putOGS();
        ogs = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Logger sioLogger = java.util.logging.Logger.getLogger(io.socket.client.Socket.class.getName());
        sioLogger.setLevel(Level.ALL);

        bv = (BoardView) findViewById(R.id.boardview);

        findViewById(R.id.chat_text_view).setVisibility(View.GONE);
        final EditText editText = (EditText) findViewById(R.id.chat_edit_text);
        editText.setVisibility(View.GONE);
        findViewById(R.id.chat_scroll_view).setVisibility(View.GONE);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_NULL
                        && keyEvent.getAction() == KeyEvent.ACTION_UP) {

                    gameCon.sendChatMessage(ogs.getPlayer(),
                            editText.getText().toString(), bv.board.moveNumber);
                    editText.setText("");
                }

                return true;
            }
        });

        Intent intent = getIntent();
        currentGameId = intent.getIntExtra("id", 0);

        getSupportActionBar().setHomeButtonEnabled(true);

        clickSound = MediaPlayer.create(this, R.raw.click);
        passSound = MediaPlayer.create(this, R.raw.pass);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (bv.zoomed) {
                bv.unZoom();
                return true;
            }
        }
        final TextView v = (TextView) findViewById(R.id.chat_text_view);
        // If the chat is visible then go back to the game
        if (v.getVisibility() == View.VISIBLE) {
            final EditText v2 = (EditText) findViewById(R.id.chat_edit_text);
            final ScrollView sv = (ScrollView) findViewById(R.id.chat_scroll_view);
            sv.setVisibility(View.GONE);
            v.setVisibility(View.GONE);
            v2.setVisibility(View.GONE);
            bv.setVisibility(View.VISIBLE);
            return true;
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
            case R.id.chat:
                final TextView v = (TextView) findViewById(R.id.chat_text_view);
                final EditText v2 = (EditText) findViewById(R.id.chat_edit_text);
                final ScrollView sv = (ScrollView) findViewById(R.id.chat_scroll_view);


                if (v.getVisibility() == View.GONE) {
                    sv.setVisibility(View.VISIBLE);
                    v.setVisibility(View.VISIBLE);
                    v2.setVisibility(View.VISIBLE);

                    sv.fullScroll(View.FOCUS_UP);

//                    v2.setImeActionLabel("Send", KeyEvent.KEYCODE_ENTER);


                    bv.setVisibility(View.GONE);
                } else {
                    sv.setVisibility(View.GONE);
                    v.setVisibility(View.GONE);
                    v2.setVisibility(View.GONE);
                    bv.setVisibility(View.VISIBLE);
                }


                return true;
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
        public int handicap;
        public String phase;
        public JSONArray moves;
        public String whitePlayer, blackPlayer;
        public int whiteId, blackId;
        public int whoseTurn;
        public String gameAuth;
        public String removed;

        GameDetails(JSONObject gameDetails) {
            try {
                height = gameDetails.getJSONObject("gamedata").getInt("height");
                width = gameDetails.getJSONObject("gamedata").getInt("width");
                phase = gameDetails.getJSONObject("gamedata").getString("phase");
                moves = gameDetails.getJSONObject("gamedata").getJSONArray("moves");
                handicap = gameDetails.getJSONObject("gamedata").getInt("handicap");
                try {
                    removed = gameDetails.getJSONObject("gamedata").getString("removed");
                } catch (JSONException e) {

                }

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
        public String outcome;

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
                    winner = obj.getInt("winner");
                    outcome = obj.getString("outcome");
                } catch (JSONException e) {
                    winner = 1;
                    outcome = "";
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

// }}}
}
