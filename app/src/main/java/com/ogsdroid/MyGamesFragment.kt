package com.ogsdroid

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

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

