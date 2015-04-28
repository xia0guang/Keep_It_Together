package com.xg.keepittogether;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.xg.keepittogether.Parse.ParseEvent;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Created by wuxiaoguang on 4/23/15.
 */
public class GoogleCalendarUtils {
    private static final String PREF_ACCOUNT_NAME = "googleAccountName";
    private static final String SYNC_TOKEN_KEY = "syncToken";
    private static SharedPreferences userPref;
    private static SharedPreferences googlePref;

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;


    private static com.google.api.services.calendar.Calendar mService;
    private static final String[] SCOPESFETCH = {CalendarScopes.CALENDAR_READONLY};
    private static final String[] SCOPESUPDATE = {CalendarScopes.CALENDAR};
    private static final Set<String> ALLSCOPES = CalendarScopes.all();
    final static HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final static JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    /** Global instance of the sync settings datastore. */
    private static DataStore<String> syncSettingsDataStore;

    public static void downloadGoogleCalendar(Context context) throws IOException, ParseException{

        userPref = context.getSharedPreferences("User_Preferences", Context.MODE_PRIVATE);
        googlePref = context.getSharedPreferences("Google_Calendar_List", Context.MODE_PRIVATE);

        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(SCOPESFETCH))
                    .setBackOff(new ExponentialBackOff())
                    .setSelectedAccountName(userPref.getString(PREF_ACCOUNT_NAME, null));

            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Keep Together")
                    .build();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

        File dataStoreDir = context.getFilesDir();
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(dataStoreDir);
        try {
            syncSettingsDataStore = dataStoreFactory.getDataStore("SyncSettings");

        } catch (NullPointerException | IOException npe) {
            npe.printStackTrace();
        }

        int listSize = googlePref.getInt("listSize", 0);
        Events events = null;
        for (int i=0; i<listSize; i++) {
            String calendarId = googlePref.getString("calendar_Id_" + i, null);
            String calendarName = googlePref.getString("calendar_" + i, null);

            Calendar.Events.List request = mService.events().list(calendarId);

            // Load the sync token stored from the last execution, if any.
            String syncToken = syncSettingsDataStore.get(SYNC_TOKEN_KEY);
            if (syncToken == null) {
                System.out.println("Performing full sync.");

                /**
                 * Set the filters you want to use during the full sync. Sync tokens aren't compatible with
                   most filters, but you may want to limit your full sync to only a certain date range.
                   In this example we are only syncing events up to a year old.
                 */
                java.util.Calendar fiveYearAgo = java.util.Calendar.getInstance();
                fiveYearAgo.add(java.util.Calendar.YEAR, -5);
                request.setTimeMin(new DateTime(fiveYearAgo.getTime()));
            } else {
                System.out.println("Performing incremental sync.");
                request.setSyncToken(syncToken);
            }

            // Retrieve the events, one page at a time.
            String pageToken = null;

            do {
                request.setPageToken(pageToken);
                try {
                    events = request.execute();
                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 410) {
                        // A 410 status code, "Gone", indicates that the sync token is invalid.
                        Toast.makeText(context, "Invalid sync token, please restart app.", Toast.LENGTH_LONG).show();
                        syncSettingsDataStore.delete(SYNC_TOKEN_KEY);
                    } else {
                        throw e;
                    }
                }

                List<Event> items = events.getItems();
                if (items.size() == 0) {
                    System.out.println("No new events to sync.");
                } else {
                    for (Event event : items) {
                        ParseQuery<ParseEvent> query = ParseQuery.getQuery(ParseEvent.class);
                        query.whereEqualTo("eventId", event.getId());
                        ParseEvent parseEvent = null;
                        try {
                            parseEvent = query.getFirst();
                        } catch (ParseException e) {
                            parseEvent = new ParseEvent();
                        }
                        if("cancelled".equals(event.getStatus())) {
                            parseEvent.deleteEventually();
                            query.fromLocalDatastore();
                            parseEvent = query.getFirst();
                            parseEvent.unpin();
                        } else {
                            parseEvent.setEventID(event.getId());
                            parseEvent.setTitle(event.getSummary());
                            parseEvent.setNote(event.getDescription());
                            parseEvent.setStartDate(getStartDateTimeCal(event));
                            parseEvent.setEndDate(getEndDateTimeCal(event));
                            parseEvent.setMemberName(userPref.getString("memberName", null));
                            parseEvent.setFrom("Google_Calendar");
                            parseEvent.setCalendarName(calendarName);
                            parseEvent.setACL(new ParseACL(ParseUser.getCurrentUser()));

                            parseEvent.pin();
                            parseEvent.save();
                        }
                    }
                }

                pageToken = events.getNextPageToken();
            } while (pageToken != null);
            // Store the sync token from the last request to be used during the next execution.
            System.out.println("Sync complete.");
        }
        syncSettingsDataStore.set(SYNC_TOKEN_KEY, events.getNextSyncToken());
    }

    public static boolean uploadSingleEventInNewThread(final Context context,final String calendarId, final String eventId,  final ParseEvent parseEvent) throws IOException {
        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(SCOPESUPDATE))
                    .setBackOff(new ExponentialBackOff())
                    .setSelectedAccountName(userPref.getString(PREF_ACCOUNT_NAME, null));

            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Keep Together")
                    .build();

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Event event = null;
                try {
                    event = mService.events().get(calendarId, eventId).execute();

                }catch (UserRecoverableAuthIOException e) {
                    ((AddEventActivity)context).startActivityForResult(e.getIntent(), 0);
                }catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                event.setSummary(parseEvent.getTitle());
                event.setDescription(parseEvent.getNote());
                EventDateTime startDateTime = new EventDateTime();
                startDateTime.setDateTime(new DateTime(parseEvent.getStartDate()));
                event.setStart(startDateTime);
                EventDateTime endDateTime = new EventDateTime();
                endDateTime.setDateTime(new DateTime(parseEvent.getEndDate()));
                event.setEnd(endDateTime);
                Event updatedEvent = null;
                try {
                    updatedEvent = mService.events().update(calendarId, event.getId(), event).execute();
                }catch (UserRecoverableAuthIOException e) {
                    ((AddEventActivity)context).startActivityForResult(e.getIntent(), 0);
                }catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                Log.d("update event: ", updatedEvent.getUpdated().toString());

                return null;
            }
        }.execute();
        return true;
    }


    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    public static boolean isDeviceOnline(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
    public static boolean isGooglePlayServicesAvailable(Activity activity) {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(activity, connectionStatusCode);
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
    static void showGooglePlayServicesAvailabilityErrorDialog(final Activity activity, final int connectionStatusCode) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode,
                        activity,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    static java.util.Calendar getStartDateTimeCal(Event event) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        EventDateTime startDate = event.getStart();
        if(startDate.getDateTime() == null) {
            cal.setTimeInMillis(startDate.getDate().getValue());
        } else {
            cal.setTimeInMillis(startDate.getDateTime().getValue());
        }
        return cal;
    }

    static java.util.Calendar getEndDateTimeCal(Event event) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        EventDateTime end = event.getEnd();
        if(end.getDateTime() == null) {
            //TODO add all day checking
            cal.setTimeInMillis(end.getDate().getValue());
        } else {
            cal.setTimeInMillis(end.getDateTime().getValue());
        }
        return cal;
    }
}
