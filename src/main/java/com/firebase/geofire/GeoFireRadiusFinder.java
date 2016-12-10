package com.firebase.geofire;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class GeoFireRadiusFinder {
    
	private GeoFire geoFire;

    public GeoFireRadiusFinder(GeoFire geoFire) {
        this.geoFire = geoFire;
    }

    public Integer radiusWithResults(Integer minResults, Integer startRadius, GeoLocation current ) {
    	return radiusWithResults(minResults, startRadius, 2.0, current);
    }
    
    public Integer radiusWithResults(Integer minResults, Integer startRadius, Double growFactor, GeoLocation current ) {
    	if( ((int)(startRadius*growFactor)) <= startRadius)
        	throw new IllegalArgumentException("check startRadius and growFatcor such that (int)(startRadius*growFactor)) > startRadius");
    	
        Integer currentRadius  = startRadius;
        Integer previousRadius = null;

        BlockingQueue<Boolean> results= new ArrayBlockingQueue<Boolean>(minResults);
        Integer totalResults;
        
        do {
            totalResults=0;
            results.clear();
            GeoQuery query = geoFire.queryAtLocation(current, currentRadius);
            try {
            	query.addGeoQueryEventListener( new GeoQueryEventListenerCounter(results) );
            } catch (IllegalArgumentException exc) {
            	System.out.println("IllegalArgumentException " + exc.getMessage() );
            	if(previousRadius!=null)
            		return previousRadius;
            	else
            		throw new IllegalArgumentException(exc.getMessage());
            }

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
            
            if(totalResults>=minResults)
            	break;

            previousRadius = currentRadius;
            currentRadius  = (int) (currentRadius * growFactor);

        } while( true );

        return currentRadius;

    }

}