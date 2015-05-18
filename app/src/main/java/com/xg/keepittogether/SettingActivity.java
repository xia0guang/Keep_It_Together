package com.xg.keepittogether;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.xg.keepittogether.Parse.Member;
import com.xg.keepittogether.Parse.ParseEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener, MultiChoiceListDialogFragment.MultiChoiceListDialogListener{



    private SharedPreferences userPref;
    private SharedPreferences googlePref;
    private boolean colorChanged = false;

    com.google.api.services.calendar.Calendar mService;

    GoogleAccountCredential credential;
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "googleAccountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};

    public List<String> googleCalendarNameList;
    public List<String> googleCalendarIdList;

    private Intent returnData = new Intent();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);


        userPref = getSharedPreferences("User_Preferences", MODE_PRIVATE);
        googlePref = getSharedPreferences("Google_Calendar_List", MODE_PRIVATE);


        TextView familyEmailView = (TextView)findViewById(R.id.familyAcountTV);
        familyEmailView.setText(ParseUser.getCurrentUser().getString("username"));
        TextView memberNameView = (TextView)findViewById(R.id.memberNameTV);
        memberNameView.setText(userPref.getString("memberName","noValue"));


        Spinner colorView = (Spinner)findViewById(R.id.colorSpinner);

        ColorApapter colorAdapter = new ColorApapter(this, R.layout.color_choose_spinner_row);
        colorView.setAdapter(colorAdapter);
        colorView.setSelection(userPref.getInt("color", EventColor.BLUE));
        colorView.setOnItemSelectedListener(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.action_bar)));

        /**
         * Google Calendar API inialization
        */
        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(userPref.getString(PREF_ACCOUNT_NAME, null));

        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Keep Together")
                .build();

        if(googlePref.getInt("listSize", 0) > 0) onOkClicked();


    }

    private class ColorApapter extends ArrayAdapter<String> {

        LayoutInflater mInflater;
        ArrayList<String> allColor = new ArrayList<>(
                Arrays.asList("black", "blue", "cyan", "Gray", "Green", "Magenta", "Red", "Yellow", "Orange"));

        public ColorApapter(Context context, int resource) {
            super(context, resource);
            mInflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, parent);
        }

        @Override
        public int getCount() {
            return allColor.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, parent);
        }

        public View getCustomView(int position, ViewGroup parent) {

            View row = mInflater.inflate(R.layout.color_choose_spinner_row, parent, false);

            TextView colorLabel = (TextView)row.findViewById(R.id.colorNameInSpinnerRow);
            ImageView colorIcon = (ImageView)row.findViewById(R.id.colorIconInSpinnerRow);
            Drawable circle = SettingActivity.this.getResources().getDrawable(R.drawable.circle);
            circle.setColorFilter(EventColor.getColor(position), PorterDuff.Mode.SRC);
            colorIcon.setImageDrawable(circle);
            colorLabel.setText(allColor.get(position));
            return row;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int itemPosition = position;
        SharedPreferences.Editor edit = userPref.edit();
        edit.putInt("color", position);
        edit.apply();

        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.whereEqualTo("memberName", userPref.getString("memberName",null));
        query.findInBackground(new FindCallback<Member>() {
            @Override
            public void done(List<Member> members, ParseException e) {
                if (e == null) {
                    Member member = members.get(0);
                    member.setColor(itemPosition);
                    member.saveEventually();
                } else {
                    Log.d("setting color error:", e.getMessage());
                }
            }
        });
        colorChanged = true;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void signOut(View view) {
        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.whereEqualTo("memberName", userPref.getString("memberName", null));
        query.getFirstInBackground(new GetCallback<Member>() {
            @Override
            public void done(Member member, ParseException e) {
                if (e == null) {
                    member.setSyncTokenLong(0);
                        member.saveEventually();
                        ParseUser.logOut();
                        userPref.edit().clear().apply();
                        googlePref.edit().clear().apply();
                        Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                } else {
                    Log.d("query member error: ", e.getMessage());
                }
            }
        });
    }

    public void setCalendar(View view) {
        if (isGooglePlayServicesAvailable()) {
            getCalendarList();
        } else {
            Toast.makeText(this, "Google Play Services required: " + "after installing, close and relaunch this app.", Toast.LENGTH_SHORT).show();
        }
    }

    public void seeOthersColor(View view) {
        ParseQuery<Member> queryMember = ParseQuery.getQuery(Member.class);
        queryMember.findInBackground(new FindCallback<Member>() {
            @Override
            public void done(final List<Member> members, ParseException e) {
                if (e == null && members.size() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                    builder.setAdapter(new ArrayAdapter<Member>(SettingActivity.this, R.layout.member_colors) {
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
                            LayoutInflater mInflater = SettingActivity.this.getLayoutInflater();
                            View row = mInflater.inflate(R.layout.member_colors, parent, false);
                            TextView memberName = (TextView) row.findViewById(R.id.memberNameInMemberColorRow);
                            memberName.setText(getItem(position).getMemberName());
                            ImageView colorIcon = (ImageView) row.findViewById(R.id.colorIconInMemberColorsRow);
                            Drawable circle = SettingActivity.this.getResources().getDrawable(R.drawable.circle);
                            circle.setColorFilter(EventColor.getColor(getItem(position).getColor()), PorterDuff.Mode.SRC);
                            colorIcon.setImageDrawable(circle);
                            return row;
                        }
                    }, null);
                    builder.setNegativeButton("Done", null);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else if (e != null) {
                    Log.d("member query:", "Error" + e.getMessage());
                }
            }
        });
    }



    @Override
    public void onOkClicked() {
        TextView googleCalendarListView = (TextView)findViewById(R.id.googleCalendarSetting);
        String calList = "";
        int listSize = googlePref.getInt("listSize", 0);
        for (int i = 0; i < listSize; i++) {
            calList += googlePref.getString("calendar_" + i, "") + "\n";
        }
        googleCalendarListView.setText(calList);
        returnData.putExtra("googleCalendarSettingChanged", true);
    }

    @Override
    public void finish() {
        if (colorChanged) {
            //TODO change place color to color setting
            returnData.putExtra("colorChanged", true);
            setResult(RESULT_OK, returnData);
        }
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == RESULT_OK) {
                    getCalendarList();
                } else {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences.Editor editor = userPref.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        getCalendarList();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Account unspecified.", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getCalendarList();
                } else {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.save_in_setting) {
            finish();
            return true;
        }
        if(id == android.R.id.home) {
            finish();
            return true;
        }
        /*if(id == R.id.delete) {
            ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
            query.whereEqualTo("calendarName", "wu890120@gmail.com");
            query.findInBackground(new FindCallback<ParseEvent>() {
                @Override
                public void done(List<ParseEvent> list, ParseException e) {
                    if (e == null) {
                        for (ParseEvent parseEvent : list) {
                            parseEvent.deleteEventually();
                            parseEvent.unpinInBackground();
                        }
                    } else {
                        Log.d("find error:", e.getMessage());
                    }
                }
            });
        }
        if(id == R.id.reset_sync_token) {
            ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
            query.whereEqualTo("memberName", userPref.getString("memberName", null));
            query.getFirstInBackground(new GetCallback<Member>() {
                @Override
                public void done(Member member, ParseException e) {
                    if (e == null) {
                        Log.d("I am now: ",member.toString());
                        Log.d("I am in:", "after find");
                        member.setSyncTokenLong(0);
                        Log.d("I am in:", "after setting");
                        try {
                            member.save();
                            Log.d("I am in:", "after save");
                        } catch (ParseException e1) {
                            Log.d("member save error:", e1.getMessage());
                        }
                    } else {
                        Log.d("query member error: ", e.getMessage());
                    }
                }
            });
        }*/
        return true;
    }

    /**
     * Attempt to get a list of calendar events to display. If the email
     * address isn't known yet, then call chooseAccount() method so the user
     * can pick an account.
     */
    public void getCalendarList() {
        if (credential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                final ProgressDialog dlg = new ProgressDialog(this);
                dlg.setTitle("Please wait.");
                dlg.setMessage("Fetching, Please wait...");
                dlg.show();
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            fetchCalendarList(dlg);
                        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                            SettingActivity.this.showGooglePlayServicesAvailabilityErrorDialog(
                                    availabilityException.getConnectionStatusCode());

                        } catch (UserRecoverableAuthIOException userRecoverableException) {
                            SettingActivity.this.startActivityForResult(
                                    userRecoverableException.getIntent(),
                                    SettingActivity.REQUEST_AUTHORIZATION);

                        } catch (IOException e) {
                            Log.d("error occurred: ",e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        DialogFragment dialog = new MultiChoiceListDialogFragment();
                        //include a tag to identify the fragment
                        dialog.show(SettingActivity.this.getFragmentManager(), "MultiChoiceListDialogFragment");
                    }
                }.execute();
            } else {
                Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchCalendarList(ProgressDialog dlg) throws IOException {
        // List the next 10 events from the primary calendar.
        CalendarList calendarListEntry = mService.calendarList().list().execute();
        dlg.dismiss();
        List<CalendarListEntry> listEntries = calendarListEntry.getItems();
        Log.d("calendar List:", listEntries.toString());
        googleCalendarNameList = new ArrayList<>();
        googleCalendarIdList = new ArrayList<>();
        for (int i = 0; i < listEntries.size(); i++) {
            googleCalendarNameList.add(listEntries.get(i).getSummary());
            googleCalendarIdList.add(listEntries.get(i).getId());
        }
    }

    /**
     * Starts an activity in Google Play Services so the user can pick an
     * account.
     */
    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode,
                        SettingActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }
}
