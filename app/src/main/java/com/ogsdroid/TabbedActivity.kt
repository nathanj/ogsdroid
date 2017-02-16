package com.ogsdroid

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import com.ogs.Challenge
import com.ogs.Gamedata
import com.ogs.OGS
import com.ogs.SeekGraphConnection
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import java.util.*

class TabbedActivity : AppCompatActivity() {
    internal var ogs: OGS? = null
    internal val gameList: ArrayList<Game> = ArrayList()
    internal val challengeList: ArrayList<Challenge> = ArrayList()
    lateinit internal var myGamesAdapter: MyGamesAdapter
    lateinit internal var challengeAdapter: ArrayAdapter<Challenge>
    internal var seek: SeekGraphConnection? = null
    internal val subscribers = CompositeDisposable()

    /**
     * Dispatch onPause() to fragments.
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        gameList.clear()
        challengeList.clear()
        challengeAdapter.notifyDataSetChanged()
        myGamesAdapter.notifyDataSetChanged()

        if (seek != null)
            seek!!.disconnect()

        println("XXXXXXXXXX disposing subscribers")
        subscribers.clear()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")

        Globals.putOGS()
        ogs = null
    }

    override fun onPostResume() {
        super.onPostResume()
        Log.d(TAG, "onPostResume")

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()

        subscribers.add(Globals.getAccessToken(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { token ->
                            if (token == "") {
                                val intent = Intent(applicationContext, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Globals.accessToken = token
                                loadEverything()
                            }
                        },
                        { e ->
                            Log.e(TAG, "error while getting access token", e)
                            val intent = Intent(applicationContext, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                ))
    }

    fun loadEverything() {
        subscribers.add(Globals.ogsService.uiConfig()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { uiConfig -> Globals.uiConfig = uiConfig },
                        { e -> Log.e(TAG, "error while getting uiConfig", e) },
                        { loadGames() }
                ))
    }

    fun loadGames() {
        val moshi = Moshi.Builder()
                //.add(TimeAdapter())
                .build()
        val adapter = moshi.adapter(Gamedata::class.java)
        val ogs = OGS()
        ogs.openSocket()

        subscribers.add(Globals.ogsService.gameList()
                // Convert to list of game ids
                .flatMap { gameList -> Observable.fromIterable(gameList.results?.map { it.id }) }
                // Convert to list of json game details
                .flatMap { gameId -> ogs.getGameDetailsViaSocketObservable(gameId!!) }
                // Remove any nulls which meant the call did not complete
                .filter { it != null }
                // Convert to list of Gamedata objects
                .flatMap { details ->
                    println("details = ${details}")
                    val gamedata = adapter.fromJson(details.toString())
                    val game = Game.fromGamedata(Globals.uiConfig!!.user.id, gamedata)
                    Observable.just(game)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { game ->
                            Log.i(TAG, "onNext " + game)
                            gameList.add(game)
                        },
                        { e -> Log.e(TAG, "error while getting game list", e) },
                        {
                            Log.i(TAG, "onComplete")
                            val pb = this@TabbedActivity.findViewById(R.id.my_games_progress_bar) as ProgressBar?
                            pb?.visibility = View.GONE
                            Collections.sort(gameList)
                            myGamesAdapter.notifyDataSetChanged()
                        }
                ))

        //*
        seek = ogs.openSeekGraph(SeekGraphConnection.SeekGraphConnectionCallbacks { events ->
            for (i in 0..events.length() - 1) {
                try {
                    val event = events.getJSONObject(i)
                    //Log.d(TAG, event.toString());
                    if (event.has("delete")) {
                        this@TabbedActivity.runOnUiThread {
                            try {
                                challengeAdapter.remove(Challenge(event.getInt("challenge_id")))
                                challengeAdapter.notifyDataSetChanged()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    } else if (event.has("game_started"))
                    else
                    // new seek
                    {
                        val c = Challenge(event)
                        //Log.d(TAG, c.toString());

                        if (c.canAccept(Globals.uiConfig!!.user.ranking)) {
                            this@TabbedActivity.runOnUiThread {
                                challengeList.add(c)
                                Collections.sort(challengeList)
                                challengeAdapter.notifyDataSetChanged()
                            }
                        } else {
                            Log.d(TAG, "could not accept " + c)
                        }

                    }// game started notificaton
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        })
        // */
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_tabbed)

        //val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        //StrictMode.setThreadPolicy(policy)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        val mViewPager = findViewById(R.id.container) as ViewPager
        mViewPager.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(mViewPager)

        challengeAdapter = ArrayAdapter(this,
                R.layout.activity_listview,
                challengeList)

        myGamesAdapter = MyGamesAdapter(this, gameList)

        val al = Alarm()
        //al.cancelAlarm(this);
        al.setAlarm(this)


        //Intent intent = new Intent(this, NotificationService.class);
        //System.out.println("NJ creating service....");
        //startService(intent);
        //System.out.println("NJ done creating service....");
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_tabbed, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        Log.d(TAG, "id = " + id)

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }

    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> return MyGamesFragment()
                1 -> return FindAGameFragment()
                else -> return CreateAGameFragment()
            }
        }

        override fun getCount(): Int {
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "My Games"
                1 -> return "Find a Game"
                2 -> return "Create a Game"
            }
            return null
        }
    }

    companion object {
        private val TAG = "TabbedActivity"
    }
}
