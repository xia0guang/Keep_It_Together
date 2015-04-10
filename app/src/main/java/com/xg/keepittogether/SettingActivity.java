package com.xg.keepittogether;

import android.app.Activity;
import android.app.usage.UsageEvents;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingActivity extends Activity implements AdapterView.OnItemSelectedListener{


    private SharedPreferences userPref;
    private Spinner spinner;
    private ArrayList<String> allColor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);


        TextView familyEmailView = (TextView)findViewById(R.id.familyAcountTV);
        familyEmailView.setText(ParseUser.getCurrentUser().getString("username"));
        TextView memberNameView = (TextView)findViewById(R.id.memberNameTV);
        memberNameView.setText(userPref.getString("memberName","noValue"));


        Spinner colorView = (Spinner)findViewById(R.id.colorSpinner);
        allColor = new ArrayList<String>(Arrays.asList("balck", "blue", "cyan", "Gray", "Green", "Magenta", "Red", "Yellow", "Orange"));
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, allColor);
        colorView.setAdapter(colorAdapter);
        colorView.setSelection(userPref.getInt("color", EventColor.BLUE));
        colorView.setOnItemSelectedListener(this);
    }

    public void signOut(View view) {
        ParseUser user = ParseUser.getCurrentUser();
        user.logOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int itemPosition = position;
        SharedPreferences.Editor edit = userPref.edit();
        edit.putInt("color", position);
        edit.commit();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Members");
        query.whereEqualTo("memberName", userPref.getString("memberName","noValue"));
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                ParseObject member = parseObjects.get(0);
                member.put("color", itemPosition);
                member.saveInBackground();
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
