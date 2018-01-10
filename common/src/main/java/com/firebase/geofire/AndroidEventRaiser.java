package com.firebase.geofire;

import android.os.Handler;
import android.os.Looper;

class AndroidEventRaiser implements EventRaiser {
    private final Handler mainThreadHandler;

    public AndroidEventRaiser() {
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void raiseEvent(Runnable r) {
        this.mainThreadHandler.post(r);
    }
}
