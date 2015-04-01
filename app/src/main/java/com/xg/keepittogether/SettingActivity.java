package com.xg.keepittogether;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;


public class SettingActivity extends Activity {


    private SharedPreferences userPreferences;
    private EditText t;
    private Spinner spinner;
    private ArrayList<String> allColor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        userPreferences = getSharedPreferences("User_Preferences", MODE_PRIVATE);
        t = (EditText)findViewById(R.id.userIdET);

        String familyID = userPreferences.getString("familyID", "noValue");
        if(!familyID.equals("noValue")) {
            t.setText(familyID);
            Log.d("Color", userPreferences.getString("userColor", "Nothing"));
        }


        allColor = new ArrayList<>(Arrays.asList("Green", "Red", "Blue"));
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getBaseContext(),android.R.layout.simple_spinner_item, allColor);
        spinner = (Spinner) findViewById(R.id.colorOptions);
        spinner.setAdapter(arrayAdapter);
    }

    public void saveSetting(View view) {
        SharedPreferences.Editor userEditor = userPreferences.edit();
        userEditor.putString("familyID",t.getText().toString());
        userEditor.putString("userColor", allColor.get(spinner.getSelectedItemPosition()));
        userEditor.commit();
        finish();
    }
}
