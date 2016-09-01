package novitskyvitaly.geogroupproject;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import DataModel.Group;
import DataModel.GroupCommonEvent;
import DataModel.User;
import DataModel.UserStatusUpdate;
import DataModel.UserToGroupAssignment;
import Fragments.CreateJoinGroupFragment;
import Fragments.LoadingFragment;
import Fragments.LoginFragment;
import Fragments.MapFragment;
import Fragments.SettingsFragment;
import Utils.CommonUtil;
import Utils.FirebaseUtil;
import Utils.GeoGroupBroadcastReceiver;
import Utils.SharedPreferencesUtil;

public class MainActivity extends AppCompatActivity
        implements //NavigationView.OnNavigationItemSelectedListener,
        MapFragment.OnMapFragmentInteractionListener,
        GeoGroupBroadcastReceiver.IBroadcastReceiverCallback,
        LoginFragment.OnLoginFragmentInteractionListener,
        CreateJoinGroupFragment.OnCreateJoinGroupInteractionListener, FirebaseUtil.IFirebaseCheckAuthCallback, View.OnClickListener {

    private final String MY_TAG = "geog_main_act";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    boolean isInternetAvailable = false;
    boolean isGoogleServiceAvailable = false;
    GeoGroupBroadcastReceiver broadcastReceiver;

    //region UI variables

    DrawerLayout drawer;
    Toolbar toolbar;
    ProgressDialog progressDialog = null;
    boolean isSideMenuOpened;

    TextView tv_nav_header_nickname;
    TextView tv_nav_header_profile;
    ImageView iv_nav_header_image;

    Button btn_side_menu_groups;
    Button btn_side_menu_events;
    Button btn_side_menu_settings;
    Button btn_side_menu_about;


    FloatingActionButton fab_plus;
    FloatingActionButton fab_create_group;
    FloatingActionButton fab_join_group;
    boolean isExpanded = false;
    Animation fab_appear_anim;
    Animation fab_collapse_anim;
    Animation fab_plus_to_x_rotate_anim;
    Animation fab_x_to_plus_rotate_anim;

    //endregion

    //region fragments variables

    public static final int ACTION_CODE_FOR_JOIN_GROUP = 11;
    public static final int ACTION_CODE_FOR_CREATE_GROUP = 12;
    public static final int ACTION_CODE_INITIAL_GROUPS_CHECK = 13;
    public static final int ACTION_CODE_START_SCREEN_ON_STARTUP = 14;
    public static final int ACTION_CODE_START_LOCATION_REPORT_SERVICE = 15;

    int currentFragmentID;
    private final int FRAGMENT_ID_MAP = 1;
    private final int FRAGMENT_ID_LOGIN = 2;
    private final int FRAGMENT_ID_JOINCREATE = 3;
    private final int FRAGMENT_ID_LOADING = 4;
    private final int FRAGMENT_ID_SETTINGS = 5;

    SettingsFragment settingsFragment;
    MapFragment mapFragment;
    LoginFragment loginFragment;
    CreateJoinGroupFragment createJoinFragment;
    LoadingFragment loadingFragment;

    private int scheduledFragmentID = -1;
    private int scheduledActionCodeForFragmentSwitch = -1;

    //endregion

    //region variables - runtime objects and event listeners

    Query myGroupsQuery;

    Map<String, Group> myGroupsDictionary;
    Map<String, User> usersDictionary;
    Map<String, Query> usersByGroupKeyQueries;

    ChildEventListener myGroupsAssignmentsListener;
    ChildEventListener userAssignmentsToMyGroupsListener;
    ChildEventListener commonEventsOfMyGroupsListener;
    ChildEventListener userStatusUpdatesListener;

    //endregion

    //region Activity overrides

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitToolbar();
        InitDrawerSideMenu();
        InitSideMenuControls();
        InitFABs();

        SwitchToLoadingFragment();
//        if (savedInstanceState != null) {
//        }

        //final FirebaseUtil.IFirebaseCheckAuthCallback authListener = this;
        //geoGroupFirebaseRef = FirebaseDatabase.getInstance().getReference();
        if (SharedPreferencesUtil.GetFCMTokenFromSharedPreferences(this).equals(""))
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    String token = FirebaseInstanceId.getInstance().getToken();
                    if (token != null) {
                        Log.i(MY_TAG, "got token: " + token);
                        SharedPreferencesUtil.SaveFCMTokenInSharedPreferences(getApplicationContext(), token);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    RequestPermissionsBeforeCheckingAuth();
                    //CheckAuthorization(authListener);
                }
            }.execute();
        else RequestPermissionsBeforeCheckingAuth();//CheckAuthorization(authListener);
    }

    private void CheckAuthorization(final FirebaseUtil.IFirebaseCheckAuthCallback callbackListener) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                FirebaseUtil.CheckAuthForActionCode(getApplicationContext(), ACTION_CODE_START_SCREEN_ON_STARTUP, callbackListener);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void RequestPermissionsBeforeCheckingAuth() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            CommonUtil.RequestLocationPermissions(this, ACTION_CODE_START_SCREEN_ON_STARTUP);
        } else CheckAuthorization(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ACTION_CODE_START_SCREEN_ON_STARTUP:
                CheckAuthorization(this);
                break;
            case ACTION_CODE_START_LOCATION_REPORT_SERVICE:
                if (!LocationListenerService.IsServiceRunning)
                    LocationListenerService.startLocationListenerService(this);
                break;
        }

    }

    @Override
    protected void onStart() {
        CommonUtil.SetIsApplicationRunningInForeground(this, true);
        NotificationManager notificationManager
                = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        if (broadcastReceiver == null) {
            broadcastReceiver = new GeoGroupBroadcastReceiver(this);
            IntentFilter filter = new IntentFilter(GeoGroupBroadcastReceiver.BROADCAST_REC_INTENT_FILTER);
            try {
                Log.i(MY_TAG, "trying to register broadcastReceiver");
                registerReceiver(broadcastReceiver, filter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onStart();

        if (scheduledFragmentID != -1) {
            switch (scheduledFragmentID) {
                case FRAGMENT_ID_LOGIN:
                    if (scheduledActionCodeForFragmentSwitch == -1) {
                        Log.e(MY_TAG, "unexpected value of scheduledActionCodeForFragmentSwitch (FRAGMENT_ID_LOGIN)");
                        return;
                    }
                    SwitchToLoginFragment(scheduledActionCodeForFragmentSwitch);
                    break;
                case FRAGMENT_ID_MAP:
                    SwitchToMapFragment();
                    break;
                case FRAGMENT_ID_JOINCREATE:
                    if (scheduledActionCodeForFragmentSwitch == -1) {
                        Log.e(MY_TAG, "unexpected value of scheduledActionCodeForFragmentSwitch (FRAGMENT_JOINCREATE)");
                        return;
                    }
                    SwitchToCreateJoinFragment(scheduledActionCodeForFragmentSwitch);
                    break;
            }
        }
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
        SharedPreferencesUtil.ClearLastLocationSavedDateTimeInMillis(this);
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
        } catch (Exception e) {
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
            if (currentFragmentID == FRAGMENT_ID_MAP) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                if (currentFragmentID == FRAGMENT_ID_JOINCREATE && createJoinFragment != null)
                    createJoinFragment.ClearFields();
                super.onBackPressed();
            }
        }
    }

    //endregion

    //region controls init

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

    private void InitSideMenuControls() {
        iv_nav_header_image = (ImageView) findViewById(R.id.iv_nav_header_image);
        tv_nav_header_nickname = (TextView) findViewById(R.id.tv_nav_header_nickname);
        tv_nav_header_profile = (TextView) findViewById(R.id.tv_nav_header_profile);

        final float scale = getResources().getDisplayMetrics().density;
        int pxSize = (int) (34 * scale + 0.5f);

        btn_side_menu_groups = (Button) findViewById(R.id.btn_side_menu_groups);
        Drawable img = getResources().getDrawable(R.drawable.logo_gray_side_menu);
        img.setBounds(0, 0, pxSize, pxSize);
        btn_side_menu_groups.setCompoundDrawables(img, null, null, null);
        btn_side_menu_groups.setOnClickListener(this);

        btn_side_menu_events = (Button) findViewById(R.id.btn_side_menu_events);
        img = getResources().getDrawable(R.drawable.nav_menu_events);
        img.setBounds(0, 0, pxSize, pxSize);
        btn_side_menu_events.setCompoundDrawables(img, null, null, null);
        btn_side_menu_events.setOnClickListener(this);

        btn_side_menu_settings = (Button) findViewById(R.id.btn_side_menu_settings);
        img = getResources().getDrawable(R.drawable.nav_menu_settings);
        img.setBounds(0, 0, pxSize, pxSize);
        btn_side_menu_settings.setCompoundDrawables(img, null, null, null);
        btn_side_menu_settings.setOnClickListener(this);

        btn_side_menu_about = (Button) findViewById(R.id.btn_side_menu_about);
        img = getResources().getDrawable(R.drawable.nav_menu_about);
        img.setBounds(0, 0, pxSize, pxSize);
        btn_side_menu_about.setCompoundDrawables(img, null, null, null);
        btn_side_menu_about.setOnClickListener(this);
    }

    private void InitFABs() {
        fab_plus = (FloatingActionButton) findViewById(R.id.fab_plus);
        fab_plus.setOnClickListener(this);
        fab_create_group = (FloatingActionButton) findViewById(R.id.fab_create_group);
        fab_create_group.setOnClickListener(this);
        fab_join_group = (FloatingActionButton) findViewById(R.id.fab_join_group);
        fab_join_group.setOnClickListener(this);
        fab_appear_anim = AnimationUtils.loadAnimation(this, R.anim.fab_appear);
        fab_collapse_anim = AnimationUtils.loadAnimation(this, R.anim.fab_collapse);
        fab_plus_to_x_rotate_anim = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_plus_to_x);
        fab_x_to_plus_rotate_anim = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_x_to_plus);
    }

    private void SetFabsVisible(boolean isVisible) {
        if (fab_plus != null)
            fab_plus.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        if (fab_create_group != null)
            fab_create_group.setVisibility(isVisible && isExpanded ? View.VISIBLE : View.GONE);
        if (fab_join_group != null)
            fab_join_group.setVisibility(isVisible && isExpanded ? View.VISIBLE : View.GONE);
    }

    //endregion

    //region dialogs


    //endregion

    //region broadcast listener

    @Override
    public void onBroadcastReceived(Intent intent) {
        Log.i(MY_TAG, "some broadcast received");
        int actionCode = intent.getIntExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_ACTION_KEY, -1);
        switch (actionCode) {
            case GeoGroupBroadcastReceiver.ACTION_CODE_USER_LOCATION_RECEIVED:
//                Location location = (Location) intent.getParcelableExtra(GeoGroupBroadcastReceiver.BROADCAST_EXTRA_LOCATION_REPORT_KEY);
//                if (location != null && currentFragmentID == FRAGMENT_ID_MAP && mapFragment != null)
//                    mapFragment.CenterMapOnPosition(location);
                //todo: report location to fbdb
                break;
        }
    }

    //endregion

    //region check internet and play services

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

    //endregion

    //region fragments and callbacks

    private void ScheduleSwitchToFragment(int fragmentIDToSwitch, @Nullable Integer actionCode) {
        scheduledFragmentID = fragmentIDToSwitch;
        if (actionCode != null)
            scheduledActionCodeForFragmentSwitch = actionCode;
    }

    private void SwitchToMapFragment() {
        if (CommonUtil.GetIsApplicationRunningInForeground(this)) {
            if (mapFragment == null)
                mapFragment = new MapFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fl_fragments_container, mapFragment);
            for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
                getSupportFragmentManager().popBackStackImmediate();
            }
            transaction.commit();
            SetFabsVisible(true);
            currentFragmentID = FRAGMENT_ID_MAP;
        } else ScheduleSwitchToFragment(FRAGMENT_ID_MAP, null);
    }

    private void SwitchToLoginFragment(int actionCode) {
        if (CommonUtil.GetIsApplicationRunningInForeground(this)) {
            if (loginFragment == null)
                loginFragment = new LoginFragment();
            loginFragment.SetAfterLoginAction(actionCode);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fl_fragments_container, loginFragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.addToBackStack("login");
            transaction.commit();
            SetFabsVisible(false);
            currentFragmentID = FRAGMENT_ID_LOGIN;
        } else ScheduleSwitchToFragment(FRAGMENT_ID_LOGIN, actionCode);
    }

    private void SwitchToCreateJoinFragment(int actionCode) {
        if (CommonUtil.GetIsApplicationRunningInForeground(this)) {
            if (createJoinFragment == null)
                createJoinFragment = new CreateJoinGroupFragment();
            createJoinFragment.SetAction(actionCode);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fl_fragments_container, createJoinFragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.addToBackStack("createJoin");
            transaction.commit();
            SetFabsVisible(false);
            currentFragmentID = FRAGMENT_ID_JOINCREATE;
        } else ScheduleSwitchToFragment(FRAGMENT_ID_JOINCREATE, actionCode);
    }

    private void SwitchToLoadingFragment() {
        if (loadingFragment == null)
            loadingFragment = new LoadingFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fl_fragments_container, loadingFragment);
        transaction.commit();
        SetFabsVisible(false);
        currentFragmentID = FRAGMENT_ID_LOADING;
    }

    @Override
    public void onLoginMade(int afterLoginAction) {
        SwitchToMapFragment();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                StartTrackingFirebaseDatabase();
                return null;
            }
        }.execute();
        //todo:continue to action
    }

    public void showLoginFragmentForAction(int actionCode) {
        SwitchToLoginFragment(actionCode);
    }

    public void openCreateJoinGroupFragment(int actionCode) {
        SwitchToCreateJoinFragment(actionCode);
    }

    @Override
    public void onMapFinishedLoading() {
        FirebaseUtil.CheckAuthForActionCode(this, ACTION_CODE_INITIAL_GROUPS_CHECK, this);
    }

    @Override
    public void onCancelCreateJoinGroup() {
        onBackPressed();
    }

    @Override
    public void onSuccessCreateJoinGroup(String groupID, String groupPassword, boolean ifSendData) {
        SwitchToMapFragment();

        if (ifSendData && !TextUtils.isEmpty(groupID) && !TextUtils.isEmpty(groupPassword)) {
            Intent smsIntent = new Intent(Intent.ACTION_SEND);
            smsIntent.setType("text/plain");
            //smsIntent.setData(Uri.parse("smsto:"));
            smsIntent.putExtra(Intent.EXTRA_TEXT, "join: groupID = " + groupID + " and password = " + groupPassword);
            if (smsIntent.resolveActivity(getPackageManager()) != null)
                startActivity(smsIntent);
        }
        //todo: start listening to group
        //todo: ifSendData => send group data via sms
    }

    @Override
    public void onCheckAuthorizationCompleted(int actionCode, boolean isAuthorized, String nickName) {
        switch (actionCode) {
            case ACTION_CODE_START_SCREEN_ON_STARTUP:
                if (isAuthorized) {
                    Log.i(MY_TAG, "authorized, continue to map");
                    SwitchToMapFragment();
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            StartTrackingFirebaseDatabase();
                            return null;
                        }
                    }.execute();
                } else {
                    Log.i(MY_TAG, "not authorized, continue to login");
                    SwitchToLoginFragment(actionCode);
                }
                break;
            case ACTION_CODE_FOR_CREATE_GROUP:
            case ACTION_CODE_FOR_JOIN_GROUP:
                if (isAuthorized)
                    JoinCreateGroupByActionCodeAndAuthType(actionCode);
                else SwitchToLoginFragmentForActionCode(actionCode);
                break;
        }
    }

    private void JoinCreateGroupByActionCodeAndAuthType(int actionCode) {
        openCreateJoinGroupFragment(actionCode);
    }

    private void SwitchToLoginFragmentForActionCode(int actionCode) {
        showLoginFragmentForAction(actionCode);
    }

    //endregion

    //region location report service

    private void StartLocationReportService() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            CommonUtil.RequestLocationPermissions(this, 0);
        } else LocationListenerService.startLocationListenerService(this);
    }

    //endregion

    //region runtime data model and event listeners

    public Map<String, Group> getMyGroupsDictionary() {
        if (myGroupsDictionary == null)
            myGroupsDictionary = new HashMap<>();
        return myGroupsDictionary;
    }

    public Map<String, User> getUsersDictionary() {
        if (usersDictionary == null)
            usersDictionary = new HashMap<>();
        return usersDictionary;
    }

    public Map<String, Query> getUsersByGroupKeyQueries() {
        if (usersByGroupKeyQueries == null)
            usersByGroupKeyQueries = new HashMap<>();
        return usersByGroupKeyQueries;
    }

    private void StartTrackingFirebaseDatabase() {
        myGroupsQuery = FirebaseUtil.GetMyGroupsQuery(this);
        myGroupsQuery.addChildEventListener(getMyGroupsAssignmentsListener());
    }

    public ChildEventListener getMyGroupsAssignmentsListener() {
        if (myGroupsAssignmentsListener == null)
            myGroupsAssignmentsListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    if (utga == null) {
                        Log.e(MY_TAG, "utga null: getMyGroupsAssignmentsListener onChildAdded");
                        return;
                    }
                    if (getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                        Log.e(MY_TAG, "group already exists: getMyGroupsAssignmentsListener onChildAdded");
                        return;
                    }
                    Log.i(MY_TAG, "got group for myGroups: " + utga.getGroupID());
                    FirebaseUtil.GetQueryForSingleGroupByGroupKey(getApplicationContext(), utga.getGroupID())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    int i = 0;
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        if (i > 0) {
                                            Log.e(MY_TAG, "unexpected amount of groups got by one key!");
                                            return;
                                        }
                                        Group group1 = ds.getValue(Group.class);
                                        group1.setKey(ds.getKey());
                                        group1.setSelfReference(FirebaseDatabase.getInstance().getReference()
                                                .child(getApplicationContext().getString(R.string.firebase_child_groups))
                                                .child(group1.getKey()));
                                        getMyGroupsDictionary().put(group1.getGeneratedID(), group1);
                                        Query usersByGroupQuery = FirebaseUtil.GetUsersOfGroupQuery(getApplicationContext(), group1.getGeneratedID());
                                        usersByGroupQuery.addChildEventListener(getUserAssignmentsToMyGroupsListener());
                                        getUsersByGroupKeyQueries().put(group1.getGeneratedID(), usersByGroupQuery);
                                        NotifyGroupAdded(group1);
                                        i++;
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    databaseError.toException().printStackTrace();
                                }
                            });
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Log.i(MY_TAG, "getMyGroupsAssignmentsListener onChildChanged - got update about my UTGA updated");
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    if (utga == null) {
                        Log.e(MY_TAG, "group null: getMyGroupsAssignmentsListener onChildRemoved");
                        return;
                    }
                    if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                        Log.e(MY_TAG, "group doesn't exist in myGroups: getMyGroupsAssignmentsListener onChildRemoved");
                        return;
                    }
                    Log.i(MY_TAG, "group removed: " + utga.getGroupID());
                    Group group = getMyGroupsDictionary().get(utga.getGroupID());
                    getMyGroupsDictionary().remove(utga.getGroupID());
                    NotifyGroupRemoved(group);
                    Query q = getUsersByGroupKeyQueries().get(group.getGeneratedID());
                    if (q == null) {
                        Log.e(MY_TAG, "users by group key query not found");
                        return;
                    }
                    q.removeEventListener(getUserAssignmentsToMyGroupsListener());
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                }
            };
        return myGroupsAssignmentsListener;
    }

    private void NotifyGroupAdded(Group group) {
        Log.i(MY_TAG, "notified about group added to myGroups");
    }

    private void NotifyGroupRemoved(Group group) {
        Log.i(MY_TAG, "notified about group removed");
    }

    public ChildEventListener getUserAssignmentsToMyGroupsListener() {
        if (userAssignmentsToMyGroupsListener == null)
            userAssignmentsToMyGroupsListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    if (utga == null) {
                        Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                        return;
                    }
                    if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                        Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                        return;
                    }
                    if (utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext())))
                        return; // my own assignment
                    if (!LocationListenerService.IsServiceRunning)
                        StartLocationReportService();
                    Log.i(MY_TAG, "got user (" + utga.getUserProfileID() + ") joined group: " + utga.getGroupID());
                    if (getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().containsKey(utga.getUserProfileID())) {
                        Log.e(MY_TAG, "user assignment already exists in this group");
                        //todo: can go to onChildChanged algorythm or do nothing
                        return;
                    }
                    HandleUserJoinedGroup(utga);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    if (utga == null) {
                        Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                        return;
                    }
                    if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                        Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                        return;
                    }
                    if (utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext())))
                        return; // my own assignment
                    if (!getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().containsKey(utga.getUserProfileID())) {
                        HandleUserJoinedGroup(utga);
                    } else {
                        getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().remove(utga.getUserProfileID());
                        getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().put(utga.getUserProfileID(), utga);
                        NotifyUserUpdatedLocation(utga);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    if (utga == null) {
                        Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                        return;
                    }
                    if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                        Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                        return;
                    }
                    getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().remove(utga.getUserProfileID());
                    if (!utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext())))
                        NotifyUserLeftGroup(utga);
                    if (!CheckIfThereAreGroupsWithUsers())
                        SharedPreferencesUtil.SetShouldStopService(getApplicationContext(), true);
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                }
            };
        return userAssignmentsToMyGroupsListener;
    }

    private boolean CheckIfThereAreGroupsWithUsers() {
        for (String groupKey : getMyGroupsDictionary().keySet()) {
            if (getMyGroupsDictionary().get(groupKey).getUserAssignments().size() > 0)
                return true;
        }
        return false;
    }

    private void HandleUserJoinedGroup(final UserToGroupAssignment utga) {
        getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().put(utga.getUserProfileID(), utga);
        if (getUsersDictionary().containsKey(utga.getUserProfileID()))
            NotifyUserJoinedGroup(utga);
        else {
            FirebaseUtil.GetQueryForSingleUserByUserProfileID(getApplicationContext(), utga.getUserProfileID())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            int i = 0;
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                if (i > 0) {
                                    Log.e(MY_TAG, "unexpected amount of users got by one profileID!");
                                    return;
                                }
                                User user = ds.getValue(User.class);
                                user.setKey(ds.getKey());
                                getUsersDictionary().put(user.getProfileID(), user);
                                NotifyUserJoinedGroup(utga);
                                i++;
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            databaseError.toException().printStackTrace();
                        }
                    });
        }
    }

    private void NotifyUserJoinedGroup(UserToGroupAssignment utga) {
        Log.i(MY_TAG, "notified user joined group");
        User user = getUsersDictionary().get(utga.getUserProfileID());
        Group group = getMyGroupsDictionary().get(utga.getGroupID());
        Snackbar.make(toolbar,
                getString(R.string.snackbar_user_joined_group).replace("{0}", user.getUsername()).replace("{1}", group.getName()),
                Snackbar.LENGTH_SHORT).show();
        if (mapFragment != null)//todo: add check if I'm tracking this group
            mapFragment.AddMarkerForNewUser(user.getUsername(), group.getName(), utga.getLastReportedLatitude(), utga.getLastReportedLongitude());
    }

    private void NotifyUserUpdatedLocation(UserToGroupAssignment utga) {
        Log.i(MY_TAG, "notified user updated location");
        if (mapFragment != null) {//todo: add check if I'm tracking this group
            User user = getUsersDictionary().get(utga.getUserProfileID());
            Group group = getMyGroupsDictionary().get(utga.getGroupID());
            mapFragment.MoveMarker(user.getUsername(), group.getName(), utga.getLastReportedLatitude(), utga.getLastReportedLongitude());
        }
    }

    private void NotifyUserLeftGroup(UserToGroupAssignment utga) {
        Log.i(MY_TAG, "notified user left group");
        if (mapFragment != null) {
            User user = getUsersDictionary().get(utga.getUserProfileID());
            Group group = getMyGroupsDictionary().get(utga.getGroupID());
            mapFragment.RemoveMarker(user.getUsername(), group.getName());
        }
    }

    public ChildEventListener getCommonEventsOfMyGroupsListener() {
        if (commonEventsOfMyGroupsListener == null)
            commonEventsOfMyGroupsListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                }
            };
        return commonEventsOfMyGroupsListener;
    }

    public ChildEventListener getUserStatusUpdatesListener() {
        if (userStatusUpdatesListener == null)
            userStatusUpdatesListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                }
            };
        return userStatusUpdatesListener;
    }

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
                FirebaseUtil.CheckAuthForActionCode(this, ACTION_CODE_FOR_CREATE_GROUP, this);
                break;
            case R.id.fab_join_group:
                FirebaseUtil.CheckAuthForActionCode(this, ACTION_CODE_FOR_JOIN_GROUP, this);
                break;

        }
    }

    //endregion
}

