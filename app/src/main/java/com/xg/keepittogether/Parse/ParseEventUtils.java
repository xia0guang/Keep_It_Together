package com.xg.keepittogether.Parse;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.calendar.model.Event;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.xg.keepittogether.EventColor;
import com.xg.keepittogether.EventIndicateDecorator;
import com.xg.keepittogether.MainActivity;
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

    public static void firstTimeParseEventFromLocal(Activity activity) {
        java.util.Calendar startCal = java.util.Calendar.getInstance();
        startCal.add(Calendar.DAY_OF_MONTH, -1);
        java.util.Calendar endCal = java.util.Calendar.getInstance();
        endCal.add(java.util.Calendar.MONTH, +6);

        MyApplication.DataWrapper dataWrapper = ((MyApplication)activity.getApplication()).dataWrapper;
        SharedPreferences googlePref = activity.getSharedPreferences("Google_Calendar_List", Context.MODE_PRIVATE);


        dataWrapper.clear();

        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        query.fromLocalDatastore();
        Date startDate = startCal.getTime();
        query.whereGreaterThanOrEqualTo("startDate", startDate);
        query.whereLessThanOrEqualTo("startDate", endCal.getTime());
        query.whereNotEqualTo("inList", false);
        query.orderByAscending("startDate");

        try {
            List<ParseEvent> list = query.find();
            //put query result into event list
            if(list.size() == 0) return;
            for (int i = 0; i < list.size(); i++) {
                ParseEvent curObj = list.get(i);


                if (dataWrapper.eventList.size() >0 && hashCalDay(dataWrapper.eventList.get(dataWrapper.eventList.size()-1).get(0).getStartCal()) == hashCalDay(curObj.getStartCal())) {
                    List<ParseEvent> l = dataWrapper.eventList.get(dataWrapper.eventList.size() - 1);
                    l.add(curObj);
                } else {
                    dataWrapper.eventList.add(new ArrayList<>(Arrays.asList(curObj)));
                }
            }

            //create positionMap and reversePositionMap
            for (int i = 0; i < dataWrapper.eventList.size(); i++) {
                java.util.Calendar cal = dataWrapper.eventList.get(i).get(0).getStartCal();
                long day = hashCalDay(cal);
//                dataWrapper.positionMap.put(day, i);
                dataWrapper.reversePositionMap.put(i, cal);
            }

            ((MainActivity)activity).mAdapter.notifyDataSetChanged();

            //initialize fetch status;
            dataWrapper.upCal.setTimeInMillis(dataWrapper.eventList.get(0).get(0).getStartCal().getTimeInMillis());
            int fetchPosition = dataWrapper.eventList.size() >= 20? 4:0;
            dataWrapper.upThresholdCal.setTimeInMillis(dataWrapper.eventList.get(fetchPosition).get(0).getStartCal().getTimeInMillis());

            List<ParseEvent> lastList = dataWrapper.eventList.get(dataWrapper.eventList.size()-1);
            dataWrapper.downCal.setTimeInMillis(lastList.get(lastList.size()-1).getStartCal().getTimeInMillis());
            fetchPosition = dataWrapper.eventList.size() >= 20? dataWrapper.eventList.size()-5:dataWrapper.eventList.size()-1;
            List<ParseEvent> thresholdList = dataWrapper.eventList.get(fetchPosition);
            dataWrapper.downThresholdCal.setTimeInMillis(thresholdList.get(thresholdList.size()-1).getStartCal().getTimeInMillis());
            dataWrapper.upFetch = true;
            dataWrapper.downFetch = true;

            buildDecorators(activity);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void updateParseEventFromLocal(final Activity activity, int dir) {

        final MyApplication.DataWrapper dataWrapper = ((MyApplication)activity.getApplication()).dataWrapper;

        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        query.fromLocalDatastore();
        query.orderByAscending("startDate");
        if(dir == UP) {
            query.whereLessThan("startDate", dataWrapper.upCal.getTime());
        } else if(dir == DOWN) {
            query.whereGreaterThan("startDate", dataWrapper.downCal.getTime());
        }
        query.whereEqualTo("inList", true);
//        query.setLimit(1);// TODO change limit
        try {
            List<ParseEvent> list = query.find();
            //put query result into event list
            List<List<ParseEvent>> newEventList = new ArrayList<>();
            if(list.size() == 0) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "No more event...", Toast.LENGTH_LONG).show();
                    }
                });
                if(dir == UP) {
                    dataWrapper.upFetch = false;
                } else if(dir == DOWN) {
                    dataWrapper.downFetch = false;
                }
                return;
            }
            // process result list events;
            for (int i = 0; i < list.size(); i++) {
                ParseEvent curObj = list.get(i);

                if (newEventList.size() >0 && hashCalDay(newEventList.get(newEventList.size()-1).get(0).getStartCal()) == hashCalDay(curObj.getStartCal())) {
                    List<ParseEvent> l = newEventList.get(newEventList.size() - 1);
                    l.add(curObj);
                } else {
                    newEventList.add(new ArrayList<>(Arrays.asList(curObj)));
                }
            }

            dataWrapper.eventList.addAll(newEventList);
            Collections.sort(dataWrapper.eventList, new Comparator<List<ParseEvent>>() {
                @Override
                public int compare(List<ParseEvent> lhs, List<ParseEvent> rhs) {
                    return lhs.get(0).getStartCal().compareTo(rhs.get(0).getStartCal());
                }
            });

            //TODO duplicate date may split to two rows, need to be solved
            //create positionMap and reversePositionMap
            for (int i = 0; i < dataWrapper.eventList.size(); i++) {
                java.util.Calendar cal = dataWrapper.eventList.get(i).get(0).getStartCal();
                long day = hashCalDay(cal);
//                dataWrapper.positionMap.put(day, i);
                dataWrapper.reversePositionMap.put(i, cal);
            }

            if(dir == UP) {
                dataWrapper.upCal.setTimeInMillis(dataWrapper.eventList.get(0).get(0).getStartCal().getTimeInMillis());
                int fetchPosition = dataWrapper.eventList.size() >= 20? 4:0;
                dataWrapper.upThresholdCal.setTimeInMillis(dataWrapper.eventList.get(fetchPosition).get(0).getStartCal().getTimeInMillis());
            } else if(dir == DOWN) {
                List<ParseEvent> lastList = dataWrapper.eventList.get(dataWrapper.eventList.size()-1);
                dataWrapper.downCal.setTimeInMillis(lastList.get(lastList.size()-1).getStartCal().getTimeInMillis());
                int fetchPosition = dataWrapper.eventList.size() >= 20? dataWrapper.eventList.size()-5:dataWrapper.eventList.size()-1;
                List<ParseEvent> thresholdList = dataWrapper.eventList.get(fetchPosition);
                dataWrapper.downThresholdCal.setTimeInMillis(thresholdList.get(thresholdList.size()-1).getStartCal().getTimeInMillis());
            }

            buildDecorators(activity);
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
        query.whereGreaterThanOrEqualTo("updateAt", syncDate);
        try {
            List<ParseEvent> events = query.find();
            for (ParseEvent parseEvent : events) {
                    parseEvent.pin();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //fetch in new thread
    public static void fetchEventInNewThread(final Activity activity, String memberName) {
        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.whereEqualTo("memberName", memberName);
        query.findInBackground(new FindCallback<Member>() {
            @Override
            public void done(List<Member> members, ParseException e) {
                if (e == null) {
                    Member member = members.get(0);
                    long syncToken = member.getSyncToken();
                    ParseEventUtils.fetchParseEventFromServer(syncToken);
                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    member.setSyncToken(calendar);
                    member.saveEventually();

                    //from local
                    firstTimeParseEventFromLocal(activity);
                    Toast.makeText(activity, "query done", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("fetch error: ", e.getMessage());
                }
            }
        });
    }

    //fetch in same thread
    public static void fetchEventInSameThread(Activity activity, String memberName) {
        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.whereEqualTo("memberName", memberName);
        try {
            List<Member> members = query.find();
            Member member = members.get(0);
            //from server -> same thread
            long syncToken = member.getSyncToken();
            ParseEventUtils.fetchParseEventFromServer(syncToken);
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            member.setSyncToken(calendar);
            member.saveEventually();

            //from local -> new thread
            firstTimeParseEventFromLocal(activity);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static long hashCalDay(Calendar cal) {
        return (long) ((cal.get(Calendar.YEAR) - 1970) * 366 + cal.get(Calendar.MONTH) * 31 + cal.get(Calendar.DAY_OF_MONTH));
    }

    public static void buildDecorators(final Activity activity) {
        final MyApplication.DataWrapper dataWrapper = ((MyApplication)activity.getApplication()).dataWrapper;
        SharedPreferences userPref = activity.getSharedPreferences("User_Preferences", Context.MODE_PRIVATE);
        dataWrapper.decorators.clear();
        for(int i=0; i<dataWrapper.eventList.size(); i++) {
            List<ParseEvent> list = dataWrapper.eventList.get(i);
            Calendar cal = list.get(0).getStartCal();
            CalendarDay day = new CalendarDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            EventIndicateDecorator decorator = new EventIndicateDecorator("●", day);
            for (int j = 0; j <list.size(); j++) {
                ParseEvent event = list.get(j);
                int color = EventColor.getColor(userPref.getInt("color." + event.getMemberName(), 1));
                decorator.addColor(color);
            }
            dataWrapper.decorators.add(decorator);
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity)activity).calendarView.addDecorators(dataWrapper.decorators);
                ((MainActivity)activity).calendarView.invalidateDecorators();
            }
        });
    }

}
