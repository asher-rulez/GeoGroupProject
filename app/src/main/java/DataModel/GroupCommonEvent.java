package DataModel;

import com.google.firebase.database.Exclude;

import Utils.FirebaseUtil;

/**
 * Created by Asher on 19.08.2016.
 */
public class GroupCommonEvent implements IFirebaseSavable {
    private final String MY_TAG = "geog_commonEvent";

    public GroupCommonEvent(){ }

    private String key;
    private double lat;
    private double lng;
    private String groupID;
    private String userProfileID;
    private long createdUnixTime;
    private String message;
    private long validUntilUnixTime;

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

    public long getValidUntilUnixTime() {
        return validUntilUnixTime;
    }

    public void setValidUntilUnixTime(long validUntilUnixTime) {
        this.validUntilUnixTime = validUntilUnixTime;
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
        return FirebaseUtil.FIREBASE_SAVABLE_TYPE_GROUP_COMMON_EVENT;
    }
}
