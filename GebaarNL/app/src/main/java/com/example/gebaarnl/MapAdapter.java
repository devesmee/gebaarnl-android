package com.example.gebaarnl;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapAdapter extends BaseAdapter {
    LayoutInflater inflater;
    int resourceLayout;
    ArrayList<JSONObject> buttonList;
    Button button;

    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    public MapAdapter(Context context, int resource, ArrayList<JSONObject> buttonList, FragmentManager fragmentManager) {
        this.resourceLayout = resource;
        this.buttonList = buttonList;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.fragmentManager = fragmentManager;
        this.fragmentTransaction = fragmentManager.beginTransaction();
    }

    @Override
    public int getCount() {
        return buttonList.size();
    }

    @Override
    public JSONObject getItem(int position) {
        return buttonList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(resourceLayout, parent, false);
        }

        if(convertView.findViewById(R.id.orangeButton) == null)
        {
            button = convertView.findViewById(R.id.greenButton);
        } else {
            button = convertView.findViewById(R.id.orangeButton);
        }

        try {
            button.setText(getLetter(getItem(position)));
            if(isEnabled(getItem(position))){
                button.setEnabled(true);
                button.setAlpha(1f);
            } else {
                button.setEnabled(false);
                button.setAlpha(.5f);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setButtonAction(button);

        return convertView;
    }

    public String getLetter(JSONObject buttonObject) throws JSONException {
        return buttonObject.getString("letter");
    }

    public boolean isEnabled(JSONObject buttonObject) throws JSONException {
        String isCompleted = buttonObject.getString("isEnabled");
        return isCompleted.equals("yes");
    }


    public void setButtonAction(Button button) {
        Bundle levelBundle = new Bundle();
        button.setOnClickListener(v -> {
            Fragment CameraFragment = new CameraFragment();
            levelBundle.putString("chosen_level", button.getText().toString());
            levelBundle.putInt("countdown", 2);
            CameraFragment.setArguments(levelBundle);
            fragmentTransaction.replace(R.id.fragment_container, CameraFragment);
            fragmentTransaction.commit();
        });
    }
}
