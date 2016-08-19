package DataModel;

import com.google.firebase.database.Exclude;

/**
 * Created by Asher on 19.08.2016.
 */
public class UserToGroupAssignment {
    private final String MY_TAG = "geog_user_to_group";

    public UserToGroupAssignment(){}

    private String key;
    private String groupID;
    private String userProfileID;

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

}
