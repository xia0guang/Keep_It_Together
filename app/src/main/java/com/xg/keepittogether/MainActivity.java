package com.xg.keepittogether;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;


public class MainActivity extends ActionBarActivity {

    private SharedPreferences userPreferences;
    EventAdapter myAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userPreferences = getSharedPreferences("User_Preferences", MODE_PRIVATE);

        mRecyclerView = (RecyclerView)findViewById(R.id.recycleView);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)




    }

    @Override
    protected void onResume() {
        super.onResume();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
//        query.whereEqualTo("memberName", userPreferences.getString("memberName", "noValue"));
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                    mAdapter = new EventAdapter(list, userPreferences);
                    mRecyclerView.setAdapter(mAdapter);
                    try{

                    } catch (ClassCastException cce) {
                        cce.printStackTrace();
                    }
                } else {
                    Log.d("Query", "Error: " + e.getMessage());
                }
            }
        });

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
}
