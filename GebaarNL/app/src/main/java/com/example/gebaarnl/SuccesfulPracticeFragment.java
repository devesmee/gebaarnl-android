package com.example.gebaarnl;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.dionsegijn.konfetti.models.Size;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SuccesfulPracticeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SuccesfulPracticeFragment extends Fragment {

    View currentView;
    Button goToMapButton;
    String completedLevel;
    TextView letterLearnedTextView;
    KonfettiView confettiView;
    JSONObject jsonObject;
    SharedPreferences mPrefs;

    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    public SuccesfulPracticeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SuccesfulPracticeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SuccesfulPracticeFragment newInstance() {
        SuccesfulPracticeFragment fragment = new SuccesfulPracticeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle completedLevelBundle = getArguments();
        completedLevel = completedLevelBundle.getString("completed_level");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        currentView = inflater.inflate(R.layout.fragment_succesful_practice, container, false);
        letterLearnedTextView = currentView.findViewById(R.id.learnedLetterText);
        confettiView = currentView.findViewById(R.id.confettiView);
        mPrefs = getActivity().getSharedPreferences("GebaarNL", Context.MODE_PRIVATE);

        setLetterLearned();
        startConfetti();
        try {
            jsonObject = getJsonObject();
            editJSON(completedLevel);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();

        goToMapButton = currentView.findViewById(R.id.buttonMap);
        goToMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment MapFragment = new MapFragment();
                fragmentTransaction.replace(R.id.fragment_container, MapFragment);
                fragmentTransaction.commit();
            }
        });

        return currentView;
    }

    public void setLetterLearned() {
        String completeString = getContext().getString(R.string.letter_learned_pt1) + " " + completedLevel + " " + getContext().getString(R.string.letter_learned_pt2);
        letterLearnedTextView.setText(completeString);
    }

    public void startConfetti() {
        DisplayMetrics display = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(display);
        confettiView.build()
                .setPosition(-50f, display.widthPixels + 50f, -50f, -50f)
                .addColors(Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN)
                .setDirection(0.0, 359.0)
                .setSpeed(5f)
                .setAccelerationEnabled(false)
                .setTimeToLive(4000L)
                .addShapes(Shape.Circle.INSTANCE, Shape.Square.INSTANCE)
                .setFadeOutEnabled(true)
                .addSizes(new Size(12 ,5f))
                .streamFor(100, 6000L);
    }

    public void editJSON(String completedLevel) throws JSONException {
        String json = mPrefs.getString("jsonProgress", "");
        JSONObject jsonObject = new JSONObject(json);

        JSONArray lettersLeftArray = jsonObject.getJSONArray("left");
        JSONArray lettersRightArray = jsonObject.getJSONArray("right");

        JSONObject nextLetter;
        SharedPreferences.Editor prefsEditor = mPrefs.edit();

        if(completedLevel.equals("A"))
        {
            nextLetter = (JSONObject)lettersRightArray.get(0);
            nextLetter.put("isEnabled", "yes");
            lettersRightArray.put(0, nextLetter);
            jsonObject.put("right", lettersRightArray);
            jsonObject.put("left", lettersLeftArray);
        } else {
            nextLetter = (JSONObject)lettersLeftArray.get(1);
            nextLetter.put("isEnabled", "yes");
            lettersLeftArray.put(1, nextLetter);
            jsonObject.put("left", lettersLeftArray);
            jsonObject.put("right", lettersRightArray);
        }
        prefsEditor.putString("jsonProgress", String.valueOf(jsonObject));
        prefsEditor.apply();
    }

    public JSONObject getJsonObject() throws JSONException {
        // Create InputStream, by passing file name
        InputStream is = null;
        try {
            is = getActivity().getApplicationContext().getAssets().open("progress.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create Scanner, by passing InputStream (Scanner is used to read text from a file):
        Scanner scn = new Scanner(is);
        // Read the whole file, and put into String:
        String s = scn.useDelimiter("\\Z").next();

        return new JSONObject(s);
    }
}