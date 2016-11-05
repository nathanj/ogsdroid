package com.example.njones.myapplication;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
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
import com.ogs.OGSGameConnection;
import com.ogs.SeekGraphConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

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

    ArrayList<Game> gameList = new ArrayList<>();
    ArrayList<Challenge> challengeList = new ArrayList<>();

    private class Challenge implements Comparable<Challenge> {
        int challengeId;
        String name;
        String username;
        boolean ranked;
        int rank;
        int minRank, maxRank;
        int handicap;
        int timePerMove;
        int width, height;

        Challenge(int id) {
            challengeId = id;
        }

        Challenge(JSONObject obj) {
            try {
                challengeId = obj.getInt("challenge_id");
                username = obj.getString("username");
                name = obj.getString("name");
                timePerMove = obj.getInt("time_per_move");
                ranked = obj.getBoolean("ranked");
                rank = obj.getInt("rank");
                minRank = obj.getInt("min_rank");
                maxRank = obj.getInt("max_rank");
                handicap = obj.getInt("handicap");
                width = obj.getInt("width");
                height = obj.getInt("height");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private String rankToString(int rank) {
            if (rank < 30)
                return String.format("%d Kyu", 30 - rank);
            else
                return String.format("%d Dan", rank - 30 + 1);
        }

        @Override
        public String toString() {
            return String.format("%s - %dx%d - %s (%s) - %s - %ds / move",
                    name, width, height, username, rankToString(rank),
                    ranked ? "Ranked" : "Casual", timePerMove);
        }

        @Override
        public boolean equals(Object obj) {
            Challenge other = (Challenge) obj;
            if (other == null)
                return false;

            return other.challengeId == challengeId;
        }

        public boolean canAccept(int myRanking) {
            return myRanking >= minRank && myRanking <= maxRank && (!ranked || Math.abs(myRanking - rank) <= 9);
        }

        public int compareTo(Challenge challenge) {
		if (challenge.timePerMove == timePerMove) {
			return rank - challenge.rank;
		}
		return timePerMove - challenge.timePerMove;
        }
    }

    private class GetChallengeList extends AsyncTask<OGS, Void, ArrayList<Game>> {
        protected ArrayList<Game> doInBackground(OGS... ogss) {
            OGS ogs = ogss[0];


            ArrayList<Game> gameList = new ArrayList<>();
            /*
            try {

                //-----------

//                JSONObject serverChallenges = ogs.listServerChallenges();
//
//                JSONArray challenges = serverChallenges.getJSONArray("results");
//                Log.w(TAG, "challenge length = " + challenges.length());
//                for (int i = 0; i < challenges.length(); i++) {
//                    JSONObject obj = challenges.getJSONObject(i);
//                    Challenge c = new Challenge(obj);
//                    Log.w(TAG, c.toString());
                }

                //-----------

//                JSONObject me = ogs.me();
//                int myid = me.getInt("id");
//                JSONObject games = ogs.listGames();
//                JSONArray results = games.getJSONArray("results");
//                for (int i = 0; i < results.length(); i++) {
//                    JSONObject game = results.getJSONObject(i);
//                    int id = game.getInt("id");
//                    JSONObject details = ogs.getGameDetails(id);
//                    Log.w(TAG, "game = " + details.toString(4));
//                    Game g = new Game();
//                    g.id = id;
//                    String white = game.getJSONObject("players").getJSONObject("white").getString("username");
//                    String black = game.getJSONObject("players").getJSONObject("black").getString("username");
//                    int currentPlayer = game.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");
//                    if (myid == currentPlayer)
//                    {
//                        g.myturn = true;
//                        g.name = String.format("Your move - %s vs %s", white, black);
//                    }
//                    else
//                    {
//                        g.myturn = false;
//                        g.name = String.format("Opponent's move - %s vs %s", white, black);
//                    }
//                    gameList.add(g);
//                }
//                Collections.sort(gameList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // */
            return gameList;
        }

    }

    private class GetGameList extends AsyncTask<OGS, Void, ArrayList<Game>> {
        protected ArrayList<Game> doInBackground(OGS... ogss) {
            OGS ogs = ogss[0];
            ArrayList<Game> gameList = new ArrayList<>();


            try {

//                JSONObject fff = new JSONObject("{\"related\":{\"reviews\":\"\\/api\\/v1\\/games\\/6790176\\/reviews\"},\"players\":{\"white\":{\"related\":{\"detail\":\"\\/api\\/v1\\/players212470\"},\"id\":212470,\"username\":\"nathanj439\",\"country\":\"us\",\"icon\":\"https:\\/\\/secure.gravatar.com\\/avatar\\/f177910dc38b29c7812c4325dfd70b5c?s=32&d=https%3A%2F%2Fa00ce0086bda2213e89f-570db0116da8eb5fdc3ce95006e46d28.ssl.cf1.rackcdn.com%2F4.0%2Fimg%2Fdefault-user-32.png\",\"ranking\":15,\"ranking_blitz\":13,\"ranking_live\":11,\"ranking_correspondence\":13,\"rating\":\"640.080\",\"rating_blitz\":\"446.307\",\"rating_live\":\"296.740\",\"rating_correspondence\":\"456.784\",\"professional\":false,\"ui_class\":\"timeout\"},\"black\":{\"related\":{\"detail\":\"\\/api\\/v1\\/players380754\"},\"id\":380754,\"username\":\"ChristopherD.Walborn\",\"country\":\"us\",\"icon\":\"https:\\/\\/secure.gravatar.com\\/avatar\\/7e766a1c8fe951f18a3fec08fc2ea923?s=32&d=https%3A%2F%2Fa00ce0086bda2213e89f-570db0116da8eb5fdc3ce95006e46d28.ssl.cf1.rackcdn.com%2F4.0%2Fimg%2Fdefault-user-32.png\",\"ranking\":8,\"ranking_blitz\":8,\"ranking_live\":8,\"ranking_correspondence\":8,\"rating\":\"-50.000\",\"rating_blitz\":\"-50.000\",\"rating_live\":\"-50.000\",\"rating_correspondence\":\"-50.000\",\"professional\":false,\"ui_class\":\"provisional\"}},\"id\":6790176,\"name\":\"Friendly Match\",\"creator\":212470,\"mode\":\"game\",\"source\":\"play\",\"black\":380754,\"white\":212470,\"width\":13,\"height\":13,\"rules\":\"japanese\",\"ranked\":false,\"handicap\":0,\"komi\":\"5.50\",\"time_control\":\"simple\",\"black_player_rank\":0,\"black_player_rating\":\"0.000\",\"white_player_rank\":0,\"white_player_rating\":\"0.000\",\"time_per_move\":86400,\"time_control_parameters\":\"{\\\"time_control\\\": \\\"simple\\\", \\\"per_move\\\": 86400}\",\"disable_analysis\":false,\"tournament\":null,\"tournament_round\":0,\"ladder\":null,\"pause_on_weekends\":false,\"outcome\":\"\",\"black_lost\":true,\"white_lost\":true,\"annulled\":false,\"started\":\"2016-11-04T01:14:58.063Z\",\"ended\":null,\"gamedata\":{\"score_stones\":false,\"allow_ko\":false,\"private\":false,\"height\":13,\"time_control\":{\"time_control\":\"simple\",\"per_move\":86400},\"free_handicap_placement\":false,\"meta_groups\":[24,25,43,28,38],\"moves\":[[9,3,9441],[3,9,6394396],[3,3,27013],[9,9,37614646],[10,6,1101865],[7,10,1457587],[10,8,11806658],[10,9,2217031],[2,7,375569],[2,8,25410783]],\"allow_superko\":true,\"score_passes\":true,\"clock\":{\"current_player\":380754,\"title\":\"Friendly Match\",\"black_player_id\":380754,\"last_move\":1478308512989,\"white_player_id\":212470,\"expiration\":1478394912989,\"game_id\":6790176,\"black_time\":1478394912989,\"white_time\":0},\"black_player_id\":380754,\"aga_handicap_scoring\":false,\"white_player_id\":212470,\"width\":13,\"initial_state\":{\"white\":\"\",\"black\":\"\"},\"score_territory_in_seki\":false,\"automatic_stone_removal\":false,\"handicap\":0,\"start_time\":1478222098,\"score_prisoners\":true,\"disable_analysis\":false,\"allow_self_capture\":false,\"ranked\":false,\"komi\":5.5,\"game_id\":6790176,\"strict_seki_mode\":false,\"opponent_plays_first_after_resume\":true,\"superko_algorithm\":\"ssk\",\"white_must_pass_last\":false,\"rules\":\"japanese\",\"players\":{\"white\":{\"username\":\"nathanj439\",\"professional\":false,\"egf\":456.784,\"rank\":13,\"id\":212470},\"black\":{\"username\":\"ChristopherD.Walborn\",\"professional\":false,\"egf\":-50,\"rank\":8,\"id\":380754}},\"phase\":\"play\",\"game_name\":\"Friendly Match\",\"score_territory\":true,\"initial_player\":\"black\",\"history\":[]},\"auth\":\"d2b404747b6e9c8e06ca82b922078249\",\"game_chat_auth\":\"814e2fb429ea7ce8eff1a08bb5e1a264\"}");
//                JSONObject gamedata = fff.getJSONObject("gamedata");
//                Log.w(TAG, "fff =" + fff.toString(4));
//                Log.w(TAG, "gamedata =" + gamedata.toString(4));

//                JSONObject me = ogs.me();
//                int myid = me.getInt("id");
                int myid = 23;
                JSONObject games = ogs.listGames();
                Log.w(TAG, "games = " + games.toString());
                JSONArray results = games.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject game = results.getJSONObject(i);
                    int id = game.getInt("id");
                    JSONObject details = ogs.getGameDetails(id);
                    Game g = new Game();
                    g.id = id;
                    String white = game.getJSONObject("players").getJSONObject("white").getString("username");
                    String black = game.getJSONObject("players").getJSONObject("black").getString("username");
                    int currentPlayer = details.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");
                    if (myid == currentPlayer) {
                        g.myturn = true;
                        g.name = String.format("Your move - %s vs %s", white, black);
                    } else {
                        g.myturn = false;
                        g.name = String.format("Opponent's move - %s vs %s", white, black);
                    }
                    Log.w(TAG, "game name = " + g.name);
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
        }
    }

    private int myRanking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_list);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ogs = new OGS("ee20259490eabd6e8fba",
                "31ce3312e5dd2b0a189c8249c3d66fd661834f32");
        ogs.setAccessToken("ec2ce38a81fbd2b708eb069cee764f907dbbe3e4");

        try {
            JSONObject me = ogs.me();
            myRanking = me.getInt("ranking");
            Log.w(TAG, "myRanking = " + myRanking);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ogs.openSocket();

        Game g = new Game();
        g.id = -1;
        g.name = "Loading...";
        gameList.add(g);

        final ListView lv = (ListView) findViewById(R.id.challenge_list);

//        ArrayAdapter<Game> adapter = new ArrayAdapter<>(this,
//                R.layout.activity_listview,
//                gameList);

        final ArrayAdapter<Challenge> adapter = new ArrayAdapter<Challenge>(this,
                R.layout.activity_listview,
                challengeList);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

//        new GetGameList().execute(ogs);
//        new GetChallengeList().execute(ogs);

        SeekGraphConnection seek = ogs.openSeekGraph();
        final Activity thisActivity = this;
        seek.setCallbacks(new SeekGraphConnection.SeekGraphConnectionCallbacks() {
            @Override
            public void event(JSONArray events) {
                for (int i = 0; i < events.length(); i++) {
                    try {
                        JSONObject event = events.getJSONObject(i);
                        Log.w(TAG, event.toString());
                        if (event.has("delete")) {
                            final Challenge c = new Challenge(event.getInt("challenge_id"));
                            int index = challengeList.indexOf(c);
                            Log.w(TAG, "index=" + index);
                            if (index != -1) {
                                challengeList.remove(index);
                                thisActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        } else if (event.has("game_started"))
                            ; // game started notificaton
                        else // new seek
                        {
                            final Challenge c = new Challenge(event);
                            Log.w(TAG, c.toString());

                            if (c.canAccept(myRanking)) {
                                challengeList.add(c);
				Collections.sort(challengeList);
                                thisActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        Challenge c = challengeList.get(position);
        Log.w(TAG, "i = " + position + " l = " + id + " c = " + c.toString());

//        Game game = gameList.get(position);
//        if (game.id == -1)
//            return;
//        Intent intent = new Intent(this, Main3Activity.class);
//        intent.putExtra("id", game.id);
//        startActivity(intent);
    }
}
