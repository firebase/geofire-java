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
import java.util.Objects;

/**
 * GeoQuery notifies listeners with this interface about dataSnapshots that entered, exited, or moved within the query.
 */
final class EventListenerBridge implements GeoQueryDataEventListener {
    private final GeoQueryEventListener listener;

    public EventListenerBridge(final GeoQueryEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDataEntered(final DataSnapshot dataSnapshot, final GeoLocation location) {
        listener.onKeyEntered(dataSnapshot.getKey(), location);
    }

    @Override
    public void onDataExited(final DataSnapshot dataSnapshot) {
        listener.onKeyExited(dataSnapshot.getKey());
    }

    @Override
    public void onDataMoved(final DataSnapshot dataSnapshot, final GeoLocation location) {
        listener.onKeyMoved(dataSnapshot.getKey(), location);
    }

    @Override
    public void onDataChanged(final DataSnapshot dataSnapshot, final GeoLocation location) {
        // No-op.
    }

    @Override
    public void onGeoQueryReady() {
        listener.onGeoQueryReady();
    }

    @Override
    public void onGeoQueryError(final DatabaseError error) {
        listener.onGeoQueryError(error);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final EventListenerBridge that = (EventListenerBridge) o;
        return listener.equals(that.listener);
    }

    @Override
    public int hashCode() {
        return listener.hashCode();
    }
}
