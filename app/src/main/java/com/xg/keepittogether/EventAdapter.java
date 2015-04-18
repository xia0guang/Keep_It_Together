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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;

/**
 * Created by wuxiaoguang on 3/30/15.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    List<List<Event>> eventList;

    Context context;
    SharedPreferences userPref;

    EventAdapter(Context context, List<List<Event>> eventList, SharedPreferences userPref) {
        this.eventList = eventList;
        this.context = context;
        this.userPref = userPref;

    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private ViewGroup rowViewGroup;
        private TextView startDateView;
        private List<SubViewHolder> elementHolderList;
        public ViewHolder(View view) {
            super(view);
            startDateView = (TextView)view.findViewById(R.id.eventStartTimeInRow);
            rowViewGroup = (ViewGroup)view.findViewById(R.id.rowView);
            elementHolderList = new ArrayList<>();
        }

        public void addElement(View view) {
            rowViewGroup.addView(view);
        }
    }

    public static class SubViewHolder extends RecyclerView.ViewHolder {

        private TextView startTimeView, endView, titleView, memberNameView;
        private ViewGroup elementView;
        public SubViewHolder(View view) {
            super(view);
            startTimeView = (TextView)view.findViewById(R.id.eventStartTimeInElement);
            endView = (TextView)view.findViewById(R.id.eventEndTimeInElement);
            titleView = (TextView)view.findViewById(R.id.eventTitleInElement);
            memberNameView = (TextView)view.findViewById(R.id.memberNameInElement);
            elementView = (ViewGroup)view.findViewById(R.id.elementView);
        }

    }



    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row, parent, false);
        ViewHolder vh = new ViewHolder(v);


        for (int i = 0; i < viewType; i++) {
            Drawable divider = context.getResources().getDrawable(R.drawable.divider);
            ImageView dividerView = new ImageView(context);
            dividerView.setImageDrawable(divider);
            vh.addElement(dividerView);

            View element = LayoutInflater.from(context).inflate(R.layout.element, vh.rowViewGroup, false);
            vh.addElement(element);
            SubViewHolder elementHolder = new SubViewHolder(element);
            vh.elementHolderList.add(elementHolder);
        }
        return vh;
    }

    @Override
    public int getItemViewType(int position) {
        return eventList.get(position).size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        List<Event> list = eventList.get(position);

        Event headEvent = list.get(0);
        Calendar startCal = headEvent.getStartCal();
        holder.startDateView.setText(String.format("%tD", startCal));

        for (int i = 0; i < list.size(); i++) {
            final Event event = list.get(i);
            Calendar startTimeCal = event.getStartCal();
            holder.elementHolderList.get(i).startTimeView.setText(String.format("%tl:%tM %tp", startTimeCal, startTimeCal, startTimeCal));
            Calendar endCal = event.getEndCal();
            holder.elementHolderList.get(i).endView.setText(String.format("%tD  %tl:%tM %tp", endCal, endCal, endCal, endCal));
            holder.elementHolderList.get(i).titleView.setText(event.getTitle());
            holder.elementHolderList.get(i).memberNameView.setText("@" + event.getMemberName());
            holder.elementHolderList.get(i).memberNameView.setBackgroundColor(EventColor.getColor(userPref.getInt("color." + event.getString("memberName"), 1)));

//            Log.d("start time: ", String.format("%tD  %tl:%tM %tp", startTimeCal, startTimeCal, startTimeCal, startTimeCal));
//            Log.d("end time: ", String.format("%tD  %tl:%tM %tp", endCal, endCal, endCal, endCal));
//            Log.d("title: ", event.getTitle());
//            Log.d("member name: ", event.getMemberName());

            if (event.getMemberName().equals(userPref.getString("memberName", "noValue"))) {

                holder.elementHolderList.get(i).elementView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, AddEventActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("title", event.getTitle());
                        bundle.putString("note", event.getString("note"));
                        bundle.putLong("startDate", event.getStartCal().getTimeInMillis());
//                            Log.d("start: ", "" + event.getDate("startDate").getTime());

                        bundle.putLong("endDate", event.getEndCal().getTimeInMillis());
//                            Log.d("start: ", "" + event.getDate("endDate").getTime());

                        bundle.putString("objectID", event.getObjectId());
                        intent.putExtras(bundle);
                        context.startActivity(intent);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }



}
