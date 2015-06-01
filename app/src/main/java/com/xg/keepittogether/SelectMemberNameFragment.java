package com.xg.keepittogether;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.xg.keepittogether.Parse.Member;

import java.util.List;


public class SelectMemberNameFragment extends Fragment {


    public SelectMemberNameFragment() {
        // Required empty public constructor
    }

    Button createButton;
    ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView = inflater.inflate(R.layout.fragment_select_member_name, container, false);
        mListView = (ListView)inflatedView.findViewById(R.id.memberNameListView);

        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.findInBackground(new FindCallback<Member>() {
            public void done(final List<Member> members, ParseException e) {
                if (e == null) {
                    ArrayAdapter<Member> arrayAdapter = new ArrayAdapter<Member>(getActivity(),R.layout.member_list_row) {
                        @Override
                        public int getCount() {
                            return members.size();
                        }

                        @Override
                        public Member getItem(int position) {
                            return members.get(position);
                        }

                        @Override
                        public int getPosition(Member item) {
                            return members.indexOf(item);
                        }

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            LayoutInflater mInflater = getActivity().getLayoutInflater();
                            View row = mInflater.inflate(R.layout.member_list_row, parent, false);
                            TextView memberName = (TextView) row.findViewById(R.id.memberNameInMemberList);
                            memberName.setText(getItem(position).getMemberName());
                            ImageView icon = (ImageView) row.findViewById(R.id.memberIconInMemberList);
                            String[] names = getItem(position).getMemberName().split(" +");
                            String nameInit = "";
                            for (int j = 0; j < names.length; j++) {
                                nameInit += names[j].substring(0,1).toUpperCase();
                            }
                            int color = EventColor.getColor(getItem(position).getColor());
                            TextDrawable drawable = TextDrawable.builder().buildRound(nameInit, color);
                            icon.setImageDrawable(drawable);
                            return row;
                        }
                    };
                    mListView.setAdapter(arrayAdapter);
                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            final View pinMatchView = getActivity().getLayoutInflater().inflate(R.layout.pin_match_dialog, null);
                            builder.setView(pinMatchView)
                            .setTitle("verify your PIN number:")
                                    .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            EditText pinText = (EditText) pinMatchView.findViewById(R.id.inputPinEdittext);
                                            String pin = pinText.getText().toString();
                                            Member member = members.get(position);
                                            if (pin != null && pin.equals(member.getPin())) {
                                                storePref(member.getMemberName(), member.getColor());
                                                startMainActivity();
                                            } else {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(getActivity(), "PIN number is wrong, please try again.", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            }
                                        }
                                    });
                            builder.show();
                        }
                    });
                } else {
                    Log.d("memberName", "Error: " + e.getMessage());
                }
            }
        });
        createButton = (Button)inflatedView.findViewById(R.id.createMemberNameButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Member member = new Member();
                EditText memberNameView = (EditText) getActivity().findViewById(R.id.createMemberNameET);
                EditText pinView = (EditText)getActivity().findViewById(R.id.createMemberPin);
                member.setMemberName(memberNameView.getText().toString());
                member.setColor(EventColor.BLUE);
                member.setPin(pinView.getText().toString());
                member.setACL(new ParseACL(ParseUser.getCurrentUser()));
                member.saveEventually();

                storePref(memberNameView.getText().toString(), EventColor.BLUE);
                startMainActivity();
            }
        });

        return inflatedView;
    }

    private void startMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void storePref(String memberName, int color) {
        SharedPreferences userPref = getActivity().getSharedPreferences("User_Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = userPref.edit();
        editor.putString("memberName", memberName);
        editor.putInt("color", color);
        editor.apply();
    }
}
