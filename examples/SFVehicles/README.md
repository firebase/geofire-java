## GeoFire - SF Vehicles Example

### Setup

The SF Vehicles example is based on Gradle. You can use Android Studio to import
the project.

#### Installing GeoFire to the local maven repository

The build script assumes GeoFire is installed in your local maven respository.
Install GeoFire from the source code by running

```sh
$> mvn -DskipTests clean install
```
in the root directory of GeoFire.

#### Adding Google Maps API key

Follow the instructions for [setting up Google Maps for
Android](https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key).
You need to obtain an API key and add it to the AndroidManifest.xml.
