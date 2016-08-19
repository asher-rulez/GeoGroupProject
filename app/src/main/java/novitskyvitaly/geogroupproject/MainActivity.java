package novitskyvitaly.geogroupproject;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

import Fragments.MapFragment;
import Utils.CommonUtil;
import Utils.GeoGroupBroadcastReceiver;
import Utils.SharedPreferencesUtil;

public class MainActivity extends AppCompatActivity
        implements //NavigationView.OnNavigationItemSelectedListener,
        MapFragment.OnMapFragmentInteractionListener, GeoGroupBroadcastReceiver.IBroadcastReceiverCallback {

    private final String MY_TAG = "geog_main_act";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    DrawerLayout drawer;
    Toolbar toolbar;
    ProgressDialog progressDialog = null;
    boolean isSideMenuOpened;

    private final int FRAGMENT_ID_MAP = 1;
    int currentFragmentID;
    MapFragment mapFragment;

    boolean isInternetAvailable = false;
    boolean isGoogleServiceAvailable = false;

    GeoGroupBroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitToolbar();
        InitDrawerSideMenu();

        if (savedInstanceState == null) {
            if (mapFragment == null)
                mapFragment = new MapFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fl_fragments_container, mapFragment);
            transaction.commit();
            currentFragmentID = FRAGMENT_ID_MAP;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!SharedPreferencesUtil.GetIsLocationUpdateServiceRunning(this))
            LocationListenerService.startLocationListenerService(this);
    }

    @Override
    protected void onStart() {
        CommonUtil.SetIsApplicationRunningInForeground(this, true);
        NotificationManager notificationManager
                = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        //if(!SharedPreferencesUtil.GetIsLocationUpdateServiceRunning(this)){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            CommonUtil.RequestLocationPermissions(this, 0);
        } else LocationListenerService.startLocationListenerService(this);
        //}
        if (broadcastReceiver == null) {
            broadcastReceiver = new GeoGroupBroadcastReceiver(this);
            IntentFilter filter = new IntentFilter(GeoGroupBroadcastReceiver.BROADCAST_REC_INTENT_FILTER);
            try{
                Log.i(MY_TAG, "trying to register broadcastReceiver");
                registerReceiver(broadcastReceiver, filter);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        isInternetAvailable = CheckInternetConnection();
        if (!isInternetAvailable)
            OnInternetNotConnected();
        isGoogleServiceAvailable = CheckPlayServices();
        if (!isGoogleServiceAvailable)
            OnGooglePlayServicesCheckError();

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        CommonUtil.SetIsApplicationRunningInForeground(this, false);
        if (broadcastReceiver == null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            if (broadcastReceiver == null) {
                unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        SharedPreferencesUtil.SetShouldStopService(this, true);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void InitToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void InitDrawerSideMenu() {
        drawer = (DrawerLayout) findViewById(R.id.drl_side_menu);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                isSideMenuOpened = false;
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                isSideMenuOpened = true;
            }
        };
        if (drawer != null) drawer.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @SuppressWarnings("StatementWithEmptyBody")
//    @Override
//    public boolean onNavigationItemSelected(MenuItem item) {
//        // Handle navigation view item clicks here.
//        int id = item.getItemId();
//
//        if (id == R.id.nav_camera) {
//            // Handle the camera action
//        } else if (id == R.id.nav_gallery) {
//
//        } else if (id == R.id.nav_slideshow) {
//
//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }
//
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
//        return true;
//    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //todo: replace with other method(s)
    }

    @Override
    public void onBroadcastReceived(Intent intent) {
        Log.i(MY_TAG, "some broadcast received");
        int actionCode = intent.getIntExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_ACTION_KEY, -1);
        switch (actionCode) {
            case GeoGroupBroadcastReceiver.ACTION_CODE_USER_LOCATION_RECEIVED:
                Location location = (Location) intent.getParcelableExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_LOCATION_REPORT_KEY);
                if (location != null && currentFragmentID == FRAGMENT_ID_MAP && mapFragment != null)
                    mapFragment.CenterMapOnPosition(location);
                break;
        }
    }

    protected boolean CheckInternetConnection() {
        Log.i(MY_TAG, "Checking internet connection...");
        return isConnectingToInternet();
    }

    public boolean isConnectingToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        }
        return false;
    }

    protected boolean CheckPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }

    public void OnGooglePlayServicesCheckError() {
    }

    public void OnInternetNotConnected() {
    }
}
