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
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateChangedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private SharedPreferences userPref;
    private EventAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private MaterialCalendarView calendarView;
    private List<List<Event>> eventList;
    private HashMap<Long, Integer> positionMap;
    private HashMap<Integer, Calendar> reversePositionMap;
    private HashMap<String, Event> eventMap;

    public static final int ADD_NEW_EVENT = 0;
    public static final int CHANGE_EVENT = 1;
    public static final int SETTING = 2;

/*TODO 1. change notification
    2. RecyclerView mess up;
    3. setScrollToPosition
    4.google calednar*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);


        //handle push register
        ParseInstallation parseInstallation = ParseInstallation.getCurrentInstallation();
        parseInstallation.put("user", ParseUser.getCurrentUser());
        parseInstallation.put("memberName", getSharedPreferences("User_Preferences", MODE_PRIVATE).getString("memberName", "noValue"));
        parseInstallation.saveInBackground();


        eventList = ((MyApplication) getApplication()).eventList;
        positionMap = ((MyApplication) getApplication()).positionMap;
        reversePositionMap = ((MyApplication) getApplication()).reversePositionMap;
        eventMap = ((MyApplication) getApplication()).eventMap;



        mRecyclerView = (RecyclerView) findViewById(R.id.recycleView);
//        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        //setAdapter

        mAdapter = new EventAdapter(MainActivity.this, eventList, userPref);
        mRecyclerView.setAdapter(mAdapter);

        //set calendarView
        calendarView = (MaterialCalendarView) findViewById(R.id.calendarView);
        calendarView.setShowOtherDates(true);
        Calendar today = Calendar.getInstance();
        calendarView.setSelectedDate(today);



        //implement calendar view date changed listener
        /*calendarView.setOnDateChangedListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
                EventAdapter eventAdapter = (EventAdapter) mRecyclerView.getAdapter();
                final int position = eventAdapter.getPosition(calendarDay.getCalendar());
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.smoothScrollToPosition(position);
                    }
                });
            }
        });*/

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
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int position = layoutManager.findFirstVisibleItemPosition();
                Calendar cal = ((MyApplication)getApplication()).getCalendarByPosition(position);
                calendarView.setSelectedDate(cal);
            }
        });

        queryEventData();

        storeColorPref();
    }
    private void queryEventData() {
        ParseQuery<Event> query = ParseQuery.getQuery(Event.class);
        query.orderByAscending("startDate");
        query.findInBackground(new FindCallback<Event>() {
            public void done(List<Event> list, ParseException e) {
                if (e == null && list.size() > 0) {

                    Toast.makeText(MainActivity.this, "query is done", Toast.LENGTH_SHORT).show();

                    //put query result into evetnList
                    eventList.add(new ArrayList<Event>(Arrays.asList(list.get(0))));
                    eventMap.put(list.get(0).getObjectId(), list.get(0));

                    for (int i = 1; i < list.size(); i++) {
                        Event curObj = list.get(i);
                        Log.d("start time: ", String.format("%tD  %tl:%tM %tp", curObj.getStartCal(), curObj.getStartCal(), curObj.getStartCal(), curObj.getStartCal()));
                        Log.d("end time: ", String.format("%tD  %tl:%tM %tp", curObj.getEndCal(), curObj.getEndCal(), curObj.getEndCal(), curObj.getEndCal()));
                        Log.d("title: ", curObj.getTitle());
                        Log.d("member name: ", curObj.getMemberName());
                        Log.d("==", "=================");


                        eventMap.put(curObj.getObjectId(), curObj);

                        List<Event> l = eventList.get(eventList.size()-1);
                        Event preObj = l.get(l.size()-1);

                        Calendar preDate = preObj.getStartCal();
                        Calendar curDate = curObj.getStartCal();

                        if(preDate.get(Calendar.DAY_OF_MONTH) == curDate.get(Calendar.DAY_OF_MONTH)) {
                            l.add(curObj);
                        } else {
                            eventList.add(new ArrayList<Event>(Arrays.asList(curObj)));
                        }
                    }

                    Log.d("==", "========end of query=========");

                    //create mAdapter positionMap and reversePositionMap
                    for (int i = 0; i <eventList.size(); i++) {
                        Calendar cal = eventList.get(i).get(0).getStartCal();
                        long day = (cal.get(Calendar.YEAR) - 1970)*366 + cal.get(Calendar.MONTH) * 31 + cal.get(Calendar.DAY_OF_MONTH);
                        positionMap.put(day, i);
                        reversePositionMap.put(i, cal);
                    }

                    mAdapter.notifyDataSetChanged();

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

    private void storeColorPref() {
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

        mAdapter.notifyDataSetChanged();
        Toast.makeText(this, "I am changing", Toast.LENGTH_SHORT).show();
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
            startActivityForResult(settingIntent, SETTING);
            return true;
        }
        if (id == R.id.action_add_new_event) {
            Intent addEventIntent = new Intent(this, AddEventActivity.class);
            startActivityForResult(addEventIntent, ADD_NEW_EVENT);
            return true;
        }
        if (id == R.id.action_today) {
            Intent intent = new Intent(this, UpcomingEventsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ADD_NEW_EVENT && resultCode==RESULT_OK){
            if(data.hasExtra("changedPosition") ){
                mAdapter.notifyItemInserted(data.getIntExtra("changedPosition", 0));
                Toast.makeText(this, "change event completed", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode == CHANGE_EVENT && resultCode==RESULT_OK){
            if(data.hasExtra("changedPosition") ){
                mAdapter.notifyItemChanged(data.getIntExtra("changedPosition", 0));
                Toast.makeText(this, "delete event completed", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode == SETTING && resultCode==RESULT_OK) {
            if(data.hasExtra("colorChanged")) {
                mAdapter.notifyItemRangeChanged(0, eventList.size());
                Toast.makeText(this, "color setting changed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
