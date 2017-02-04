package com.ogsdroid

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.ogs.Challenge
import com.ogs.OGS
import com.ogs.SeekGraphConnection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import java.util.*

class TabbedActivity : AppCompatActivity() {
    internal var ogs: OGS? = null
    internal var gameList: ArrayList<Game> = ArrayList()
    internal var challengeList: ArrayList<Challenge> = ArrayList()
    lateinit internal var myGamesAdapter: MyGamesAdapter
    lateinit internal var challengeAdapter: ArrayAdapter<Challenge>
    internal var seek: SeekGraphConnection? = null
    internal var gameListSubscriber: Disposable? = null

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

        if (gameListSubscriber != null && !gameListSubscriber!!.isDisposed) {
            gameListSubscriber!!.dispose()
            gameListSubscriber = null
        }
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

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val accessToken = pref.getString("accessToken", "")
        if (ogs == null) {
            ogs = Globals.getOGS()
        }
        ogs!!.accessToken = accessToken
        ogs!!.openSocket()

        val pb = this@TabbedActivity.findViewById(R.id.my_games_progress_bar) as ProgressBar?
        if (pb != null)
            pb.visibility = View.VISIBLE

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()

        ogs!!.meObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { },
                        { e ->
                            Log.e(TAG, "error while getting me", e)

                            AlertDialog.Builder(this@TabbedActivity)
                                    .setMessage("There was an error connecting to online-go.com. Please check your network connection. If your connection is fine, then your authentication token could have expired. You can remove it and type in your credentials again.")
                                    .setPositiveButton("Close") { dialog, id -> finish() }
                                    .setNegativeButton("Remove access token") { dialog, id ->
                                        val editor = pref.edit()
                                        editor.remove("accessToken")
                                        editor.apply()
                                        finish()
                                    }
                                    .show()
                        },
                        { getGameListAndOpenSeek() }
                )
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

    internal class MyGamesAdapter(var mActivity: Activity, var mGames: List<Game>) : RecyclerView.Adapter<MyGamesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            //Log.d(TAG, "onCreateViewHolder");
            val v = LayoutInflater.from(parent.context).inflate(R.layout.my_card, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //Log.d(TAG, "onBindViewHolder position=" + position);
            val game = mGames[position]
            //Log.d(TAG, "onBindViewHolder myturn=" + game.myturn);
            if (game.myturn)
                holder.itemView.setBackgroundColor(Color.argb(255, 200, 255, 200))
            else
                holder.itemView.setBackgroundColor(Color.WHITE)

            holder.itemView.setOnClickListener {
                Log.d(TAG, "You clicked on position " + holder.adapterPosition)
                val intent = Intent(holder.itemView.context, Main3Activity::class.java)
                intent.putExtra("id", game.id)
                mActivity.startActivity(intent)
            }
            val iv = holder.itemView.findViewById(R.id.image) as ImageView
            val tv = holder.itemView.findViewById(R.id.my_games_text) as TextView
            tv.text = game.name
            val b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            val p = Paint()
            p.color = Color.argb(255, 200, 200, 155)
            c.drawRect(0f, 0f, 300f, 300f, p)
            game.board!!.draw(c, 300)
            iv.setImageBitmap(b)
        }

        override fun getItemCount(): Int {
            //Log.d(TAG, "getItemCount=" + mGames.size());
            return mGames.size
        }

        internal class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
    }

    private fun getGameListAndOpenSeek() {
        gameListSubscriber = Game.getGamesList(ogs!!)
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
                            if (pb != null)
                                pb.visibility = View.GONE
                            Collections.sort(gameList)
                            myGamesAdapter.notifyDataSetChanged()
                        }
                )

        //*
        seek = ogs!!.openSeekGraph(SeekGraphConnection.SeekGraphConnectionCallbacks { events ->
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

                        if (c.canAccept(ogs!!.player!!.ranking)) {
                            this@TabbedActivity.runOnUiThread {
                                challengeList.add(c)
                                Collections.sort(challengeList)
                                challengeAdapter.notifyDataSetChanged()
                            }
                        } else {
                            //Log.d(TAG, "could not accept " + c);
                        }

                    }// game started notificaton
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        })
        // */
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
            return 2
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
