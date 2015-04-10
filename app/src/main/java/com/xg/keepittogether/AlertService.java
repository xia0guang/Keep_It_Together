package com.xg.keepittogether;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by wuxiaoguang on 4/9/15.
 */
public class AlertService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();

//        Log.d("Notification service?", "works");
//        Toast.makeText(getApplicationContext(), "Alert works", Toast.LENGTH_LONG).show();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setTicker("Here comes event!");
        mBuilder.setContentTitle(bundle.getString("title"));
        mBuilder.setContentText(bundle.getString("content"));
        mBuilder.setAutoCancel(true);
        mBuilder.setWhen(System.currentTimeMillis()+2000);
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        //TODO handle notification
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notifyIntent, 0);
        mBuilder.setContentIntent(pi);
        NotificationManager notiManager = (NotificationManager)getSystemService(this.NOTIFICATION_SERVICE);
        notiManager.notify(1, mBuilder.build());
        stopSelf();
        return START_NOT_STICKY;
    }
}
