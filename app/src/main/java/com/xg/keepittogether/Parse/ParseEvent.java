package com.xg.keepittogether.Parse;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by wuxiaoguang on 4/15/15.
 */
@ParseClassName("ParseEvent")
public class ParseEvent extends ParseObject{

    public Calendar getStartCal(){
        Calendar cal = Calendar.getInstance();
        Date date = getStartDate();
        cal.setTime(date);
        return cal;
    }

    public Calendar getEndCal(){
        Calendar cal = Calendar.getInstance();
        Date date = getEndDate();
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

    public Date getStartDate() {
        return getDate("startDate");
    }

    public Date getEndDate() {
        return getDate("endDate");
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
        put("memberName", name);
    }

    public String getNote(){
        return getString("note");
    }

    public void setNote(String note) {
        if(note == null) return;
        put("note", note);
    }

    public void setFrom(String from) {
        put("from", from);
    }

    public String getFrom() {
        return getString("from");
    }

    public void setCalendarName(String calendarName) {
        put("calendarName", calendarName);
    }

    public String getCalendarName() {
        return getString("calendarName");
    }

    public void setEventID(String id) {
        put("eventId", id);
    }

    public String getEventID() {
        return getString("eventId");
    }

    public void setCancelled() {
        put("inList", false);
    }
    public void setConfirmed() {
        put("inList", true);
    }

    public boolean getStatus() {
        return getBoolean("inList");
    }

    @Override
    public String toString() {
        return getTitle() + ", " + String.format("%tD", getStartCal()) + ", " + String.valueOf(getStartCal().getTimeInMillis());
    }

    //TODO all day event
}
