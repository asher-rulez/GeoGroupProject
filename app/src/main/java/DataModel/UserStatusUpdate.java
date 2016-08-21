package DataModel;

import com.google.firebase.database.Exclude;

import Utils.FirebaseUtil;

/**
 * Created by Asher on 19.08.2016.
 */
public class UserStatusUpdate implements IFirebaseSavable {
    private final String MY_TAG = "geog_status_update";

    public UserStatusUpdate() { }

    private String key;
    private long createUnixTime;
    private long statusUpdateTypeID;
    private String message;
    private String groupID;
    private String userProfileID;

    public long getCreateUnixTime() {
        return createUnixTime;
    }

    public void setCreateUnixTime(long createUnixTime) {
        this.createUnixTime = createUnixTime;
    }

    public long getStatusUpdateTypeID() {
        return statusUpdateTypeID;
    }

    public void setStatusUpdateTypeID(long statusUpdateTypeID) {
        this.statusUpdateTypeID = statusUpdateTypeID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
        return FirebaseUtil.FIREBASE_SAVABLE_TYPE_USER_STATUS_UPDATE;
    }
}
