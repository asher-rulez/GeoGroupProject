package DataModel;

import com.google.firebase.database.Exclude;

import Utils.FirebaseUtil;

/**
 * Created by Asher on 19.08.2016.
 */
public class UserLocationReport implements IFirebaseSavable {
    private final String MY_TAG = "geog_report";

    public static final String USER_LOCATION_REPORT_KEY_LATITUDE = "lat";
    public static final String USER_LOCATION_REPORT_KEY_LONGITUDE = "lng";
    public static final String USER_LOCATION_REPORT_KEY_GROUP_ID = "groupID";
    public static final String USER_LOCATION_REPORT_KEY_USER_ID = "userProfileID";
    public static final String USER_LOCATION_REPORT_KEY_DATE = "createdUnixTime";
    public static final String USER_LOCATION_REPORT_KEY_MESSAGE = "message";

    public UserLocationReport(){ }

    private String key;
    private double lat;
    private double lng;
    private String groupID;
    private String userProfileID;
    private long createdUnixTime;
    private String message;

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getUserProfileID() {
        return userProfileID;
    }

    public void setUserProfileID(String userProfileID) {
        this.userProfileID = userProfileID;
    }

    public long getCreatedUnixTime() {
        return createdUnixTime;
    }

    public void setCreatedUnixTime(long createdUnixTime) {
        this.createdUnixTime = createdUnixTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Exclude
    public String getKey() {
        return key;
    }

    //@Exclude
    public void setKey(String key) {
        this.key = key;
    }

    @Exclude
    @Override
    public int getSavableClassType() {
        return FirebaseUtil.FIREBASE_SAVABLE_TYPE_USER_LOCATION_REPORT;
    }
}
