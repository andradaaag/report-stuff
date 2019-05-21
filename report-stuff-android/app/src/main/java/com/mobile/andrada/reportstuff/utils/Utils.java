package com.mobile.andrada.reportstuff.utils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

public class Utils {
    public enum Role {
        citizen,
        policeman,
        firefighter,
        smurd
    }

    public static String prettyTime(Date date){
        PrettyTime p = new PrettyTime();
        return p.format(date);
    }
}
