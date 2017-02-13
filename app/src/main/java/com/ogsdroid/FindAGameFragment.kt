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
import com.ogs.Challenge
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class FindAGameFragment : Fragment() {
    val subscribers = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")
        val activity = activity as TabbedActivity
        val rootView = inflater!!.inflate(R.layout.fragment_tabbed, container, false)
        val lv = rootView.findViewById(R.id.my_listview) as ListView
        lv.adapter = activity.challengeAdapter
        lv.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val c = activity.challengeList[i]

            AlertDialog.Builder(activity)
                    .setMessage(String.format("Are you sure you want to accept the challenge %s?", c))
                    .setCancelable(true)
                    .setPositiveButton("Yes") { dialog, id -> acceptChallenge(c) }
                    .setNegativeButton("No", null)
                    .show()
        }
        return rootView
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        subscribers.clear()
    }

    fun acceptChallenge(c: Challenge) {
        subscribers.add(Globals.ogsService.acceptChallenge(c.challengeId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { resp ->
                            if (resp.game == 0) {
                                AlertDialog.Builder(activity)
                                        .setMessage(String.format("Error accepting challenge. Maybe someone else accepted it first."))
                                        .setCancelable(true)
                                        .setPositiveButton("Ok", null)
                                        .show()
                            } else {
                                val intent = Intent(activity, Main3Activity::class.java)
                                intent.putExtra("id", resp.game)
                                startActivity(intent)
                            }
                        },
                        { e ->
                            Log.e(TAG, "error while accepting challenge", e)
                            AlertDialog.Builder(activity)
                                    .setMessage(String.format("Error accepting challenge. Maybe someone else accepted it first."))
                                    .setCancelable(true)
                                    .setPositiveButton("Ok", null)
                                    .show()
                        },
                        {
                        }
                ))
    }

    companion object {
        val TAG = "FindAGameFragment"
    }
}
