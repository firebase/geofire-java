/*
 * Firebase GeoFire Java Library
 *
 * Copyright © 2014 Firebase - All Rights Reserved
 * https://www.firebase.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY FIREBASE AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL FIREBASE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.firebase.geofire;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

/**
 * GeoQuery notifies listeners with this interface about dataSnapshots that entered, exited, or moved within the query.
 */
public interface GeoQueryDataEventListener {

    /**
     * Called if a dataSnapshot entered the search area of the GeoQuery. This method is called for every dataSnapshot currently in the
     * search area at the time of adding the listener.
     *
     * This method is once per datasnapshot, and is only called again if onDataExited was called in the meantime.
     *
     * @param dataSnapshot The associated dataSnapshot that entered the search area
     * @param location The location for this dataSnapshot as a GeoLocation object
     */
    void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location);

    /**
     * Called if a datasnapshot exited the search area of the GeoQuery. This is method is only called if onDataEntered was called
     * for the datasnapshot.
     *
     * @param dataSnapshot The associated dataSnapshot that exited the search area
     */
    void onDataExited(DataSnapshot dataSnapshot);

    /**
     * Called if a dataSnapshot moved within the search area.
     *
     * This method can be called multiple times.
     *
     * @param dataSnapshot The associated dataSnapshot that moved within the search area
     * @param location The location for this dataSnapshot as a GeoLocation object
     */
    void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location);

    /**
     * Called if a dataSnapshot changed within the search area.
     *
     * An onDataMoved() is always followed by onDataChanged() but it is be possible to see
     * onDataChanged() without an preceding onDataMoved().
     *
     * This method can be called multiple times for a single location change, due to the way
     * the Realtime Database handles floating point numbers.
     *
     * Note: this method is not related to ValueEventListener#onDataChange(DataSnapshot).
     *
     * @param dataSnapshot The associated dataSnapshot that moved within the search area
     * @param location The location for this dataSnapshot as a GeoLocation object
     */
    void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location);

    /**
     * Called once all initial GeoFire data has been loaded and the relevant events have been fired for this query.
     * Every time the query criteria is updated, this observer will be called after the updated query has fired the
     * appropriate dataSnapshot entered or dataSnapshot exited events.
     */
    void onGeoQueryReady();

    /**
     * Called in case an error occurred while retrieving locations for a query, e.g. violating security rules.
     * @param error The error that occurred while retrieving the query
     */
    void onGeoQueryError(DatabaseError error);

}
