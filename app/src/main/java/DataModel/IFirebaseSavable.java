package DataModel;

import com.google.firebase.database.Exclude;

/**
 * Created by Asher on 21.08.2016.
 */
public interface IFirebaseSavable {
    @Exclude
    int getSavableClassType();
}
