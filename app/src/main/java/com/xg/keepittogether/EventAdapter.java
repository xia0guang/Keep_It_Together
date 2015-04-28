package com.xg.keepittogether;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xg.keepittogether.Parse.ParseEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wuxiaoguang on 3/30/15.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    public List<List<ParseEvent>> eventList;

    Context context;
    SharedPreferences userPref;
    public static final int REQUEST_ADD_OR_CHANGE_NEW_EVENT = 0;

    EventAdapter(Context context, List<List<ParseEvent>> eventList, SharedPreferences userPref) {
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

        private TextView startTimeView, endView, titleView, memberNameView, indicateView;
        private ViewGroup elementView;
        public SubViewHolder(View view) {
            super(view);
            startTimeView = (TextView)view.findViewById(R.id.eventStartTimeInElement);
            endView = (TextView)view.findViewById(R.id.eventEndTimeInElement);
            titleView = (TextView)view.findViewById(R.id.eventTitleInElement);
            indicateView = (TextView)view.findViewById(R.id.indicateGoogleCalendarTV);
            memberNameView = (TextView)view.findViewById(R.id.memberNameInElement);
            elementView = (ViewGroup)view.findViewById(R.id.elementView);
        }

    }



    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_row, parent, false);
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
    public void onBindViewHolder(ViewHolder holder, final int position) {
        List<ParseEvent> list = eventList.get(position);

        ParseEvent headParseEvent = list.get(0);
        Calendar startCal = headParseEvent.getStartCal();
        holder.startDateView.setText(String.format("%tD", startCal));

        for (int i = 0; i < list.size(); i++) {
            final ParseEvent parseEvent = list.get(i);
            Calendar startTimeCal = parseEvent.getStartCal();
            holder.elementHolderList.get(i).startTimeView.setText(String.format("%tl:%tM %tp", startTimeCal, startTimeCal, startTimeCal));
            Calendar endCal = parseEvent.getEndCal();
            holder.elementHolderList.get(i).endView.setText(String.format("%tD  %tl:%tM %tp", endCal, endCal, endCal, endCal));
            holder.elementHolderList.get(i).titleView.setText(parseEvent.getTitle());
            if ("Google_Calendar".equals(parseEvent.getFrom())) {
                holder.elementHolderList.get(i).indicateView.setText("Google");
            } else {
                holder.elementHolderList.get(i).indicateView.setText("");
            }
            holder.elementHolderList.get(i).memberNameView.setText("@" + parseEvent.getMemberName());
            holder.elementHolderList.get(i).memberNameView.setBackgroundColor(EventColor.getColor(userPref.getInt("color." + parseEvent.getString("memberName"), 1)));
            //TODO change color based on google calendar
            if (parseEvent.getMemberName().equals(userPref.getString("memberName", null))) {
                holder.elementHolderList.get(i).elementView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, AddEventActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("eventId", parseEvent.getEventID());
                        bundle.putString("calendarName", parseEvent.getCalendarName());
                        bundle.putString("from", parseEvent.getFrom());
                        bundle.putString("title", parseEvent.getTitle());
                        bundle.putString("note", parseEvent.getString("note"));
                        bundle.putLong("startDate", parseEvent.getStartCal().getTimeInMillis());
                        bundle.putLong("endDate", parseEvent.getEndCal().getTimeInMillis());
                        bundle.putString("objectID", parseEvent.getObjectId());
                        bundle.putInt("listPosition", position);
                        intent.putExtras(bundle);
                        ((Activity)context).startActivityForResult(intent, REQUEST_ADD_OR_CHANGE_NEW_EVENT);
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
