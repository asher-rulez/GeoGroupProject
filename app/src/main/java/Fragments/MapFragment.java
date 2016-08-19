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

import Utils.CommonUtil;
import novitskyvitaly.geogroupproject.R;

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, View.OnClickListener {

    private static final String MY_TAG = "geog_mapFragment";

    SupportMapFragment mapFragment;
    GoogleMap googleMap;

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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
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

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    public void CenterMapOnPosition(Location location) {
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
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnMapFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
