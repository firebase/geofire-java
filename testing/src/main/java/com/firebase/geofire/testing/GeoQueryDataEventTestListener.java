package com.firebase.geofire.testing;

import static java.util.Locale.US;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;

/**
 * This listener can be used for testing your Geofire instance and asserting that certain events were sent.
 */
public final class GeoQueryDataEventTestListener extends TestListener implements
    GeoQueryDataEventListener {
  public static String dataEntered(String key, double latitude, double longitude) {
    return String.format(US, "DATA_ENTERED(%s,%f,%f)", key, latitude, longitude);
  }

  public static String dataExited(String key) {
    return String.format("DATA_EXITED(%s)", key);
  }

  public static String dataMoved(String key, double latitude, double longitude) {
    return String.format(US, "DATA_MOVED(%s,%f,%f)", key, latitude, longitude);
  }

  public static String dataChanged(String key, double latitude, double longitude) {
    return String.format(US, "DATA_CHANGED(%s,%f,%f)", key, latitude, longitude);
  }

  private final boolean recordEntered;
  private final boolean recordMoved;
  private final boolean recordChanged;
  private final boolean recordExited;

  /** This will by default record all of the events. */
  public GeoQueryDataEventTestListener() {
    this(true, true, true, true);
  }

  /** Allows you to specify exactly which of the events you want to record. */
  public GeoQueryDataEventTestListener(boolean recordEntered, boolean recordMoved,
      boolean recordChanged, boolean recordExited) {
    this.recordEntered = recordEntered;
    this.recordMoved = recordMoved;
    this.recordChanged = recordChanged;
    this.recordExited = recordExited;
  }

  @Override
  public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
    if (recordEntered) {
      addEvent(dataEntered(dataSnapshot.getKey(), location.latitude, location.longitude));
    }
  }

  @Override
  public void onDataExited(DataSnapshot dataSnapshot) {
    if (recordExited) {
      addEvent(dataExited(dataSnapshot.getKey()));
    }
  }

  @Override
  public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {
    if (recordMoved) {
      addEvent(dataMoved(dataSnapshot.getKey(), location.latitude, location.longitude));
    }
  }

  @Override
  public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {
    if (recordChanged) {
      addEvent(dataChanged(dataSnapshot.getKey(), location.latitude, location.longitude));
    }
  }

  @Override
  public void onGeoQueryReady() {
    // No-op.
  }

  @Override
  public void onGeoQueryError(DatabaseError error) {
    throw error.toException();
  }
}
