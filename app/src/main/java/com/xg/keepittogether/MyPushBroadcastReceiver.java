package com.xg.keepittogether;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuxiaoguang on 4/15/15.
 */
public class MyPushBroadcastReceiver extends ParsePushBroadcastReceiver {
    @Override
    protected Class<? extends Activity> getActivity(Context context, Intent intent) {
        return AddEventActivity.class;
    }


}
