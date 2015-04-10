package com.xg.keepittogether;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by wuxiaoguang on 4/8/15.
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {


    public static TimePickerFragment getInstance(long time, boolean isStart) {
        TimePickerFragment frag = new TimePickerFragment();
        Bundle args = new Bundle();
        args.putLong("time", time);
        args.putBoolean("isStart", isStart);
        frag.setArguments(args);
        return frag;
    }

    OnTimeSetListener timeSetListener;

    Calendar calendar;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        calendar = Calendar.getInstance();
        long time = getArguments().getLong("time");
        calendar.setTimeInMillis(time);

        try{
            timeSetListener = (OnTimeSetListener)getActivity();
        } catch (ClassCastException cce) {
            cce.printStackTrace();
            Log.d("Error:", "Cast failed, please implements required interface");
        }

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        timeSetListener.setTime(calendar, getArguments().getBoolean("isStart"));
    }

    interface OnTimeSetListener {
        public void setTime(Calendar cal, boolean isStart);
    }
}