package com.firebase.geofire;

import com.google.appengine.api.ThreadManager;
import java.util.concurrent.Executors;

class AppEngineEventRaiser implements EventRaiser {
    
    private final ExecutorService executorService;

    public AppEngineEventRaiser() {
        this.executorService = Executors.newSingleThreadExecutor(ThreadManager.backgroundThreadFactory());
    }

    public void raiseEvent(Runnable r) {
        this.executorService.submit(r);
    }
}

