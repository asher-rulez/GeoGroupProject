package Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

import novitskyvitaly.geogroupproject.R;

/**
 * Created by Asher on 19.08.2016.
 */
public class SharedPreferencesUtil {
    private static final String MY_TAG = "geog_SP_Util";

    public static void SetLocationRefreshFrequency(Context ctx, int frequencyMillis) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(ctx.getString(R.string.sp_key_location_refresh_frequency_key), frequencyMillis);
        editor.commit();
    }

    public static int GetLocationRefreshFrequency(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        int fr = sp.getInt(ctx.getString(R.string.sp_key_location_refresh_frequency_key), -1);
        if (fr == -1) {
            fr = ctx.getResources().getInteger(R.integer.location_refresh_default_frequency_rate_milliseconds);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(ctx.getString(R.string.sp_key_location_refresh_frequency_key), fr);
            editor.commit();
        }
        return fr;
    }

    public static void SetIfReportLocation(Context ctx, boolean ifReport) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ctx.getString(R.string.sp_key_if_report_location), ifReport);
        editor.commit();
    }

    public static boolean GetIfReportLocationFromSharedPreferences(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        boolean result = sp.getBoolean(ctx.getString(R.string.sp_key_if_report_location), true);
        if (result) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(ctx.getString(R.string.sp_key_if_report_location), true);
            editor.commit();
        }
        return result;
    }

    public static void SetIfSaveHistory(Context ctx, boolean ifSaveHistory){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ctx.getString(R.string.sp_key_if_save_history), ifSaveHistory);
        editor.commit();
    }

    public static boolean GetIfSaveHistory(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        boolean result = sp.getBoolean(ctx.getString(R.string.sp_key_if_save_history), true);
        if (result) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(ctx.getString(R.string.sp_key_if_save_history), true);
            editor.commit();
        }
        return result;
    }

    public static void SetIfReportInBackground(Context ctx, boolean ifSaveHistory){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ctx.getString(R.string.sp_key_if_report_in_background), ifSaveHistory);
        editor.commit();
    }

    public static boolean GetIfReportInBackground(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        boolean result = sp.getBoolean(ctx.getString(R.string.sp_key_if_report_in_background), true);
        if (result) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(ctx.getString(R.string.sp_key_if_report_in_background), true);
            editor.commit();
        }
        return result;
    }

    public static void SetKeepScreenOn(Context ctx, boolean keepScreenOn){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ctx.getString(R.string.sp_key_keep_screen_on), keepScreenOn);
        editor.commit();
    }

    public static boolean GetKeepScreenOn(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.app_settings_token), Context.MODE_PRIVATE);
        boolean result = sp.getBoolean(ctx.getString(R.string.sp_key_keep_screen_on), false);
        if (result) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(ctx.getString(R.string.sp_key_keep_screen_on), false);
            editor.commit();
        }
        return result;
    }

    public static void SaveLocationInSharedPreferences(Context ctx, double latitude, double longitude, Date date) {
        Log.d(MY_TAG, "ctx is " + ctx == null ? "null" : "not null");
        int str = R.string.last_location_token;
        String token = ctx.getString(str);
        SharedPreferences sp = ctx.getSharedPreferences(token, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(ctx.getString(R.string.last_location_latitude), (float) latitude);
        editor.putFloat(ctx.getString(R.string.last_location_longitude), (float) longitude);
        editor.putLong(ctx.getString(R.string.last_location_datetime), date.getTime());
        editor.commit();
    }

    public static void ClearLastLocationSavedDateTimeInMillis(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.last_location_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(ctx.getString(R.string.last_location_datetime), -1);
        editor.commit();
    }

    public static long GetLastLocationSavedDateTimeInMillis(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.last_location_token), Context.MODE_PRIVATE);
        return sp.getLong(ctx.getString(R.string.last_location_datetime), -1);
    }

    public static LatLng GetLastLocationLatLng(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.last_location_token), Context.MODE_PRIVATE);
        float lat = sp.getFloat(ctx.getString(R.string.last_location_latitude), -1);
        float lng = sp.getFloat(ctx.getString(R.string.last_location_longitude), -1);
        if (lat == -1 && lng == -1)
            return null;
        return new LatLng((double) lat, (double) lng);
    }

    public static void SaveNicknameInSharedPreferences(Context ctx, String nickname) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.nickname_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ctx.getString(R.string.nickname_key), nickname);
        editor.commit();
    }

    public static String GetMyNickname(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.nickname_token), Context.MODE_PRIVATE);
        return sp.getString(ctx.getString(R.string.nickname_key), "");
    }

    public static void SaveProfileIDInSharedPreferences(Context ctx, String nickname) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.profile_id_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ctx.getString(R.string.profile_id_key), nickname);
        editor.commit();
    }

    public static String GetMyProfileID(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.profile_id_token), Context.MODE_PRIVATE);
        return sp.getString(ctx.getString(R.string.profile_id_key), "");
    }

    public static void SaveSocialNameInSharedPreferences(Context ctx, String nickname) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.social_name_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ctx.getString(R.string.social_name_key), nickname);
        editor.commit();
    }

    public static String GetMySocialName(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.social_name_token), Context.MODE_PRIVATE);
        return sp.getString(ctx.getString(R.string.social_name_key), "");
    }

    public static void SetIsLocationUpdateServiceRunning(Context ctx, boolean flag) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.location_service_running_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ctx.getString(R.string.location_service_running_key), flag);
        editor.commit();
    }

    public static boolean GetIsLocationUpdateServiceRunning(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.location_service_running_token), Context.MODE_PRIVATE);
        return sp.getBoolean(ctx.getString(R.string.location_service_running_key), false);
    }

    public static void SetShouldStopService(Context ctx, boolean flag) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.should_stop_loc_service_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ctx.getString(R.string.should_stop_loc_service_key), flag);
        editor.commit();
    }

    public static boolean GetShouldStopService(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.should_stop_loc_service_token), Context.MODE_PRIVATE);
        boolean result = sp.getBoolean(ctx.getString(R.string.should_stop_loc_service_key), false);
        if (result) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(ctx.getString(R.string.should_stop_loc_service_key), true);
            editor.commit();
        }
        return result;
    }

    public static void SaveFCMTokenInSharedPreferences(Context ctx, String nickname) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.fcm_token_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ctx.getString(R.string.fcm_token_key), nickname);
        editor.commit();
    }

    public static String GetFCMTokenFromSharedPreferences(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.fcm_token_token), Context.MODE_PRIVATE);
        return sp.getString(ctx.getString(R.string.fcm_token_key), "");
    }

    public static void SaveMapStateInSharedPrefs(Context ctx, CameraPosition cameraPosition) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.gmap_state_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(ctx.getString(R.string.gmap_state_lat), (float) cameraPosition.target.latitude);
        editor.putFloat(ctx.getString(R.string.gmap_state_lng), (float) cameraPosition.target.longitude);
        editor.putFloat(ctx.getString(R.string.gmap_state_zoom), cameraPosition.zoom);
        editor.putFloat(ctx.getString(R.string.gmap_state_bearing), cameraPosition.bearing);
        editor.putFloat(ctx.getString(R.string.gmap_state_tilt), cameraPosition.tilt);
        editor.commit();
    }

    public static CameraPosition GetMapStateFromPrefsAsCameraPosition(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.gmap_state_token), Context.MODE_PRIVATE);
        double lat = (double) sp.getFloat(ctx.getString(R.string.gmap_state_lat), -1);
        if (lat == -1)
            return null;
        double lng = (double) sp.getFloat(ctx.getString(R.string.gmap_state_lng), -1);
        LatLng latLng = new LatLng(lat, lng);
        float zoom = sp.getFloat(ctx.getString(R.string.gmap_state_zoom), -1);
        float bearing = sp.getFloat(ctx.getString(R.string.gmap_state_bearing), -1);
        float tilt = sp.getFloat(ctx.getString(R.string.gmap_state_tilt), -1);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .bearing(bearing)
                .tilt(tilt)
                .build();
        return cameraPosition;
    }

    public static void ClearSavedMapState(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.gmap_state_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
    }
}
