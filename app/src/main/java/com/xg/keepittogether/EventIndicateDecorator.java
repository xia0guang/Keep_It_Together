package com.xg.keepittogether;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;

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
}
