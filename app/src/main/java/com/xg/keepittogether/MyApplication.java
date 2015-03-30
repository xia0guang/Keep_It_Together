package com.xg.keepittogether;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
//import com.parse.ParseCrashReporting;
import com.parse.ParseUser;

/**
 * Created by wuxiaoguang on 3/29/15.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Crash Reporting.
//        ParseCrashReporting.enable(this);

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(this, "4NH6F5TWo5Mn9H7hyesQfaFqmLm6YqgyG9q3L06u", "RLwcsKj1Cg9Me9a7fuewNjKQRgbHG8THnvHo4I1J");


        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        // Optionally enable public read access.
        // defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
    }
}
