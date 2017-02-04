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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class TabbedActivity extends AppCompatActivity {
    private static final String TAG = "TabbedActivity";
    static ArrayList<Game> gameList = new ArrayList<>();
    public OGS ogs;
    ArrayList<Challenge> challengeList = new ArrayList<>();
    MyGamesAdapter myGamesAdapter;
    ArrayAdapter<Challenge> challengeAdapter;
    SeekGraphConnection seek;
    private SharedPreferences pref;
    private Disposable gameListSubscriber;

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

        if (gameListSubscriber != null && !gameListSubscriber.isDisposed()) {
            gameListSubscriber.dispose();
            gameListSubscriber = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        Globals.INSTANCE.putOGS();
        ogs = null;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(TAG, "onPostResume");

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        String accessToken = pref.getString("accessToken", "");
        if (ogs == null) {
            ogs = Globals.INSTANCE.getOGS();
        }
        ogs.setAccessToken(accessToken);
        ogs.openSocket();

        ProgressBar pb = (ProgressBar) TabbedActivity.this.findViewById(R.id.my_games_progress_bar);
        if (pb != null)
            pb.setVisibility(View.VISIBLE);

        new GetMe(ogs).execute();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
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

        Alarm al = new Alarm();
        //al.cancelAlarm(this);
        al.setAlarm(this);


        //Intent intent = new Intent(this, NotificationService.class);
        //System.out.println("NJ creating service....");
        //startService(intent);
        //System.out.println("NJ done creating service....");
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
            if (game.getMyturn())
                holder.itemView.setBackgroundColor(Color.argb(255, 200, 255, 200));
            else
                holder.itemView.setBackgroundColor(Color.WHITE);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "You clicked on position " + holder.getAdapterPosition());
                    Intent intent = new Intent(holder.itemView.getContext(), Main3Activity.class);
                    intent.putExtra("id", game.getId());
                    mActivity.startActivity(intent);
                }
            });
            ImageView iv = (ImageView) holder.itemView.findViewById(R.id.image);
            TextView tv = (TextView) holder.itemView.findViewById(R.id.my_games_text);
            tv.setText(game.getName());
            Bitmap b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Paint p = new Paint();
            p.setColor(Color.argb(255, 200, 200, 155));
            c.drawRect(0f, 0f, 300f, 300f, p);
            game.getBoard().draw(c, 300);
            iv.setImageBitmap(b);
        }

        @Override
        public int getItemCount() {
            //Log.d(TAG, "getItemCount=" + mGames.size());
            return mGames.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View v) {
                super(v);
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
                ogs.me();
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            Log.d(TAG, String.format("GetMe.onPostExecute(%s)", success));
            if (!success) {
                new AlertDialog.Builder(TabbedActivity.this)
                        .setMessage("There was an error connecting to online-go.com. Please check your network connection. If your connection is fine, then your authentication token could have expired. You can remove it and type in your credentials again.")
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .setNegativeButton("Remove access token", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences.Editor editor = pref.edit();
                                editor.remove("accessToken");
                                editor.apply();
                                finish();
                            }
                        })
                        .show();
                return;
            }

            gameListSubscriber = Game.Companion.getGamesList(ogs)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            new Consumer<Game>() {
                                @Override
                                public void accept(Game game) throws Exception {
                                    Log.i(TAG, "onNext " + game);
                                    gameList.add(game);
                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable e) throws Exception {
                                    Log.e(TAG, "error while getting game list", e);
                                }
                            },
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    Log.i(TAG, "onComplete");
                                    ProgressBar pb = (ProgressBar) TabbedActivity.this.findViewById(R.id.my_games_progress_bar);
                                    if (pb != null)
                                        pb.setVisibility(View.GONE);
                                    Collections.sort(gameList);
                                    myGamesAdapter.notifyDataSetChanged();
                                }
                            }
                    );

            //*
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
                                    //Log.d(TAG, "could not accept " + c);
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
            return 2;
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
