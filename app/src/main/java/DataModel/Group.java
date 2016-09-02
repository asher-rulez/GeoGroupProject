package DataModel;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Utils.FirebaseUtil;

/**
 * Created by Asher on 19.08.2016.
 */
public class Group implements IFirebaseSavable, Comparable<Group>, Serializable {
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

    @Exclude
    @Override
    public int getSavableClassType() {
        return FirebaseUtil.FIREBASE_SAVABLE_TYPE_GROUP;
    }

    @Exclude
    private DatabaseReference selfReference;

    @Exclude
    public DatabaseReference getSelfReference() {
        return selfReference;
    }

    @Exclude
    public void setSelfReference(DatabaseReference selfReference) {
        this.selfReference = selfReference;
    }

    @Exclude
    private Query assignedUsersReference;

    @Exclude
    public Query getAssignedUsersReference() {
        return assignedUsersReference;
    }

    @Exclude
    public void setAssignedUsersReference(Query usersReference) {
        this.assignedUsersReference = usersReference;
    }

    @Override
    public int compareTo(Group otherGroup) {
        if(!this.getName().equals(otherGroup.getName())
                || !this.getPassword().equals(otherGroup.getPassword())
                || !this.getGeneratedID().equals(otherGroup.getGeneratedID())
                || !this.getOwnerProfileID().equals(otherGroup.getOwnerProfileID())) return 1;
        return 0;
    }

    @Exclude
    private Map<String, UserToGroupAssignment> userAssignments;

    @Exclude
    public Map<String, UserToGroupAssignment> getUserAssignments() {
        if(userAssignments == null)
            userAssignments = new HashMap<>();
        return userAssignments;
    }

    @Exclude
    private ArrayList<GroupCommonEvent> commonEvents;

    @Exclude
    public ArrayList<GroupCommonEvent> getCommonEvents() {
        if(commonEvents == null)
            commonEvents = new ArrayList<>();
        return commonEvents;
    }
}
