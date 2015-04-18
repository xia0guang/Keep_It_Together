package com.xg.keepittogether;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by wuxiaoguang on 4/15/15.
 */
@ParseClassName("Event")
public class Event extends ParseObject{

    public Calendar getStartCal(){
        Calendar cal = Calendar.getInstance();
        Date date = getDate("startDate");
        cal.setTime(date);
        return cal;
    }

    public Calendar getEndCal(){
        Calendar cal = Calendar.getInstance();
        Date date = getDate("endDate");
        cal.setTime(date);
        return cal;
    }

    public void setStartDate(Calendar cal) {
        Date date = cal.getTime();
        put("startDate", date);
    }

    public void setEndDate(Calendar cal) {
        Date date = cal.getTime();
        put("endDate", date);
    }

    public String getTitle() {
        return getString("title");
    }

    public void setTitle(String title) {
        put("title", title);
    }

    public String getMemberName() {
        return getString("memberName");
    }

    public void setMemberName(String name) {
        put("mamberName", name);
    }

    public String getNote(){
        return getString("note");
    }

    public void setNote(String note) {
        put("note", note);
    }

}
