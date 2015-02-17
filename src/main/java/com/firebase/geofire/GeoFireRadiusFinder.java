package com.firebase.geofire;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class GeoFireRadiusFinder {
    private static final Integer MAX_RADIUS = 2400;
	private GeoFire geoFire;

    public GeoFireRadiusFinder(GeoFire geoFire) {
        this.geoFire = geoFire;
    }

    public Integer radiusWithResults(Integer minResults, Integer startRadius, GeoLocation current ) {
    	return radiusWithResults(minResults, startRadius, 2.0, current);
    }
    
    public Integer radiusWithResults(Integer minResults, Integer startRadius, Double growFactor, GeoLocation current ) {
        Integer currentRadius = startRadius;

        BlockingQueue<Boolean> results= new ArrayBlockingQueue<Boolean>(minResults);
        Integer totalResults;
        
        do {
            totalResults=0;
            results.clear();
            GeoQuery query = geoFire.queryAtLocation(current, currentRadius);
            query.addGeoQueryEventListener( new GeoQueryEventListenerCounter(results) );

            Boolean result;
            do {
                try {
					result=results.take();
				} catch (InterruptedException e) {
					return null;
				}
                if(result)
                	totalResults++;

            } while( result==true && totalResults<minResults );
            query.removeAllListeners();
            System.out.println("currentRadius=" + currentRadius + " totalResults=" + totalResults);
            
            if(totalResults>=minResults || currentRadius >=MAX_RADIUS)
            	break;

            currentRadius = Math.min( (int) (currentRadius * growFactor) , MAX_RADIUS );

        } while( true );

        return currentRadius;

    }

}