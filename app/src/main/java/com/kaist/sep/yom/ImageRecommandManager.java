package com.kaist.sep.yom;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by 조준완 on 2018-04-30.
 */

public class ImageRecommandManager {
    private LocationManager locationManager;
    private LocationListener locationListener;
    Context mContext;

    static float lat_now;
    static float lng_now;
    static Location now = new Location("point now");
    static Location old = new Location("point old");

    ImageRecommandManager(Context context) {
        mContext = context;
        Location currentLocation = null;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                lat_now = (float) location.getLatitude();
                lng_now = (float) location.getLongitude();

                now.setLatitude(lat_now);
                now.setLongitude(lng_now);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        getMyLocation();
    }

    private Location getMyLocation() {
        Location currentLocation = null;
        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 사용자 권한 요청
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Integer.REQUEST_CODE_LOCATION);
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            // 수동으로 위치 구하기
            String locationProvider = LocationManager.GPS_PROVIDER;
            currentLocation = locationManager.getLastKnownLocation(locationProvider);
            if (currentLocation != null) {
                lng_now = (float) currentLocation.getLongitude();
                lat_now = (float) currentLocation.getLatitude();

                now.setLatitude(lat_now);
                now.setLongitude(lng_now);
            }
        }
        return currentLocation;
    }

    public static boolean isNearby (String path) {
        float[] latLong = new float[2];
        try {
            ExifInterface exif = new ExifInterface(path);
            exif.getLatLong(latLong);
        } catch (Exception e) {
        }

        if (latLong[0] == 0f || latLong[1] == 0)
            return false;

        old.setLatitude(latLong[0]);
        old.setLongitude(latLong[1]);

        if (calDistance() < 500)
            return true;
        else
         return false;
    }

    public static float calDistance() {
        float dist = old.distanceTo(now);

        Log.e("[wan]", "old lat : " + old.getLatitude() + ", old long :" + old.getLongitude());
        Log.e("[wan]", "now lat : " + now.getLatitude() + ", old long :" + now.getLongitude());
        Log.e("[wan]", "dist : " + dist);

        return dist;
    }
}
