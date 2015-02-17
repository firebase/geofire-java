package com.firebase.geofire;

import java.util.concurrent.BlockingQueue;

import com.firebase.client.FirebaseError;

public class GeoQueryEventListenerCounter implements GeoQueryEventListener {
    private BlockingQueue<Boolean> results;

    public GeoQueryEventListenerCounter(BlockingQueue<Boolean> results) {
    	this.results=results;
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        try {
			results.put(true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @Override
    public void onKeyExited(String key) {

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {

    }

    @Override
    public void onGeoQueryReady() {
        try {
			results.put(false);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    @Override
    public void onGeoQueryError(FirebaseError error) {

    }
}
