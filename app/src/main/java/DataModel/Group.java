package DataModel;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.Map;

import Utils.FirebaseUtil;

/**
 * Created by Asher on 19.08.2016.
 */
public class Group implements IFirebaseSavable, Comparable<Group> {
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

    private DatabaseReference selfReference;

    @Exclude
    public DatabaseReference getSelfReference() {
        return selfReference;
    }

    public void setSelfReference(DatabaseReference selfReference) {
        this.selfReference = selfReference;
    }

    private Query assignedUsersReference;

    @Exclude
    public Query getAssignedUsersReference() {
        return assignedUsersReference;
    }

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

    private ArrayList<User> users;

    @Exclude
    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }
}
