package com.example.gebaarnl;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {

    View currentView;
    ArrayList<JSONObject> buttonsLeft;
    ArrayList<JSONObject> buttonsRight;
    ListView buttonsLeftListView;
    ListView buttonsRightListView;
    SharedPreferences mPrefs;

    FragmentManager fragmentManager;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapFragment.
     */
    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        currentView = inflater.inflate(R.layout.fragment_map_list, container, false);

        mPrefs = getActivity().getSharedPreferences("GebaarNL", Context.MODE_PRIVATE);

        buttonsLeft = getProgress("left");
        buttonsRight = getProgress("right");

        buttonsLeftListView = currentView.findViewById(R.id.leftButtonsList);
        buttonsRightListView = currentView.findViewById(R.id.rightButtonsList);

        fragmentManager = getFragmentManager();

        MapAdapter buildingsLeftAdapter = new MapAdapter(getContext(), R.layout.list_item_map_orange, buttonsLeft, fragmentManager);
        buttonsLeftListView.setAdapter(buildingsLeftAdapter);

        MapAdapter buildingsRightAdapter = new MapAdapter(getContext(), R.layout.list_item_map_green, buttonsRight, fragmentManager);
        buttonsRightListView.setAdapter(buildingsRightAdapter);

        return currentView;
    }

    public ArrayList<JSONObject> getProgress(String place){
        ArrayList<JSONObject> lettersList = new ArrayList<>();
        try {
            String json = mPrefs.getString("jsonProgress", "");
            JSONObject jsonObject = new JSONObject(json);
            Log.e("FRAGMENT JSON OBJECT: ", jsonObject.toString());
            JSONArray jsonArray = jsonObject.getJSONArray(place);
            for(int i=0;i<jsonArray.length();i++) {
                // each array element is an object
                JSONObject letterObject = jsonArray.getJSONObject(i);
                lettersList.add(letterObject);}

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return lettersList;
    }
}