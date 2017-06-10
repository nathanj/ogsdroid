package com.ogsdroid

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ogs.Gamedata
import com.ogs.OGS
import com.ogs.TimeAdapter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class MyGamesFragment : Fragment() {
    var nextPage = 1
    val subscribers = CompositeDisposable()
    private val gameList = ArrayList<Game>()
    private var myGamesAdapter: MyGamesAdapter? = null
    var ogs: OGS? = null
    var refresh: SwipeRefreshLayout? = null
    var isRefreshing = false
    var lastRefreshTime = 0L

    init {
        println("$TAG init $this")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        println("$TAG onCreate this=$this savedInstanceState=$savedInstanceState")
        super.onCreate(savedInstanceState)

        val ogs = OGS(Globals.uiConfig!!)
        ogs.openSocket()
        loadGames(ogs)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")
        val activity = activity as TabbedActivity
        val rootView = inflater!!.inflate(R.layout.fragment_my_games, container, false)

        refresh = rootView.findViewById(R.id.my_games_swipe_refresh) as SwipeRefreshLayout
        refresh?.setOnRefreshListener {
            nextPage = 1
            gameList.clear()
            myGamesAdapter?.notifyDataSetChanged()
            ogs?.let {
                loadGames(it)
            }
        }
        refresh?.isRefreshing = isRefreshing

        val rv = rootView.findViewById(R.id.my_games_recycler_view) as RecyclerView
        rv.setHasFixedSize(true)

        val dm = activity.resources.displayMetrics
        val dpWidth = dm.widthPixels / dm.density
        val columns = (dpWidth / 310).toInt()

        val manager = GridLayoutManager(activity, columns)
        rv.layoutManager = manager
        myGamesAdapter = MyGamesAdapter(activity, gameList)
        rv.adapter = myGamesAdapter
        myGamesAdapter?.notifyDataSetChanged()

        return rootView
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

        // Reload the games automatically after one hour.
        if (!isRefreshing && lastRefreshTime + 1000 * 60 * 60 < Date().time) {
            ogs?.openSocket()
            loadGames(ogs!!)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        subscribers.clear()
    }

    fun loadGames(ogs: OGS) {
        isRefreshing = true
        refresh?.isRefreshing = true

        this.ogs = ogs

        lastRefreshTime = Date().time

        println("$TAG loading games with nextPage=$nextPage")
        val currentPage = nextPage

        subscribers.add(Globals.ogsService.overview()
                .flatMap { overview -> Observable.fromIterable(overview.active_games) }
                .flatMap { gamedata ->
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
                        { e ->
                            Log.e(TAG, "error while getting game list", e)
                            throw e
                        },
                        {
                            Log.i(TAG, "onComplete")
                            refresh?.isRefreshing = false
                            isRefreshing = false
                            Collections.sort(gameList)
                            println("$TAG myGamesAdapter = ${myGamesAdapter}")
                            myGamesAdapter?.notifyDataSetChanged()
                            ogs.closeSocket()
                        }
                ))

        /*
        subscribers.add(Globals.ogsService.gameList(nextPage)
                // Convert to list of game ids
                .flatMap { gameList ->
                    if (gameList.next != null)
                        nextPage++
                    Observable.fromIterable(gameList.results?.map { it.id })
                }
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
                            println("$TAG nextPage = ${nextPage}")
                            println("$TAG currentPage = ${currentPage}")
                            if (nextPage == currentPage) {
                                refresh?.isRefreshing = false
                                isRefreshing = false
                                Collections.sort(gameList)
                                println("$TAG myGamesAdapter = ${myGamesAdapter}")
                                myGamesAdapter?.notifyDataSetChanged()
                                ogs.closeSocket()
                            } else {
                                loadGames(ogs)
                            }
                        }
                ))
                */
    }

    companion object {
        val TAG = "MyGamesFragment"
    }

}

internal class MyGamesAdapter(val mActivity: Activity, val mGames: List<Game>) : RecyclerView.Adapter<MyGamesAdapter.ViewHolder>() {

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
            Log.d("asdf", "You clicked on position " + holder.adapterPosition)
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

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)


}

