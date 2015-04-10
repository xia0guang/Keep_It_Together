package com.xg.keepittogether;

/**
 * Created by wuxiaoguang on 4/1/15.
 */
public class EventColor{
    final static int BLACK = 0;
    final static int BLUE = 1;
    final static int CYAN = 2;
    final static int GRAY = 3;
    final static int GREEN = 4;
    final static int MAGENTA = 5;
    final static int RED = 6;
    final static int YELLOW = 7;
    final static int ORANGE = 8;

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