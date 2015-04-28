package com.xg.keepittogether;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.xg.keepittogether.Parse.Member;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingActivity extends Activity implements AdapterView.OnItemSelectedListener, MultiChoiceListDialogFragment.MultiChoiceListDialogListener{


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

        /**
         * Google Calendar Api inialization
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int itemPosition = position;
        SharedPreferences.Editor edit = userPref.edit();
        edit.putInt("color", position);
        edit.apply();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Members");
        query.whereEqualTo("memberName", userPref.getString("memberName",null));
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                ParseObject member = parseObjects.get(0);
                member.put("color", itemPosition);
                member.saveEventually();
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
                } else {
                    Log.d("query member error: ", e.getMessage());
                }
            }
        });
        ParseUser.logOut();
        userPref.edit().clear().apply();
        googlePref.edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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

    private static class ColorApapter extends ArrayAdapter<String> {

        LayoutInflater mInflater;
        ArrayList<String> allColor = new ArrayList<>(
                Arrays.asList("black", "blue", "cyan", "Gray", "Green", "Magenta", "Red", "Yellow", "Orange"));

        public ColorApapter(Context context, int resource) {
            super(context, resource);
            mInflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public int getCount() {
            return allColor.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            View row = mInflater.inflate(R.layout.color_choose_spinner_row, parent, false);

            TextView colorLabel = (TextView)row.findViewById(R.id.colorNameInSpinnerRow);
            ImageView colorIcon = (ImageView)row.findViewById(R.id.colorIconInSpinnerRow);
            colorLabel.setText(allColor.get(position));
            colorIcon.setBackgroundColor(EventColor.getColor(position));
            return row;
        }
    }

    public void setCalendar(View view) {
        if (isGooglePlayServicesAvailable()) {
            getCalendarList();
        } else {
           Toast.makeText(this, "Google Play Services required: " + "after installing, close and relaunch this app.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void finish() {
        if (colorChanged) {
            returnData.putExtra("colorChanged", true);//TODO change place color to color setting
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
                    //TODO
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
                            Log.d("The following error occurred: ",e.getMessage());
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
