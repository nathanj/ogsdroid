package com.ogsdroid

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import com.ogs.OGS
import org.json.JSONException
import java.io.IOException

class FindAGameFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass.name, "onCreateView")
        val activity = activity as TabbedActivity
        val rootView = inflater!!.inflate(R.layout.fragment_tabbed, container, false)
        val lv = rootView.findViewById(R.id.my_listview) as ListView
        lv.adapter = activity.challengeAdapter
        lv.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val c = activity.challengeList[i]

            AlertDialog.Builder(activity)
                    .setMessage(String.format("Are you sure you want to accept the challenge %s?", c))
                    .setCancelable(true)
                    .setPositiveButton("Yes") { dialog, id ->
                        try {
                            val ogs = Globals.getOGS()
                            val gameId = ogs.acceptChallenge(c.challengeId)
                            Globals.putOGS()
                            if (gameId == 0) {
                                AlertDialog.Builder(activity)
                                        .setMessage(String.format("Error accepting challenge. Maybe someone else accepted it first."))
                                        .setCancelable(true)
                                        .setPositiveButton("Ok", null)
                                        .show()
                            } else {
                                val intent = Intent(activity, Main3Activity::class.java)
                                intent.putExtra("id", gameId)
                                startActivity(intent)
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
        }
        return rootView
    }
}
