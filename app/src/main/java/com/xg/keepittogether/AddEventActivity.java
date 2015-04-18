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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class AddEventActivity extends Activity implements DatePickerFragment.OnDateSetListener, TimePickerFragment.OnTimeSetListener {


    private SharedPreferences userPref;
    Bundle bundle;

    int[] alertTimeList = {-1, 0, 5, 30, 60, 120, 1440};
    String[] weekdayList = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    String[] alertTimeSpinnerList = {"Off", "At time of event", "5 minutes before", "half hour before", "1 hour before", "2 hours before", "1 day before"};
    private Calendar startCal, endCal, alertCal;

    private TextView startDateView;
    private TextView startTimeView;
    private TextView endDateView;
    private TextView endTimeView;
    private EditText titleView;
    private EditText noteView;
    private Spinner alertTimeSpinner;
    private Switch notifyOtherSwitch;

    private List<List<Event>> eventList;
    private HashMap<String, Event> eventMap;

    private boolean notifyOther = false;
    private int returnDayPosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        bundle = getIntent().getExtras();

        eventList = ((MyApplication)getApplication()).eventList;
        eventMap = ((MyApplication)getApplication()).eventMap;


        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);
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


        Button deleteEventButton = (Button)findViewById(R.id.deleteEvent);
        if(bundle != null) {
            deleteEventButton.setVisibility(View.VISIBLE);
        }



        startCal = Calendar.getInstance();
        endCal = Calendar.getInstance();
        if (bundle == null) {
            endCal.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY) + 1);
        } else {
            startCal.setTimeInMillis(bundle.getLong("startDate"));
//            Log.d("start: ", "" + bundle.getLong("start"));
            endCal.setTimeInMillis(bundle.getLong("endDate"));
            titleView.setText(bundle.getString("title"));
            noteView.setText(bundle.getString("note"));


        }
        setEventDateView(startDateView, startCal);
        setEventTimeView(startTimeView, startCal);
        setEventDateView(endDateView, endCal);
        setEventTimeView(endTimeView, endCal);

        alertTimeSpinner = (Spinner)findViewById(R.id.eventAlertSpinner);
        ArrayAdapter<String> alertSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, alertTimeSpinnerList);
        alertTimeSpinner.setAdapter(alertSpinnerAdapter);

    }


    public void addEvent(View view) {
        //upload event to server

        try {
            uploadEvent();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // set alert
        alertCal = (Calendar)startCal.clone();
        int minuteOffset = alertTimeList[alertTimeSpinner.getSelectedItemPosition()];
        alertCal.set(Calendar.MINUTE, alertCal.get(Calendar.MINUTE) - minuteOffset);

        if (minuteOffset >= 0) {
            //register notification
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

        finish();
    }

    private void uploadEvent() throws JSONException {
        //send push notification

        if (notifyOther) {
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

        Event event;
        if (bundle == null) {
            //upload to server
            event = new Event();
            event.put("memberName", userPref.getString("memberName", "noValue"));
            event.put("title", titleView.getText().toString());
            event.setStartDate(startCal);
            event.setEndDate(endCal);
            event.put("note", noteView.getText().toString());
            event.setACL(new ParseACL(ParseUser.getCurrentUser()));
            event.saveEventually();


            //add local


        } else {
            //change in server

            ParseQuery<Event> query = ParseQuery.getQuery(Event.class);
            query.getInBackground(bundle.getString("objectID"), new GetCallback<Event>() {
                @Override
                public void done(Event event, ParseException e) {
                    if (e == null) {
                        event.setTitle(titleView.getText().toString());
                        event.setStartDate(startCal);
                        event.setEndDate(endCal);
                        event.setNote(noteView.getText().toString());
                        event.saveEventually();
                    } else {
                        Log.d("Query", "Error: " + e.getMessage());
                        Log.d("ObjectId: ", bundle.getString("objectID"));
                    }
                }
            });

            //change locally
            String objectID = bundle.getString("objectID");
            event = eventMap.get(objectID);
            int originalDayPosition = ((MyApplication)getApplication()).getPosition(event.getStartCal());
            List<Event> eventDayList = eventList.get(originalDayPosition);
            eventDayList.remove(event);
            if(eventDayList.size() == 0) eventList.remove(eventDayList);

            event.setTitle(titleView.getText().toString());
            event.setStartDate(startCal);
            event.setEndDate(endCal);
            event.setNote(noteView.getText().toString());

        }

        int dayPosition = ((MyApplication)getApplication()).getPosition(startCal);
        if (dayPosition >= 0) {
            List<Event> eventDayList = eventList.get(dayPosition);
            eventDayList.add(event);
            Collections.sort(eventDayList, new Comparator<Event>() {
                @Override
                public int compare(Event lhs, Event rhs) {
                    return (int)lhs.getStartCal().getTimeInMillis() - (int)rhs.getStartCal().getTimeInMillis();
                }
            });
        } else {
            eventList.add(new ArrayList<Event>(Arrays.asList(event)));
            Collections.sort(eventList, new Comparator<List<Event>>() {
                @Override
                public int compare(List<Event> lhs, List<Event> rhs) {
                    return (int)lhs.get(0).getStartCal().getTimeInMillis() - (int)rhs.get(0).getStartCal().getTimeInMillis();
                }
            });
        }

        returnDayPosition = dayPosition;

        Log.d("","");
    }

    private void setEventDateView(TextView dateView, Calendar cal) {
//        dateView.setText(weekdayList[calendar.get(Calendar.WEEK_OF_MONTH)] + ", " + monthsList[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.DAY_OF_MONTH) + ", " + calendar.get(Calendar.YEAR));
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

    public void deleteEvent(View view) {
        ParseQuery<Event> query = ParseQuery.getQuery(Event.class);
        query.getInBackground(bundle.getString("objectID"), new GetCallback<Event>() {
            @Override
            public void done(Event event, ParseException e) {
                if (e == null) {
                    event.deleteEventually();
                } else {
                    Log.d("Query", "Error: " + e.getMessage());
                    Log.d("ObjectId: ", bundle.getString("objectID"));
                }
            }
        });


        String objectID = bundle.getString("objectID");
        Event event = eventMap.get(objectID);
        int originalDayPosition = ((MyApplication)getApplication()).getPosition(event.getStartCal());
        List<Event> eventDayList = eventList.get(originalDayPosition);
        eventDayList.remove(event);
        if(eventDayList.size() == 0) eventList.remove(eventDayList);

        returnDayPosition = originalDayPosition;

        finish();
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
        //TODO add name prefix
        Intent data = new Intent();
        data.putExtra("changedPosition", returnDayPosition);
        // Activity finished ok, return the data
        setResult(RESULT_OK, data);
        super.finish();
    }
}
