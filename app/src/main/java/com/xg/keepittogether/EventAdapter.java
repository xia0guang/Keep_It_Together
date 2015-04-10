package com.xg.keepittogether;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by wuxiaoguang on 3/30/15.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    List<ParseObject> eventList;
//    Context context;
//    LayoutInflater inflater;

    EventAdapter(List<ParseObject> eventList, SharedPreferences userPreferences) {
        this.eventList = eventList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView titleView, dateTimeView;
        public ViewHolder(View view) {
            super(view);
            this.titleView = (TextView)view.findViewById(R.id.eventContent);
            this.dateTimeView = (TextView)view.findViewById(R.id.eventTime);
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
        holder.titleView.setText(eventList.get(position).getString("title"));
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date)eventList.get(position).get("startDate"));
        holder.dateTimeView.setText(String.format("%tl:%tM %tp", cal, cal, cal));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

}
