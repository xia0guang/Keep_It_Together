package com.xg.keepittogether;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.parse.ParseObject;


public class MainActivity extends ActionBarActivity implements EventAdapter.CompleteQueryListner {

    private SharedPreferences userPreferences;
    EventAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userPreferences = getSharedPreferences("User_Preferences", MODE_PRIVATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        /*myAdapter = new EventAdapter(this, userPreferences);
        ListView allEvent = (ListView)findViewById(R.id.listView);
        allEvent.setAdapter(myAdapter);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingIntent = new Intent(this, SettingActivity.class);
            startActivity(settingIntent);
            return true;
        }
        if (id == R.id.action_add_new_event) {
            Intent addEventIntent = new Intent(this, AddEventActivity.class);
            startActivity(addEventIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setList() {
        myAdapter.notifyDataSetChanged();
    }
}
