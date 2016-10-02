## GeoFire - SF Vehicles Example

**Please note: This example will not function as the Open Data set it depends on has been deprecated. It is left here for historical purposes.**

### Setup

The SF Vehicles example is based on Gradle. You can use Android Studio to import
the project. You will also need to register for and add a Google Maps API key.

#### Adding Google Maps API key

Follow the instructions for [setting up Google Maps for
Android](https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key).
You need to obtain an API key and add it to the [AndroidManifest.xml](https://github.com/firebase/geofire-java/blob/master/examples/SFVehicles/SF%20Vehicles/src/main/AndroidManifest.xml).


### About this sample

SF Vehicles loads realtime transit data from a
[transit open data](https://www.firebase.com/docs/open-data/transit.html) set maintained by [Firebase](https://firebase.com).

This sample loads its Firebase and GeoFire dependencies from Maven Central.
If you modify and build GeoFire locally make sure to update to modify the gradle file to load GeoFire
either directly or from your local maven repo.
