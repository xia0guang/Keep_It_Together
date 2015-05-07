package com.xg.keepittogether;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
    public MaterialCalendarView calendarView;
    public EventAdapter mAdapter;

    public static final int REQUEST_ADD_OR_CHANGE_NEW_EVENT = 0;
    public static final int REQUEST_SETTING = 1;

    private MyApplication.DataWrapper dataWrapper;



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



        //RecyclerView initialization
        mRecyclerView = (RecyclerView) findViewById(R.id.recycleView);

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
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
                                    Log.d("loop:", j++ + "");
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
                if (!dataWrapper.loading) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int firstPosition = layoutManager.findFirstVisibleItemPosition();
                    java.util.Calendar firstCal = dataWrapper.reversePositionMap.get(firstPosition);
                    calendarView.setSelectedDate(firstCal);
                    if (dy > 0 && dataWrapper.downFetch) {
                        int lastPosition = layoutManager.findLastVisibleItemPosition();
                        java.util.Calendar lastCal = dataWrapper.reversePositionMap.get(lastPosition);
                        if (lastCal.compareTo(dataWrapper.downThresholdCal) >= 0) {
                            new LoadMoreEvent().execute(ParseEventUtils.DOWN);
                        }
                    }
                    if (dy < 0 && dataWrapper.upFetch) {
                        if (firstCal.compareTo(dataWrapper.upThresholdCal) <= 0) {
                            new LoadMoreEvent().execute(ParseEventUtils.UP);
                        }
                    }
                }
                calendarView.setOnDateChangedListener(mOnDateChangedListener);
            }
        });

        ParseEventUtils.firstTimeParseEventFromLocal(this);
        int listSize = googlePref.getInt("listSize", 0);
        if (listSize > 0) {
            if (GoogleCalendarUtils.isGooglePlayServicesAvailable(this)) {
                syncCalendarEvent(listSize);
            } else {
                Toast.makeText(this, "Google Play Services required: " +
                        "after installing, close and relaunch this app.", Toast.LENGTH_LONG).show();
            }
        }else{
            String memberName = userPref.getString("memberName", null);
            ParseEventUtils.fetchEventInNewThread(this, memberName);
        }

        storeColorPref();
    }


    @Override
    protected void onResume() {
        super.onResume();

    }


    public void syncCalendarEvent(final int listSize) {
        if (GoogleCalendarUtils.isDeviceOnline(this)) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        GoogleCalendarUtils.downloadGoogleCalendar(MainActivity.this);

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
                    //Fetch ParseEvent from server in order to make consistent between local and server
                    String memberName = userPref.getString("memberName", null);
                    ParseEventUtils.fetchEventInSameThread(MainActivity.this, memberName);
                }
            }.execute();
        } else {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        }
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
//                mAdapter.notifyItemInserted(data.getIntExtra("changedPosition", 0));
                mAdapter.notifyDataSetChanged();
//                Toast.makeText(this, "add/change event completed", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode == REQUEST_SETTING && resultCode==RESULT_OK) {
            if(data.hasExtra("colorChanged")) {
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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
            return true;
        }
        if (id == R.id.action_sync_with_server) {
            int listSize = googlePref.getInt("listSize", 0);
            if (listSize > 0) {
                if (GoogleCalendarUtils.isGooglePlayServicesAvailable(this)) {
                    syncCalendarEvent(listSize);
                } else {
                    Toast.makeText(this, "Google Play Services required: " +
                            "after installing, close and relaunch this app.", Toast.LENGTH_LONG).show();
                }
            }else{
                String memberName = userPref.getString("memberName", null);
                ParseEventUtils.fetchEventInNewThread(this, memberName);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private class LoadMoreEvent extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            int dir = params[0];
            ParseEventUtils.updateParseEventFromLocal(MainActivity.this, dir);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dataWrapper.loading = false;
            mAdapter.notifyDataSetChanged();
        }
    }

}
