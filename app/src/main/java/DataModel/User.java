package DataModel;

import com.google.firebase.database.Exclude;

import Utils.FirebaseUtil;

/**
 * Created by Asher on 19.08.2016.
 */
public class User implements IFirebaseSavable {
    private final String MY_TAG = "geog_user";

    public static final String USER_KEY_USERNAME = "username";
    public static final String USER_KEY_PROFILEID = "profileID";
    public static final String USER_KEY_PROFILETYPEID = "profileTypeID";
    public static final String USER_KEY_PHOTOURL = "photoURL";
    public static final String USER_KEY_FCM_TOKEN = "fcmToken";

    public User(){ }

    private String key;
    private String username;
    private long profileTypeID;
    private String profileID;
    private String photoURL;
    private String fcmToken;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getProfileTypeID() {
        return profileTypeID;
    }

    public void setProfileTypeID(long profileTypeID) {
        this.profileTypeID = profileTypeID;
    }

    public String getProfileID() {
        return profileID;
    }

    public void setProfileID(String profileID) {
        this.profileID = profileID;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
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
        return FirebaseUtil.FIREBASE_SAVABLE_TYPE_USER;
    }
}
