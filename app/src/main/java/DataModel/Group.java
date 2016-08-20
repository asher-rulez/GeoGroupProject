package DataModel;

import com.google.firebase.database.Exclude;

/**
 * Created by Asher on 19.08.2016.
 */
public class Group {
    private final String MY_TAG = "geog_group";

    public static final String GROUP_KEY_NAME = "name";
    public static final String GROUP_KEY_GENERATED_ID = "generatedID";
    public static final String GROUP_KEY_OWNER_PROFILE_ID = "ownerProfileID";
    public static final String GROUP_KEY_PASSWORD = "password";

    private String key;
    private String generatedID;
    private String name;
    private String ownerProfileID;
    private String password;

    @Exclude
    public String getKey() {
        return key;
    }

    //@Exclude
    public void setKey(String key) {
        this.key = key;
    }

    public String getGeneratedID() {
        return generatedID;
    }

    public void setGeneratedID(String generatedID) {
        this.generatedID = generatedID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerProfileID() {
        return ownerProfileID;
    }

    public void setOwnerProfileID(String ownerProfileID) {
        this.ownerProfileID = ownerProfileID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
