package Fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import DataModel.User;
import Utils.CommonUtil;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;
import novitskyvitaly.geogroupproject.MainActivity;
import novitskyvitaly.geogroupproject.R;

public class MapFragment extends SupportMapFragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        View.OnClickListener,
        FirebaseUtil.IFirebaseUtilCallback {

    private static final String MY_TAG = "geog_mapFragment";

    private static View view;

    SupportMapFragment mapFragment;
    GoogleMap googleMap;
    Location lastLocation;

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

        if(view != null){
            ViewGroup parent = (ViewGroup)view.getParent();
            if(parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e){
            e.printStackTrace();
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (googleMap == null) {
            mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_map);
            if (mapFragment != null) mapFragment.getMapAsync(this);
        }
        InitFABs();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
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

    private void InitFABs(){
        fab_plus = (FloatingActionButton)getView().findViewById(R.id.fab_plus);
        fab_plus.setOnClickListener(this);
        fab_create_group = (FloatingActionButton)getView().findViewById(R.id.fab_create_group);
        fab_create_group.setOnClickListener(this);
        fab_join_group = (FloatingActionButton)getView().findViewById(R.id.fab_join_group);
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
            if(lastLocation != null)
                CenterMapOnPosition(lastLocation);
        }

    }

    public void CenterMapOnPosition(Location location) {
        lastLocation = location;
        if(location != null)
            AnimateCameraFocusOnLatLng(location, null);
    }

    private void AnimateCameraFocusOnLatLng(Location location, @Nullable Integer zoomLevel) {
        if (googleMap == null) return;
        CameraUpdate cameraUpdate
                = CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()),
                zoomLevel == null ? 15 : zoomLevel);
        googleMap.animateCamera(cameraUpdate);
    }

    //endregion

    //region Clicks

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fab_plus:
                if(isExpanded){
                    fab_join_group.startAnimation(fab_collapse_anim);
                    fab_create_group.startAnimation(fab_collapse_anim);
                    fab_plus.startAnimation(fab_x_to_plus_rotate_anim);
                    fab_join_group.setClickable(false);
                    fab_create_group.setClickable(false);
                    isExpanded = false;
                }else {
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

    private void JoinCreateGroupByActionCodeAndAuthType(int actionCode){
        //Log.i(MY_TAG, "JoinCreateGroupByActionCodeAndAuthType");
        if(mListener != null)
            mListener.openCreateJoinGroupFragment(actionCode);
    }

    //endregion

    //region Firebase


    @Override
    public void OnCheckAuthorizationCompleted(int actionCode, boolean isAuthorized, String nickname){
        if(isAuthorized)
            JoinCreateGroupByActionCodeAndAuthType(actionCode);
        else SwitchToLoginFragmentForActionCode(actionCode);
    }

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

    private void SwitchToLoginFragmentForActionCode(int actionCode){
        Log.i(MY_TAG, "SwitchToLoginFragmentForActionCode");
        mListener.showLoginFragmentForAction(actionCode);
    }

    public interface OnMapFragmentInteractionListener {
        void showLoginFragmentForAction(int actionCode);
        void openCreateJoinGroupFragment(int actionCode);
    }

    //endregion

}
