package com.xg.keepittogether;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.SaveCallback;
import com.xg.keepittogether.Parse.Member;
import com.xg.keepittogether.Parse.ParseEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wuxiaoguang on 3/29/15.
 */
public class MyApplication extends Application {




    public static class DataWrapper{
        public List<List<ParseEvent>> eventList;
//        public HashMap<Long, Integer> positionMap;
        public HashMap<Integer, Calendar> reversePositionMap;
//        public HashMap<String, ParseEvent> eventMap;
        public Calendar upCal, upThresholdCal, downCal, downThresholdCal;
        public boolean upFetch, downFetch, loading;
        DataWrapper() {
            eventList = new ArrayList<List<ParseEvent>>();
//            positionMap = new HashMap<>();
            reversePositionMap = new HashMap<>();
//            eventMap = new HashMap<>();
            upCal = Calendar.getInstance();
            upThresholdCal = Calendar.getInstance();
            downCal = Calendar.getInstance();
            downThresholdCal = Calendar.getInstance();
            upFetch = true; downFetch = true;
            loading = false;
        }

        public void clear() {
            eventList.clear();
//            positionMap.clear();
            reversePositionMap.clear();
//            eventMap.clear();
        }
    }

    public DataWrapper dataWrapper;

    @Override
    public void onCreate() {
        super.onCreate();

        dataWrapper = new DataWrapper();
        ParseCrashReporting.enable(this);
        ParseObject.registerSubclass(ParseEvent.class);
        ParseObject.registerSubclass(Member.class);

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.app_key), getString(R.string.client_key));
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

    /*public int getPosition(Calendar cal) {
        long day = (cal.get(Calendar.YEAR) - 1970)*366 + cal.get(Calendar.MONTH) * 31 + cal.get(Calendar.DAY_OF_MONTH);

        return -1;
    }*/

    public Calendar getCalendarByPosition(int position) {
        if(position >= 0 && position < dataWrapper.eventList.size()) {
            return dataWrapper.reversePositionMap.get(position);
        }
        return null;
    }

}
