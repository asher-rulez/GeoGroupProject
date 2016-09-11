package novitskyvitaly.geogroupproject;

import android.app.Application;
import android.content.Intent;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

/**
 * Created by Asher on 20.08.2016.
 */
public class GeoGroupProjectApplication extends Application {
    public static GeoGroupProjectApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }

    public static synchronized GeoGroupProjectApplication getInstance(){
        return mInstance;
    }

    public void stopLocationReportService(){
        stopService(new Intent(getBaseContext(), LocationListenerService.class));
    }
}
