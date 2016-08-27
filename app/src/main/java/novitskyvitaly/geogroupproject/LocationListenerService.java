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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.OnDisconnect;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;

import DataModel.UserLocationReport;
import DataModel.UserToGroupAssignment;
import Utils.CommonUtil;
import Utils.FirebaseUtil;
import Utils.GeoGroupBroadcastReceiver;
import Utils.SharedPreferencesUtil;

public class LocationListenerService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final String MY_TAG = "geog_lls";

    public static boolean IsServiceRunning;
    public static boolean IsLocationListenerConnected;

    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    public static void startLocationListenerService(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), LocationListenerService.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mGoogleApiClient == null)
            buildGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferencesUtil.SetIsLocationUpdateServiceRunning(this, true);
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        IsServiceRunning = true;
        return Service.START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        IsServiceRunning = false;
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
        if (SharedPreferencesUtil.GetShouldStopService(this)) {
            Log.i(MY_TAG, "stop location service by sp value");
            SharedPreferencesUtil.SetShouldStopService(this, false);
            stopSelf();
            return;
        }
        Log.i(MY_TAG, "onConnected");
        IsLocationListenerConnected = true;
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(SharedPreferencesUtil.GetLocationRefreshFrequency(this));

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(GeoGroupBroadcastReceiver.BROADCAST_REC_INTENT_FILTER);
            intent.putExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_ACTION_KEY,
                    GeoGroupBroadcastReceiver.ACTION_CODE_LOCATION_PERMISSION_NEEDED);
            sendBroadcast(intent);
            SharedPreferencesUtil.SetIsLocationUpdateServiceRunning(this, false);
            this.stopSelf();
            return;
        }
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null)
            onLocationChanged(mLastLocation);
    }

    @Override
    public void onConnectionSuspended(int i) {
        IsLocationListenerConnected = false;
        Log.i(MY_TAG, "onConnectionSuspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        IsLocationListenerConnected = true;
        if (location == null) return;
        Log.i(MY_TAG, "onLocationChanged");
        SendLocationUpdatesToFirebase(location);
        SharedPreferencesUtil.SaveLocationInSharedPreferences(
                this, mLastLocation.getLatitude(), mLastLocation.getLongitude(), new Date());
        Intent intent = new Intent(GeoGroupBroadcastReceiver.BROADCAST_REC_INTENT_FILTER);
        intent.putExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_ACTION_KEY,
                GeoGroupBroadcastReceiver.ACTION_CODE_USER_LOCATION_RECEIVED);
        intent.putExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_LOCATION_REPORT_KEY, location);
        sendBroadcast(intent);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        buildGoogleApiClient();
        IsLocationListenerConnected = false;
    }

    private void SendLocationUpdatesToFirebase(final Location location){
        final Query myAssignmentsToGroups = FirebaseUtil.GetMyGroupsQuery(this);
        myAssignmentsToGroups.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ds) {
                if(!ds.hasChildren()){
                    Log.e(MY_TAG, "got no group assignments");
                    return;
                }
                for(DataSnapshot dataSnapshot : ds.getChildren()){
                    UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    if(utga == null){
                        Log.e(MY_TAG, "got null utga from query");
                        return;
                    }
                    utga.setLastReportedLatitude(location.getLatitude());
                    utga.setLastReportedLongitude(location.getLongitude());
                    FirebaseDatabase.getInstance().getReference()
                            .child(getString(R.string.firebase_user_to_group_assignment))
                            .child(dataSnapshot.getKey())
                            .setValue(utga, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if(databaseError != null)
                                        databaseError.toException().printStackTrace();
                                }
                            });
                    UserLocationReport locationReport = new UserLocationReport();
                    locationReport.setGroupID(utga.getGroupID());
                    locationReport.setUserProfileID(utga.getUserProfileID());
                    locationReport.setLat(location.getLatitude());
                    locationReport.setLng(location.getLongitude());
                    locationReport.setCreatedUnixTime(new Date().getTime());
                    FirebaseDatabase.getInstance().getReference()
                            .child(getString(R.string.firebase_location_reports))
                            .push()
                            .setValue(locationReport, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if(databaseError != null)
                                        databaseError.toException().printStackTrace();
                                }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }
}









