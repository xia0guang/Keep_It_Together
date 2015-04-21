package com.xg.keepittogether;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStore;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateChangedListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private static final String PREF_ACCOUNT_NAME = "googleAccountName";
    private static final String SYNC_TOKEN_KEY = "syncToken";
    private SharedPreferences userPref;
    private SharedPreferences googlePref;
    private EventAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private MaterialCalendarView calendarView;
    private List<List<ParseEvent>> eventList;
    private HashMap<Long, Integer> positionMap;
    private HashMap<Integer, java.util.Calendar> reversePositionMap;
    private HashMap<String, ParseEvent> eventMap;

    public static final int REQUEST_ADD_NEW_EVENT = 0;
    public static final int REQUEST_CHANGE_EVENT = 1;
    public static final int REQUEST_SETTING = 2;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;


    private GoogleAccountCredential credential;
    private com.google.api.services.calendar.Calendar mService;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    /** Global instance of the sync settings datastore. */
    private static DataStore<String> syncSettingsDataStore;


    /**
     * TODO 1. change notification
            2. RecyclerView mess up;
            3. setScrollToPosition
     *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);
        googlePref = getSharedPreferences("Google_Calendar_List", MODE_PRIVATE);


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
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        //setAdapter

        mAdapter = new EventAdapter(MainActivity.this, eventList, userPref);
        mRecyclerView.setAdapter(mAdapter);

        //set calendarView
        calendarView = (MaterialCalendarView) findViewById(R.id.calendarView);
        calendarView.setShowOtherDates(true);
        java.util.Calendar today = java.util.Calendar.getInstance();
        calendarView.setSelectedDate(today);

        //Goolge Authorization

        File dataStoreDir = getFilesDir();
        FileDataStoreFactory dataStoreFactory = null;
        try{
            dataStoreFactory = new FileDataStoreFactory(dataStoreDir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        try {
            assert dataStoreFactory != null;
            syncSettingsDataStore = dataStoreFactory.getDataStore("SyncSettings");

        } catch (NullPointerException | IOException npe) {
            npe.printStackTrace();
        }

        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(userPref.getString(PREF_ACCOUNT_NAME, null));

        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Keep Together")
                .build();



        //implement calendar view date changed listener
        calendarView.setOnDateChangedListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
                int position = ((MyApplication)getApplication()).getPosition(calendarDay.getCalendar());
                mRecyclerView.smoothScrollToPosition(position);
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
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int position = layoutManager.findFirstVisibleItemPosition();
                java.util.Calendar cal = ((MyApplication)getApplication()).getCalendarByPosition(position);
                calendarView.setSelectedDate(cal);
            }
        });

        syncCalendarEvent();

        storeColorPref();
    }

    public void syncCalendarEvent() {
        final int listSize = googlePref.getInt("listSize", 0);
        if(listSize > 0) {
            if (isDeviceOnline()) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {

                            for (int i=0; i<listSize; i++) {
                                String calendarName = googlePref.getString("calendar_" + i, null);
                                syncGoogleCalendar(calendarName);
                            }
                        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                            MainActivity.this.showGooglePlayServicesAvailabilityErrorDialog(
                                    availabilityException.getConnectionStatusCode());

                        } catch (UserRecoverableAuthIOException userRecoverableException) {
                            MainActivity.this.startActivityForResult(
                                    userRecoverableException.getIntent(),
                                    SettingActivity.REQUEST_AUTHORIZATION);

                        } catch (IOException e) {
                            Log.d("The following error occurred: ",e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        fetchParseEvent();
                    }
                }.execute();
            } else {
                Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void syncGoogleCalendar(String calendarName) throws IOException{
        if(calendarName == null) return;
        Calendar.Events.List request = mService.events().list(calendarName);

        // Load the sync token stored from the last execution, if any.
        String syncToken = syncSettingsDataStore.get(SYNC_TOKEN_KEY);
        if (syncToken == null) {
            System.out.println("Performing full sync.");

            // Set the filters you want to use during the full sync. Sync tokens aren't compatible with
            // most filters, but you may want to limit your full sync to only a certain date range.
            // In this example we are only syncing events up to a year old.
            java.util.Calendar fiveYearAgo = java.util.Calendar.getInstance();
            fiveYearAgo.add(java.util.Calendar.YEAR, -5);
            request.setTimeMin(new DateTime(fiveYearAgo.getTime()));
        } else {
            System.out.println("Performing incremental sync.");
            request.setSyncToken(syncToken);
        }

        // Retrieve the events, one page at a time.
        String pageToken = null;
        Events events = null;
        do {
            request.setPageToken(pageToken);
            try {
                events = request.execute();
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 410) {
                    // A 410 status code, "Gone", indicates that the sync token is invalid.
                    Toast.makeText(this,"Invalid sync token, please restart app.", Toast.LENGTH_LONG).show();
                    syncSettingsDataStore.delete(SYNC_TOKEN_KEY);
                } else {
                    throw e;
                }
            }

            List<Event> items = events.getItems();
            if (items.size() == 0) {
                System.out.println("No new events to sync.");
            } else {
                for (Event event : items) {
                    final ParseEvent parseEvent = new ParseEvent();
                    parseEvent.setTitle(event.getSummary());
                    parseEvent.setNote(event.getDescription());
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTimeInMillis(event.getStart().getDateTime().getValue());
                    parseEvent.setStartDate(cal);
                    cal.setTimeInMillis(event.getEnd().getDateTime().getValue());
                    parseEvent.setEndDate(cal);
                    parseEvent.setMemberName(userPref.getString("memberName", null));
                    parseEvent.setFrom("Google_Calendar");
                    parseEvent.setCalendarName(calendarName);
                    parseEvent.setACL(new ParseACL(ParseUser.getCurrentUser()));

                    parseEvent.pinInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            parseEvent.saveEventually();
                        }
                    });
                }
            }

            pageToken = events.getNextPageToken();
        } while (pageToken != null);

        // Store the sync token from the last request to be used during the next execution.
        syncSettingsDataStore.set(SYNC_TOKEN_KEY, events.getNextSyncToken());

        System.out.println("Sync complete.");
    }

    private void fetchParseEvent() {
        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        query.orderByAscending("startDate");
        query.findInBackground(new FindCallback<ParseEvent>() {
            public void done(List<ParseEvent> list, ParseException e) {
                if (e == null && list.size() > 0) {

                    Toast.makeText(MainActivity.this, "query is done", Toast.LENGTH_SHORT).show();

                    //put query result into evetnList
                    eventList.add(new ArrayList<ParseEvent>(Arrays.asList(list.get(0))));
                    eventMap.put(list.get(0).getObjectId(), list.get(0));

                    for (int i = 1; i < list.size(); i++) {
                        ParseEvent curObj = list.get(i);
                        eventMap.put(curObj.getObjectId(), curObj);
                        List<ParseEvent> l = eventList.get(eventList.size()-1);
                        ParseEvent preObj = l.get(l.size()-1);

                        java.util.Calendar preDate = preObj.getStartCal();
                        java.util.Calendar curDate = curObj.getStartCal();

                        if(preDate.get(java.util.Calendar.DAY_OF_MONTH) == curDate.get(java.util.Calendar.DAY_OF_MONTH)) {
                            l.add(curObj);
                        } else {
                            eventList.add(new ArrayList<ParseEvent>(Arrays.asList(curObj)));
                        }
                    }

                    //create mAdapter positionMap and reversePositionMap
                    for (int i = 0; i <eventList.size(); i++) {
                        java.util.Calendar cal = eventList.get(i).get(0).getStartCal();
                        long day = (cal.get(java.util.Calendar.YEAR) - 1970)*366 + cal.get(java.util.Calendar.MONTH) * 31 + cal.get(java.util.Calendar.DAY_OF_MONTH);
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
            startActivityForResult(settingIntent, REQUEST_SETTING);
            return true;
        }
        if (id == R.id.action_add_new_event) {
            Intent addEventIntent = new Intent(this, AddEventActivity.class);
            startActivityForResult(addEventIntent, REQUEST_ADD_NEW_EVENT);
            return true;
        }
        if (id == R.id.action_today) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ADD_NEW_EVENT && resultCode==RESULT_OK){
            if(data.hasExtra("changedPosition") ){
                mAdapter.notifyItemInserted(data.getIntExtra("changedPosition", 0));
                Toast.makeText(this, "change event completed", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode == REQUEST_CHANGE_EVENT && resultCode==RESULT_OK){
            if(data.hasExtra("changedPosition") ){
                mAdapter.notifyItemChanged(data.getIntExtra("changedPosition", 0));
                Toast.makeText(this, "delete event completed", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode == REQUEST_SETTING && resultCode==RESULT_OK) {
            if(data.hasExtra("colorChanged")) {
                mAdapter.notifyItemRangeChanged(0, eventList.size());
                Toast.makeText(this, "color setting changed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode,
                        MainActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }
}
