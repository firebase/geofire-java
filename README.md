# GeoFire for Android/Java — Realtime location queries with Firebase

GeoFire is an open-source library for Android/Java that allows you to store and query a
set of keys based on their geographic location.

At its heart, GeoFire simply stores locations with string keys. Its main
benefit however, is the possibility of querying keys within a given geographic
area - all in realtime.

GeoFire uses [Firebase](https://www.firebase.com/) for data storage, allowing
query results to be updated in realtime as they change.  GeoFire *selectively
loads only the data near certain locations, keeping your applications light and
responsive*, even with extremely large datasets.

### Integrating GeoFire with your data

GeoFire is designed as a lightweight add-on to Firebase. However, to keep things
simple, GeoFire stores data in its own format and its own location within
your Firebase. This allows your existing data format and security rules to
remain unchanged and for you to add GeoFire as an easy solution for geo queries
without modifying your existing data.

#### Example
Assume you are building an app to rate bars and you store all information for a
bar, e.g. name, business hours and price range, at `/bars/<bar-id>`.  Later, you
want to add the possibility for users to search for bars in their vicinity. This
is where GeoFire comes in. You can store the location for each bar using
GeoFire, using the bar IDs as GeoFire keys. GeoFire then allows you to easily
query which bar IDs (the keys) are nearby. To display any additional information
about the bars, you can load the information for each bar returned by the query
at `/bars/<bar-id>`.

## GeoFire for Android/Java Beta

GeoFire for Android/Java is still in an open beta. It will be ready for your
production applications soon, but the API is subject to change until then.

## Including GeoFire in your project Android/Java

In order to use GeoFire in your project, you need to [add the Firebase
SDK](https://www.firebase.com/docs/java-quickstart.html). There are then
multiple possibilities to use GeoFire in your project.

### Maven
*TODO: add to maven repository*

### Jar-File
You can also download the jar file in the folder `dist` and add it directly to
your project.

## Quick Start

This is a quick start on how to use GeoFire's core features.  There is also a
[full API reference available
online](https://geofire-java.firebaseapp.com/docs/).

### GeoFire

A `GeoFire` object is used to read and write geo location data to your Firebase
and to create queries.

#### Creating a new GeoFire instance

To create a new `GeoFire` instance you need to attach it to a Firebase
reference.

```java
GeoFire geoFire = new GeoFire(new Firebase("https://<your-firebase>.firebaseio.com/"));
```
Note that you can point your reference to anywhere in your Firebase, but don't
forget to [setup security rules for
GeoFire](https://github.com/firebase/geofire/blob/master/examples/securityRules/rules.json).

#### Setting location data
In GeoFire you can set and query locations by key. To set a location for a key
simply call the `setLocation` method. The location method is passed a key as
string and the location as latitude and longitude doubles.

```java
geoFire.setLocation("firebase-hq", 37.7853889, -122.4056973);
```

To check if a write was successfully saved on the server, you can add a
`GeoFire.CompletionListener` to the `setLocation` call.
```java
geoFire.setLocation("firebase-hq", 37.7853889, -122.4056973, new GeoFire.CompletionListener() {
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

To remove a location and delete it from Firebase simply call
```java
geoFire.removeKey("firebase-hq");
```

#### Retrieving a location
Retrieving locations happens with listeners.  Like that, your app can always
stay up-to-date automatically.  Like with any Firebase reference, the listener
is called once for the initial position and then for every update of the
location. If the key is not present (or is removed from GeoFire) `onKeyRemoved`
is called.

```java
geoFire.addLocationEventListener("firebase-hq", new LocationEventListener() {
    @Override
    public void onLocationChanged(String key, double lat, double lng) {
        System.out.println(String.format("The location for key %s changed to [%f,%f]", key, lat, lng));
    }

    @Override
    public void onKeyRemoved(String key) {
        System.out.println(String.format("The location for key %s was removed", key));
    }

    @Override
    public void onCancelled(FirebaseError error) {
        System.err.println("There was an error reading data from Firebase: " + error);
    }
});
```

To stop receiving updates you can remove a single location listener or all
location listeners

```java
geoFire.removeEventListener("firebase-hq", eventListener); // remove event listener for single key
geoFire.removeEventListener(eventListener); // remove event listener for all keys
geoFire.removeAllEventListeners(); // remove all event listeners
```

### Geo Queries

GeoFire allows to query all keys within a geographic area using `GeoQuery`
objects. If locations for keys change the query will be updated in realtime (see
"Receiving events for geo queries" below). `GeoQuery` parameters can be updated
later to change the area that is queried.

```java
// creates a new query around [37.7832, -122.4056] with a radius of 0.6 kilometers
GeoQuery geoQuery = geoFire.queryAtLocation(37.7832, -122.4056, 0.6);
```

#### Receiving events for geo queries

There are 5 kind of events that can occur with a geo query:

1. **Key Entered**: The location of a key now matches the query criteria
2. **Key Exited**: The location of a key does not match the query criteria any more
3. **Key Moved**: The location of a key changed and the location still matches the query criteria
4. **Query Ready**: All current data has been loaded from the server and all
   initial events have been fired
5. **Query Error**: There was an error while performing this query, e.g. a
   violation of security rules.

Key entered events will be fired for all keys initially matching the query. Key
moved and key exited events are guaranteed to be preceded by a key entered
event.

The ready event is fired once the current data has been loaded from the server
and the initial key entered events for all keys currently within the query have
been fired. Note that locations might change while loading the data and key
moved and key exited events might therefor still occur before the ready event
was fired.  If the query criteria is updated, the ready event will be fired
again once all events for the new query criteria have been fired. This includes
key exited events for keys that no longer match the query.

To listen for events you must add a `GeoQueryEventListener` to the `GeoQuery`.
```java
geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
    @Override
    public void onKeyEntered(String key, double lat, double lng) {
        System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, lat, lng));
    }

    @Override
    public void onKeyExited(String key) {
        System.out.println(String.format("Key %s is no longer in the search area", key));
    }

    @Override
    public void onKeyMoved(String key, double lat, double lng) {
        System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, lat, lng));
    }

    @Override
    public void onGeoQueryReady() {
        System.out.println("All initial key entered events have been fired!");
    }

    @Override
    public void onGeoQueryError(FirebaseError error) {
        System.err.println("There was an error with this query: " + error);
    }
});
```

To remove the listener call either `removeGeoQueryEventListener` to remove a
single event listener, or `removeAllListeners` to remove all event listeners
for a `GeoQuery`.

#### Updating the query criteria

The GeoQuery search area can be changed with `setCenter` and `setRadius` Key
exited and key entered events will be fired for keys moving in and out of
the old and new search area respectively. No key moved events will be
fired, however key moved events might occur independently.

Updating the search area can be helpful for e.g. updating the query to the new
visible map area after a user scrolls.

## API Reference

[A full API reference is available here](https://geofire-java.firebaseapp.com/docs/).

## Contributing

If you want to contribute to GeoFire for Java, just can clone the repository
and just start making pull requests.
```bash
git clone https://github.com/firebase/geofire-java.git
```
