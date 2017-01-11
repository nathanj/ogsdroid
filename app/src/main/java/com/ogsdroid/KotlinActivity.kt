package com.ogsdroid

import android.graphics.Bitmap
import android.graphics.Bitmap.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class KotlinActivity : AppCompatActivity() {
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: MyAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin)

        val dpWidth = baseContext.resources.displayMetrics.let {
            it.widthPixels / it.density
        }.toInt()
        val columns = dpWidth / 110

        mRecyclerView = findViewById(R.id.my_recycler_view) as RecyclerView
        mRecyclerView.setHasFixedSize(true)

        mLayoutManager = GridLayoutManager(this, columns)
        //mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = mLayoutManager

        val myDataset = arrayOf(
                "D'Aaron Fox",
                "Malik Monk",
                "Bam Adebayou",
                "Brad Calipari",
                "Derek Willis"
        )

        mAdapter = MyAdapter(myDataset);
        mRecyclerView.adapter = mAdapter

        //myfab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //            .setAction("Action", null).show()
        //}
    }

}

class MyAdapter(val mDataset: Array<String>) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView)

    override fun getItemCount() = mDataset.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.my_card, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //val tv = holder.mView.findViewById(R.id.info_text) as TextView
        //println("height: ${holder.mView.height} width: ${holder.mView.width}")
        //tv.text = mDataset[position]

        //val cv = holder.mView.findViewById(R.id.card_view) as CardView
        //cv.setCardBackgroundColor(Color.argb(255, 200, 200, 200))

        val iv = holder.mView.findViewById(R.id.image) as ImageView

        val b = createBitmap(200, 200, Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint()
        p.color = Color.argb(255, 200, 200, 155)
        c.drawRect(0f, 0f, 200f, 200f, p)
        val board = Board(0, 19, 19)
        board.addStone(10, 10)
        board.addStone(8, 5)
        board.addStone(3, 3)
        board.addStone(9, 8)
        board.addStone(9, 9)
        board.draw(c, 200)

        iv.setImageBitmap(b)
    }
}
