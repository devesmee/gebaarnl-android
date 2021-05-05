package com.example.gebaarnl;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefs = getSharedPreferences("GebaarNL", MODE_PRIVATE);

        if(!sharedPrefs.contains("jsonProgress")) {
            try {
                setupSharedPreferences();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(getIntent().getStringExtra("completed_level") != null){
            Bundle completedLevelBundle = new Bundle();
            completedLevelBundle.putString("completed_level", getIntent().getStringExtra("completed_level"));
            SuccesfulPracticeFragment successFragment = new SuccesfulPracticeFragment();
            successFragment.setArguments(completedLevelBundle);

            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, successFragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MapFragment()).commit();
        }

    }

    @Override
    public void onBackPressed(){
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MapFragment()).commit();
    }

    public void setupSharedPreferences() throws JSONException {
        SharedPreferences.Editor prefsEditor = sharedPrefs.edit();

        InputStream is = null;
        try {
            is = getApplicationContext().getAssets().open("progress.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create Scanner, by passing InputStream (Scanner is used to read text from a file):
        Scanner scn = new Scanner(is);
        // Read the whole file, and put into String:
        String s = scn.useDelimiter("\\Z").next();
        ArrayList<JSONObject> lettersList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(s);
            prefsEditor.putString("jsonProgress", String.valueOf(jsonObject));
            prefsEditor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}