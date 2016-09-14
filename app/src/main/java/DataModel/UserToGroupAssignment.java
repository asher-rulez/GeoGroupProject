package DataModel;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

import Utils.FirebaseUtil;

/**
 * Created by Asher on 19.08.2016.
 */
public class UserToGroupAssignment implements IFirebaseSavable, Comparable<UserToGroupAssignment>, Serializable {
    private final String MY_TAG = "geog_user_to_group";

    public final static String UTGA_KEY_GROUP_ID = "groupID";
    public final static String UTGA_KEY_USER_PROFILE_ID = "userProfileID";
    public final static String UTGA_KEY_LAST_LATITUDE = "lastReportedLatitude";
    public final static String UTGA_KEY_LAST_LONGITUDE = "lastReportedLongitude";
    public final static String UTGA_KEY_IS_TRACKING = "isTracking";

    public UserToGroupAssignment(){}

    private String key;
    private String groupID;
    private String userProfileID;
    private Double lastReportedLatitude;
    private Double lastReportedLongitude;
    private long lastReportedUnixTime;
    private boolean isTracking;

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

    public Double getLastReportedLatitude() {
        return lastReportedLatitude;
    }

    public void setLastReportedLatitude(Double lastReportedLatitude) {
        this.lastReportedLatitude = lastReportedLatitude;
    }

    public Double getLastReportedLongitude() {
        return lastReportedLongitude;
    }

    public void setLastReportedLongitude(Double lastReportedLongitude) {
        this.lastReportedLongitude = lastReportedLongitude;
    }

    public boolean getIsTracking() {
        return isTracking;
    }

    public void setIsTracking(boolean tracking) {
        isTracking = tracking;
    }

    public long getLastReportedUnixTime() {
        return lastReportedUnixTime;
    }

    public void setLastReportedUnixTime(long lastReportedUnixTime) {
        this.lastReportedUnixTime = lastReportedUnixTime;
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
        return FirebaseUtil.FIREBASE_SAVABLE_TYPE_USER_TO_GROUP_ASSIGNMENT;
    }

    @Override
    public int compareTo(UserToGroupAssignment userToGroupAssignment) {
        if(this.getLastReportedLatitude() != userToGroupAssignment.getLastReportedLatitude()
                || this.getLastReportedLongitude() != userToGroupAssignment.getLastReportedLongitude()) return 1;
        return 0;
    }

    private UserStatusUpdates userStatus;

    public UserStatusUpdates getUserStatus() {
        return userStatus;
    }

    @Exclude
    public void setUserStatus(UserStatusUpdates userStatus) {
        this.userStatus = userStatus;
    }

    private Group group;

    @Exclude
    public Group getGroup() {
        return group;
    }

    @Exclude
    public void setGroup(Group group) {
        this.group = group;
    }

    private User user;

    @Exclude
    public User getUser() {
        return user;
    }

    @Exclude
    public void setUser(User user) {
        this.user = user;
    }
}
