# GeoFire for Android/Java — Realtime location queries with Firebase

GeoFire is an open-source library for Android/Java that allows you to store and query a
set of keys based on their geographic location.

At its heart, GeoFire simply stores locations with string keys. Its main
benefit however, is the possibility of querying keys within a given geographic
area - all in realtime.

GeoFire uses [Firebase](https://www.firebase.com/?utm_source=geofire-java) for
data storage, allowing query results to be updated in realtime as they change.
GeoFire *selectively loads only the data near certain locations, keeping your
applications light and responsive*, even with extremely large datasets.

A compatible GeoFire client is also available for [Objective-C](https://github.com/firebase/geofire-objc)
and [JavaScript](https://github.com/firebase/geofire-js).

### Integrating GeoFire with your data

GeoFire is designed as a lightweight add-on to Firebase. However, to keep things
simple, GeoFire stores data in its own format and its own location within
your Firebase. This allows your existing data format and security rules to
remain unchanged and for you to add GeoFire as an easy solution for geo queries
without modifying your existing data.

### Example Usage

Assume you are building an app to rate bars and you store all information for a
bar, e.g. name, business hours and price range, at `/bars/<bar-id>`. Later, you
want to add the possibility for users to search for bars in their vicinity. This
is where GeoFire comes in. You can store the location for each bar using
GeoFire, using the bar IDs as GeoFire keys. GeoFire then allows you to easily
query which bar IDs (the keys) are nearby. To display any additional information
about the bars, you can load the information for each bar returned by the query
at `/bars/<bar-id>`.


## Upgrading GeoFire

### Upgrading from GeoFire 1.0.x to 1.1.x

With the release of GeoFire for Android/Java 1.1.0, this library now uses [the new query
functionality found in Firebase 2.0.0](https://www.firebase.com/blog/2014-11-04-firebase-realtime-queries.html).
As a result, you will need to upgrade to Firebase 2.x.x and add a new `.indexOn` rule to your
Security and Firebase Rules to get the best performance. You can view [the updated rules
here](https://github.com/firebase/geofire-js/blob/master/examples/securityRules/rules.json)
and [read our docs for more information about indexing your data](https://www.firebase.com/docs/security/guide/indexing-data.html).


## Including GeoFire in your project Android/Java

In order to use GeoFire in your project, you need to [add the Firebase Java
SDK](https://www.firebase.com/docs/java-quickstart.html?utm_source=geofire-java).
There are then multiple possibilities to use GeoFire in your project.

### Maven

Add GeoFire to your Maven enabled projects with a new dependency:

```xml
<dependency>
  <groupId>com.firebase</groupId>
  <artifactId>geofire</artifactId>
  <version>[1.0.0,)</version>
</dependency>
```

### Gradle

Likewise, you can add a dependency to your `gradle.build` file:

```groovy
dependencies {
    compile 'com.firebase:geofire:1.0.0+'
}
```

### Jar-File

You can also download the jar file from the latest release on the [releases
page](https://github.com/firebase/geofire-java/releases).


## Getting Started with Firebase

GeoFire requires Firebase in order to store location data. You can [sign up here for a free
account](https://www.firebase.com/signup/?utm_source=geofire-java).


## Quickstart

This is a quickstart on how to use GeoFire's core features. There is also a
[full API reference available online](https://geofire-java.firebaseapp.com/docs/).

### GeoFire

A `GeoFire` object is used to read and write geo location data to your Firebase
and to create queries. To create a new `GeoFire` instance you need to attach it to a Firebase
reference.

```java
GeoFire geoFire = new GeoFire(new Firebase("https://<your-firebase>.firebaseio.com/"));
```

Note that you can point your reference to anywhere in your Firebase, but don't
forget to [setup security rules for
GeoFire](https://github.com/firebase/geofire-js/blob/master/examples/securityRules/rules.json).

#### Setting location data

In GeoFire you can set and query locations by string keys. To set a location for
a key simply call the `setLocation` method. The method is passed a key
as a string and the location as a `GeoLocation` object containing the location's latitude and longitude:

```java
geoFire.setLocation("firebase-hq", new GeoLocation(37.7853889, -122.4056973));
```

To check if a write was successfully saved on the server, you can add a
`GeoFire.CompletionListener` to the `setLocation` call:

```java
geoFire.setLocation("firebase-hq", new GeoLocation(37.7853889, -122.4056973), new GeoFire.CompletionListener() {
    @Override
    public void onComplete(String key, FirebaseError error) {
        if (error != null) {
            System.err.println("There was an error saving the location to GeoFire: " + error);
        } else {
            System.out.println("Location saved on server successfully!");
        }
    }
});
```

To remove a location and delete it from Firebase simply pass the location's key to `removeKey`:

```java
geoFire.removeKey("firebase-hq");
```

#### Retrieving a location

Retrieving a location for a single key in GeoFire happens with callbacks:

```java
geoFire.getLocation("firebase-hq", new LocationCallback() {
    @Override
    public void onLocationResult(String key, GeoLocation location) {
        if (location != null) {
            System.out.println(String.format("The location for key %s is [%f,%f]", key, location.latitude, location.longitude));
        } else {
            System.out.println(String.format("There is no location for key %s in GeoFire", key));
        }
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {
        System.err.println("There was an error getting the GeoFire location: " + firebaseError);
    }
});
```

### Geo Queries

GeoFire allows you to query all keys within a geographic area using `GeoQuery`
objects. As the locations for keys change, the query is updated in ealtime and fires events
letting you know if any relevant keys have moved. `GeoQuery` parameters can be updated
later to change the size and center of the queried area.

```java
// creates a new query around [37.7832, -122.4056] with a radius of 0.6 kilometers
GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(37.7832, -122.4056), 0.6);
```

#### Receiving events for geo queries

There are five kinds of events that can occur with a geo query:

1. **Key Entered**: The location of a key now matches the query criteria.
2. **Key Exited**: The location of a key no longer matches the query criteria.
3. **Key Moved**: The location of a key changed but the location still matches the query criteria.
4. **Query Ready**: All current data has been loaded from the server and all
   initial events have been fired.
5. **Query Error**: There was an error while performing this query, e.g. a
   violation of security rules.

Key entered events will be fired for all keys initially matching the query as well as any time
afterwards that a key enters the query. Key moved and key exited events are guaranteed to be
preceded by a key entered event.

Sometimes you want to know when the data for all the initial keys has been
loaded from the server and the corresponding events for those keys have been
fired. For example, you may want to hide a loading animation after your data has
fully loaded. This is what the "ready" event is used for.

Note that locations might change while initially loading the data and key moved and key
exited events might therefore still occur before the ready event is fired.

When the query criteria is updated, the existing locations are re-queried and the
ready event is fired again once all events for the updated query have been
fired. This includes key exited events for keys that no longer match the query.

To listen for events you must add a `GeoQueryEventListener` to the `GeoQuery`:

```java
geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
    }

    @Override
    public void onKeyExited(String key) {
        System.out.println(String.format("Key %s is no longer in the search area", key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
    }

    @Override
    public void onGeoQueryReady() {
        System.out.println("All initial data has been loaded and events have been fired!");
    }

    @Override
    public void onGeoQueryError(FirebaseError error) {
        System.err.println("There was an error with this query: " + error);
    }
});
```

You can call either `removeGeoQueryEventListener` to remove a
single event listener or `removeAllListeners` to remove all event listeners
for a `GeoQuery`.

#### Updating the query criteria

The `GeoQuery` search area can be changed with `setCenter` and `setRadius`. Key
exited and key entered events will be fired for keys moving in and out of
the old and new search area, respectively. No key moved events will be
fired; however, key moved events might occur independently.

Updating the search area can be helpful in cases such as when you need to update
the query to the new visible map area after a user scrolls.


## API Reference

[A full API reference is available here](https://geofire-java.firebaseapp.com/docs/).


## Contributing

If you want to contribute to GeoFire for Java, clone the repository
and just start making pull requests.

```bash
git clone https://github.com/firebase/geofire-java.git
```
