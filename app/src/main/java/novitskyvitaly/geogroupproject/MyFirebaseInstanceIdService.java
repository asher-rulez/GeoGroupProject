package novitskyvitaly.geogroupproject;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import Utils.SharedPreferencesUtil;

/**
 * Created by Asher on 22.08.2016.
 */
public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    private static final String MY_TAG = "geog_fbInstanceId";

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String token = FirebaseInstanceId.getInstance().getToken();
        if(token != null){
            Log.i(MY_TAG, "got token: " + token);
            SharedPreferencesUtil.SaveFCMTokenInSharedPreferences(getApplicationContext(), token);
            //todo: update firebase - save refreshed token in user
        }
    }
}
