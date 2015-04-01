package com.xg.keepittogether;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.parse.ParseObject;


public class AddEventActivity extends Activity {


    private SharedPreferences userPreferences;
    EditText nameET;
    EditText eventET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        userPreferences = getSharedPreferences("User_Preferences", MODE_PRIVATE);
        nameET = (EditText)findViewById(R.id.signUpMemberNameET);
        eventET = (EditText)findViewById(R.id.eventET);
    }


    public void addEvent(View view) {
        ParseObject event = new ParseObject("Event");
        event.put("familyID", userPreferences.getString("familyID", "noValue"));
        event.put("userColor", userPreferences.getString("userColor", "Green"));
        event.put("name", nameET.getText().toString());
        event.put("event", eventET.getText().toString());
        event.saveInBackground();
        finish();
    }
}
