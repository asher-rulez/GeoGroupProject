package Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import novitskyvitaly.geogroupproject.R;

/**
 * Created by Asher on 19.08.2016.
 */
public class CommonUtil {
    private static final String MY_TAG = "geog_commonUtil";


    public static String GetAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void SetIsApplicationRunningInForeground(Context ctx, boolean isRunning){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.is_app_running_in_foreground_token), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ctx.getString(R.string.is_app_running_in_foreground_key), isRunning);
        editor.commit();
    }

    public static boolean GetIsApplicationRunningInForeground(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.is_app_running_in_foreground_token), Context.MODE_PRIVATE);
        return sp.getBoolean(ctx.getString(R.string.is_app_running_in_foreground_key), false);
    }

    public static void RequestLocationPermissions(Activity activity, int requestCode){
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                requestCode);
    }

    public static int GetDPSize(Context ctx, int dps){
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }
}
