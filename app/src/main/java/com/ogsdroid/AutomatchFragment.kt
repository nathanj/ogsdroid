package com.ogsdroid

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.ogs.OGS

class AutomatchFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass.name, "onCreateView")
        val activity = activity as TabbedActivity
        val rootView = inflater!!.inflate(R.layout.fragment_automatch, container, false)

        val b = rootView.findViewById(R.id.blitz_automatch) as Button
        b.setOnClickListener {
            val ogs = OGS(Globals.uiConfig!!)
            ogs.createAutomatch("blitz", listOf("19x19"))
        }


//[
//  "automatch/find_match",
//  {
//    "uuid": "666fc565-7fa2-4840-8d4d-70ec7df28b74",
//    "size_speed_options": [
//      {
//        "size": "19x19",
//        "speed": "live"
//      }
//    ],
//    "lower_rank_diff": 3,
//    "upper_rank_diff": 3,
//    "rules": {
//      "condition": "no-preference",
//      "value": "japanese"
//    },
//    "time_control": {
//      "condition": "no-preference",
//      "value": {
//        "system": "byoyomi"
//      }
//    },
//    "handicap": {
//      "condition": "no-preference",
//      "value": "enabled"
//    }
//  }
//]


        return rootView
    }
}
