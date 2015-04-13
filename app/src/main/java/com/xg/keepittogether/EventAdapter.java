package com.xg.keepittogether;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;

/**
 * Created by wuxiaoguang on 3/30/15.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    List<List<ParseObject>> eventList;
    HashMap<Long, Integer> positionMap;
    HashMap<Integer, Calendar> reversePositionMap;
    Context context;
    SharedPreferences userPref;

    EventAdapter(Context context, List<List<ParseObject>> eventList, SharedPreferences userPref) {
        this.eventList = eventList;
        this.context = context;
        this.userPref = userPref;
        positionMap = new HashMap<>();
        reversePositionMap = new HashMap<>();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private ViewGroup rowViewGroup;
        int rows;
        public ViewHolder(View view) {
            super(view);
            rows = 0;
            rowViewGroup = (ViewGroup)view.findViewById(R.id.rowView);

        }
    }



    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        List<ParseObject> list = eventList.get(position);
        if (holder.rows < list.size()) {
            holder.rowViewGroup.removeAllViews();
            holder.rows = 0;


            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView startView = new TextView(context);
            startView.setLayoutParams(layoutParams);
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(list.get(0).getDate("startDate"));
            startView.setText(String.format("%tD", startCal));
            holder.rowViewGroup.addView(startView);

            for (final ParseObject parseObject : list) {
                View element = LayoutInflater.from(context).inflate(R.layout.element, null);
                //start time view
                TextView startTimeView = (TextView)element.findViewById(R.id.eventStartTimeInElement);
                Calendar startTimeCal = Calendar.getInstance();
                startTimeCal.setTime(parseObject.getDate("startDate"));
                startTimeView.setText(String.format("%tl:%tM %tp", startTimeCal, startTimeCal, startTimeCal));
                //end date time view
                TextView endView = (TextView)element.findViewById(R.id.eventEndTimeInElement);
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(parseObject.getDate("endDate"));
                endView.setText(String.format("%tD  %tl:%tM %tp", endCal, endCal, endCal, endCal));
                //title view
                TextView title = (TextView)element.findViewById(R.id.eventTitleInElement);
                title.setText(parseObject.getString("title"));
                //member name
                TextView memberName = (TextView)element.findViewById(R.id.memberNameInElement);
                memberName.setText("@" + parseObject.getString("memberName"));

                memberName.setBackgroundColor(EventColor.getColor(userPref.getInt("color." + parseObject.getString("memberName"), 1)));
                //add element to row

                Drawable divider = context.getResources().getDrawable(R.drawable.divider);
                ImageView dividerView = new ImageView(context);
                dividerView.setImageDrawable(divider);
                holder.rowViewGroup.addView(dividerView);
                holder.rowViewGroup.addView(element);
                holder.rows++;


//                Log.d("local:", parseObject.getString("memberName"));
//                Log.d("remote:", userPref.getString("memberName", "noValue"));
                if (parseObject.getString("memberName").equals(userPref.getString("memberName", "noValue"))) {

                    element.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(context, AddEventActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("title", parseObject.getString("title"));
                            bundle.putString("note", parseObject.getString("note"));
                            bundle.putLong("startDate", parseObject.getDate("startDate").getTime());
                            Log.d("start: ", "" + parseObject.getDate("startDate").getTime());

                            bundle.putLong("endDate", parseObject.getDate("endDate").getTime());
                            Log.d("start: ", "" + parseObject.getDate("endDate").getTime());

                            bundle.putString("objectID", parseObject.getObjectId());
                            intent.putExtras(bundle);
                            context.startActivity(intent);
                        }
                    });
                }
            }
        }

    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public int getPosition(Calendar cal) {
        long dayTime = cal.getTimeInMillis()/(1000*60*60*24);
        if(positionMap.get(dayTime) != null) return positionMap.get(dayTime);
        else return -1;
    }

    public Calendar getCalendarByPosition(int position) {
        if(position >= 0 && position < getItemCount()) {
            return reversePositionMap.get(position);
        }
        return null;
    }

}
