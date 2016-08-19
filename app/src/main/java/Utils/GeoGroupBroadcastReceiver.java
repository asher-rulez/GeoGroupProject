package Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Asher on 12.08.2016.
 */
public class GeoGroupBroadcastReceiver extends BroadcastReceiver {
    private static final String MY_TAG = "geog_bReceiver";

    public static final String BROADCAST_REC_INTENT_FILTER = "geogroup.broadcast.receiver.common";

    IBroadcastReceiverCallback callback;

    public static final String BROADCAST_EXTRA_ACTION_KEY = "action_extra_key";
    public static final String BROADCAST_EXTRA_LOCATION_REPORT_KEY = "location_report_extra_key";
    public static final String BROADCAST_EXTRA_LOCATION_PERMISSIONS_NEEDED_KEY = "location_permission_needed_key";

    public static final int ACTION_CODE_USER_LOCATION_RECEIVED = 1;
    public static final int ACTION_CODE_LOCATION_PERMISSION_NEEDED = 2;

    public GeoGroupBroadcastReceiver(IBroadcastReceiverCallback callback){
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(MY_TAG, "some broadcast received");
        if(callback != null)
            callback.onBroadcastReceived(intent);
    }

    public interface IBroadcastReceiverCallback {
        void onBroadcastReceived(Intent intent);
    }
}