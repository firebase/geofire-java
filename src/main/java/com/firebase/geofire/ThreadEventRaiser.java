package com.firebase.geofire;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ThreadEventRaiser implements EventRaiser {

    private final ExecutorService executorService;

    public ThreadEventRaiser() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void raiseEvent(Runnable r) {
        this.executorService.submit(r);
    }
}
