package com.xg.keepittogether.Parse;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.xg.keepittogether.EventIndicateDecorator;
import com.xg.keepittogether.GoogleCalendarUtils;
import com.xg.keepittogether.MyApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by wuxiaoguang on 4/23/15.
 */
public class ParseEventUtils {
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int LOAD_ITEM_QUANTITY = 10;

    public static int firstTimeParseEventFromLocal(Activity activity) {
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        java.util.Calendar startCal = java.util.Calendar.getInstance();
        startCal.add(Calendar.MONTH, -6);
        java.util.Calendar endCal = java.util.Calendar.getInstance();
        endCal.add(java.util.Calendar.MONTH, +6);

        MyApplication.DataWrapper dataWrapper = ((MyApplication)activity.getApplication()).dataWrapper;
        dataWrapper.clear();

        int returnPosition = 0;

        ParseQuery<ParseEvent> query1 = ParseQuery.getQuery(ParseEvent.class);
        query1.fromLocalDatastore();
        query1.whereNotEqualTo("inList", false);
        query1.whereGreaterThanOrEqualTo("startDate", startCal.getTime());
        query1.whereLessThanOrEqualTo("startDate", todayCal.getTime());
        query1.orderByAscending("startDate");

        ParseQuery<ParseEvent> query2 = ParseQuery.getQuery(ParseEvent.class);
        query2.fromLocalDatastore();
        query2.whereNotEqualTo("inList", false);
        query2.whereGreaterThan("startDate", todayCal.getTime());
        query2.whereLessThanOrEqualTo("startDate", endCal.getTime());
        query2.orderByAscending("startDate");

        try {
            List<ParseEvent> list = query1.find();
            returnPosition = list.size();
            list.addAll(query2.find());
            //put query result into event list
            if(list.size() == 0) return 0;
            for (int i = 0; i < list.size(); i++) {
                ParseEvent curObj = list.get(i);
                if (dataWrapper.eventList.size() >0 && hashCalDay(dataWrapper.eventList.get(dataWrapper.eventList.size()-1).get(0).getStartCal()) == hashCalDay(curObj.getStartCal())) {
                    List<ParseEvent> l = dataWrapper.eventList.get(dataWrapper.eventList.size() - 1);
                    l.add(curObj);
                } else {
                    dataWrapper.eventList.add(new ArrayList<>(Arrays.asList(curObj)));
                }
            }

            //create positionMap and positionCalMap
            for (int i = 0; i < dataWrapper.eventList.size(); i++) {
                java.util.Calendar cal = dataWrapper.eventList.get(i).get(0).getStartCal();
                dataWrapper.positionCalMap.put(i, cal);
            }

            //initialize fetch status;
            dataWrapper.upCal.setTimeInMillis(dataWrapper.eventList.get(0).get(0).getStartCal().getTimeInMillis());
            List<ParseEvent> lastList = dataWrapper.eventList.get(dataWrapper.eventList.size()-1);
            dataWrapper.downCal.setTimeInMillis(lastList.get(lastList.size() - 1).getStartCal().getTimeInMillis());
            dataWrapper.upFetch = true;
            dataWrapper.downFetch = true;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return returnPosition;
    }

    public static void updateParseEventFromLocal(final Activity activity, int dir) {

        final MyApplication.DataWrapper dataWrapper = ((MyApplication)activity.getApplication()).dataWrapper;

        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        query.fromLocalDatastore();
        if(dir == UP) {
            query.whereLessThan("startDate", dataWrapper.upCal.getTime());
            query.orderByDescending("startDate");
        } else if(dir == DOWN) {
            query.whereGreaterThan("startDate", dataWrapper.downCal.getTime());
            query.orderByAscending("startDate");
        }
        query.whereEqualTo("inList", true);
        query.setLimit(LOAD_ITEM_QUANTITY);
        try {
            List<ParseEvent> list = query.find();
            if(dir == UP) {
                Collections.reverse(list);
            }
            //put query result into event list
            List<List<ParseEvent>> newEventList = new ArrayList<>();
            if(list.size() == 0) {
                String appStr = "";
                if(dir == UP) {
                    dataWrapper.upFetch = false;
                    appStr = "before";
                } else if(dir == DOWN) {
                    dataWrapper.downFetch = false;
                    appStr = "later";
                }
                final String append = appStr;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "No more event " + append+ " ...", Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }
            // handle result list events;
            for (int i = 0; i < list.size(); i++) {
                ParseEvent curObj = list.get(i);
                if (newEventList.size() >0 && hashCalDay(newEventList.get(newEventList.size()-1).get(0).getStartCal()) == hashCalDay(curObj.getStartCal())) {
                    List<ParseEvent> l = newEventList.get(newEventList.size() - 1);
                    l.add(curObj);
                } else {
                    newEventList.add(new ArrayList<>(Arrays.asList(curObj)));
                }
            }

            if(dir == UP) dataWrapper.eventList.addAll(0,newEventList);
            if(dir == DOWN) dataWrapper.eventList.addAll(newEventList);

            //TODO duplicate date may split to two rows, need to be solved
            //create positionCalMap
            for (int i = 0; i < dataWrapper.eventList.size(); i++) {
                java.util.Calendar cal = dataWrapper.eventList.get(i).get(0).getStartCal();
                dataWrapper.positionCalMap.put(i, cal);
            }

            if(dir == UP) {
                dataWrapper.upCal.setTimeInMillis(dataWrapper.eventList.get(0).get(0).getStartCal().getTimeInMillis());
            } else if(dir == DOWN) {
                List<ParseEvent> lastList = dataWrapper.eventList.get(dataWrapper.eventList.size()-1);
                dataWrapper.downCal.setTimeInMillis(lastList.get(lastList.size()-1).getStartCal().getTimeInMillis());
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static boolean enableOrDisableAllEventsForSpecificCalendarInNewThread(final String calendarName, final boolean enable) {
        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        query.whereEqualTo("calendarName", calendarName);
        query.findInBackground(new FindCallback<ParseEvent>() {
            @Override
            public void done(List<ParseEvent> events, ParseException e) {
                if (e == null) {
                    for (ParseEvent event : events) {
                        if (enable) {
                            event.setConfirmed();
                        } else {
                            event.setCancelled();
                        }
                        try {
                            event.pin();
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }
                        event.saveEventually();
                    }
                } else {
                    Log.d("enabling or disabling query " + calendarName + " error: ", e.getMessage());
                }
            }
        });
        return true;
    }

    public static void fetchParseEventFromServer(long syncToken) {
        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        Date syncDate = new Date(syncToken);
        query.whereGreaterThanOrEqualTo("updatedAt", syncDate);
        try {
            List<ParseEvent> events = query.find();
            for (ParseEvent parseEvent : events) {
                    parseEvent.pin();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //fetch in same thread
    public static int fetchEventInSameThread(final Activity activity, String memberName) {
        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.whereEqualTo("memberName", memberName);
        int position = 0;
        try {
            //from server -> same thread
            if (GoogleCalendarUtils.isDeviceOnline(activity)) {
                List<Member> members = query.find();
                Member member = members.get(0);
                long syncToken = member.getSyncToken();
                ParseEventUtils.fetchParseEventFromServer(syncToken);
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                member.setSyncToken(calendar);
                member.saveEventually();
            } else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "No network connection available.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            position = firstTimeParseEventFromLocal(activity);
        }
        return position;
    }

    public static long hashCalDay(Calendar cal) {
        return (long) ((cal.get(Calendar.YEAR) - 1970) * 366 + cal.get(Calendar.MONTH) * 31 + cal.get(Calendar.DAY_OF_MONTH));
    }



}
