package com.ogsdroid

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.ogs.OGS
import java.util.*

class AutomatchFragment : Fragment() {
    lateinit var rootView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass.name, "onCreateView")
        rootView = inflater!!.inflate(R.layout.fragment_automatch, container, false)

        val b = rootView.findViewById<Button>(R.id.blitz_automatch)
        b.setOnClickListener {
            go("blitz")
        }

        val n = rootView.findViewById<Button>(R.id.normal_automatch)
        n.setOnClickListener {
            go("live")
        }

        return rootView
    }

    fun go(speed: String) {
        Log.d(javaClass.name, "go speed=$speed")
        val ogs = OGS(Globals.uiConfig!!)
        var dialog: AlertDialog? = null

        val sizeList = ArrayList<String>()

        if (rootView.findViewById<CheckBox>(R.id.checkbox_9).isChecked)
            sizeList.add("9x9")
        if (rootView.findViewById<CheckBox>(R.id.checkbox_13).isChecked)
            sizeList.add("13x13")
        if (rootView.findViewById<CheckBox>(R.id.checkbox_19).isChecked)
            sizeList.add("19x19")

        val uuid = ogs.createAutomatch(speed, sizeList) { obj ->
            println("listenForGameData: onSuccess")
            dialog?.dismiss()
            ogs.closeSocket()
            val intent = Intent(activity, Main3Activity::class.java)
            intent.putExtra("id", obj.getInt("game_id"))
            startActivity(intent)
        }

        dialog = AlertDialog.Builder(activity)
                .setMessage("Searching for opponent...")
                .setNegativeButton("Cancel") { dialogInterface, i ->
                    ogs.cancelAutomatch(uuid)
                }
                .show()
    }
}
