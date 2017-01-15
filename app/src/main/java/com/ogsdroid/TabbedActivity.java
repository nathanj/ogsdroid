package com.ogsdroid;

import android.app.Activity;
import android.app.IntentService;
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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ogs.Challenge;
import com.ogs.GameConnection;
import com.ogs.OGS;
import com.ogs.SeekGraphConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabbedActivity extends AppCompatActivity {
    private static final String TAG = "TabbedActivity";
    static ArrayList<Game> gameList = new ArrayList<>();
    static ArrayList<Challenge> challengeList = new ArrayList<>();
    static MyGamesAdapter myGamesAdapter;
    static ArrayAdapter<Challenge> challengeAdapter;
    static Activity mainActivity;
    static OGS ogs;
    SeekGraphConnection seek;
    private int myRanking, myId;
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
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

        PlaceholderFragment f = (PlaceholderFragment) mSectionsPagerAdapter.getItem(0);

        challengeAdapter = new ArrayAdapter<Challenge>(this,
                R.layout.activity_listview,
                challengeList);

        myGamesAdapter = new MyGamesAdapter(this, gameList);

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

        return super.onOptionsItemSelected(item);
    }

    static class MyGamesAdapter extends RecyclerView.Adapter<MyGamesAdapter.ViewHolder> {
        List<Game> mGames;
        Activity mActivity;

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View v) {
                super(v);
            }
        }

        MyGamesAdapter(Activity activity, List<Game> games) {
            mActivity = activity;
            mGames = games;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder");
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Log.d(TAG, "onBindViewHolder position=" + position);
            final Game game = mGames.get(position);
            Log.d(TAG, "onBindViewHolder myturn=" + game.myturn);
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
            Log.d(TAG, "getItemCount=" + mGames.size());
            return mGames.size();
        }

        void addAll(List<Game> games) {
            mGames.addAll(games);
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

        static String[] mainTimes = {
                "1 Minute",
                "5 Minutes",
                "10 Minutes",
                "20 Minutes",
                "30 Minutes",
                "1 Hour",
        };

        static int[] mainTimesTimes = {
                1 * 60,
                5 * 60,
                10 * 60,
                20 * 60,
                30 * 60,
                60 * 60,
        };

        static String[] byoYomiTimes = {
                "5 x 10 Seconds",
                "5 x 15 Seconds",
                "5 x 20 Seconds",
                "5 x 30 Seconds",
                "5 x 45 Seconds",
                "5 x 1 Minute",
        };

        static int[] byoYomiTimesTimes = {
                10,
                15,
                20,
                30,
                45,
                60,
        };

        View createMyGames(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Log.d(TAG, "createMyGames");
            final Context context = container.getContext();
            View rootView = inflater.inflate(R.layout.fragment_my_games, container, false);

            RecyclerView rv = (RecyclerView) rootView.findViewById(R.id.my_games_recycler_view);
            rv.setHasFixedSize(true);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            Log.d(TAG, "pixels = " + dm.widthPixels);
            float dpWidth = dm.widthPixels / dm.density;
            int columns = (int) (dpWidth / 310);

            RecyclerView.LayoutManager manager = new GridLayoutManager(context, columns);
            rv.setLayoutManager(manager);
            rv.setAdapter(myGamesAdapter);

            return rootView;
        }

        View createSeek(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final Context context = container.getContext();
            View rootView = inflater.inflate(R.layout.fragment_tabbed, container, false);
            ListView lv = (ListView) rootView.findViewById(R.id.my_listview);
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
                                        int gameId = ogs.acceptChallenge(c.getChallengeId());
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
                                    } catch (IOException e) {
                                        e.printStackTrace();
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

        View createChallengeView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final Context context = container.getContext();
            final View rootView = inflater.inflate(R.layout.fragment_create, container, false);
            final TextView gameNameText = (TextView) rootView.findViewById(R.id.name);
            final SeekBar mainTime = (SeekBar) rootView.findViewById(R.id.main_time);
            final TextView mainTimeText = (TextView) rootView.findViewById(R.id.main_time_text);
            mainTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    mainTimeText.setText("Main Time: " + mainTimes[i]);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            mainTime.setProgress(3);
            mainTimeText.setText("Main Time: " + mainTimes[3]);

            final TextView byoYomiText = (TextView) rootView.findViewById(R.id.byo_yomi_text);
            final SeekBar byoYomiTime = (SeekBar) rootView.findViewById(R.id.byo_yomi);
            byoYomiTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    byoYomiText.setText("Byo-Yomi: " + byoYomiTimes[i]);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            byoYomiTime.setProgress(3);
            byoYomiText.setText("Byo-Yomi: " + byoYomiTimes[3]);


            Button b = (Button) rootView.findViewById(R.id.challenge);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RadioGroup rankedGroup = (RadioGroup) rootView.findViewById(R.id.ranked_group);
                    final boolean ranked = (rankedGroup.getCheckedRadioButtonId() == R.id.ranked);

                    RadioGroup sizeGroup = (RadioGroup) rootView.findViewById(R.id.size_group);

                    int dim = 9;
                    switch (sizeGroup.getCheckedRadioButtonId()) {
                        case R.id.size9x9:
                            dim = 9;
                            break;
                        case R.id.size13x13:
                            dim = 13;
                            break;
                        case R.id.size19x19:
                            dim = 19;
                            break;
                    }
                    final int width = dim;
                    final int height = dim;
                    final int periods = 5;
                    Log.d(TAG, "clicked on the challenge button");
                    Log.d(TAG, "byo yomi selected = " + byoYomiTimes[byoYomiTime.getProgress()]);
                    JSONObject result;
                    try {
                        result = ogs.createChallenge(gameNameText.getText().toString(), ranked,
                                width, height,
                                mainTimesTimes[mainTime.getProgress()],
                                byoYomiTimesTimes[byoYomiTime.getProgress()],
                                periods);
                    } catch (Exception ex) {
                        new AlertDialog.Builder(mainActivity)
                                .setMessage("Create challenge failed.\n" + ex.toString())
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                })
                                .show();
                        return;
                    }
                    try {
                        final int challenge = result.getInt("challenge");
                        final int game = result.getInt("game");

                        GameConnection conn = ogs.openGameConnection(game);
                        if (conn == null) {
                            final AlertDialog dialog = new AlertDialog.Builder(mainActivity)
                                    .setMessage("Failed to create challenge.")
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    })
                                    .show();
                        } else {
                            final AlertDialog dialog = new AlertDialog.Builder(mainActivity)
                                    .setMessage("Challenge created. Waiting for challenger. Click cancel to delete the challenge.")
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            try {
                                                ogs.deleteChallenge(challenge);
                                            } catch (Exception ex) {
                                                new AlertDialog.Builder(mainActivity)
                                                        .setMessage("Cancel challenge failed.\n" + ex.toString())
                                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                            }
                                                        })
                                                        .show();
                                            }
                                        }
                                    })
                                    .show();
                            conn.setResetCallback(new GameConnection.OGSGameConnectionResetCallback() {
                                @Override
                                public void reset() {
                                    dialog.dismiss();
                                    Intent intent = new Intent(context, Main3Activity.class);
                                    intent.putExtra("id", game);
                                    startActivity(intent);
                                }
                            });
                            conn.waitForStart();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return rootView;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1:
                    return createMyGames(inflater, container, savedInstanceState);
                case 2:
                    return createSeek(inflater, container, savedInstanceState);
                case 3:
                    return createChallengeView(inflater, container, savedInstanceState);
                default:
                    throw new RuntimeException("Tried to create view " + getArguments().getInt(ARG_SECTION_NUMBER));
            }
        }
    }

    private class GetMe extends AsyncTask<Void, Void, Boolean> {
        private final OGS ogs;

        GetMe(OGS ogs) {
            Log.d(TAG, "GetMe()");
            this.ogs = ogs;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.d(TAG, "GetMe.doInBackground()");
            try {
                JSONObject me = ogs.me();
                myRanking = me.getInt("ranking");
                myId = me.getInt("id");
                Log.d(TAG, "myRanking = " + myRanking);
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            Log.d(TAG, String.format("GetMe.onPostExecute(%s)", success ? "true" : "false"));
            if (!success) {
                SharedPreferences.Editor editor = pref.edit();
                editor.remove("accessToken");
                editor.apply();
                new AlertDialog.Builder(mainActivity)
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
        }
    }

    private class GetMyGamesList extends AsyncTask<OGS, Void, ArrayList<Game>> {

        @Override
        protected void onPreExecute() {
            ProgressBar pb = (ProgressBar) TabbedActivity.this.findViewById(R.id.my_games_progress_bar);
            pb.setVisibility(View.VISIBLE);
        }

        protected ArrayList<Game> doInBackground(OGS... ogss) {
            Log.d(TAG, "GetGameList doInBackground");
            OGS ogs = ogss[0];
            ArrayList<Game> gameList = new ArrayList<>();

            try {
                JSONObject games = ogs.listGames();
                Log.d(TAG, "games = " + games);
                JSONArray results = games.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject game = results.getJSONObject(i);
                    int id = game.getInt("id");
                    JSONObject details = ogs.getGameDetails(id);
                    Log.d(TAG, "details = " + details);
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
                    if (myId == currentPlayer) {
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
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
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