//region not used

//
//    private void StartListeningToFirebaseUpdates() {
//
//    }
//
//    private void StartListeningToMyGroupsQuery() {
//        Query myGroupsQuery = FirebaseUtil.GetMyGroupsQuery(this);
//        if (myGroupsChildEventListener == null)
//            myGroupsChildEventListener = new ChildEventListener() {
//                @Override
//                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                    UserToGroupAssignment utgaGroupAdded = dataSnapshot.getValue(UserToGroupAssignment.class);
//                    if (utgaGroupAdded != null) {
//                        Log.i(MY_TAG, "you were assigned to group:" + utgaGroupAdded.getGroupID());
//                        HandleGroupAdded(utgaGroupAdded.getGroupID());
//                    }
//                }
//
//                @Override
//                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
//                    Log.e(MY_TAG, "unexpected firebase event: onChildChanged for myGroupsListener");
//                }
//
//                @Override
//                public void onChildRemoved(DataSnapshot dataSnapshot) {
//                    UserToGroupAssignment utgaGroupAssignmentRemoved = dataSnapshot.getValue(UserToGroupAssignment.class);
//                    if (utgaGroupAssignmentRemoved != null)
//                        HandleGroupRemoved(utgaGroupAssignmentRemoved.getGroupID());
//                }
//
//                @Override
//                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                    databaseError.toException().printStackTrace();
//                }
//            };
//        myGroupsQuery.addChildEventListener(myGroupsChildEventListener);
//    }
//
////    private void HandleGroupAdded(String groupKey) {
////        FirebaseUtil.GetSingleGroupReferenceByGroupKey(this, groupKey, this);
////    }
//
//    private void HandleGroupRemoved(String groupKey) {
//
//    }
//
//    private void NotifyMyGroupsArrayChanged() {
//
//    }
//
//    private void NotifyUTGAChanged(UserToGroupAssignment utga) {
//
//    }
//
//    @Override
//    public void OnSingleGroupResolved(final Group group) {
//        if (myGroups == null)
//            myGroups = new HashMap<>();
//        myGroups.put(group.getGeneratedID(), group);
//        if (singleGroupValueEventListener == null)
//            singleGroupValueEventListener = new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    Log.i(MY_TAG, "group was updated");
////                    if(!dataSnapshot.hasChildren()) return;
////                    Group g = null;
////                    for(DataSnapshot ds : dataSnapshot.getChildren())
////                        g = ds.getValue(Group.class);
//                    Group g = dataSnapshot.getValue(Group.class);
//                    for (Group myGroup : myGroups) {
//                        if (myGroup.getGeneratedID().equals(g.getGeneratedID())) {
//                            if (myGroup.compareTo(g) != 0) {
//                                myGroup.setName(g.getName());
//                                myGroup.setPassword(g.getPassword());
//                                NotifyMyGroupsArrayChanged();
//                            }
//                            break;
//                        }
//                    }
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                    databaseError.toException().printStackTrace();
//                }
//            };
//        group.getSelfReference().addValueEventListener(singleGroupValueEventListener);
//        if (singleUTGAValueEventListener == null)
//            singleUTGAValueEventListener = new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    UserToGroupAssignment utgaUpdated = dataSnapshot.getValue(UserToGroupAssignment.class);
//                    if (utgaUpdated.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext()))
//                            || userAssignments == null)
//                        return;
//                    for (UserToGroupAssignment utgaTmp : userAssignments) {
//                        if (utgaTmp.getUserProfileID().equals(utgaUpdated.getUserProfileID())
//                                && utgaTmp.getGroupID().equals(utgaUpdated.getGroupID())) {
//                            if (utgaTmp.compareTo(utgaUpdated) != 0) {
//                                utgaTmp.setLastReportedLatitude(utgaUpdated.getLastReportedLatitude());
//                                utgaTmp.setLastReportedLongitude(utgaUpdated.getLastReportedLongitude());
//                                NotifyUTGAChanged(utgaTmp);
//                            }
//                            break;
//                        }
//                    }
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                    databaseError.toException().printStackTrace();
//                }
//            };
//        if (assignedUsersChildEventListener == null)
//            assignedUsersChildEventListener = new ChildEventListener() {
//                @Override
//                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                    UserToGroupAssignment utgaNew = dataSnapshot.getValue(UserToGroupAssignment.class);
//                    if (utgaNew.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext())))
//                        return;
//                    if (userAssignments == null)
//                        userAssignments = new ArrayList<>();
//                    userAssignments.add(utgaNew);
//                    NotifyUTGAChanged(utgaNew);
//                    if (userAssignmentReferences == null)
//                        userAssignmentReferences = new ArrayList<>();
//                    DatabaseReference utgaRef = dataSnapshot.getRef();
//                    utgaRef.addValueEventListener(singleUTGAValueEventListener);
//                    userAssignmentReferences.add(utgaRef);
//                }
//
//                @Override
//                public void onChildRemoved(DataSnapshot dataSnapshot) {
//
//                }
//
//                @Override
//                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
//                }
//
//                @Override
//                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                    databaseError.toException().printStackTrace();
//                }
//            };
//    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

//endregion

