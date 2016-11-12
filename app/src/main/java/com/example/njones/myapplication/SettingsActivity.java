package com.example.njones.myapplication;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Created by njones on 11/5/16.
 */

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SettingsActivity", "onCreate");
        addPreferencesFromResource(R.xml.preferences);
    }
}
