package com.xg.keepittogether;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuxiaoguang on 3/30/15.
 */
public class EventAdapter extends BaseAdapter {

    List<ParseObject> eventList;
    Context context;
    LayoutInflater inflater;

    EventAdapter(Context ctx, SharedPreferences userPreferences) {
        eventList = new ArrayList<ParseObject>();
        context = ctx;
        inflater= (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
        query.whereEqualTo("familyID", userPreferences.getString("familyID", "noValue"));
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                    eventList = list;
                    try{
                        CompleteQueryListner activity = (CompleteQueryListner)context;
                        activity.setList();
                    } catch (ClassCastException cce) {
                        cce.printStackTrace();
                    }
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });
    }
    @Override
    public int getCount() {
        return eventList.size();
    }

    @Override
    public Object getItem(int position) {
        return eventList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = inflater.inflate(R.layout.row, null);
        TextView text = (TextView) rowView.findViewById(R.id.eventContent);
        ParseObject event = (ParseObject)getItem(position);
        if(event.getString("event") != null) {
            text.setText(event.getString("event"));
        }
        if(event.getString("userColor").equals("Green")) {
            rowView.setBackgroundColor(Color.GREEN);
//            Log.d("Color: ", event.getString("userColor"));
        } else if(event.getString("userColor").equals("Red")) {
            rowView.setBackgroundColor(Color.RED);
        } else {
            rowView.setBackgroundColor(Color.BLUE);
        }
        return rowView;
    }

    public interface CompleteQueryListner{
        public void setList();
    }
}
