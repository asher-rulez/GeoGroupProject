package DataModel;

import com.google.firebase.database.Exclude;

/**
 * Created by Asher on 19.08.2016.
 */
public class User {
    private final String MY_TAG = "geog_user";

    public static final String USER_KEY_USERNAME = "username";
    public static final String USER_KEY_PROFILEID = "profileID";
    public static final String USER_KEY_PROFILETYPEID = "profileTypeID";
    public static final String USER_KEY_PHOTOURL = "photoURL";

    public User(){ }

    private String key;
    private String username;
    private long profileTypeID;
    private String profileID;
    private String photoURL;

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

    @Exclude
    public String getKey() {
        return key;
    }

    //@Exclude
    public void setKey(String key) {
        this.key = key;
    }
}
