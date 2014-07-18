package com.firebase.geofire.util;

import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.core.SimpleFuture;

public class ReadFuture extends SimpleFuture<Object> {

    public ReadFuture(Query ref) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ReadFuture.this.put(dataSnapshot);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                ReadFuture.this.put(firebaseError);
            }
        });
    }
}
