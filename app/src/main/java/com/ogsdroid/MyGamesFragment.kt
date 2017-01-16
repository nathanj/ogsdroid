package com.ogsdroid

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class MyGamesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("MyGamesFragment", "onCreateView")
        val activity = activity as TabbedActivity
        val rootView = inflater!!.inflate(R.layout.fragment_my_games, container, false)

        val rv = rootView.findViewById(R.id.my_games_recycler_view) as RecyclerView
        rv.setHasFixedSize(true)

        val dm = activity.resources.displayMetrics
        val dpWidth = dm.widthPixels / dm.density
        val columns = (dpWidth / 310).toInt()

        val manager = GridLayoutManager(activity, columns)
        rv.layoutManager = manager
        rv.adapter = activity.myGamesAdapter

        return rootView
    }
}

