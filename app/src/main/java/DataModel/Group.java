package DataModel;

import com.google.firebase.database.Exclude;

/**
 * Created by Asher on 19.08.2016.
 */
public class Group {
    private final String MY_TAG = "geog_group";

    private String key;
    private String GeneratedID;
    private String Name;
    private String OwnerProfileID;
    private String Password;

    @Exclude
    public String getKey() {
        return key;
    }

    //@Exclude
    public void setKey(String key) {
        this.key = key;
    }

    public String getGeneratedID() {
        return GeneratedID;
    }

    public void setGeneratedID(String generatedID) {
        GeneratedID = generatedID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getOwnerProfileID() {
        return OwnerProfileID;
    }

    public void setOwnerProfileID(String ownerProfileID) {
        OwnerProfileID = ownerProfileID;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }
}
