package com.ogsdroid

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.ogs.GameConnection
import org.json.JSONException
import org.json.JSONObject

class CreateAGameFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass.name, "onCreateView")
        val activity = activity as TabbedActivity
        val rootView = inflater!!.inflate(R.layout.fragment_create, container, false)
        val gameNameText = rootView.findViewById(R.id.name) as TextView
        val mainTime = rootView.findViewById(R.id.main_time) as SeekBar
        val mainTimeText = rootView.findViewById(R.id.main_time_text) as TextView

        mainTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                mainTimeText.text = "Main Time: " + mainTimes[i].description
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        mainTime.progress = 3
        mainTimeText.text = "Main Time: " + mainTimes[3].description

        val byoYomiText = rootView.findViewById(R.id.byo_yomi_text) as TextView
        val byoYomiTime = rootView.findViewById(R.id.byo_yomi) as SeekBar
        byoYomiTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                byoYomiText.text = "Byo-Yomi: " + byoYomiTimes[i].description
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        byoYomiTime.progress = 3
        byoYomiText.text = "Byo-Yomi: " + byoYomiTimes[3].description


        val b = rootView.findViewById(R.id.challenge) as Button
        b.setOnClickListener(View.OnClickListener {
            val rankedGroup = rootView.findViewById(R.id.ranked_group) as RadioGroup
            val ranked = rankedGroup.checkedRadioButtonId == R.id.ranked

            val sizeGroup = rootView.findViewById(R.id.size_group) as RadioGroup

            var dim = 9
            when (sizeGroup.checkedRadioButtonId) {
                R.id.size9x9 -> dim = 9
                R.id.size13x13 -> dim = 13
                R.id.size19x19 -> dim = 19
            }
            val width = dim
            val height = dim
            val periods = 5
            Log.d(javaClass.name, "clicked on the challenge button")
            Log.d(javaClass.name, "byo yomi selected = " + byoYomiTimes[byoYomiTime.progress].description)
            val result: JSONObject
            try {
                result = activity.ogs.createChallenge(gameNameText.text.toString(), ranked,
                        width, height,
                        mainTimes[mainTime.progress].time,
                        byoYomiTimes[byoYomiTime.progress].time,
                        periods)
                println("NJ result=${result.toString(2)}")
            } catch (ex: Exception) {
                AlertDialog.Builder(activity)
                        .setMessage("Create challenge failed.\n" + ex.toString())
                        .setPositiveButton("Ok") { dialog, id -> }
                        .show()
                return@OnClickListener
            }

            try {
                val challenge = result.getInt("challenge")
                val game = result.getInt("game")

                val conn = activity.ogs.openGameConnection(game)
                if (conn == null) {
                    AlertDialog.Builder(activity)
                            .setMessage("Failed to create challenge.")
                            .setNegativeButton("Ok") { dialogInterface, i -> }
                            .show()
                } else {
                    val dialog = AlertDialog.Builder(activity)
                            .setMessage("Challenge created. Waiting for challenger. Click cancel to delete the challenge.")
                            .setNegativeButton("Cancel") { dialogInterface, i ->
                                try {
                                    activity.ogs.deleteChallenge(challenge)
                                } catch (ex: Exception) {
                                    AlertDialog.Builder(activity)
                                            .setMessage("Cancel challenge failed.\n" + ex.toString())
                                            .setPositiveButton("Ok") { dialog, id -> }
                                            .show()
                                }
                            }
                            .show()
                    conn!!.setResetCallback(object : GameConnection.OGSGameConnectionResetCallback {
                        override fun reset() {
                            dialog.dismiss()
                            val intent = Intent(activity, Main3Activity::class.java)
                            intent.putExtra("id", game)
                            startActivity(intent)
                        }
                    })
                    conn!!.waitForStart()
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        })
        return rootView
    }

    data class TimesString(val time: Int, val description: String)

    companion object {
        internal val mainTimes = arrayOf(
                TimesString(1 * 60, "1 Minute"),
                TimesString(5 * 60, "5 Minutes"),
                TimesString(10 * 60, "10 Minutes"),
                TimesString(20 * 60, "20 Minutes"),
                TimesString(30 * 60, "30 Minutes"),
                TimesString(60 * 60, "1 Hour")
        )

        internal var byoYomiTimes = arrayOf(
                TimesString(10, "5 x 10 Seconds"),
                TimesString(15, "5 x 15 Seconds"),
                TimesString(20, "5 x 20 Seconds"),
                TimesString(30, "5 x 30 Seconds"),
                TimesString(45, "5 x 45 Seconds"),
                TimesString(60, "5 x 1 Minute")
        )
    }
}

