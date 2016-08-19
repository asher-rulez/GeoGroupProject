package novitskyvitaly.geogroupproject;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Date;

import Utils.CommonUtil;
import Utils.GeoGroupBroadcastReceiver;
import Utils.SharedPreferencesUtil;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class LocationListenerService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final String MY_TAG = "geog_lls";

    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    String lat, lon;

    public static void startLocationListenerService(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), LocationListenerService.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(mGoogleApiClient == null)
            buildGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferencesUtil.SetIsLocationUpdateServiceRunning(this, true);
        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        return Service.START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(SharedPreferencesUtil.GetShouldStopService(this)){
            stopSelf();
            return;
        }
        Log.i(MY_TAG, "onConnected");
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100); // Update location every second

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(GeoGroupBroadcastReceiver.BROADCAST_REC_INTENT_FILTER);
            intent.putExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_ACTION_KEY,
                    GeoGroupBroadcastReceiver.ACTION_CODE_LOCATION_PERMISSION_NEEDED);
            sendBroadcast(intent);
            SharedPreferencesUtil.SetIsLocationUpdateServiceRunning(this,false);
            this.stopSelf();
            return;
        }
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if(mLastLocation != null)
            onLocationChanged(mLastLocation);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(MY_TAG, "onConnectionSuspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(MY_TAG, "onLocationChanged");
        long a=SharedPreferencesUtil.GetLastLocationSavedDateTimeInMillis(this);
                long b=SharedPreferencesUtil.GetLocationRefreshFrequency(this);
                        long c=new Date().getTime();
        if(a
                + b
                        <= c) {
            SharedPreferencesUtil.SaveLocationInSharedPreferences(
                    this, mLastLocation.getLatitude(), mLastLocation.getLongitude(), new Date());
            Intent intent = new Intent(GeoGroupBroadcastReceiver.BROADCAST_REC_INTENT_FILTER);
            intent.putExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_ACTION_KEY,
                    GeoGroupBroadcastReceiver.ACTION_CODE_USER_LOCATION_RECEIVED);
            intent.putExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_LOCATION_REPORT_KEY, location);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }
}
