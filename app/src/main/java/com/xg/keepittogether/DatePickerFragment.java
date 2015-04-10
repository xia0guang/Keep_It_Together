package com.xg.keepittogether;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Created by wuxiaoguang on 4/8/15.
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener{

    public static DatePickerFragment getInstance(long time, boolean isStart) {
        DatePickerFragment frag = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putLong("time", time);
        args.putBoolean("isStart", isStart);
        frag.setArguments(args);
        return frag;
    }

    OnDateSetListener dateSetListener;
    Calendar calendar;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        calendar = Calendar.getInstance();
        long time = getArguments().getLong("time");
        calendar.setTimeInMillis(time);

        try{
            dateSetListener = (OnDateSetListener)getActivity();
        } catch (ClassCastException cce) {
            cce.printStackTrace();
            Log.d("Error:", "Cast failed, please implements required interface");
        }

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH) );
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        dateSetListener.setDate(calendar, getArguments().getBoolean("isStart"));
    }

    interface OnDateSetListener {
        public void setDate(Calendar cal, boolean isStart);
    }

}