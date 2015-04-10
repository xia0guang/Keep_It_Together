package com.xg.keepittogether;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Calendar;


public class AddEventActivity extends Activity implements DatePickerFragment.OnDateSetListener, TimePickerFragment.OnTimeSetListener {


    private SharedPreferences userPref;

//    String[] monthsList = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    String[] weekdayList = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
//    String[] amPmList = {"AM", "PM"};
    int[] alertTimeList = {0, 5, 30, 60, 120, 1440};
    String[] alertTimeSpinnerList = {"At time of event", "5 minutes before", "half hour before", "1 hour before", "2 hours before", "1 day before"};
    private Calendar startCal, endCal, alertCal;

    private TextView startDateView;
    private TextView startTimeView;
    private TextView endDateView;
    private TextView endTimeView;
    private Spinner alertTimeSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);
        startDateView = (TextView)findViewById(R.id.eventStartDate);
        startTimeView = (TextView)findViewById(R.id.eventStartTime);
        endDateView = (TextView)findViewById(R.id.eventEndDate);
        endTimeView = (TextView)findViewById(R.id.eventEndTime);

        startCal = Calendar.getInstance();
        setEventDate(startDateView, startCal);
        setEventTime(startTimeView, startCal);
        endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY)+1);
        setEventDate(endDateView, endCal);
        setEventTime(endTimeView, endCal);

        alertTimeSpinner = (Spinner)findViewById(R.id.eventAlertSpinner);
        ArrayAdapter<String> alertSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, alertTimeSpinnerList);
        alertTimeSpinner.setAdapter(alertSpinnerAdapter);

    }


    public void addEvent(View view) {
        //upload event to server
        alertCal = (Calendar)startCal.clone();
        int minuteOffset = alertTimeList[alertTimeSpinner.getSelectedItemPosition()];
        alertCal.set(Calendar.MINUTE, alertCal.get(Calendar.MINUTE) - minuteOffset);
//        Toast.makeText(this, weekdayList[alertCal.get(Calendar.WEEK_OF_MONTH)] + ", " + monthsList[alertCal.get(Calendar.MONTH)] + " " + alertCal.get(Calendar.DAY_OF_MONTH) + ", " + alertCal.get(Calendar.YEAR), Toast.LENGTH_LONG).show();
//        Toast.makeText(this, alertCal.get(Calendar.HOUR) + ":" + alertCal.get(Calendar.MINUTE) + " " + amPmList[alertCal.get(Calendar.AM_PM)], Toast.LENGTH_LONG).show();


        EditText titleView = (EditText)findViewById(R.id.eventTitleET);
        ParseObject event = new ParseObject("Event");
        event.put("memberName", userPref.getString("memberName", "noValue"));
        event.put("title", titleView.getText().toString());
        event.put("startDate", startCal.getTime());
        event.put("endDate", endCal.getTime());
        EditText noteView = (EditText)findViewById(R.id.eventNoteET);
        event.put("note", noteView.getText().toString());
        event.setACL(new ParseACL(ParseUser.getCurrentUser()));
        event.saveInBackground();

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



        finish();
    }

    private void setEventDate(TextView dateView, Calendar cal) {
//        dateView.setText(weekdayList[calendar.get(Calendar.WEEK_OF_MONTH)] + ", " + monthsList[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.DAY_OF_MONTH) + ", " + calendar.get(Calendar.YEAR));
        dateView.setText(weekdayList[cal.get(Calendar.WEEK_OF_MONTH)] + String.format(", %tB %te, %tY", cal, cal, cal));
    }

    private void setEventTime(TextView timeView, Calendar cal) {
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
            setEventDate(startDateView, startCal);

            if(startCal.compareTo(endCal) > 0) {
                endCal = (Calendar)startCal.clone();
                endCal.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY) + 1);
                setEventDate(endDateView, endCal);
                setEventTime(endTimeView, endCal);
            }
        } else {
            endCal = cal;
            setEventDate(endDateView, endCal);
        }
    }


    @Override
    public void setTime(Calendar cal, boolean isStart) {
        if(isStart) {
            startCal = cal;
            setEventTime(startTimeView, startCal);
            if(startCal.compareTo(endCal) > 0) {
                endCal = (Calendar)startCal.clone();
                endCal.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY)+1);
                setEventDate(endDateView, endCal);
                setEventTime(endTimeView, endCal);
            }
        } else {
            endCal = cal;
            setEventTime(endTimeView, endCal);
        }
    }
}
