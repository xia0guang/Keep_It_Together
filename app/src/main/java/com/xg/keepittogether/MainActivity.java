package com.xg.keepittogether;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateChangedListener;
import com.xg.keepittogether.Parse.Member;
import com.xg.keepittogether.Parse.ParseEventUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private SharedPreferences userPref;
    private SharedPreferences googlePref;

    public RecyclerView mRecyclerView;
    public EventAdapter mAdapter;
    public LinearLayoutManager mLayoutManager;
    public MaterialCalendarView calendarView;
    public Calendar currentCal = Calendar.getInstance();

    public static final int REQUEST_ADD_OR_CHANGE_NEW_EVENT = 0;
    public static final int REQUEST_SETTING = 1;
    public static final int ITEM_LEFT_LOAD_MORE = 5;

    private MyApplication.DataWrapper dataWrapper;

    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);
        googlePref = getSharedPreferences("Google_Calendar_List", MODE_PRIVATE);
        dataWrapper = ((MyApplication)getApplication()).dataWrapper;

        //handle push register
        ParseInstallation parseInstallation = ParseInstallation.getCurrentInstallation();
        parseInstallation.put("user", ParseUser.getCurrentUser());
        parseInstallation.put("memberName", getSharedPreferences("User_Preferences", MODE_PRIVATE).getString("memberName", null));
        parseInstallation.saveInBackground();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.action_bar)));

        //RecyclerView initialization
        mRecyclerView = (RecyclerView) findViewById(R.id.recycleView);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        //setAdapter
        mAdapter = new EventAdapter(MainActivity.this, dataWrapper.eventList, userPref);
        mRecyclerView.setAdapter(mAdapter);

        //set CalendarView
        calendarView = (MaterialCalendarView) findViewById(R.id.calendarView);
        calendarView.setShowOtherDates(true);
        java.util.Calendar today = java.util.Calendar.getInstance();
        calendarView.setSelectedDate(today);



        //implement calendar view date changed listener
        final OnDateChangedListener mOnDateChangedListener = new OnDateChangedListener() {
            @Override
            public void onDateChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
                calendarView.setCurrentDate(calendarDay);
                for (int i = 0; i < dataWrapper.eventList.size(); i++) {//limit size is 10000 or it will lag
                    Calendar changedCal = calendarDay.getCalendar();
                    Calendar curCal = dataWrapper.eventList.get(i).get(0).getStartCal();
                    if (ParseEventUtils.hashCalDay(curCal) >= ParseEventUtils.hashCalDay(changedCal)) {
                        mLayoutManager.scrollToPositionWithOffset(i, 0);
                        final int position = i;
                        mRecyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                View row = null;
                                int j = 0;
                                while(row == null) {
                                    row = mLayoutManager.findViewByPosition(position);
                                }
                                Animation blink = AnimationUtils.loadAnimation(MainActivity.this, R.anim.blink);
                                if(row != null)row.startAnimation(blink);
                            }
                        }, 100);
                        break;
                    }
                }
            }
        };

        calendarView.setOnDateChangedListener(mOnDateChangedListener);

        //implement date change based on recycler view scrolling
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                calendarView.setOnDateChangedListener(null);
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                EventAdapter adapter = (EventAdapter) recyclerView.getAdapter();
                int firstPosition = layoutManager.findFirstVisibleItemPosition();
                Calendar firstCal = dataWrapper.positionCalMap.get(firstPosition);
                if (!firstCal.equals(currentCal)) {
                    currentCal = firstCal;
                    calendarView.setSelectedDate(firstCal);
                    calendarView.setCurrentDate(firstCal);
                }
                if (dy > 0 && dataWrapper.downFetch) {
                        int lastPosition = layoutManager.findLastVisibleItemPosition();
                        if(!dataWrapper.loading && adapter.getItemCount() - lastPosition <= ITEM_LEFT_LOAD_MORE) {
                            dataWrapper.loading = true;
                            new LoadMoreEvent().execute(ParseEventUtils.DOWN);
                        }
                    }
                    if (dy < 0 && dataWrapper.upFetch ) {
                        if (!dataWrapper.loading && firstPosition <= ITEM_LEFT_LOAD_MORE) {
                            dataWrapper.loading = true;
                            new LoadMoreEvent().execute(ParseEventUtils.UP);
                        }
                    }
                calendarView.setOnDateChangedListener(mOnDateChangedListener);
                }
        });
        new syncCalendarAndFetch().execute();
        storeColorPref();
    }


    @Override
    protected void onResume() {
        super.onResume();

    }



    private void storeColorPref() {
        ParseQuery<Member> queryMember = ParseQuery.getQuery(Member.class);
        queryMember.findInBackground(new FindCallback<Member>() {
            @Override
            public void done(List<Member> members, ParseException e) {
                if (e == null && members.size() > 0) {
                    SharedPreferences.Editor edit = userPref.edit();
                    for (Member member : members) {
                        edit.putInt("color." + member.getMemberName(), member.getColor());
                    }
                    edit.apply();
                } else if(e != null) {
                    Log.d("member query:", "Error" + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ADD_OR_CHANGE_NEW_EVENT && resultCode==RESULT_OK){
            if(data.hasExtra("changedPosition") ){
                mAdapter.notifyDataSetChanged();
            }
        } else if(requestCode == REQUEST_SETTING && resultCode==RESULT_OK) {
            if(data.hasExtra("colorChanged")) {
                //TODO
            }
            if(data.hasExtra("googleCalendarSettingChanged")) {
                Toast.makeText(this, "google calendar changed", Toast.LENGTH_SHORT).show();
                ParseEventUtils.firstTimeParseEventFromLocal(this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingIntent = new Intent(this, SettingActivity.class);
            startActivityForResult(settingIntent, REQUEST_SETTING);
            return true;
        }
        if (id == R.id.action_add_new_event) {
            Intent addEventIntent = new Intent(this, AddEventActivity.class);
            startActivityForResult(addEventIntent, REQUEST_ADD_OR_CHANGE_NEW_EVENT);

            return true;
        }
        if (id == R.id.action_today) {
            java.util.Calendar today = java.util.Calendar.getInstance();
            calendarView.setSelectedDate(today);
            calendarView.setCurrentDate(today);
            return true;
        }
        if (id == R.id.action_sync_with_server) {
            if (GoogleCalendarUtils.isDeviceOnline(this)) {
                new syncCalendarAndFetch().execute();
            }else {
                Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private class syncCalendarAndFetch extends AsyncTask<Void, Void, Void> {
        int position = 0;
        @Override
        protected Void doInBackground(Void... params) {
            try {
                int listSize = googlePref.getInt("listSize", 0);
                if (listSize > 0) {
                    if (GoogleCalendarUtils.isGooglePlayServicesAvailable(MainActivity.this)) {
                        if (GoogleCalendarUtils.isDeviceOnline(MainActivity.this)) {
                            GoogleCalendarUtils.downloadGoogleCalendar(MainActivity.this);
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "No network connection available.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Google Play Services required: " +
                                        "after installing, close and relaunch this app.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                String memberName = userPref.getString("memberName", null);
                position = ParseEventUtils.fetchEventInSameThread(MainActivity.this, memberName);
            } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                GoogleCalendarUtils.showGooglePlayServicesAvailabilityErrorDialog(
                        MainActivity.this, availabilityException.getConnectionStatusCode());
            } catch (UserRecoverableAuthIOException userRecoverableException) {
                MainActivity.this.startActivityForResult(
                        userRecoverableException.getIntent(),
                        SettingActivity.REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.d("error occurred: ",e.getMessage());
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter.notifyDataSetChanged();
            EventIndicateDecorator.buildDecorators(MainActivity.this);
            mLayoutManager.scrollToPositionWithOffset(position, 0);
            dataWrapper.loading = false;
        }
    }

    private class LoadMoreEvent extends AsyncTask<Integer, Void, Void> {

        int dir;
        @Override
        protected Void doInBackground(Integer... params) {
            dir = params[0];
            ParseEventUtils.updateParseEventFromLocal(MainActivity.this, dir);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter.notifyDataSetChanged();
            EventIndicateDecorator.buildDecorators(MainActivity.this);
            if(ParseEventUtils.UP == dir) {
                mLayoutManager.scrollToPositionWithOffset(ParseEventUtils.LOAD_ITEM_QUANTITY + ITEM_LEFT_LOAD_MORE, 0);
            }
            dataWrapper.loading = false;
            Log.d("List size: ", dataWrapper.eventList.size() + "");
        }
    }

}
