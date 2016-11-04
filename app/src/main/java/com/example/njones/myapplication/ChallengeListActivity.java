package com.example.njones.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ogs.OGS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChallengeListActivity extends AppCompatActivity implements OnItemClickListener {
    private static final String TAG = "ChallengeListActivity";
    private OGS ogs;

    private class Game implements Comparable<Game> {
        int id;
        String name;
        boolean myturn;

        @Override
        public String toString() {
            return name;
        }

        public int compareTo(Game game) {
            if (myturn && !game.myturn)
                return -1;
            if (!myturn && game.myturn)
                return 1;
            return 0;
        }
    }

    ArrayList<Game> gameList;

    private class Challenge {
        String name;
        String challenger;
        boolean ranked;
        int ranking;
        int minRanking, maxRanking;
        int handicap;
        int timePerMove;

        void Challenge(JSONObject obj) {
            challenger = obj.getJSONObject("challenger").getString("username");
            name = obj.getJSONObject("game").getString("name");
            timePerMove = obj.getJSONObject("game").getInt("time_per_move");
            ranked = obj.getJSONObject("game").getInt("ranked");
            ranking = obj.getJSONObject("challenger").getInt("ranking");
            minRanking = obj.getInt("min_ranking");
            maxRanking = obj.getInt("max_ranking");
            handicap = obj.getJSONObject("game").getInt("handicap");
        }

        @Override
        public String toString() {
            return String.format("%s - %s (%d) (%d - %d) - %s - %d seconds per move",
                    name, challenger, ranking, minRanking, maxRanking,
                    ranked ? "Ranked" : "Casual", timePerMove);
        }
    }

    private class GetChallengeList extends AsyncTask<OGS, Void, ArrayList<Game>> {
        protected ArrayList<Game> doInBackground(OGS... ogss) {
            OGS ogs = ogss[0];
            ArrayList<Game> gameList = new ArrayList<>();
            try {

                //-----------

                JSONObject serverChallenges = ogs.listServerChallenges();

                JSONArray results = obj.getJSONArray("results");
                Log.w(TAG, "challenge length = " + results.length());
                for (int i = 0; i < results.length(); i++) {
                    JSONObject obj = results.getJSONObject(i);
                    Challenge c = new Challenge(obj);
                    Log.w(TAG, c.toString());
                }

                //-----------

                JSONObject me = ogs.me();
                int myid = me.getInt("id");
                JSONObject games = ogs.listGames();
                JSONArray results = games.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject game = results.getJSONObject(i);
                    int id = game.getInt("id");
                    JSONObject details = ogs.getGameDetails(id);
                    Log.w(TAG, "game = " + details.toString());
                    Game g = new Game();
                    g.id = id;
                    String white = game.getJSONObject("players").getJSONObject("white").getString("username");
                    String black = game.getJSONObject("players").getJSONObject("black").getString("username");
                    int currentPlayer = game.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");
                    if (myid == currentPlayer)
                    {
                        g.myturn = true;
                        g.name = String.format("Your move - %s vs %s", white, black);
                    }
                    else
                    {
                        g.myturn = false;
                        g.name = String.format("Opponent's move - %s vs %s", white, black);
                    }
                    gameList.add(g);
                }
                Collections.sort(gameList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return gameList;
        }

    }

    private class GetGameList extends AsyncTask<OGS, Void, ArrayList<Game>> {
        protected ArrayList<Game> doInBackground(OGS... ogss) {
            OGS ogs = ogss[0];
            ArrayList<Game> gameList = new ArrayList<>();
            try {
                JSONObject me = ogs.me();
                int myid = me.getInt("id");
                JSONObject games = ogs.listGames();
                JSONArray results = games.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject game = results.getJSONObject(i);
                    int id = game.getInt("id");
                    JSONObject details = ogs.getGameDetails(id);
                    Log.w(TAG, "game = " + details.toString());
                    Game g = new Game();
                    g.id = id;
                    String white = game.getJSONObject("players").getJSONObject("white").getString("username");
                    String black = game.getJSONObject("players").getJSONObject("black").getString("username");
                    int currentPlayer = game.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");
                    if (myid == currentPlayer)
                    {
                        g.myturn = true;
                        g.name = String.format("Your move - %s vs %s", white, black);
                    }
                    else
                    {
                        g.myturn = false;
                        g.name = String.format("Opponent's move - %s vs %s", white, black);
                    }
                    gameList.add(g);
                }
                Collections.sort(gameList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return gameList;
        }

        protected void onPostExecute(ArrayList<Game> list) {
            gameList = list;

            ListView lv = (ListView) findViewById(R.id.challenge_list);
            ArrayAdapter<Game> adapter = (ArrayAdapter<Game>) lv.getAdapter();
            adapter.clear();
            adapter.addAll(gameList);

            invalidate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_list);

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);

        ogs = new OGS("ee20259490eabd6e8fba",
                "31ce3312e5dd2b0a189c8249c3d66fd661834f32");
        ogs.setAccessToken("ec2ce38a81fbd2b708eb069cee764f907dbbe3e4");

        Game g = new Game();
        g.id = -1;
        g.name = "Loading...";
        gameList.add(g);

        ListView lv = (ListView) findViewById(R.id.challenge_list);

        ArrayAdapter<Game> adapter = new ArrayAdapter<>(this,
                R.layout.activity_listview,
                gameList);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        new GetGameList().execute(ogs);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.w(TAG, "i = " + position + " l = " + id);

        Game game = gameList.get(position);
        if (game.id == -1)
            return;
        Intent intent = new Intent(this, Main3Activity.class);
        intent.putExtra("id", game.id);
        startActivity(intent);
    }
}
