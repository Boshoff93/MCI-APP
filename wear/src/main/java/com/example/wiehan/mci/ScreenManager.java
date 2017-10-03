package com.example.wiehan.mci;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by wiehan on 2017/06/10.
 */

public class ScreenManager {

    SharedPreferences pref ;
    SharedPreferences.Editor editor ;
    Context context ;

    public ScreenManager(Context context) {
        this.context = context ;
        pref = context.getSharedPreferences("first",0) ;
        editor = pref.edit();
    }

    public void setFirst(boolean isFirst) {
        editor.putBoolean("check", isFirst) ;
        editor.commit();
    }

    public boolean Check() {
        return pref.getBoolean("check", true) ;
    }

}

