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
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateChangedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private SharedPreferences userPref;
    private EventAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private MaterialCalendarView calendarView;

//    private List<List<ParseObject>> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);
//        eventList = new ArrayList<List<ParseObject>>();

        mRecyclerView = (RecyclerView)findViewById(R.id.recycleView);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        calendarView = (MaterialCalendarView)findViewById(R.id.calendarView);
        calendarView.setShowOtherDates(true);
        Calendar today = Calendar.getInstance();
        calendarView.setSelectedDate(today);


        //implement calendar view date changed lisstener
        calendarView.setOnDateChangedListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
                EventAdapter eventAdapter = (EventAdapter)mRecyclerView.getAdapter();
                final int position = eventAdapter.getPosition(calendarDay.getCalendar());
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.smoothScrollToPosition(position);
                    }
                });
            }
        });

        //implement date change based on recycler view scrolling
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                Toast.makeText(MainActivity.this, "Scroll has been starting", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
                int position = layoutManager.findFirstVisibleItemPosition();
                Calendar cal =  ((EventAdapter)recyclerView.getAdapter()).getCalendarByPosition(position);
                calendarView.setSelectedDate(cal);
            }
        });

        ParseQuery<ParseObject> queryMember = ParseQuery.getQuery("Members");
        queryMember.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(e == null && list.size() > 0) {
                    SharedPreferences.Editor edit = userPref.edit();
                    for (ParseObject member : list) {
                        edit.putInt("color." + member.getString("memberName"), member.getInt("color"));
                    }
                    edit.apply();
                } else {
                    Log.d("Query:", "Error" + e.getMessage());
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
        query.orderByAscending("startDate");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null && list.size() > 0) {

                    List<List<ParseObject>> eventList = new ArrayList<List<ParseObject>>();
                    eventList.add(new ArrayList<ParseObject>(Arrays.asList(list.get(0))));
                    for (int i = 1; i < list.size(); i++) {
                        List<ParseObject> l = eventList.get(eventList.size()-1);
                        ParseObject preObj = l.get(l.size()-1);
                        ParseObject curObj = list.get(i);
                        Calendar preDate = Calendar.getInstance();
                        preDate.setTime(preObj.getDate("startDate"));
                        Calendar curDate = Calendar.getInstance();
                        curDate.setTime(curObj.getDate("startDate"));
                        if(preDate.get(Calendar.DAY_OF_MONTH) == curDate.get(Calendar.DAY_OF_MONTH)) {
                            l.add(curObj);
                        } else {
                            eventList.add(new ArrayList<ParseObject>(Arrays.asList(curObj)));
                        }
                    }

                    mAdapter = new EventAdapter(MainActivity.this, eventList, userPref);
                    mRecyclerView.setAdapter(mAdapter);
                    try{

                    } catch (ClassCastException cce) {
                        cce.printStackTrace();
                    }
                } else {
                    Log.d("Query", "Error: ");
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
