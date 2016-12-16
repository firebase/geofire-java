package com.firebase.geofire;

import com.google.appengine.api.ThreadManager;
import java.util.concurrent.Executors;

class AppEngineEventRaiser extends ThreadEventRaiser {

    @Override
    public AppEngineEventRaiser() {
        this.executorService = Executors.newSingleThreadExecutor(ThreadManager.backgroundThreadFactory());
    }
}
