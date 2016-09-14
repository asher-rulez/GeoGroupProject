package Fragments;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
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
import android.util.Property;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import DataModel.Group;
import DataModel.User;
import DataModel.UserToGroupAssignment;
import Utils.CommonUtil;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;
import Utils.UIUtil;
import novitskyvitaly.geogroupproject.MainActivity;
import novitskyvitaly.geogroupproject.R;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class MapFragment extends SupportMapFragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        View.OnClickListener,
        FirebaseUtil.IFirebaseCheckAuthCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final String MY_TAG = "geog_mapFragment";

    private static View view;

    //SupportMapFragment mapFragment;
    MapView mapView;
    GoogleMap googleMap;
    LatLng lastLocation;
    Location locationFromService;

    Map<String, Marker> myMarkers;

    private Context appContext;
    private OnMapFragmentInteractionListener mListener;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static final int REQUEST_CODE_ASK_LOCATION_PERMISSION = 10;

    public MapFragment() {
        // Required empty public constructor
    }

    //region Fragment overrides

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = (MapView) view.findViewById(R.id.mv_map);
        mapView.onCreate(savedInstanceState);
        //mapView.onResume();
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        mListener.showFabsForMap();
        mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_map));
        mListener.SetMainToolbarGoToMapVisible(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        mListener.hideFabsOnMapPaused();
        SharedPreferencesUtil.SaveMapStateInSharedPrefs(getContext(), googleMap.getCameraPosition());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        SharedPreferencesUtil.ClearSavedMapState(getContext());
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
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

    //endregion

    //region Map

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            CommonUtil.RequestLocationPermissions(getActivity(), REQUEST_CODE_ASK_LOCATION_PERMISSION);
        } else SetMapProperties();

        if (getMyMarkers().size() > 0) {
            for (String key : getMyMarkers().keySet()) {
                Marker prevMarker = getMyMarkers().get(key);
                prevMarker.remove();
                getMyMarkers().remove(key);
            }
        }

        Bitmap markerIcon;
        BitmapDescriptor icon = null;
        markerIcon = UIUtil.decodeScaledBitmapFromDrawableResource(getResources(), R.drawable.map_marker_blue, 128, 128);
        icon = BitmapDescriptorFactory.fromBitmap(markerIcon);

        ArrayList<UserToGroupAssignment> utgas = mListener.getUTGAsForShowing();
        for (UserToGroupAssignment utga : utgas) {
            getMyMarkers().put(utga.getGroupID() + ":" + utga.getUserProfileID(),
                    AddMarker(utga.getLastReportedLatitude(), utga.getLastReportedLongitude(),
                            utga.getGroup().getName() + ":" + utga.getUser().getUsername(), icon));
        }
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
            CameraPosition cameraPosition = SharedPreferencesUtil.GetMapStateFromPrefsAsCameraPosition(getContext());
            if (cameraPosition != null)
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            else
                initLocationRequest();
        }
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void initLocationRequest() {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100000);

        if(getContext() == null)
            return;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        locationFromService = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {
        Log.i(MY_TAG, "got current location");
        SetLastLocation(location == null ? locationFromService : location, true);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    private void SetLastLocation(LatLng latLng) {
        if (latLng != null)
            SharedPreferencesUtil.SaveLocationInSharedPreferences(getContext(), latLng.latitude, latLng.longitude, new Date());
        lastLocation = latLng;
    }

    private void SetLastLocation(LatLng latLng, boolean ifCenterOnMyLocation) {
        SetLastLocation(latLng);
        if (ifCenterOnMyLocation)
            CheckIfLocationSavedInSPAndCenterOnIt();
    }

    private void SetLastLocation(Location location) {
        if (location != null)
            SharedPreferencesUtil.SaveLocationInSharedPreferences(getContext(), location.getLatitude(), location.getLongitude(), new Date());
        lastLocation = location == null ? null : new LatLng(location.getLatitude(), location.getLongitude());
    }

    private void SetLastLocation(Location location, boolean ifCenterOnMyLocation) {
        SetLastLocation(location);
        if (ifCenterOnMyLocation)
            CheckIfLocationSavedInSPAndCenterOnIt();
    }

    private void CheckIfLocationSavedInSPAndCenterOnIt() {
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
        if (myMarkers == null)
            myMarkers = new HashMap<>();
        return myMarkers;
    }

    public void AddMarkerForNewUser(User user, Group group, double latitude, double longitude) {
        Bitmap markerIcon;
        BitmapDescriptor icon = null;

        markerIcon = UIUtil.decodeScaledBitmapFromDrawableResource(getResources(), R.drawable.map_marker_blue, 128, 128);
        icon = BitmapDescriptorFactory.fromBitmap(markerIcon);

        if (googleMap != null)
            getMyMarkers().put(group.getGeneratedID() + ":" + user.getProfileID(), AddMarker(latitude, longitude, group.getName() + ":" + user.getUsername(), icon));
    }

    private Marker AddMarker(double latitude, double longtitude, String title, BitmapDescriptor icon) {
        MarkerOptions newMarker = new MarkerOptions().position(new LatLng(latitude, longtitude)).title(title).draggable(false);
        if (icon != null)
            newMarker.icon(icon);
        return googleMap.addMarker(newMarker);
    }

    public void MoveMarker(String username, String groupname, double latitude, double longitude) {
        Marker m = getMyMarkers().get(groupname + ":" + username);
        if (m != null) {
            LatLngInterpolator.Linear interpolator = new LatLngInterpolator.Linear();
            animateMarker(m, new LatLng(latitude, longitude), interpolator);
        }
    }

    static void animateMarker(Marker marker, LatLng newPosition, final LatLngInterpolator interpolator) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return interpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, newPosition);
        animator.setDuration(1500);
        animator.start();
    }

    public void RemoveMarker(String username, String groupname) {
        Marker m = getMyMarkers().get(groupname + ":" + username);
        if (m != null)
            m.remove();
    }

    //endregion

    //region Clicks

    @Override
    public void onClick(View view) {
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

    //endregion

    //region Interaction with parent activity

    @Override
    public void onCheckAuthorizationCompleted(int actionCode, boolean isAuthorized, String nickName) {
    }

    public interface OnMapFragmentInteractionListener extends ICommonFragmentInteraction {
        //        void showLoginFragmentForAction(int actionCode);
//
//        void openCreateJoinGroupFragment(int actionCode);
//
        void onMapFinishedLoading();

        void showFabsForMap();

        void hideFabsOnMapPaused();

        ArrayList<UserToGroupAssignment> getUTGAsForShowing();
    }

    //endregion

    interface LatLngInterpolator {
        public LatLng interpolate(float fraction, LatLng a, LatLng b);

        public class Linear implements LatLngInterpolator {
            @Override
            public LatLng interpolate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lng = (b.longitude - a.longitude) * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        }

        public class LinearFixed implements LatLngInterpolator {
            @Override
            public LatLng interpolate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lngDelta = b.longitude - a.longitude;

                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360;
                }
                double lng = lngDelta * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        }

        public class Spherical implements LatLngInterpolator {

            /* From github.com/googlemaps/android-maps-utils */
            @Override
            public LatLng interpolate(float fraction, LatLng from, LatLng to) {
                // http://en.wikipedia.org/wiki/Slerp
                double fromLat = toRadians(from.latitude);
                double fromLng = toRadians(from.longitude);
                double toLat = toRadians(to.latitude);
                double toLng = toRadians(to.longitude);
                double cosFromLat = cos(fromLat);
                double cosToLat = cos(toLat);

                // Computes Spherical interpolation coefficients.
                double angle = computeAngleBetween(fromLat, fromLng, toLat, toLng);
                double sinAngle = sin(angle);
                if (sinAngle < 1E-6) {
                    return from;
                }
                double a = sin((1 - fraction) * angle) / sinAngle;
                double b = sin(fraction * angle) / sinAngle;

                // Converts from polar to vector and interpolate.
                double x = a * cosFromLat * cos(fromLng) + b * cosToLat * cos(toLng);
                double y = a * cosFromLat * sin(fromLng) + b * cosToLat * sin(toLng);
                double z = a * sin(fromLat) + b * sin(toLat);

                // Converts interpolated vector back to polar.
                double lat = atan2(z, sqrt(x * x + y * y));
                double lng = atan2(y, x);
                return new LatLng(toDegrees(lat), toDegrees(lng));
            }

            private double computeAngleBetween(double fromLat, double fromLng, double toLat, double toLng) {
                // Haversine's formula
                double dLat = fromLat - toLat;
                double dLng = fromLng - toLng;
                return 2 * asin(sqrt(pow(sin(dLat / 2), 2) +
                        cos(fromLat) * cos(toLat) * pow(sin(dLng / 2), 2)));
            }
        }
    }
}
