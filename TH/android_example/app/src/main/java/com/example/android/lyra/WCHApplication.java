package com.example.android.lyra;

import android.app.Application;
import android.content.Context;

import cn.wch.uartlib.WCHUARTManager;

public class WCHApplication extends Application {
    private static Application application;
    @Override
    public void onCreate() {
        super.onCreate();
        application=this;
        WCHUARTManager.getInstance().init(this);
//        WCHUARTManager.setReadTimeout(0);
//        WCHUARTManager.addNewHardware(0x1a86,0x7523);
//        WCHUARTManager.setDebug(false);
    }

    public static Context getContext(){
        return application;
    }
}
