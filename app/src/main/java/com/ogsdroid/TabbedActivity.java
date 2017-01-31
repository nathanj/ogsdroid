package com.ogsdroid;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ogs.Challenge;
import com.ogs.OGS;
import com.ogs.SeekGraphConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabbedActivity extends AppCompatActivity {
    private static final String TAG = "TabbedActivity";
    static ArrayList<Game> gameList = new ArrayList<>();
    static OGS ogs;
    ArrayList<Challenge> challengeList = new ArrayList<>();
    MyGamesAdapter myGamesAdapter;
    ArrayAdapter<Challenge> challengeAdapter;
    SeekGraphConnection seek;
    private SharedPreferences pref;

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
        if (myGamesAdapter != null)
            myGamesAdapter.notifyDataSetChanged();

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

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        String accessToken = pref.getString("accessToken", "");
        ogs = Globals.INSTANCE.getOgs();
        ogs.setAccessToken(accessToken);
        ogs.openSocket();
        new GetMe(ogs).execute();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();

        Alarm al = new Alarm();
        al.cancelAlarm(this);
        al.setAlarm(this);
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
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        challengeAdapter = new ArrayAdapter<Challenge>(this,
                R.layout.activity_listview,
                challengeList);

        myGamesAdapter = new MyGamesAdapter(this, gameList);
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

        return super.onOptionsItemSelected(item);
    }

    public static class MyService extends IntentService {
        MyService() {
            super("MyService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String data = intent.getDataString();
        }
    }

    static class MyGamesAdapter extends RecyclerView.Adapter<MyGamesAdapter.ViewHolder> {
        List<Game> mGames;
        Activity mActivity;

        MyGamesAdapter(Activity activity, List<Game> games) {
            mActivity = activity;
            mGames = games;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //Log.d(TAG, "onCreateViewHolder");
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            //Log.d(TAG, "onBindViewHolder position=" + position);
            final Game game = mGames.get(position);
            //Log.d(TAG, "onBindViewHolder myturn=" + game.myturn);
            if (game.myturn)
                holder.itemView.setBackgroundColor(Color.argb(255, 200, 255, 200));
            else
                holder.itemView.setBackgroundColor(Color.WHITE);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "You clicked on position " + holder.getAdapterPosition());
                    Intent intent = new Intent(holder.itemView.getContext(), Main3Activity.class);
                    intent.putExtra("id", game.id);
                    mActivity.startActivity(intent);
                }
            });
            ImageView iv = (ImageView) holder.itemView.findViewById(R.id.image);
            TextView tv = (TextView) holder.itemView.findViewById(R.id.my_games_text);
            tv.setText(game.name);
            Bitmap b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Paint p = new Paint();
            p.setColor(Color.argb(255, 200, 200, 155));
            c.drawRect(0f, 0f, 300f, 300f, p);
            game.board.draw(c, 300);
            iv.setImageBitmap(b);
        }

        @Override
        public int getItemCount() {
            //Log.d(TAG, "getItemCount=" + mGames.size());
            return mGames.size();
        }

        void addAll(List<Game> games) {
            mGames.addAll(games);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View v) {
                super(v);
            }
        }
    }


    private class GetMe extends AsyncTask<Void, Void, Integer> {
        private final OGS ogs;

        GetMe(OGS ogs) {
            Log.d(TAG, "GetMe()");
            this.ogs = ogs;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Log.d(TAG, "GetMe.doInBackground()");
            try {
                ogs.me();
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
                return 2;
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                return 3;
            } catch (IOException e) {
                e.printStackTrace();
                return 3;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(final Integer success) {
            Log.d(TAG, String.format("GetMe.onPostExecute(%s)", success));
            if (success == 2) {
                new AlertDialog.Builder(TabbedActivity.this)
                        .setMessage("There was an error connecting to online-go.com. Please check your network connection.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .show();
                return;
            }
            if (success == 3) {
                SharedPreferences.Editor editor = pref.edit();
                editor.remove("accessToken");
                editor.apply();
                new AlertDialog.Builder(TabbedActivity.this)
                        .setMessage("Access token did not work. It may have expired. Restart the app and login again.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .show();
                return;
            }

            new GetMyGamesList().execute(ogs);

            // /*
            seek = ogs.openSeekGraph(new SeekGraphConnection.SeekGraphConnectionCallbacks() {
                @Override
                public void event(JSONArray events) {
                    for (int i = 0; i < events.length(); i++) {
                        try {
                            final JSONObject event = events.getJSONObject(i);
                            //Log.d(TAG, event.toString());
                            if (event.has("delete")) {
                                TabbedActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            challengeAdapter.remove(new Challenge(event.getInt("challenge_id")));
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
                                final Challenge c = new Challenge(event);
                                //Log.d(TAG, c.toString());

                                if (c.canAccept(ogs.getPlayer().getRanking())) {
                                    TabbedActivity.this.runOnUiThread(new Runnable() {
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
            // */
        }
    }

    private class GetMyGamesList extends AsyncTask<OGS, Void, ArrayList<Game>> {

        @Override
        protected void onPreExecute() {
            ProgressBar pb = (ProgressBar) TabbedActivity.this.findViewById(R.id.my_games_progress_bar);
            if (pb != null)
                pb.setVisibility(View.VISIBLE);
        }

        protected ArrayList<Game> doInBackground(OGS... ogss) {
            Log.d(TAG, "GetGameList doInBackground");
            OGS ogs = ogss[0];
            ArrayList<Game> gameList = new ArrayList<>();

            try {
                JSONObject games = ogs.listGames();
                //Log.d(TAG, "games = " + games);
                JSONArray results = games.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject game = results.getJSONObject(i);
                    int id = game.getInt("id");
                    JSONObject details = ogs.getGameDetails(id);
                    //Log.d(TAG, "details = " + details);
                    JSONArray moves = details.getJSONObject("gamedata").getJSONArray("moves");
                    Board tmpBoard = new Board(0, details.getInt("height"), details.getInt("width"));
                    for (int m = 0; m < moves.length(); m++) {
                        int x = moves.getJSONArray(m).getInt(0);
                        int y = moves.getJSONArray(m).getInt(1);
                        if (x != -1)
                            tmpBoard.addStone(x, y);
                    }
                    Game g = new Game();
                    g.board = tmpBoard;
                    g.id = id;
                    String white = game.getJSONObject("players").getJSONObject("white").getString("username");
                    String black = game.getJSONObject("players").getJSONObject("black").getString("username");
                    int currentPlayer = details.getJSONObject("gamedata").getJSONObject("clock").getInt("current_player");
                    if (ogs.getPlayer().getId() == currentPlayer) {
                        g.myturn = true;
                        g.name = String.format("%s vs %s", white, black);
                    } else {
                        g.myturn = false;
                        g.name = String.format("%s vs %s", white, black);
                    }
                    Log.d(TAG, "adding game name = " + g.name);
                    gameList.add(g);
                }
                Collections.sort(gameList);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return gameList;
        }

        protected void onPostExecute(ArrayList<Game> list) {
            Log.d(TAG, "GetGameList onPostExecute");
            ProgressBar pb = (ProgressBar) TabbedActivity.this.findViewById(R.id.my_games_progress_bar);
            if (pb != null)
                pb.setVisibility(View.GONE);
            myGamesAdapter.addAll(list);
            myGamesAdapter.notifyDataSetChanged();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new MyGamesFragment();
                case 1:
                    return new FindAGameFragment();
                default:
                    return new CreateAGameFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "My Games";
                case 1:
                    return "Find a Game";
                case 2:
                    return "Create a Game";
            }
            return null;
        }
    }
}
