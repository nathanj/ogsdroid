package com.example.njones.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ogs.OGS;
import com.ogs.SeekGraphConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class TabbedActivity extends AppCompatActivity {
    private static final String TAG = "TabbedActivity";
    static ArrayList<Game> gameList = new ArrayList<>();
    static ArrayList<Challenge> challengeList = new ArrayList<>();
    static ArrayAdapter<Game> gameAdapter;
    static ArrayAdapter<Challenge> challengeAdapter;
    static Activity mainActivity;
    static OGS ogs;
    SeekGraphConnection seek;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private int myRanking, myId;

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (gameList != null)
            gameList.clear();
        if (challengeList != null)
            challengeList.clear();
        if (challengeAdapter != null)
            challengeAdapter.notifyDataSetChanged();
        if (gameAdapter != null)
            gameAdapter.notifyDataSetChanged();

        if (seek != null)
            seek.disconnect();
        if (ogs != null)
            ogs.closeSocket();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(TAG, "onPostResume");

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String accessToken = pref.getString("accessToken", "");
        OGS ogs = new OGS("82ff83f2631a55273c31", "cd42d95fd978348d57dc909a9aecd68d36b17bd2");
        ogs.setAccessToken(accessToken);
        ogs.openSocket();

        try {
            JSONObject me = ogs.me();
            myRanking = me.getInt("ranking");
            myId = me.getInt("id");
            Log.d(TAG, "myRanking = " + myRanking);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            SharedPreferences.Editor editor = pref.edit();
            editor.remove("accessToken");
            editor.apply();
            new AlertDialog.Builder(this)
                    .setMessage("Access token did not work. It may have expired. Restart the app and login again.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .show();
            return;
        }
        seek = ogs.openSeekGraph(new SeekGraphConnection.SeekGraphConnectionCallbacks() {
            @Override
            public void event(JSONArray events) {
                for (int i = 0; i < events.length(); i++) {
                    try {
                        final JSONObject event = events.getJSONObject(i);
                        Log.d(TAG, event.toString());
                        if (event.has("delete")) {
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        challengeAdapter.remove(new TabbedActivity.Challenge(event.getInt("challenge_id")));
                                        challengeAdapter.notifyDataSetChanged();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } else if (event.has("game_started"))
                            ; // game started notificaton
                        else // new seek
                        {
                            final TabbedActivity.Challenge c = new TabbedActivity.Challenge(event);
                            Log.d(TAG, c.toString());

                            if (c.canAccept(myRanking)) {
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        challengeList.add(c);
                                        Collections.sort(challengeList);
                                        challengeAdapter.notifyDataSetChanged();
                                    }
                                });
                            } else {
                                Log.d(TAG, "could not accept " + c);
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        new GetGameList().execute(ogs);
//        new GetChallengeList().execute(seek);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_tabbed);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        PlaceholderFragment f = (PlaceholderFragment) mSectionsPagerAdapter.getItem(0);

        challengeAdapter = new ArrayAdapter<Challenge>(this,
                R.layout.activity_listview,
                challengeList);

        gameAdapter = new ArrayAdapter<Game>(this,
                R.layout.activity_listview,
                gameList);

        mainActivity = this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tabbed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Log.d(TAG, "id = " + id);
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        }

        /*
        if (id == R.id.create_game) {
            new AlertDialog.Builder(this)
                    .setMessage("Not implemented yet, sorry!")
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        }
        */

        return super.onOptionsItemSelected(item);
    }

    public static class Challenge implements Comparable<Challenge> {
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
            String handicapStr = handicap == -1 ? "Auto Handicap" : (handicap == 0 ? "No Handicap" : "");
            if (handicap > 0)
                handicapStr = String.format("%d Stones", handicap);

            return String.format("%s - %dx%d - %s (%s) - %s - %s - %ds / move",
                    name, width, height, username, rankToString(rank),
                    ranked ? "Ranked" : "Casual",
                    handicapStr,
                    timePerMove);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tabbed, container, false);
            ListView lv = (ListView) rootView.findViewById(R.id.my_listview);
            final Context context = container.getContext();
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1:
                    lv.setAdapter(gameAdapter);
                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            Game game = gameList.get(i);

                            Intent intent = new Intent(context, Main3Activity.class);
                            intent.putExtra("id", game.id);
                            startActivity(intent);
                        }
                    });
                    return rootView;
                case 2:
                    lv.setAdapter(challengeAdapter);
                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            final Challenge c = challengeList.get(i);

                            new AlertDialog.Builder(mainActivity)
                                    .setMessage(String.format("Are you sure you want to accept the challenge %s?", c))
                                    .setCancelable(true)
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            try {
                                                int gameId = ogs.acceptChallenge(c.challengeId);
                                                if (gameId == 0) {
                                                    new AlertDialog.Builder(mainActivity)
                                                            .setMessage(String.format("Error accepting challenge. Maybe someone else accepted it first."))
                                                            .setCancelable(true)
                                                            .setPositiveButton("Ok", null)
                                                            .show();
                                                } else {
                                                    Intent intent = new Intent(context, Main3Activity.class);
                                                    intent.putExtra("id", gameId);
                                                    startActivity(intent);
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        }
                    });
                    return rootView;
            }
            return null;
        }
    }

    private class GetGameList extends AsyncTask<OGS, Void, ArrayList<Game>> {
        protected ArrayList<Game> doInBackground(OGS... ogss) {
            OGS ogs = ogss[0];
            ArrayList<Game> gameList = new ArrayList<>();

            try {
                JSONObject games = ogs.listGames();
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
                    if (myId == currentPlayer) {
                        g.myturn = true;
                        g.name = String.format("Your move - %s vs %s", white, black);
                    } else {
                        g.myturn = false;
                        g.name = String.format("Opponent's move - %s vs %s", white, black);
                    }
                    //Log.d(TAG, "game name = " + g.name);
                    gameList.add(g);
                }
                Collections.sort(gameList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return gameList;
        }

        protected void onPostExecute(ArrayList<Game> list) {
            gameAdapter.addAll(list);
            gameAdapter.notifyDataSetChanged();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "My Games";
                case 1:
                    return "Find a Game";
            }
            return null;
        }
    }
}
