package com.xg.keepittogether;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.xg.keepittogether.Parse.ParseEvent;
import com.xg.keepittogether.Parse.ParseEventUtils;

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
        private ViewGroup rowViewGroup;
        private TextView startDateView;
        private ImageView longDivider;
        private List<SubViewHolder> elementHolderList;
        public ViewHolder(View view) {
            super(view);
            startDateView = (TextView)view.findViewById(R.id.eventStartTimeInRow);
            longDivider = (ImageView)view.findViewById(R.id.long_divider);
            rowViewGroup = (ViewGroup)view.findViewById(R.id.rowView);
            elementHolderList = new ArrayList<>();
        }

        public void addElement(View view) {
            rowViewGroup.addView(view);
        }
    }

    public static class SubViewHolder extends RecyclerView.ViewHolder {

        private TextView startToEndView, titleView;
        private ImageView memberIcon, shortDivider, indicateView;
        private ViewGroup elementView;
        public SubViewHolder(View view) {
            super(view);
            startToEndView = (TextView)view.findViewById(R.id.eventStartTimeToEndTimeTV);
            titleView = (TextView)view.findViewById(R.id.eventTitleInElement);
            indicateView = (ImageView)view.findViewById(R.id.indicateGoogleCalendarImage);
            memberIcon = (ImageView)view.findViewById(R.id.memberIcon);
            elementView = (ViewGroup)view.findViewById(R.id.elementView);
            shortDivider = (ImageView)view.findViewById(R.id.short_divider);
        }

    }



    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_row, parent, false);
        ViewHolder vh = new ViewHolder(v);
        Drawable longDivider = context.getResources().getDrawable(R.drawable.divider);
        vh.longDivider.setImageDrawable(longDivider);
        for (int i = 0; i < viewType; i++) {
            View element = LayoutInflater.from(context).inflate(R.layout.element, vh.rowViewGroup, false);
            vh.addElement(element);
            SubViewHolder elementHolder = new SubViewHolder(element);
            vh.elementHolderList.add(elementHolder);
            if(i == viewType-1) {
                elementHolder.shortDivider.setVisibility(View.GONE);
            }
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
        String[] weekdayList = context.getResources().getStringArray(R.array.weekday_list);
        holder.startDateView.setText(weekdayList[startCal.get(Calendar.WEEK_OF_MONTH)] + String.format(", %tD", startCal));

        for (int i = 0; i < list.size(); i++) {
            final ParseEvent parseEvent = list.get(i);
            Calendar startTimeCal = parseEvent.getStartCal();

            String startToEndStr = String.format("%tl:%tM %tp - ", startTimeCal, startTimeCal, startTimeCal);
            Calendar endCal = parseEvent.getEndCal();
            if(ParseEventUtils.hashCalDay(startCal) == ParseEventUtils.hashCalDay(endCal)) {
                startToEndStr += String.format("%tl:%tM %tp", endCal, endCal, endCal);
            }else if(ParseEventUtils.hashCalDay(startCal) == ParseEventUtils.hashCalDay(endCal) + 1) {
                startToEndStr += String.format("tomorrow %tl:%tM %tp", endCal, endCal, endCal);
            } else {
                startToEndStr += String.format("%tD  %tl:%tM %tp", endCal, endCal, endCal, endCal);
            }
            holder.elementHolderList.get(i).startToEndView.setText(startToEndStr);
            holder.elementHolderList.get(i).titleView.setText(parseEvent.getTitle());
            if ("Google_Calendar".equals(parseEvent.getFrom())) {
                Drawable googleIcon = context.getResources().getDrawable(R.drawable.google_icon);
                holder.elementHolderList.get(i).indicateView.setImageDrawable(googleIcon);
                holder.elementHolderList.get(i).indicateView.setVisibility(View.VISIBLE);
            } else {
                holder.elementHolderList.get(i).indicateView.setVisibility(View.GONE);
            }

            // draw member icon
            String[] names = parseEvent.getMemberName().split(" +");
            String nameInit = "";
            for (int j = 0; j < names.length; j++) {
                nameInit += names[j].substring(0,1).toUpperCase();
            }
            int color = EventColor.getColor(userPref.getInt("color." + parseEvent.getString("memberName"), 1));
            TextDrawable drawable = TextDrawable.builder().buildRound(nameInit, color);
            holder.elementHolderList.get(i).memberIcon.setImageDrawable(drawable);

            Drawable shortDivider = context.getResources().getDrawable(R.drawable.short_divider);
            holder.elementHolderList.get(i).shortDivider.setImageDrawable(shortDivider);
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
            } else {
                holder.elementHolderList.get(i).elementView.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }



}
