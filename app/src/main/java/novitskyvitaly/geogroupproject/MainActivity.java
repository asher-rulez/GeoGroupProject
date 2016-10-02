package novitskyvitaly.geogroupproject;

import android.app.Activity;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
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
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import DataModel.Group;
import DataModel.GroupCommonEvent;
import DataModel.User;
import DataModel.UserStatusUpdates;
import DataModel.UserToGroupAssignment;
import Fragments.CreateJoinGroupFragment;
import Fragments.GroupFragment;
import Fragments.GroupsListFragment;
import Fragments.LoadingFragment;
import Fragments.LoginFragment;
import Fragments.MapFragment;
import Fragments.SettingsFragment;
import Utils.CommonUtil;
import Utils.FirebaseUtil;
import Utils.GeoGroupBroadcastReceiver;
import Utils.SharedPreferencesUtil;
import Utils.UIUtil;

public class MainActivity extends AppCompatActivity
        implements //NavigationView.OnNavigationItemSelectedListener,
        MapFragment.OnMapFragmentInteractionListener,
        GeoGroupBroadcastReceiver.IBroadcastReceiverCallback,
        LoginFragment.OnLoginFragmentInteractionListener,
        CreateJoinGroupFragment.OnCreateJoinGroupInteractionListener,
        FirebaseUtil.IFirebaseCheckAuthCallback,
        View.OnClickListener,
        Toolbar.OnMenuItemClickListener,
        SettingsFragment.ISettingsFragmentInteraction,
        GroupsListFragment.IGroupsListFragmentInteraction,
        GroupFragment.IGroupFragmentInteraction,
        Handler.Callback {

    private final String MY_TAG = "geog_main_act";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    boolean isInternetAvailable = false;
    boolean isGoogleServiceAvailable = false;
    GeoGroupBroadcastReceiver broadcastReceiver;
    BatteryStateChangesReceiver phoneStateReceiver;

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

    int performActionAfterDrawerClose = -1;

    TextView tv_toolbar_title;

    FloatingActionButton fab_plus;
    FloatingActionButton fab_create_group;
    FloatingActionButton fab_join_group;
    boolean isExpanded = false;
    //Animation fab_appear_anim;
    //Animation fab_collapse_anim;
    Animation fab_plus_to_x_rotate_anim;
    Animation fab_x_to_plus_rotate_anim;

    //endregion

    //region fragments variables

    public static final int ACTION_CODE_FOR_JOIN_GROUP = 11;
    public static final int ACTION_CODE_FOR_CREATE_GROUP = 12;
    public static final int ACTION_CODE_INITIAL_GROUPS_CHECK = 13;
    public static final int ACTION_CODE_START_SCREEN_ON_STARTUP = 14;
    public static final int ACTION_CODE_START_LOCATION_REPORT_SERVICE = 15;

    int currentFragmentID = -1;
    private final int FRAGMENT_ID_MAP = 1;
    private final int FRAGMENT_ID_LOGIN = 2;
    private final int FRAGMENT_ID_JOINCREATE = 3;
    private final int FRAGMENT_ID_LOADING = 4;
    private final int FRAGMENT_ID_SETTINGS = 5;
    private final int FRAGMENT_ID_GROUPS_LIST = 6;
    private final int FRAGMENT_GROUP = 7;
    private final int FRAGMENT_GROUP_MEMBER = 8;

    private final String FRAGMENT_TAG_MAP = "tag_map";
    private final String FRAGMENT_TAG_LOGIN = "tag_login";
    private final String FRAGMENT_TAG_JOINCREATE = "tag_join_create";
    private final String FRAGMENT_TAG_LOADING = "tag_loading";
    private final String FRAGMENT_TAG_SETTINGS = "tag_settings";
    private final String FRAGMENT_TAG_GROUPS_LIST = "groups_list";
    private final String FRAGMENT_TAG_GROUP = "group";
    private final String FRAGMENT_TAG_GROUP_MEMBER = "group_member";

    SettingsFragment settingsFragment;
    MapFragment mapFragment;
    LoginFragment loginFragment;
    CreateJoinGroupFragment createJoinFragment;
    LoadingFragment loadingFragment;
    GroupsListFragment groupsListFragment;
    GroupFragment groupFragment;

    private int scheduledFragmentID = -1;
    private int scheduledActionCodeForFragmentSwitch = -1;

    //endregion

    //region constants - saved instance state

    private final String SAVED_INSTANCE_STATE_KEY_GROUPS_DICTIONARY = "group";
    private final String SAVED_INSTANCE_STATE_KEY_USERS_DICTIONARY = "users";
    private final String SAVED_INSTANCE_STATE_KEY_FRAGMENT_ID = "fragment_id";

    //endregion

    FbDbEventsHandlerThread handlerThreadFbDbListener;
    private Handler listenerCallbackHandler;
    private HashMap<String, Group> tmpMyGroupsDictionary;
    private HashMap<String, User> tmpUsersDictionary;

    //region Activity overrides

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitToolbar();
        InitDrawerSideMenu();
        InitSideMenuControls();
        InitFABs();

        handlerThreadFbDbListener = new FbDbEventsHandlerThread("listener", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        listenerCallbackHandler = new Handler(this);

        if (savedInstanceState == null) {
            SharedPreferencesUtil.ClearSavedMapState(this);
            SwitchToLoadingFragment();
        } else {
            handlerThreadFbDbListener.getMyGroupsDictionary().putAll(
                    (HashMap<String, Group>) savedInstanceState.getSerializable(SAVED_INSTANCE_STATE_KEY_GROUPS_DICTIONARY));
            handlerThreadFbDbListener.getUsersDictionary().putAll(
                    (HashMap<String, User>) savedInstanceState.getSerializable(SAVED_INSTANCE_STATE_KEY_USERS_DICTIONARY));
            currentFragmentID = savedInstanceState.getInt(SAVED_INSTANCE_STATE_KEY_FRAGMENT_ID);
        }

        CheckIsKeepScreenOnSetting();

        phoneStateReceiver = new BatteryStateChangesReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        registerReceiver(phoneStateReceiver, filter);

        CheckTokenAndAuth();
    }

    private void CheckTokenAndAuth() {
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

        ReFindFragments();

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

        if (!SharedPreferencesUtil.GetMyProfileID(this).equals("")) {
            RestartListenerHandler();
        }
        tmpMyGroupsDictionary = null;
        tmpUsersDictionary = null;

        super.onResume();
    }

    private void RestartListenerHandler() {
        if (handlerThreadFbDbListener != null
                && handlerThreadFbDbListener.isAlive()) return;
        if (handlerThreadFbDbListener == null)
            handlerThreadFbDbListener = new FbDbEventsHandlerThread("listener", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThreadFbDbListener.setMyGroupsDictionary(tmpMyGroupsDictionary);
        handlerThreadFbDbListener.setUsersDictionary(tmpUsersDictionary);
        handlerThreadFbDbListener.start();
        handlerThreadFbDbListener.setContext(getApplicationContext());
        handlerThreadFbDbListener.SetCallback(listenerCallbackHandler);
        handlerThreadFbDbListener.StartListeningToFbDb();
    }

    private void ReFindFragments() {
        mapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
        createJoinFragment = (CreateJoinGroupFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_JOINCREATE);
        loginFragment = (LoginFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_LOGIN);
        loadingFragment = (LoadingFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_LOADING);
        settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_SETTINGS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tmpMyGroupsDictionary = handlerThreadFbDbListener.getMyGroupsDictionary();
        tmpUsersDictionary = handlerThreadFbDbListener.getUsersDictionary();
        handlerThreadFbDbListener.SetCallback(null);
        handlerThreadFbDbListener.StopListeners();
        handlerThreadFbDbListener.quit();
        handlerThreadFbDbListener = null;
    }

    @Override
    protected void onStop() {
        CommonUtil.SetIsApplicationRunningInForeground(this, false);
        SharedPreferencesUtil.ClearLastLocationSavedDateTimeInMillis(this);
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            if (broadcastReceiver != null) {
                unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SharedPreferencesUtil.SetShouldStopService(this, true);
        SharedPreferencesUtil.ClearSavedMapState(this);

        if (phoneStateReceiver != null)
            unregisterReceiver(phoneStateReceiver);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (currentFragmentID == FRAGMENT_ID_LOGIN)
                return;
            if (currentFragmentID == FRAGMENT_ID_MAP) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            if (currentFragmentID == FRAGMENT_ID_JOINCREATE) {
                if (currentFragmentID == FRAGMENT_ID_JOINCREATE && createJoinFragment != null)
                    createJoinFragment.ClearFields();
                getSupportFragmentManager().popBackStackImmediate();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (tmpMyGroupsDictionary != null)
            outState.putSerializable(SAVED_INSTANCE_STATE_KEY_GROUPS_DICTIONARY, tmpMyGroupsDictionary);
        if (tmpUsersDictionary != null)
            outState.putSerializable(SAVED_INSTANCE_STATE_KEY_USERS_DICTIONARY, tmpUsersDictionary);
        outState.putInt(SAVED_INSTANCE_STATE_KEY_FRAGMENT_ID, currentFragmentID);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        switch (requestCode){
//            case REQUEST_CODE_GOOGLE_SIGNIN:
//                if(loginFragment != null)
//                    loginFragment.onActivityResult(requestCode, resultCode, data);
//                break;
//        }
//    }

    //endregion

    //region controls init

    private void InitToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setOnMenuItemClickListener(this);
        tv_toolbar_title = (TextView) findViewById(R.id.tv_toolbar_title);
    }

    private void InitDrawerSideMenu() {
        drawer = (DrawerLayout) findViewById(R.id.drl_side_menu);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                isSideMenuOpened = false;
                closeDrawerWithAction();
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
        //fab_appear_anim = AnimationUtils.loadAnimation(this, R.anim.fab_appear);
        //fab_collapse_anim = AnimationUtils.loadAnimation(this, R.anim.fab_collapse);
        fab_plus_to_x_rotate_anim = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_plus_to_x);
        fab_x_to_plus_rotate_anim = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_x_to_plus);
    }

    private void SetFabsVisible(boolean isVisible) {
/*
        if (fab_plus == null) return;
        final View.OnClickListener clickListener = this;
        Animation anim = isVisible ? fab_appear_anim : fab_collapse_anim;
        anim.setAnimationListener(new Animation.AnimationListener() {
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
        fab_plus.startAnimation(anim);
        fab_plus.setClickable(isVisible);
*/
        if (isVisible) {
            fab_plus.setVisibility(FloatingActionButton.VISIBLE);
            fab_create_group.setVisibility(isExpanded ? FloatingActionButton.VISIBLE : FloatingActionButton.GONE);
            fab_join_group.setVisibility(isExpanded ? FloatingActionButton.VISIBLE : FloatingActionButton.GONE);
        } else {
            if (isExpanded)
                CollapseFabs(true);
            else {
                fab_plus.setVisibility(FloatingActionButton.GONE);
                fab_create_group.setVisibility(FloatingActionButton.GONE);
                fab_join_group.setVisibility(FloatingActionButton.GONE);
            }
        }
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
            currentFragmentID = FRAGMENT_ID_MAP;
            if (mapFragment == null) {
                mapFragment = new MapFragment();
            } else {
                boolean fragmentPopped
                        = getSupportFragmentManager().popBackStackImmediate(mapFragment.getClass().getName(), 0);
                if (fragmentPopped) return;
            }
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fl_fragments_container, mapFragment, FRAGMENT_TAG_MAP);
            for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
                getSupportFragmentManager().popBackStackImmediate();
            }
            transaction.commit();
        } else ScheduleSwitchToFragment(FRAGMENT_ID_MAP, null);
    }

    private void SwitchToLoginFragment(int actionCode) {
        if (CommonUtil.GetIsApplicationRunningInForeground(this)) {
            if (loginFragment == null)
                loginFragment = new LoginFragment();
            loginFragment.SetAfterLoginAction(actionCode);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fl_fragments_container, loginFragment, FRAGMENT_TAG_LOGIN);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.addToBackStack("login");
            transaction.commit();
            //tv_toolbar_title.setText(getString(R.string.toolbar_title_fragment_login));
            currentFragmentID = FRAGMENT_ID_LOGIN;
        } else ScheduleSwitchToFragment(FRAGMENT_ID_LOGIN, actionCode);
    }

    private void SwitchToCreateJoinFragment(int actionCode) {
        if (CommonUtil.GetIsApplicationRunningInForeground(this)) {
            if (createJoinFragment == null)
                createJoinFragment = new CreateJoinGroupFragment();
            createJoinFragment.SetAction(actionCode);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fl_fragments_container, createJoinFragment, FRAGMENT_TAG_JOINCREATE);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.addToBackStack(FRAGMENT_TAG_JOINCREATE);
            transaction.commit();
//            switch (actionCode){
//                case ACTION_CODE_FOR_CREATE_GROUP:
//                    tv_toolbar_title.setText(getString(R.string.toolbar_title_fragment_create_group));
//                    break;
//                case ACTION_CODE_FOR_JOIN_GROUP:
//                    tv_toolbar_title.setText(getString(R.string.toolbar_title_fragment_join_group));
//                    break;
//            }
            currentFragmentID = FRAGMENT_ID_JOINCREATE;
        } else ScheduleSwitchToFragment(FRAGMENT_ID_JOINCREATE, actionCode);
    }

    private void SwitchToLoadingFragment() {
        if (loadingFragment == null)
            loadingFragment = new LoadingFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fl_fragments_container, loadingFragment, FRAGMENT_TAG_LOADING);
        transaction.commit();
        //tv_toolbar_title.setText(getString(R.string.toolbar_title_fragment_map));
        currentFragmentID = FRAGMENT_ID_LOADING;
    }

    private void SwitchToSettingsFragment() {
        if (settingsFragment == null)
            settingsFragment = new SettingsFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fl_fragments_container, settingsFragment, FRAGMENT_TAG_SETTINGS);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.addToBackStack(FRAGMENT_TAG_SETTINGS);
        transaction.commit();
        currentFragmentID = FRAGMENT_ID_SETTINGS;
    }

    private void SwitchToGroupsListFragment() {
        if (groupsListFragment == null)
            groupsListFragment = new GroupsListFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fl_fragments_container, groupsListFragment, FRAGMENT_TAG_GROUPS_LIST);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.addToBackStack(FRAGMENT_TAG_GROUPS_LIST);
        transaction.commit();
        currentFragmentID = FRAGMENT_ID_GROUPS_LIST;
    }

    private void SwitchToGroupFragment(String groupKey) {
        if (groupFragment == null)
            groupFragment = new GroupFragment();
        groupFragment.setGroupKey(groupKey);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fl_fragments_container, groupFragment, FRAGMENT_TAG_GROUP);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.addToBackStack(FRAGMENT_TAG_GROUP);
        transaction.commit();
        currentFragmentID = FRAGMENT_GROUP;
    }

    @Override
    public void onLoginMade(int afterLoginAction) {
        SwitchToMapFragment();
        RestartListenerHandler();
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
    public void showFabsForMap() {
        HideSoftKeyboard();
        SetFabsVisible(true);
        currentFragmentID = FRAGMENT_ID_MAP;
    }

    @Override
    public void hideFabsOnMapPaused() {
        SetFabsVisible(false);
    }

    @Override
    public ArrayList<UserToGroupAssignment> getUTGAsForShowing() {
        ArrayList<UserToGroupAssignment> result = new ArrayList<>();
        if (handlerThreadFbDbListener.getMyGroupsDictionary() == null
                || handlerThreadFbDbListener.getMyGroupsDictionary().size() == 0)
            return result;
        for (String groupName : handlerThreadFbDbListener.getMyGroupsDictionary().keySet()) {
            Group group = handlerThreadFbDbListener.getMyGroupsDictionary().get(groupName);
            if (group.getUserAssignments() == null || group.getUserAssignments().size() == 0)
                continue;
            for (String profileID : group.getUserAssignments().keySet()) {
                UserToGroupAssignment utga = new UserToGroupAssignment();
                utga.setGroupID(group.getGeneratedID());
                utga.setUserProfileID(profileID);
                utga.setGroup(group);
                utga.setUser(handlerThreadFbDbListener.getUsersDictionary().get(profileID));
                utga.setLastReportedLatitude(group.getUserAssignments().get(profileID).getLastReportedLatitude());
                utga.setLastReportedLongitude(group.getUserAssignments().get(profileID).getLastReportedLongitude());
                result.add(utga);
            }
        }
        return result;
    }

    @Override
    public void onCancelCreateJoinGroup() {
        onBackPressed();
    }

    @Override
    public void onSuccessCreateJoinGroup(String groupID, String groupPassword, boolean ifSendData) {
        onBackPressed();

        if (ifSendData && !TextUtils.isEmpty(groupID) && !TextUtils.isEmpty(groupPassword)) {
            SendGroupInvitationData(groupID, groupPassword);
        }
        //todo: start listening to group
        //todo: ifSendData => send group data via sms
    }

    private void SendGroupInvitationData(String groupID, String groupPassword) {
        Intent smsIntent = new Intent(Intent.ACTION_SEND);
        smsIntent.setType("text/plain");
        //smsIntent.setData(Uri.parse("smsto:"));
        smsIntent.putExtra(Intent.EXTRA_TEXT, "join: groupID = " + groupID + " and password = " + groupPassword);
        if (smsIntent.resolveActivity(getPackageManager()) != null)
            startActivity(smsIntent);
    }

    @Override
    public void makeFabsInvisible() {
        SetFabsVisible(false);
    }

    @Override
    public void onCheckAuthorizationCompleted(int actionCode, boolean isAuthorized, String nickName) {
//        switch (currentFragmentID){
//            case FRAGMENT_ID_SETTINGS:
//                SwitchToSettingsFragment();
//                break;
//            default:
        switch (actionCode) {
            case ACTION_CODE_START_SCREEN_ON_STARTUP:
                if (isAuthorized) {
                    Log.i(MY_TAG, "authorized, continue to map");
                    if (currentFragmentID == -1 || currentFragmentID == FRAGMENT_ID_LOADING)
                        SwitchToMapFragment();
                    RestartListenerHandler();
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
//                break;
//        }
    }

    private void JoinCreateGroupByActionCodeAndAuthType(int actionCode) {
        openCreateJoinGroupFragment(actionCode);
    }

    private void SwitchToLoginFragmentForActionCode(int actionCode) {
        showLoginFragmentForAction(actionCode);
    }

    //endregion

    //region location report service

    private synchronized void StartLocationReportService() {
        if (LocationListenerService.IsServiceRunning)
            return;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            CommonUtil.RequestLocationPermissions(this, 0);
        } else LocationListenerService.startLocationListenerService(this);
    }

    //endregion

    //region runtime data model and event listeners


    private void MoveMarkerOnMap(User user, Group group, double lat, double lng) {
        if (mapFragment != null)//todo: add check if I'm tracking this group
            mapFragment.MoveMarker(user, group, lat, lng);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_plus:
                if (isExpanded) {
                    CollapseFabs(false);
                } else {
                    ExpandFabs();
                }
                break;
            case R.id.fab_create_group:
                CollapseFabs(false);
                progressDialog = UIUtil.ShowProgressDialog(this, getString(R.string.progress_loading));
                FirebaseUtil.CheckAuthForActionCode(this, ACTION_CODE_FOR_CREATE_GROUP, this);
                break;
            case R.id.fab_join_group:
                CollapseFabs(false);
                progressDialog = UIUtil.ShowProgressDialog(this, getString(R.string.progress_loading));
                FirebaseUtil.CheckAuthForActionCode(this, ACTION_CODE_FOR_JOIN_GROUP, this);
                break;
            default:
                performActionAfterDrawerClose = view.getId();
                if (drawer.isDrawerOpen(GravityCompat.START))
                    drawer.closeDrawer(GravityCompat.START);
                break;
        }
    }

    private void closeDrawerWithAction(){
        String profileID;
        switch (performActionAfterDrawerClose){
            case R.id.btn_side_menu_settings:
                profileID = SharedPreferencesUtil.GetMyProfileID(this);
                if (profileID.equals("")) {
                    ShowSnackLoginFirst();
                } else {
                    progressDialog = UIUtil.ShowProgressDialog(this, getString(R.string.progress_loading));
                    SwitchToSettingsFragment();
                }
                break;
            case R.id.btn_side_menu_events:
                profileID = SharedPreferencesUtil.GetMyProfileID(this);
                if (profileID.equals("")) {
                    ShowSnackLoginFirst();
                } else {
                    progressDialog = UIUtil.ShowProgressDialog(this, getString(R.string.progress_loading));
                }
                break;
            case R.id.btn_side_menu_groups:
                profileID = SharedPreferencesUtil.GetMyProfileID(this);
                if (profileID.equals("")) {
                    ShowSnackLoginFirst();
                } else {
                    progressDialog = UIUtil.ShowProgressDialog(this, getString(R.string.progress_loading));
                    SwitchToGroupsListFragment();
                }
                break;
            case R.id.btn_side_menu_about:
                progressDialog = UIUtil.ShowProgressDialog(this, getString(R.string.progress_loading));
                break;
        }
        performActionAfterDrawerClose = -1;
    }

    private void ShowSnackLoginFirst() {
        Snackbar.make(fab_plus, getString(R.string.snack_message_login_first), Snackbar.LENGTH_SHORT).show();
    }

    private void ExpandFabs() {
        final View.OnClickListener clickListener = this;
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
        Animation fab_appear_anim_join = AnimationUtils.loadAnimation(this, R.anim.fab_appear);
        fab_appear_anim_join.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab_join_group.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab_join_group.setClickable(true);
                fab_join_group.clearAnimation();
                fab_join_group.setVisibility(FloatingActionButton.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        final Animation fab_appear_anim_create = AnimationUtils.loadAnimation(this, R.anim.fab_appear);
        fab_appear_anim_create.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab_create_group.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab_create_group.setClickable(true);
                fab_create_group.clearAnimation();
                fab_create_group.setVisibility(FloatingActionButton.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab_join_group.startAnimation(fab_appear_anim_join);
        fab_create_group.startAnimation(fab_appear_anim_create);
        fab_plus.startAnimation(fab_plus_to_x_rotate_anim);
        isExpanded = true;
    }

    private void CollapseFabs(final boolean makePlusInvisible) {
        final View.OnClickListener clickListener = this;
        fab_x_to_plus_rotate_anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab_plus.setOnClickListener(null);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab_plus.setOnClickListener(clickListener);
                fab_plus.clearAnimation();
                if (makePlusInvisible) fab_plus.setVisibility(FloatingActionButton.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        Animation fab_collapse_anim_join = AnimationUtils.loadAnimation(this, R.anim.fab_collapse);
        Animation fab_collapse_anim_create = AnimationUtils.loadAnimation(this, R.anim.fab_collapse);
        fab_collapse_anim_join.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab_join_group.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab_join_group.clearAnimation();
                fab_join_group.setVisibility(FloatingActionButton.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab_collapse_anim_create.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab_create_group.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab_create_group.clearAnimation();
                fab_create_group.setVisibility(FloatingActionButton.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab_join_group.startAnimation(fab_collapse_anim_join);
        fab_create_group.startAnimation(fab_collapse_anim_create);
        fab_plus.startAnimation(fab_x_to_plus_rotate_anim);

        isExpanded = false;
    }

    @Override
    public void SetupMainToolbarTitle(String title) {
        tv_toolbar_title.setText(title);
    }

    @Override
    public void SetMainToolbarGoToMapVisible(boolean ifVisible) {
        toolbar.getMenu().clear();
        if (ifVisible)
            toolbar.inflateMenu(R.menu.back_to_map_menu);
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        SwitchToMapFragment();
        return true;
    }

    @Override
    public void SettingIfKeepScreenOn(boolean flag) {
        if (flag) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Log.i(MY_TAG, "set to screen on");
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Log.i(MY_TAG, "set to screen off");
        }
    }

    private void CheckIsKeepScreenOnSetting() {
        SettingIfKeepScreenOn(SharedPreferencesUtil.GetKeepScreenOn(this));
    }

    public void HideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public void MakeFabsVisibleForGroupsList() {
        HideSoftKeyboard();
        SetFabsVisible(true);
        currentFragmentID = FRAGMENT_ID_GROUPS_LIST;
    }

    @Override
    public void OnGroupSelectedFromList(String groupKey) {
        SwitchToGroupFragment(groupKey);
    }

    //endregion

    //region fbdb listener callbacks

    private static final int HANDLER_MESSAGE_TYPE_USER_JOINED_GROUP = 1;
    private static final int HANDLER_MESSAGE_TYPE_USER_UPDATED_LOCATION = 2;
    private static final int HANDLER_MESSAGE_TYPE_GROUP_REMOVED = 3;
    private static final int HANDLER_MESSAGE_TYPE_USER_LEFT_GROUP = 4;
    private static final int HANDLER_MESSAGE_TYPE_START_SERVICE = 5;
    //private static final int HANDLER_MESSAGE_TYPE_

    private static final String HANDLER_MESSAGE_EXTRA_GROUP = "group";
    private static final String HANDLER_MESSAGE_EXTRA_USER = "user";
    private static final String HANDLER_MESSAGE_EXTRA_UTGA = "utga";
    private static final String HANDLER_MESSAGE_EXTRA_SHOW_SNACKBAR = "show_snackbar";

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case HANDLER_MESSAGE_TYPE_GROUP_REMOVED:
                Group group = (Group) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_GROUP);
                handleGroupRemoved(group);
                break;
            case HANDLER_MESSAGE_TYPE_USER_LEFT_GROUP:
                Group group1 = (Group) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_GROUP);
                User user1 = (User) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_USER);
                UserToGroupAssignment utga1 = (UserToGroupAssignment) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_UTGA);
                NotifyUserLeftGroup(utga1, user1, group1);
                break;
            case HANDLER_MESSAGE_TYPE_START_SERVICE:
                StartLocationReportService();
                break;
            case HANDLER_MESSAGE_TYPE_USER_JOINED_GROUP:
                Group group2 = (Group) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_GROUP);
                User user2 = (User) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_USER);
                UserToGroupAssignment utga2 = (UserToGroupAssignment) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_UTGA);
                boolean showSnackbar = message.getData().getBoolean(HANDLER_MESSAGE_EXTRA_SHOW_SNACKBAR);
                NotifyUserJoinedGroup(utga2, user2, group2, showSnackbar);
                break;
            case HANDLER_MESSAGE_TYPE_USER_UPDATED_LOCATION:
                Group group3 = (Group) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_GROUP);
                User user3 = (User) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_USER);
                UserToGroupAssignment utga3 = (UserToGroupAssignment) message.getData().getSerializable(HANDLER_MESSAGE_EXTRA_UTGA);
                MoveMarkerOnMap(user3, group3, utga3.getLastReportedLatitude(), utga3.getLastReportedLongitude());
                break;
        }
        return true;
    }

    private void handleGroupRemoved(Group group) {
//        Snackbar.make(fab_plus,
//                getString(R.string.snackbar_group_removed).replace("{0}", group.getName()),
//                Snackbar.LENGTH_SHORT).show();
        if (mapFragment != null) {
            mapFragment.RemoveMarkersByGroup(group.getGeneratedID());
        }
    }

    private void NotifyUserLeftGroup(UserToGroupAssignment utga, User user, Group group) {
        Log.i(MY_TAG, "notified user left group");
        if (mapFragment != null) {
            mapFragment.RemoveMarker(group.getGeneratedID(), user.getProfileID());
        }
    }

    private void NotifyUserJoinedGroup(UserToGroupAssignment utga, User user, Group group, boolean showSnackbar) {
        Log.i(MY_TAG, "notified user joined group");
        if (showSnackbar)
            Snackbar.make(toolbar,
                    getString(R.string.snackbar_user_joined_group).replace("{0}", user.getUsername()).replace("{1}", group.getName()),
                    Snackbar.LENGTH_SHORT).show();
        if (mapFragment != null && utga.getLastReportedLatitude() != null && utga.getLastReportedLongitude() != null)//todo: add check if I'm tracking this group
            mapFragment.AddMarkerForNewUser(user, group, utga.getLastReportedLatitude(), utga.getLastReportedLongitude());
    }

    @Override
    public void sendGroupJoinData(String groupKey, String groupPassword) {
        SendGroupInvitationData(groupKey, groupPassword);
    }

    @Override
    public void backToGroupsOnGroupLeftDeleted() {
        onBackPressed();
    }

    //endregion

    //region HandlerThread for firebase database events

    public class FbDbEventsHandlerThread extends HandlerThread implements Handler.Callback {

        //region variables - runtime objects and event listeners

        Query myGroupsQuery;

        HashMap<String, Group> myGroupsDictionary;
        HashMap<String, User> usersDictionary;
        HashMap<String, Query> usersByGroupKeyQueries;

        ChildEventListener myGroupsAssignmentsListener;
        ChildEventListener userAssignmentsToMyGroupsListener;
        ChildEventListener commonEventsOfMyGroupsListener;
        ChildEventListener userStatusUpdatesListener;

        //endregion

        boolean flagShouldStart;

        Context context;
        private Handler innerHandler, callbackHandler;

        public FbDbEventsHandlerThread(String name) {
            super(name);
        }

        public FbDbEventsHandlerThread(String name, int priority) {
            super(name, priority);
        }

        @Override
        protected void onLooperPrepared() {
            innerHandler = new Handler(getLooper(), this);
            if (flagShouldStart) innerHandler.sendEmptyMessage(0);
        }

        public void StartListeningToFbDb() {
            if (innerHandler != null)
                innerHandler.sendEmptyMessage(0);
            else flagShouldStart = true;
        }

        public void SetCallback(Handler cb) {
            callbackHandler = cb;
        }

        public void setContext(Context ctx) {
            context = ctx.getApplicationContext();
        }

        @Override
        public boolean handleMessage(Message message) {
            StartTrackingFirebaseDatabase();
            return false;
        }

        private void StartTrackingFirebaseDatabase() {
            myGroupsQuery = FirebaseUtil.GetMyGroupsQuery(context);
            myGroupsQuery.addChildEventListener(getMyGroupsAssignmentsListener());
        }

        private void StopListeners() {
            if (myGroupsQuery != null)
                myGroupsQuery.removeEventListener(getMyGroupsAssignmentsListener());
            for (Query q : getUsersByGroupKeyQueries().values())
                q.removeEventListener(getUserAssignmentsToMyGroupsListener());
        }

        public synchronized HashMap<String, Group> getMyGroupsDictionary() {
            if (myGroupsDictionary == null)
                myGroupsDictionary = new HashMap<>();
            return myGroupsDictionary;
        }

        public synchronized void setMyGroupsDictionary(HashMap<String, Group> groupsDictionary){
            myGroupsDictionary = groupsDictionary;
        }

        public synchronized HashMap<String, User> getUsersDictionary() {
            if (usersDictionary == null)
                usersDictionary = new HashMap<>();
            return usersDictionary;
        }

        public synchronized void setUsersDictionary(HashMap<String, User> usersDictionary){
            this.usersDictionary = usersDictionary;
        }

        public synchronized HashMap<String, Query> getUsersByGroupKeyQueries() {
            if (usersByGroupKeyQueries == null)
                usersByGroupKeyQueries = new HashMap<>();
            return usersByGroupKeyQueries;
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
                            Query usersByGroupQ = getUsersByGroupKeyQueries().get(utga.getGroupID());
                            if (usersByGroupQ == null) {
                                usersByGroupQ = FirebaseUtil.GetUsersOfGroupQuery(context, utga.getGroupID());
                                getUsersByGroupKeyQueries().put(utga.getGroupID(), usersByGroupQ);
                            }
                            usersByGroupQ.removeEventListener(getUserAssignmentsToMyGroupsListener());
                            usersByGroupQ.addChildEventListener(getUserAssignmentsToMyGroupsListener());
                            //return;
                        } else {
                            Log.i(MY_TAG, "got group for myGroups: " + utga.getGroupID());
                            FirebaseUtil.GetQueryForSingleGroupByGroupKey(context, utga.getGroupID())
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
                                                        .child(context.getString(R.string.firebase_child_groups))
                                                        .child(group1.getKey()));
                                                getMyGroupsDictionary().put(group1.getGeneratedID(), group1);
                                                Query usersByGroupQuery = FirebaseUtil.GetUsersOfGroupQuery(context, group1.getGeneratedID());
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

        public ChildEventListener getUserAssignmentsToMyGroupsListener() {
            if (userAssignmentsToMyGroupsListener == null)
                userAssignmentsToMyGroupsListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                        onUTGAAdded(utga);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                        onUTGAUpdated(utga);
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
                        if (!utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(context))) {
                            User user = getUsersDictionary().get(utga.getUserProfileID());
                            Group group = getMyGroupsDictionary().get(utga.getGroupID());
                            NotifyUserLeftGroupInternal(utga, user, group);
                        }
                        if (!CheckIfThereAreGroupsWithUsers())
                            SharedPreferencesUtil.SetShouldStopService(context, true);
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

        private void NotifyUserLeftGroupInternal(UserToGroupAssignment utga, User user, Group group) {
            Message msg = new Message();
            msg.what = HANDLER_MESSAGE_TYPE_USER_LEFT_GROUP;
            Bundle b = new Bundle();
            b.putSerializable(HANDLER_MESSAGE_EXTRA_USER, user);
            b.putSerializable(HANDLER_MESSAGE_EXTRA_GROUP, group);
            b.putSerializable(HANDLER_MESSAGE_EXTRA_UTGA, utga);
            msg.setData(b);
            callbackHandler.sendMessage(msg);
        }

        private void NotifyGroupAdded(Group group) {
            Log.i(MY_TAG, "notified about group added to myGroups");
        }

        private void NotifyGroupRemoved(Group group) {
            Log.i(MY_TAG, "notified about group removed");
            if (callbackHandler == null) return;
            Message msg = new Message();
            msg.what = HANDLER_MESSAGE_TYPE_GROUP_REMOVED;
            Bundle bundle = new Bundle();
            bundle.putSerializable(HANDLER_MESSAGE_EXTRA_GROUP, group);
            msg.setData(bundle);
            callbackHandler.sendMessage(msg);
        }


        private void onUTGAAdded(UserToGroupAssignment utga) {
            if (utga == null) {
                Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(context)))
                return; // my own assignment
            notifyStartService();
            Log.i(MY_TAG, "got user (" + utga.getUserProfileID() + ") joined group: " + utga.getGroupID());
            if (getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().containsKey(utga.getUserProfileID())) {
                Log.e(MY_TAG, "user assignment already exists in this group");
                onUTGAUpdated(utga);
                return;
            }
            HandleUserJoinedGroup(utga);
        }

        private void notifyStartService() {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && !LocationListenerService.IsServiceRunning)
                LocationListenerService.startLocationListenerService(context);
            else if (callbackHandler != null) {
                Message msg = new Message();
                msg.what = HANDLER_MESSAGE_TYPE_START_SERVICE;
                callbackHandler.sendMessage(msg);
            }
        }

        private void onUTGAUpdated(UserToGroupAssignment utga) {
            if (utga == null) {
                Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(context)))
                return; // my own assignment
            if (!getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().containsKey(utga.getUserProfileID())) {
                HandleUserJoinedGroup(utga);
            } else {
                getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().remove(utga.getUserProfileID());
                getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().put(utga.getUserProfileID(), utga);
                NotifyUserUpdatedLocation(utga);
            }
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
            if (getUsersDictionary().containsKey(utga.getUserProfileID())) {
                User user = getUsersDictionary().get(utga.getUserProfileID());
                Group group = getMyGroupsDictionary().get(utga.getGroupID());
                NotifyUserJoinedGroupInternal(utga, user, group, false);
            } else {
                FirebaseUtil.GetQueryForSingleUserByUserProfileID(context, utga.getUserProfileID())
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
                                    Group group = getMyGroupsDictionary().get(utga.getGroupID());
                                    NotifyUserJoinedGroupInternal(utga, user, group, true);
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

        private void NotifyUserJoinedGroupInternal(UserToGroupAssignment utga, User user, Group group, boolean showSnackbar) {
            if (callbackHandler == null) return;
            Message msg = new Message();
            msg.what = HANDLER_MESSAGE_TYPE_USER_JOINED_GROUP;
            Bundle b = new Bundle();
            b.putSerializable(HANDLER_MESSAGE_EXTRA_UTGA, utga);
            b.putSerializable(HANDLER_MESSAGE_EXTRA_USER, user);
            b.putSerializable(HANDLER_MESSAGE_EXTRA_GROUP, group);
            b.putBoolean(HANDLER_MESSAGE_EXTRA_SHOW_SNACKBAR, showSnackbar);
            msg.setData(b);
            callbackHandler.sendMessage(msg);
        }

        private void NotifyUserUpdatedLocation(final UserToGroupAssignment utga) {
            Log.i(MY_TAG, "notified user updated location");
            final User user = getUsersDictionary().get(utga.getUserProfileID());
            if (user == null) {
                FirebaseUtil.GetQueryForSingleUserByUserProfileID(context, utga.getUserProfileID())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                int i = 0;
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    if (i > 0) {
                                        Log.e(MY_TAG, "unexpected amount of users got by one profileID!");
                                        return;
                                    }
                                    final User user1 = ds.getValue(User.class);
                                    user1.setKey(ds.getKey());
                                    getUsersDictionary().put(user1.getProfileID(), user1);
                                    i++;
                                    Group group = getMyGroupsDictionary().get(utga.getGroupID());
                                    if (group == null) {
                                        FirebaseUtil.GetQueryForSingleGroupByGroupKey(context, utga.getGroupID())
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        int j = 0;
                                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                                            if (j > 0) {
                                                                Log.e(MY_TAG, "unexpected amount of groups got by one key!");
                                                                return;
                                                            }
                                                            Group group = ds.getValue(Group.class);
                                                            group.setKey(ds.getKey());
                                                            getMyGroupsDictionary().put(group.getGeneratedID(), group);
                                                            notifyUserChangedLocation(user1, group, utga);
                                                            j++;
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        databaseError.toException().printStackTrace();
                                                    }
                                                });
                                    } else {
                                        if (utga.getLastReportedLatitude() != null && utga.getLastReportedLongitude() != null)
                                            notifyUserChangedLocation(user1, group, utga);
                                    }

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                databaseError.toException().printStackTrace();
                            }
                        });
            } else {
                Group group = getMyGroupsDictionary().get(utga.getGroupID());
                if (group == null) {
                    FirebaseUtil.GetQueryForSingleGroupByGroupKey(context, utga.getGroupID())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    int j = 0;
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        if (j > 0) {
                                            Log.e(MY_TAG, "unexpected amount of groups got by one key!");
                                            return;
                                        }
                                        Group group = ds.getValue(Group.class);
                                        group.setKey(ds.getKey());
                                        getMyGroupsDictionary().put(group.getGeneratedID(), group);
                                        notifyUserChangedLocation(user, group, utga);
                                        j++;
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    databaseError.toException().printStackTrace();
                                }
                            });
                } else {
                    if (utga.getLastReportedLatitude() != null && utga.getLastReportedLongitude() != null)
                        notifyUserChangedLocation(user, group, utga);
                }

            }
        }

        private void notifyUserChangedLocation(User user, Group group, UserToGroupAssignment utga) {
            Message msg = new Message();
            msg.what = HANDLER_MESSAGE_TYPE_USER_UPDATED_LOCATION;
            Bundle b = new Bundle();
            b.putSerializable(HANDLER_MESSAGE_EXTRA_GROUP, group);
            b.putSerializable(HANDLER_MESSAGE_EXTRA_USER, user);
            b.putSerializable(HANDLER_MESSAGE_EXTRA_UTGA, utga);
            msg.setData(b);
            callbackHandler.sendMessage(msg);
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


    }

    //endregion

}

//region not used

//endregion

