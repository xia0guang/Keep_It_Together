package com.xg.keepittogether;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.xg.keepittogether.Parse.ParseEvent;
import com.xg.keepittogether.Parse.ParseEventUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class AddEventActivity extends Activity implements DatePickerFragment.OnDateSetListener, TimePickerFragment.OnTimeSetListener {


    private SharedPreferences userPref;
    private SharedPreferences googlePref;
    Bundle bundle;

    int[] alertTimeList = {-1, 0, 5, 30, 60, 120, 1440};
    private Calendar startCal, endCal, alertCal;

    private TextView startDateView;
    private TextView startTimeView;
    private TextView endDateView;
    private TextView endTimeView;
    private EditText titleView;
    private EditText noteView;
    private Spinner alertTimeSpinner;
    private Switch notifyOtherSwitch;
    private Switch uploadToGoogleCalendarSwitch;

    private MyApplication.DataWrapper dataWrapper;

    private boolean notifyOther = false;
    private boolean uploadToGoogleCalendar = false;
    private boolean changedStartDate = false;
    private int returnDayPosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        bundle = getIntent().getExtras();

        dataWrapper = ((MyApplication)getApplication()).dataWrapper;

        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);
        googlePref = getSharedPreferences("Google_Calendar_List", MODE_PRIVATE);
        startDateView = (TextView)findViewById(R.id.eventStartDate);
        startTimeView = (TextView)findViewById(R.id.eventStartTime);
        endDateView = (TextView)findViewById(R.id.eventEndDate);
        endTimeView = (TextView)findViewById(R.id.eventEndTime);
        titleView = (EditText)findViewById(R.id.eventTitleET);
        noteView = (EditText)findViewById(R.id.eventNoteET);
        notifyOtherSwitch = (Switch)findViewById(R.id.eventNotifyOtherSwitch);
        notifyOtherSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyOther = !notifyOther;
            }
        });
        uploadToGoogleCalendarSwitch = (Switch)findViewById(R.id.uploadToGoogleSwitch);
        uploadToGoogleCalendarSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadToGoogleCalendar = !uploadToGoogleCalendar;
            }
        });


        LinearLayout addLayout = (LinearLayout)findViewById(R.id.add_event_BT_layout);
        LinearLayout saveLayout = (LinearLayout)findViewById(R.id.save_event_BT_layout);

        startCal = Calendar.getInstance();
        endCal = Calendar.getInstance();
        if (bundle == null) {
            endCal.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY) + 1);

            addLayout.setVisibility(View.VISIBLE);
            saveLayout.setVisibility(View.GONE);
        } else {
            startCal.setTimeInMillis(bundle.getLong("startDate"));
            endCal.setTimeInMillis(bundle.getLong("endDate"));
            titleView.setText(bundle.getString("title"));
            noteView.setText(bundle.getString("note"));

            addLayout.setVisibility(View.GONE);
            saveLayout.setVisibility(View.VISIBLE);
        }
        setEventDateView(startDateView, startCal);
        setEventTimeView(startTimeView, startCal);
        setEventDateView(endDateView, endCal);
        setEventTimeView(endTimeView, endCal);

        alertTimeSpinner = (Spinner)findViewById(R.id.eventAlertSpinner);
        String[] alertTimeSpinnerList = getResources().getStringArray(R.array.alert_time_spinner_list);
        ArrayAdapter<String> alertSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, alertTimeSpinnerList);
        alertTimeSpinner.setAdapter(alertSpinnerAdapter);
    }


    public void addEvent(View view) {
        ParseEvent parseEvent = new ParseEvent();
        parseEvent.setMemberName(userPref.getString("memberName", null));
        parseEvent.setTitle(titleView.getText().toString());
        parseEvent.setStartDate(startCal);
        parseEvent.setEndDate(endCal);
        parseEvent.setNote(noteView.getText().toString());
        parseEvent.setACL(new ParseACL(ParseUser.getCurrentUser()));
        parseEvent.setConfirmed();
        parseEvent.saveEventually();
        parseEvent.pinInBackground();

        if (notifyOther) {
            try {
                notifyOther();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(uploadToGoogleCalendar && bundle == null) {
            try {
                GoogleCalendarUtils.insertSingleEventInNewThread(this, "primary", parseEvent);
                parseEvent.setCancelled();
                parseEvent.saveEventually();
                parseEvent.pinInBackground();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // set alert
        int minuteOffset = alertTimeList[alertTimeSpinner.getSelectedItemPosition()];
        if (minuteOffset >= 0) {
            setAlert(minuteOffset);
        }
        if(startCal.compareTo(dataWrapper.eventList.get(0).get(0).getStartCal()) >= 0
                && ParseEventUtils.hashCalDay(startCal) <= ParseEventUtils.hashCalDay(dataWrapper.eventList.get(dataWrapper.eventList.size()-1).get(0).getStartCal()))
        {
            updateEventList(parseEvent);
        } else {
            if(startCal.compareTo(dataWrapper.eventList.get(0).get(0).getStartCal()) < 0) {
                dataWrapper.upFetch = true;
            }
            if(ParseEventUtils.hashCalDay(startCal) > ParseEventUtils.hashCalDay(dataWrapper.eventList.get(dataWrapper.eventList.size()-1).get(0).getStartCal())) {
                dataWrapper.downFetch = true;
            }
        }

        finish();
    }

    public void saveEvent(View view) {
        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        query.fromLocalDatastore();
        ParseEvent event;
        try {
            event = query.get(bundle.getString("objectID"));
            if(!startCal.equals(event.getStartCal())) {
                int originalDayPosition = bundle.getInt("listPosition");
                List<ParseEvent> parseEventDayList = dataWrapper.eventList.get(originalDayPosition);
                parseEventDayList.remove(event);
                if(parseEventDayList.size() == 0) {
                    dataWrapper.eventList.remove(parseEventDayList);
                }
            }
            event.setTitle(titleView.getText().toString());
            if(!startCal.equals(event.getStartCal())) {
                changedStartDate = true;
            }
            event.setStartDate(startCal);
            event.setEndDate(endCal);
            event.setNote(noteView.getText().toString());
            event.saveEventually();
            event.pinInBackground();

            if(changedStartDate) {
                if(startCal.compareTo(dataWrapper.eventList.get(0).get(0).getStartCal()) >= 0
                        && ParseEventUtils.hashCalDay(startCal) <= ParseEventUtils.hashCalDay(dataWrapper.eventList.get(dataWrapper.eventList.size()-1).get(0).getStartCal()))
                {
                    updateEventList(event);
                } else {
                    if(startCal.compareTo(dataWrapper.eventList.get(0).get(0).getStartCal()) < 0) {
                        dataWrapper.upFetch = true;
                    }
                    if(ParseEventUtils.hashCalDay(startCal) > ParseEventUtils.hashCalDay(dataWrapper.eventList.get(dataWrapper.eventList.size()-1).get(0).getStartCal())) {
                        dataWrapper.downFetch = true;
                    }
                }
            }

            if("Google_Calendar".equals(bundle.getString("from"))) {
                String calendarId = getCalendarId(bundle.getString("calendarName"));
                String eventId = bundle.getString("eventId");
                try {
                    GoogleCalendarUtils.uploadSingleEventInNewThread(this, calendarId, eventId, event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (ParseException pe) {
            Log.d("Query", "Error: " + pe.getMessage());
            Log.d("ObjectId: ", bundle.getString("objectID"));
        }

        if (notifyOther) {
            try {
                notifyOther();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // set alert
        int minuteOffset = alertTimeList[alertTimeSpinner.getSelectedItemPosition()];
        if (minuteOffset >= 0) {
            setAlert(minuteOffset);
        }
        finish();
    }

    public void deleteEvent(View view) {
        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
        query.fromLocalDatastore();
        ParseEvent event;
        try {
            event = query.get(bundle.getString("objectID"));
            event.setCancelled();
            event.saveEventually();
            event.pinInBackground();

            int originalDayPosition = bundle.getInt("listPosition");
            List<ParseEvent> parseEventDayList = dataWrapper.eventList.get(originalDayPosition);
            parseEventDayList.remove(event);
            if(parseEventDayList.size() == 0) {
                dataWrapper.eventList.remove(parseEventDayList);
            }
            returnDayPosition = originalDayPosition;
        } catch (ParseException pe) {
            Log.d("Query", "Error: " + pe.getMessage());
            Log.d("ObjectId: ", bundle.getString("objectID"));
            finish();
        }

        if("Google_Calendar".equals(bundle.getString("from"))) {
            String calendarId = getCalendarId(bundle.getString("calendarName"));
            String eventId = bundle.getString("eventId");
            try {
                GoogleCalendarUtils.deleteSingleEventInNewThread(this, calendarId, eventId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        finish();
    }

    //send push notification
    private void notifyOther() throws JSONException{
        ParseQuery pushQuery = ParseInstallation.getQuery();
        pushQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        String memberName = userPref.getString("memberName", "noValue");
        pushQuery.whereNotEqualTo("memberName", memberName);
        Log.d("memberName", memberName);
        ParsePush push = new ParsePush();
        push.setChannel("EventUpdate");
        JSONObject data = new JSONObject("{\"name\": \"Vaughn\",\"newsItem\": \"Man bites dog\"}");
        push.setData(data);
        push.setQuery(pushQuery); // Set our Installation query
        if (bundle == null) {
            push.setMessage(memberName + "just added a new Event:" + titleView.getText().toString());
        } else {
            push.setMessage(memberName + "just changed a new Event:" + titleView.getText().toString());
        }
        push.sendInBackground();
    }

    private void setAlert(int minuteOffset) {
        alertCal = (Calendar)startCal.clone();
        alertCal.set(Calendar.MINUTE, alertCal.get(Calendar.MINUTE) - minuteOffset);
        Intent myIntent = new Intent(this, AlertBroadcastReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString("title", titleView.getText().toString());
        //add content to notification
        String contentPrefix = "";
        String content;
        Calendar midnightCal = (Calendar)alertCal.clone();
        midnightCal.set(Calendar.HOUR_OF_DAY, 23);
        midnightCal.set(Calendar.MINUTE, 59);
        midnightCal.set(Calendar.SECOND, 59);
        if(startCal.compareTo(midnightCal) > 0) {
            contentPrefix = "Tomorrow, ";
        }
        content = contentPrefix + String.format("%tl:%tM %tp - %tl:%tM %tp",startCal, startCal, startCal, endCal, endCal, endCal);
        bundle.putString("content", content);
        myIntent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, 0);
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, alertCal.getTimeInMillis(), pendingIntent);
    }

    private void updateEventList(ParseEvent event) {
        for (int i = 0; i < dataWrapper.eventList.size(); i++) {
            List<ParseEvent> curList = dataWrapper.eventList.get(i);
            ParseEvent curEvent = curList.get(0);
            Calendar curCal = curEvent.getStartCal();
            if(ParseEventUtils.hashCalDay(startCal) == ParseEventUtils.hashCalDay(curCal))  {
                curList.add(event);
                Collections.sort(curList, new Comparator<ParseEvent>() {
                    @Override
                    public int compare(ParseEvent lhs, ParseEvent rhs) {
                        return lhs.getEndCal().compareTo(rhs.getEndCal());
                    }
                });
                break;
            } else if(ParseEventUtils.hashCalDay(startCal) < ParseEventUtils.hashCalDay(curCal)) {
                List<ParseEvent> insertList = new ArrayList<>(Arrays.asList(event));
                dataWrapper.eventList.add(i, insertList);
                break;
            }
        }
    }

    private String getCalendarId(String calendarName) {
        if(calendarName == null) return null;
        String cid = "";
        int listSize = googlePref.getInt("listSize", 0);
        for (int i = 0; i < listSize; i++) {
            if(calendarName.equals(googlePref.getString("calendar_" + i, null))) {
                cid = googlePref.getString("calendar_Id_" + i, null);
            }
        }
        return cid;
    }

    private void setEventDateView(TextView dateView, Calendar cal) {
        String[] weekdayList = getResources().getStringArray(R.array.weekday_list);
        dateView.setText(weekdayList[cal.get(Calendar.WEEK_OF_MONTH)] + String.format(", %tB %te, %tY", cal, cal, cal));
    }

    private void setEventTimeView(TextView timeView, Calendar cal) {
//        timeView.setText(String.format("%d:%02d ", calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE)) + amPmList[calendar.get(Calendar.AM_PM)]);
        timeView.setText(String.format("%tl:%tM %tp", cal, cal, cal));
    }

    public void setEventStartDate(View view) {
        DialogFragment frag = DatePickerFragment.getInstance(startCal.getTimeInMillis(), true);
        frag.show(getFragmentManager(), "startDateFrag");
    }

    public void setEventStartTime(View view) {
        DialogFragment frag = TimePickerFragment.getInstance(startCal.getTimeInMillis(), true);
        frag.show(getFragmentManager(), "startTimeFrag");
    }

    public void setEventEndDate(View view) {
        DialogFragment frag = DatePickerFragment.getInstance(endCal.getTimeInMillis(), false);
        frag.show(getFragmentManager(), "endDateFrag");
    }

    public void setEventEndTime(View view) {
        DialogFragment frag = TimePickerFragment.getInstance(endCal.getTimeInMillis(), false);
        frag.show(getFragmentManager(), "endTimeFrag");
    }

    @Override
    public void setDate(Calendar cal, boolean isStart) {
        if(isStart) {
            startCal = cal;
            setEventDateView(startDateView, startCal);

            endCal = (Calendar)startCal.clone();
            endCal.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY) + 1);
            setEventDateView(endDateView, endCal);
            setEventTimeView(endTimeView, endCal);

        } else {
            endCal = cal;
            setEventDateView(endDateView, endCal);
        }
    }


    @Override
    public void setTime(Calendar cal, boolean isStart) {
        if(isStart) {
            startCal = cal;
            setEventTimeView(startTimeView, startCal);

            endCal = (Calendar)startCal.clone();
            endCal.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY)+1);
            setEventDateView(endDateView, endCal);
            setEventTimeView(endTimeView, endCal);

        } else {
            endCal = cal;
            setEventTimeView(endTimeView, endCal);
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("changedPosition", returnDayPosition);
        // Activity finished ok, return the data
        setResult(RESULT_OK, data);
        super.finish();
    }
}
