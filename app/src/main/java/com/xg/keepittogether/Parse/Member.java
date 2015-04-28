package com.xg.keepittogether.Parse;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.util.Calendar;

/**
 * Created by wuxiaoguang on 4/23/15.
 */
@ParseClassName("Members")
public class Member extends ParseObject{
    public void setColor(int color) {
        put("color", color);
    }
    public int getColor() {
        return getInt("color");
    }

    public void setMemberName(String memberName) {
        put("memberName", memberName);
    }

    public String getMemberName() {
        return getString("memberName");
    }

    public void setSyncToken(Calendar cal) {
        put("syncTokenLong", cal.getTimeInMillis());
    }
    public void setSyncTokenLong(long dayInMS) {
        put("syncTokenLong", dayInMS);
    }
    public long getSyncToken() {
        return getLong("syncTokenLong");
    }

}
