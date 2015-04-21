package com.xg.keepittogether;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wuxiaoguang on 3/29/15.
 */
public class MyApplication extends Application {

    protected List<List<ParseEvent>> eventList;
    protected HashMap<Long, Integer> positionMap;
    protected HashMap<Integer, Calendar> reversePositionMap;
    protected HashMap<String, ParseEvent> eventMap;

    @Override
    public void onCreate() {
        super.onCreate();
        eventList = new ArrayList<List<ParseEvent>>();
        positionMap = new HashMap<>();
        reversePositionMap = new HashMap<>();
        eventMap = new HashMap<>();

        // Initialize Crash Reporting.
        ParseCrashReporting.enable(this);

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        // Add your initialization code here
        ParseObject.registerSubclass(ParseEvent.class);

        Parse.initialize(this, getString(R.string.app_key), getString(R.string.client_key));
//        ParseUser.enableAutomaticUser();
//        ParseUser.getCurrentUser().saveInBackground();
//        ParseACL defaultACL = new ParseACL();
        // Optionally enable public read access.
//        defaultACL.setPublicReadAccess(true);
//        ParseACL.setDefaultACL(defaultACL, true);


        //subscribe to push notification
        ParsePush.subscribeInBackground("EventUpdate", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
                } else {
                    Log.e("com.parse.push", "failed to subscribe for push", e);
                }
            }
        });


    }

    public int getPosition(Calendar cal) {
        long day = (cal.get(Calendar.YEAR) - 1970)*366 + cal.get(Calendar.MONTH) * 31 + cal.get(Calendar.DAY_OF_MONTH);
        if(positionMap.get(day) != null) return positionMap.get(day);
        else return -1;
    }

    public Calendar getCalendarByPosition(int position) {
        if(position >= 0 && position < eventList.size()) {
            return reversePositionMap.get(position);
        }
        return null;
    }

}
