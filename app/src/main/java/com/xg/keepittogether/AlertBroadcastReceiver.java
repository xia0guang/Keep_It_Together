package com.xg.keepittogether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by wuxiaoguang on 4/9/15.
 */
public class AlertBroadcastReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context, "Receiver works", Toast.LENGTH_LONG).show();
        Intent service = new Intent(context, AlertService.class);
        service.putExtras(intent.getExtras());
        context.startService(service);
    }
}
