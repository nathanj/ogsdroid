package com.example.njones.myapplication;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ChallengeListActivity extends AppCompatActivity implements OnItemClickListener {
    private static final String TAG = "ChallengeListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_list);

        String[] mystrs = {"Hello", "world"};

        ListView lv = (ListView) findViewById(R.id.challenge_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.activity_listview,
                mystrs);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.w(TAG, "i = " + position + " l = " + id);

        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to pass?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
