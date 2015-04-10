package com.xg.keepittogether;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class SelectMemberNameFragment extends Fragment {


    public SelectMemberNameFragment() {
        // Required empty public constructor
    }

    Button saveButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View inflatedView = inflater.inflate(R.layout.fragment_select_member_name, container, false);
        saveButton = (Button)inflatedView.findViewById(R.id.saveMemberNameBT);

        return inflatedView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Members");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                    List<String> memberNameList = new ArrayList<String>();
                    for(ParseObject po : list) {
                        memberNameList.add(po.getString("memberName"));
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_dropdown_item, memberNameList);
                    Spinner spinner = (Spinner) getActivity().findViewById(R.id.memberNameSpinner);
                    spinner.setAdapter(arrayAdapter);
                } else {
                    Log.d("memberName", "Error: " + e.getMessage());
                }
            }
        });


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText newMemberView = (EditText)getActivity().findViewById(R.id.createMemberNameET);
                SharedPreferences userPref = getActivity().getSharedPreferences("User_Preferences", Context.MODE_PRIVATE);
                final SharedPreferences.Editor editor = userPref.edit();
                if(newMemberView.getText().toString().trim().length() == 0) {
                    Spinner memberSpinner = (Spinner)getActivity().findViewById(R.id.memberNameSpinner);
                    String memberName = memberSpinner.getSelectedItem().toString();
                    editor.putString("memberName", memberName);

                    //query color to store in pref
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Members");
                    query.whereEqualTo("memberName", memberName);
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> parseObjects, ParseException e) {
                            editor.putInt("color", parseObjects.get(0).getInt("color"));
                        }
                    });

                    editor.commit();
                } else {
                    //Store member name in Members Table
                    ParseObject member = new ParseObject("Members");
                    EditText memberNameView = (EditText)getActivity().findViewById(R.id.createMemberNameET);
                    member.put("memberName", memberNameView.getText().toString());
                    member.put("color", EventColor.BLUE);
                    member.setACL(new ParseACL(ParseUser.getCurrentUser()));
                    member.saveInBackground();

                    //Set member name in preferences file
                    editor.putString("memberName", memberNameView.getText().toString());
                    editor.putInt("color", EventColor.BLUE);
                    editor.commit();
                }

                Intent intent = new Intent(getActivity(), DispatchActivity.class);
                startActivity(intent);
            }
        });
    }
}