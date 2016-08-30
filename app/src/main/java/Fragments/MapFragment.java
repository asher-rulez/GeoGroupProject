package Fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import DataModel.User;
import DataModel.UserToGroupAssignment;
import Utils.CommonUtil;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;
import Utils.UIUtil;
import novitskyvitaly.geogroupproject.MainActivity;
import novitskyvitaly.geogroupproject.R;

public class MapFragment extends SupportMapFragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        View.OnClickListener,
        FirebaseUtil.IFirebaseCheckAuthCallback {

    private static final String MY_TAG = "geog_mapFragment";

    private static View view;

    //SupportMapFragment mapFragment;
    MapView mapView;
    GoogleMap googleMap;
    LatLng lastLocation;

    Map<String, Marker> myMarkers;

    private Context appContext;
    private OnMapFragmentInteractionListener mListener;

    private static final int REQUEST_CODE_ASK_LOCATION_PERMISSION = 10;

    FloatingActionButton fab_plus;
    FloatingActionButton fab_create_group;
    FloatingActionButton fab_join_group;
    boolean isExpanded = false;
    Animation fab_appear_anim;
    Animation fab_collapse_anim;
    Animation fab_plus_to_x_rotate_anim;
    Animation fab_x_to_plus_rotate_anim;

    public MapFragment() {
        // Required empty public constructor
    }

    //region Fragment overrides

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = (MapView)view.findViewById(R.id.mv_map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        try{
            MapsInitializer.initialize(getActivity().getApplicationContext());
        }catch (Exception e){
            e.printStackTrace();
        }
        mapView.getMapAsync(this);

/*
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {
            e.printStackTrace();
        }
*/
        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
/*
        if (googleMap == null) {
            mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_map);
            if (mapFragment != null) mapFragment.getMapAsync(this);
        }
*/
        InitFABs();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMapFragmentInteractionListener) {
            mListener = (OnMapFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        appContext = context.getApplicationContext();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    //endregion

    //region Controls init

    private void InitFABs() {
        fab_plus = (FloatingActionButton) getView().findViewById(R.id.fab_plus);
        fab_plus.setOnClickListener(this);
        fab_create_group = (FloatingActionButton) getView().findViewById(R.id.fab_create_group);
        fab_create_group.setOnClickListener(this);
        fab_join_group = (FloatingActionButton) getView().findViewById(R.id.fab_join_group);
        fab_join_group.setOnClickListener(this);
        fab_appear_anim = AnimationUtils.loadAnimation(getContext(), R.anim.fab_appear);
        fab_collapse_anim = AnimationUtils.loadAnimation(getContext(), R.anim.fab_collapse);
        fab_plus_to_x_rotate_anim = AnimationUtils.loadAnimation(getContext(), R.anim.fab_rotate_plus_to_x);
        fab_x_to_plus_rotate_anim = AnimationUtils.loadAnimation(getContext(), R.anim.fab_rotate_x_to_plus);
    }

    //endregion

    //region Map

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            CommonUtil.RequestLocationPermissions(getActivity(), REQUEST_CODE_ASK_LOCATION_PERMISSION);
        } else SetMapProperties();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SetMapProperties();
    }

    private void SetMapProperties() {
        if (googleMap != null) {
            if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
            googleMap.setOnMarkerClickListener(this);
            googleMap.setOnInfoWindowClickListener(this);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            TryGetLocationAndCenterMap();
        }
    }

    private void TryGetLocationAndCenterMap() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                final LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                final Location location = null;
                if (locationManager == null) {
                    Log.e(MY_TAG, "cant get location manager!");
                    return null;
                }
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (location != null)
                            SetLastLocation(location, true);
                    }

                    @Override
                    public void onProviderDisabled(String s) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                SetLastLocation(location, true);
                            }

                            @Override
                            public void onStatusChanged(String s, int i, Bundle bundle) {
                            }

                            @Override
                            public void onProviderEnabled(String s) {
                            }

                            @Override
                            public void onProviderDisabled(String s) {
                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (loc != null) {
                                    SetLastLocation(loc, true);
                                    return;
                                }
                                loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                if (loc != null){
                                    SetLastLocation(loc, true);
                                    return;
                                }
                                CheckIfLocationSavedInSPAndCenterOnIt();
                            }
                        }, Looper.getMainLooper());
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {
                    }

                    @Override
                    public void onProviderEnabled(String s) {
                    }

                }, Looper.getMainLooper());
                return null;
            }
        }.execute();
    }

    private void SetLastLocation(LatLng latLng) {
        if(latLng != null)
            SharedPreferencesUtil.SaveLocationInSharedPreferences(getContext(), latLng.latitude, latLng.longitude, new Date());
        lastLocation = latLng;
    }
    private void SetLastLocation(LatLng latLng, boolean ifCenterOnMyLocation){
        SetLastLocation(latLng);
        if(ifCenterOnMyLocation)
            CheckIfLocationSavedInSPAndCenterOnIt();
    }

    private void SetLastLocation(Location location) {
        if(location != null)
            SharedPreferencesUtil.SaveLocationInSharedPreferences(getContext(), location.getLatitude(), location.getLongitude(), new Date());
        lastLocation = location == null ? null : new LatLng(location.getLatitude(), location.getLongitude());
    }
    private void SetLastLocation(Location location, boolean ifCenterOnMyLocation){
        SetLastLocation(location);
        if(ifCenterOnMyLocation)
            CheckIfLocationSavedInSPAndCenterOnIt();
    }

    private void CheckIfLocationSavedInSPAndCenterOnIt(){
        if (lastLocation == null) {
            LatLng latLngSP = SharedPreferencesUtil.GetLastLocationLatLng(getContext());
            if (latLngSP != null)
                SetLastLocation(latLngSP);
        }
        CenterMapOnPosition(lastLocation);
    }

    public void CenterMapOnPosition(Location location) {
        if (location != null)
            AnimateCameraFocusOnLatLng(location.getLatitude(), location.getLongitude(), null);
    }

    public void CenterMapOnPosition(LatLng latLng) {
        if (latLng != null)
            AnimateCameraFocusOnLatLng(latLng.latitude, latLng.longitude, null);
    }

    private void AnimateCameraFocusOnLatLng(double lat, double lng, @Nullable Integer zoomLevel) {
        if (googleMap == null) return;
        CameraUpdate cameraUpdate
                = CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lng),
                zoomLevel == null ? 15 : zoomLevel);
        googleMap.animateCamera(cameraUpdate);
    }

    public Map<String, Marker> getMyMarkers() {
        if(myMarkers == null)
            myMarkers = new HashMap<>();
        return myMarkers;
    }

    public void AddMarkerForNewUser(String username, String groupname, double latitude, double longitude){
        Bitmap markerIcon;
        BitmapDescriptor icon = null;

        markerIcon = UIUtil.decodeScaledBitmapFromDrawableResource(getResources(), R.drawable.map_marker_blue, 128, 128);
        icon = BitmapDescriptorFactory.fromBitmap(markerIcon);

        if(googleMap != null)
            getMyMarkers().put(groupname + ":" + username, AddMarker(latitude, longitude, groupname + ":" + username, icon));
    }

    private Marker AddMarker(double latitude, double longtitude, String title, BitmapDescriptor icon) {
        MarkerOptions newMarker = new MarkerOptions().position(new LatLng(latitude, longtitude)).title(title).draggable(false);
        if (icon != null)
            newMarker.icon(icon);
        return googleMap.addMarker(newMarker);
    }

    public void MoveMarker(String username, String groupname, double latitude, double longitude){
        Marker m = getMyMarkers().get(groupname + ":" + username);
        if(m != null)
            m.setPosition(new LatLng(latitude, longitude));
    }

    public void RemoveMarker(String username, String groupname){
        Marker m = getMyMarkers().get(groupname + ":" + username);
        if(m != null)
            m.remove();
    }

    //endregion

    //region Clicks

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_plus:
                final View.OnClickListener clickListener = this;
                if (isExpanded) {
                    fab_x_to_plus_rotate_anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            fab_plus.setOnClickListener(null);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            fab_plus.setOnClickListener(clickListener);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    fab_join_group.startAnimation(fab_collapse_anim);
                    fab_create_group.startAnimation(fab_collapse_anim);
                    fab_plus.startAnimation(fab_x_to_plus_rotate_anim);
                    fab_join_group.setClickable(false);
                    fab_create_group.setClickable(false);
                    isExpanded = false;
                } else {
                    fab_plus_to_x_rotate_anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            fab_plus.setOnClickListener(null);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            fab_plus.setOnClickListener(clickListener);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    fab_join_group.startAnimation(fab_appear_anim);
                    fab_create_group.startAnimation(fab_appear_anim);
                    fab_plus.startAnimation(fab_plus_to_x_rotate_anim);
                    fab_join_group.setClickable(true);
                    fab_create_group.setClickable(true);
                    isExpanded = true;
                }
                break;
            case R.id.fab_create_group:
                FirebaseUtil.CheckAuthForActionCode(getContext(), MainActivity.ACTION_CODE_FOR_CREATE_GROUP, this);
                break;
            case R.id.fab_join_group:
                FirebaseUtil.CheckAuthForActionCode(getContext(), MainActivity.ACTION_CODE_FOR_JOIN_GROUP, this);
                break;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    //endregion

    //region Join/create group

    private void JoinCreateGroupByActionCodeAndAuthType(int actionCode) {
        //Log.i(MY_TAG, "JoinCreateGroupByActionCodeAndAuthType");
        if (mListener != null)
            mListener.openCreateJoinGroupFragment(actionCode);
    }

    //endregion

    //region Firebase

//    private boolean CheckIfAuthorizedToFirebase(){
//        FirebaseAuth firebaseAuthorization = FirebaseAuth.getInstance();
//        firebaseAuthorization.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                FirebaseUser user = firebaseAuth.getCurrentUser()
//            }
//        });
//    }

    //endregion

    //region Interaction with parent activity

    private void SwitchToLoginFragmentForActionCode(int actionCode) {
        Log.i(MY_TAG, "SwitchToLoginFragmentForActionCode");
        mListener.showLoginFragmentForAction(actionCode);
    }

    @Override
    public void onCheckAuthorizationCompleted(int actionCode, boolean isAuthorized, String nickName) {
        if (isAuthorized)
            JoinCreateGroupByActionCodeAndAuthType(actionCode);
        else SwitchToLoginFragmentForActionCode(actionCode);
    }

    public interface OnMapFragmentInteractionListener {
        void showLoginFragmentForAction(int actionCode);

        void openCreateJoinGroupFragment(int actionCode);

        void onMapFinishedLoading();
    }

    //endregion

}
