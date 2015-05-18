package com.xg.keepittogether;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.xg.keepittogether.Parse.ParseEvent;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * Created by wuxiaoguang on 4/30/15.
 */
public class EventIndicateDecorator implements DayViewDecorator{

    CharSequence symbol;
    CalendarDay day;
    HashSet<Integer> colors;

    public EventIndicateDecorator(CharSequence symbol, CalendarDay day) {
        this.symbol = symbol;
        colors = new HashSet<>();
        this.day = day;
    }


    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return this.day != null && this.day.equals(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        CharSequence text = view.getText();
        SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
        for (Integer color : colors) {
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
            spannableString.append(symbol);
            spannableString.setSpan(colorSpan, spannableString.length()-1, spannableString.length(), 0);
        }
        view.setText(spannableString);
    }

    public void addColor(int  color) {
        colors.add(color);
    }

    public static void buildDecorators(final Activity activity) {
        final MyApplication.DataWrapper dataWrapper = ((MyApplication)activity.getApplication()).dataWrapper;
        SharedPreferences userPref = activity.getSharedPreferences("User_Preferences", Context.MODE_PRIVATE);
        dataWrapper.decorators.clear();
        for(int i=0; i<dataWrapper.eventList.size(); i++) {
            List<ParseEvent> list = dataWrapper.eventList.get(i);
            Calendar cal = list.get(0).getStartCal();
            CalendarDay day = new CalendarDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            EventIndicateDecorator decorator = new EventIndicateDecorator("●", day);
            for (int j = 0; j <list.size(); j++) {
                ParseEvent event = list.get(j);
                int color = EventColor.getColor(userPref.getInt("color." + event.getMemberName(), 1));
                decorator.addColor(color);
            }
            dataWrapper.decorators.add(decorator);
        }

        ((MainActivity) activity).calendarView.addDecorators(dataWrapper.decorators);
        ((MainActivity) activity).calendarView.invalidateDecorators();

        /*activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity) activity).calendarView.addDecorators(dataWrapper.decorators);
                ((MainActivity) activity).calendarView.invalidateDecorators();
            }
        });*/
    }
}
