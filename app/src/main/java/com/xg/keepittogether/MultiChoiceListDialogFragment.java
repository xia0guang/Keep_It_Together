package com.xg.keepittogether;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Created by clive on 2/12/14.
 */
public class MultiChoiceListDialogFragment extends DialogFragment {

    /*array list to save the indexes of the selected array items*/
    private ArrayList<Integer> selectedItemsIndexList;

    /*the interface to communicate with the host activity*/
    public interface multiChoiceListDialogListener {
        public void onOkay(ArrayList<Integer> arrayList);

        public void onCancel();
    }

    multiChoiceListDialogListener dialogListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // ensure that the host activity implements the callback interface
        try {
            // Instantiate the dialogListener so we can send events to the host
            dialogListener = (multiChoiceListDialogListener) activity;
        } catch (ClassCastException e) {
            // if activity doesn't implement the interface, throw an exception
            throw new ClassCastException(activity.toString() + " must implement multiChoiceListDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //saves list of selected items indexes
        selectedItemsIndexList = new ArrayList();
        final boolean[] isSelectedArray = new boolean[selectedItemsIndexList.size()];
        final CharSequence[] calendarList = ((SettingActivity)getActivity()).googleCalendarList;

        final SharedPreferences calendarPref = getActivity().getSharedPreferences("Google_Calendar_List", Context.MODE_PRIVATE);
        final int listSize = calendarPref.getInt("listSize", 0);
        for (int i = 0; i < listSize; i++) {
            isSelectedArray[i] = calendarPref.getBoolean(calendarList.toString(), false);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        builder.setTitle(R.string.select_calendar_list_dialog_title)
                // Specify the list array
                .setMultiChoiceItems(calendarList, isSelectedArray,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item,
                                    // add it to the selected items list
//                                    selectedItemsIndexList.add(which);

                                } else if (selectedItemsIndexList.contains(which)) {
                                    // Else, if the item is already in the list, remove it
//                                    selectedItemsIndexList.remove(which);
                                }
                            }
                        })

                        // Set the action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so pass the selectedItemsIndexList
                        // results to the host activity
//                        dialogListener.onOkay(selectedItemsIndexList);
                        SharedPreferences.Editor editor = calendarPref.edit();
                        editor.putInt("listSize", calendarList.length);
                        for (int i = 0; i < calendarList.length; i++) {
                            editor.putBoolean(calendarList.toString(), isSelectedArray[i]);
                        }
                        editor.apply();
                    }
                })

                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialogListener.onCancel();
                    }
                });

        return builder.create();
    }
}
