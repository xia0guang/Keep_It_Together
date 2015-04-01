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
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, memberNameList);
                    Spinner spinner = (Spinner) getActivity().findViewById(R.id.memberNameSpinner);
                    spinner.setAdapter(arrayAdapter);
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText newMemberView = (EditText)getActivity().findViewById(R.id.createMemberNameET);
                SharedPreferences userPref = getActivity().getSharedPreferences("User_Preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = userPref.edit();
                if(newMemberView.getText().toString().trim().length() == 0) {
                    Spinner memberSpinner = (Spinner)getActivity().findViewById(R.id.memberNameSpinner);
                    editor.putString("memberName", memberSpinner.getSelectedItem().toString());
                    editor.commit();
                } else {
                    //Store member name in Members Table
                    ParseObject privateNote = new ParseObject("Members");
                    EditText memberNameView = (EditText)getActivity().findViewById(R.id.createMemberNameET);
                    privateNote.put("memberName", memberNameView.getText().toString());
                    privateNote.setACL(new ParseACL(ParseUser.getCurrentUser()));
                    privateNote.saveInBackground();
                    //Set member name in preferences file
                    editor.putString("memberName", memberNameView.getText().toString());
                    editor.commit();
                }

                Intent intent = new Intent(getActivity(), DispatchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }
}
