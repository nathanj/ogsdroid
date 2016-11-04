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

import java.util.Vector;

public class ChallengeListActivity extends AppCompatActivity implements OnItemClickListener {
    private static final String TAG = "ChallengeListActivity";
    private OGS ogs;

    private class Game {
        int id;
        String name;

        @Override
        public String toString() {
            return name;
        }
    }

    private String gameToString(JSONObject game, int myid) {
        try {
            String white = game.getJSONObject("players").getJSONObject("white").getString("username");
            String black = game.getJSONObject("players").getJSONObject("black").getString("username");
            int currentPlayer = game.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");
            if (myid == currentPlayer)
                return String.format("Your move - %s vs %s", white, black);
            else
                return String.format("Opponent's move - %s vs %s", white, black);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    Vector<Game> gameList = new Vector<>();

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
            Log.w(TAG, "me = " + me.toString());
            int myid = me.getInt("id");
            JSONObject games = ogs.listGames();
            Log.w(TAG, "games length = " + games.length());
            JSONArray results = games.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject game = results.getJSONObject(i);
                int id = game.getInt("id");
                JSONObject details = ogs.getGameDetails(id);
                Log.w(TAG, "game = " + details.toString());
                Game g = new Game();
                g.id = id;
                g.name = gameToString(details, myid);
                gameList.add(g);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        ListView lv = (ListView) findViewById(R.id.challenge_list);

        ArrayAdapter<Game> adapter = new ArrayAdapter<>(this,
                R.layout.activity_listview,
                gameList);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.w(TAG, "i = " + position + " l = " + id);

        Game game = gameList.get(position);
        Intent intent = new Intent(this, Main3Activity.class);
        intent.putExtra("id", game.id);
        startActivity(intent);
    }
}
