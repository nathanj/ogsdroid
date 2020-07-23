package com.ogsdroid

import android.content.Intent
import android.os.AsyncTask
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
import com.ogs.OGS
import com.ogs.createJsonObject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject

class CreateAGameFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass.name, "onCreateView")
        val activity = activity as TabbedActivity
        val rootView = inflater!!.inflate(R.layout.fragment_create, container, false)
        val gameNameText = rootView.findViewById<TextView>(R.id.name)
        val mainTime = rootView.findViewById<SeekBar>(R.id.main_time)
        val mainTimeText = rootView.findViewById<TextView>(R.id.main_time_text)
        var waitForGameSubscription: Disposable? = null
        val hancidap = rootView.findViewById<SeekBar>(R.id.handicap)
        val handicap_text = rootView.findViewById<TextView>(R.id.handicap_text)

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

        val byoYomiText = rootView.findViewById<TextView>(R.id.byo_yomi_text)
        val byoYomiTime = rootView.findViewById<SeekBar>(R.id.byo_yomi)
        byoYomiTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                byoYomiText.text = "Byo-Yomi: " + byoYomiTimes[i].description
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        hancidap.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val handicap_int = hancidap.progress
                handicap_text.text = "Handicap: " + Integer.toString(handicap_int)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        byoYomiTime.progress = 3
        byoYomiText.text = "Byo-Yomi: " + byoYomiTimes[3].description


        val b = rootView.findViewById<Button>(R.id.challenge)
        b.setOnClickListener(View.OnClickListener {
            val rankedGroup = rootView.findViewById<RadioGroup>(R.id.ranked_group)
            val ranked = rankedGroup.checkedRadioButtonId == R.id.ranked

            val sizeGroup = rootView.findViewById<RadioGroup>(R.id.size_group)

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
                val post = createJsonObject {
                    put("challenger_color", "automatic")
                    put("min_ranking", -1000)
                    put("max_ranking", 1000)
                    put("game", createJsonObject {
                        put("name", Globals.uiConfig!!.user.username)
                        put("rules", "japanese")
                        put("ranked", ranked)
                        put("handicap", hancidap.progress)
                        put("pause_on_weekends", false)
                        put("width", width)
                        put("height", height)
                        put("disable_analysis", true)
                        put("time_control", "byoyomi")
                        put("time_control_parameters", createJsonObject {
                            put("time_control", "byoyomi")
                            put("main_time", mainTimes[mainTime.progress].time)
                            put("period_time", byoYomiTimes[byoYomiTime.progress].time)
                            put("periods", periods)
                        })
                    })
                }
                val body = RequestBody.create(MediaType.parse("application/json"), post.toString())
                val ogs = OGS(Globals.uiConfig!!)
                ogs.openSocket()
                Globals.ogsService.createChallenge(body)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { challenge ->
                                    ogs.openGameSocket(challenge.game)
                                    val keepalive = Keepalive(challenge.challenge, challenge.game, ogs)
                                    keepalive.execute()

                                    val dialog = AlertDialog.Builder(activity)
                                            .setMessage("Challenge created. Waiting for challenger. Click cancel to delete the challenge.")
                                            .setNegativeButton("Cancel") { dialogInterface, i ->
                                                waitForGameSubscription?.dispose()
                                                ogs.closeGameSocket(challenge.game)
                                                keepalive.cancel(true)
                                                Globals.ogsService.deleteChallenge(challenge.challenge)
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(
                                                                {},
                                                                { e -> Log.e("CreateAGameFragment", "error while deleting challenge", e) }
                                                        )
                                            }
                                            .show()

                                    waitForGameSubscription = ogs.listenForGameData(challenge.game)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                    {
                                                        println("listenForGameData: onSuccess")
                                                        dialog.dismiss()
                                                        ogs.closeGameSocket(challenge.game)
                                                        ogs.closeSocket()
                                                        keepalive.cancel(true)
                                                        val intent = Intent(activity, Main3Activity::class.java)
                                                        intent.putExtra("id", challenge.game)
                                                        startActivity(intent)
                                                    },
                                                    { e ->
                                                        println("listenForGameData: onError")
                                                        Log.e("CreateAGameFragment", "error while waiting for game data", e)
                                                    }
                                            )
                                },
                                { e ->
                                    Log.e("FindAGameFragment", "create challenge failed", e)
                                    AlertDialog.Builder(activity)
                                            .setMessage("Create challenge failed.\n" + e.toString())
                                            .setPositiveButton("Ok") { dialog, id -> }
                                            .show()
                                }
                        )
            } catch (ex: Exception) {
                AlertDialog.Builder(activity)
                        .setMessage("Create challenge failed.\n" + ex.toString())
                        .setPositiveButton("Ok") { dialog, id -> }
                        .show()
                return@OnClickListener
            }
        })
        return rootView
    }

    inner class Keepalive(val challengeId: Int, val gameId: Int, val ogs: OGS) : AsyncTask<Void, Void, Int>() {
        override fun doInBackground(vararg p0: Void?): Int {
            println("doing keepalive background thread")
            while (true) {
                try {
                    ogs.challengeKeepalive(challengeId, gameId)
                    Thread.sleep(1000)
                    if (isCancelled) {
                        println("finished keepalive background thread")
                        return 0
                    }
                } catch (ex: InterruptedException) {
                    println("finished keepalive background thread")
                    return 0
                }
            }
        }
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

