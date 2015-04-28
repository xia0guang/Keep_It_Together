package com.xg.keepittogether;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clive on 2/12/14.
 */
public class MultiChoiceListDialogFragment extends DialogFragment {

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try{
            mDialogListener = (MultiChoiceListDialogListener)activity;
        } catch (ClassCastException cce) {
            throw new ClassCastException(activity.toString()
                    + " must implement MultiChoiceListDialogListener");
        }
    }

    public interface MultiChoiceListDialogListener {
        public void onOkClicked();
    }

    MultiChoiceListDialogListener mDialogListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //saves list of selected items indexes
        final List<String> calendarList = ((SettingActivity)getActivity()).googleCalendarNameList;
        final List<String> calendarIdList = ((SettingActivity)getActivity()).googleCalendarIdList;

        final boolean[] isSelectedArray = new boolean[calendarList.size()];
        final String[] calendarArray = calendarList.toArray(new String[calendarList.size()]);
        final List<String> selectedCalendars = new ArrayList<>();
        final List<String> selectedCalendarIds = new ArrayList<>();

        final SharedPreferences calendarPref = getActivity().getSharedPreferences("Google_Calendar_List", Context.MODE_PRIVATE);
        final int listSize = calendarPref.getInt("listSize", 0);
        for (int i = 0; i < listSize; i++) {
            String calendar = calendarPref.getString("calendar_" + i, null);
            if(calendarList.contains(calendar)) {
                isSelectedArray[calendarList.indexOf(calendar)] = true;
                selectedCalendars.add(calendar);
            }

            String calendarId = calendarPref.getString("calendar_Id_" + i, null);
            if(calendarIdList.contains(calendarId)) {
                selectedCalendarIds.add(calendarId);
            }

        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        builder.setTitle(R.string.select_calendar_list_dialog_title)
                // Specify the list array
                .setMultiChoiceItems(calendarArray, isSelectedArray,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if(isChecked && !selectedCalendars.contains(calendarArray[which])) {
                                    selectedCalendars.add(calendarArray[which]);
                                    selectedCalendarIds.add(calendarIdList.get(which));
                                    return;
                                }
                                if(!isChecked && selectedCalendars.contains(calendarArray[which])) {
                                    selectedCalendars.remove(calendarArray[which]);
                                    selectedCalendarIds.remove(calendarIdList.get(which));
                                }
                            }
                        })

                        // Set the action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences.Editor editor = calendarPref.edit();
                        editor.putInt("listSize", selectedCalendars.size());
                        for (int i = 0; i < selectedCalendars.size(); i++) {
                            editor.putString("calendar_" + i, selectedCalendars.get(i));
                            editor.putString("calendar_Id_" + i, selectedCalendarIds.get(i));
                        }
                        editor.apply();
                        mDialogListener.onOkClicked();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getActivity(), "Action has been cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        return builder.create();
    }
}
