package com.firebase.geofire.util;

import com.firebase.geofire.core.SimpleFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ReadFuture extends SimpleFuture<Object> {

    public ReadFuture(Query ref) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ReadFuture.this.put(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ReadFuture.this.put(databaseError);
            }
        });
    }
}
