package com.xg.keepittogether;

/**
 * Created by wuxiaoguang on 4/1/15.
 */
public class EventColor{
    public final static int BLACK = 0;
    public final static int BLUE = 1;
    public final static int CYAN = 2;
    public final static int GRAY = 3;
    public final static int GREEN = 4;
    public final static int MAGENTA = 5;
    public final static int RED = 6;
    public final static int YELLOW = 7;
    public final static int ORANGE = 8;

    public static int getColor(int colorSeq) {
        switch (colorSeq) {
            case BLACK: {
                return 0xFF000000;
            }
            case BLUE: {
                return 0xFF5DB4FF;
            }
            case CYAN: {
                return 0xFF00FFFF;
            }
            case GRAY: {
                return 0xFF9F9F9F;
            }
            case GREEN: {
                return 0xFFA5FF69;
            }
            case MAGENTA: {
                return 0xFF81217F;
            }
            case RED: {
                return 0xC8FF2020;
            }
            case YELLOW: {
                return 0xFFFFEE6D;
            }
            case ORANGE: {
                return 0xFFFF994E;
            }
            default: return 0xFFFF994E;
        }
    }
}