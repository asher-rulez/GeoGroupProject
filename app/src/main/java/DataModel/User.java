package DataModel;

import com.google.firebase.database.Exclude;

/**
 * Created by Asher on 19.08.2016.
 */
public class User {
    private final String MY_TAG = "geog_user";

    public User(){ }

    private String key;
    private String Username;
    private long ProfileTypeID;
    private String ProfileID;
    private String PhotoURL;

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public long getProfileTypeID() {
        return ProfileTypeID;
    }

    public void setProfileTypeID(long profileTypeID) {
        ProfileTypeID = profileTypeID;
    }

    public String getProfileID() {
        return ProfileID;
    }

    public void setProfileID(String profileID) {
        ProfileID = profileID;
    }

    public String getPhotoURL() {
        return PhotoURL;
    }

    public void setPhotoURL(String photoURL) {
        PhotoURL = photoURL;
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
