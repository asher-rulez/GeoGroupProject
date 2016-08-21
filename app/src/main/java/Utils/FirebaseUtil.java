package Utils;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Stack;

import DataModel.Group;
import DataModel.IFirebaseSavable;
import DataModel.User;
import DataModel.UserToGroupAssignment;
import novitskyvitaly.geogroupproject.R;

/**
 * Created by Asher on 21.08.2016.
 */
public class FirebaseUtil {
    private static final String MY_TAG = "geog_fib_util";

    public static final int FIREBASE_SAVABLE_TYPE_GROUP = 1;
    public static final int FIREBASE_SAVABLE_TYPE_GROUP_COMMON_EVENT = 2;
    public static final int FIREBASE_SAVABLE_TYPE_USER = 3;
    public static final int FIREBASE_SAVABLE_TYPE_USER_LOCATION_REPORT = 4;
    public static final int FIREBASE_SAVABLE_TYPE_USER_STATUS_UPDATE = 5;
    public static final int FIREBASE_SAVABLE_TYPE_USER_TO_GROUP_ASSIGNMENT = 6;

    public static void CheckAuthForActionCode(final Context ctx, final int actionCode, final IFirebaseUtilCallback callbackListener){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null) {
            Query currentUserQuery
                    = FirebaseDatabase.getInstance().getReference().child(ctx.getString(R.string.firebase_child_users))
                    .orderByChild(User.USER_KEY_PROFILEID).equalTo(CommonUtil.GetAndroidID(ctx));
            currentUserQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChildren()) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            User user1 = ds.getValue(User.class);
                            if (user1.getProfileID().equals(CommonUtil.GetAndroidID(ctx))) {
                                String nickname = user1.getUsername();
                                SharedPreferencesUtil.SaveNicknameInSharedPreferences(ctx, nickname);
                                callbackListener.OnCheckAuthorizationCompleted(actionCode, true, nickname);
                            }
                        }
                    } else callbackListener.OnCheckAuthorizationCompleted(actionCode, false, null);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                }
            });
        } else {
            // TODO: get display name saved in fb by google/facebook authorization
        }
    }

    public static void SaveDataArrayToFirebase(final Context ctx, final ArrayList<IFirebaseSavable> dataArray, final Stack<DatabaseReference> savedObjectsReferences, final IFirebaseSaveArrayOfObjectsCallback callbackListener){
        if(dataArray.size() == 0){
            callbackListener.OnSavingFinishedSuccessfully(savedObjectsReferences);
            return;
        }
        final IFirebaseSavable savable = dataArray.get(0);
        DatabaseReference fdRef = FirebaseDatabase.getInstance().getReference();
        switch (savable.getSavableClassType()){
            case FIREBASE_SAVABLE_TYPE_GROUP:
                Group group = (Group)savable;
                fdRef.child(ctx.getString(R.string.firebase_child_groups)).push().setValue(group, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError != null)
                            RevertStackAndReturnErrorToCallback(savedObjectsReferences, databaseError, callbackListener);
                        else {
                            dataArray.remove(savable);
                            savedObjectsReferences.push(databaseReference);
                            SaveDataArrayToFirebase(ctx, dataArray, savedObjectsReferences, callbackListener);
                        }
                    }
                });
                break;
            case FIREBASE_SAVABLE_TYPE_GROUP_COMMON_EVENT:
                break;
            case FIREBASE_SAVABLE_TYPE_USER:
                break;
            case FIREBASE_SAVABLE_TYPE_USER_LOCATION_REPORT:
                break;
            case FIREBASE_SAVABLE_TYPE_USER_STATUS_UPDATE:
                break;
            case FIREBASE_SAVABLE_TYPE_USER_TO_GROUP_ASSIGNMENT:
                UserToGroupAssignment utga = (UserToGroupAssignment)savable;
                fdRef.child(ctx.getString(R.string.firebase_user_to_group_assignment)).push().setValue(utga, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError != null)
                            RevertStackAndReturnErrorToCallback(savedObjectsReferences, databaseError, callbackListener);
                        else {
                            dataArray.remove(savable);
                            savedObjectsReferences.push(databaseReference);
                            SaveDataArrayToFirebase(ctx, dataArray, savedObjectsReferences, callbackListener);
                        }
                    }
                });
                break;
        }
    }

    public static void RevertStackAndReturnErrorToCallback(Stack<DatabaseReference> changesStack, DatabaseError error, IFirebaseSaveArrayOfObjectsCallback callbackListener){
        //todo: revert stack
        callbackListener.OnSavingError(error);
    }

    public interface IFirebaseUtilCallback{
        void OnCheckAuthorizationCompleted(int actionCode, boolean isAuthorized, String nickName);
    }

    public interface IFirebaseSaveArrayOfObjectsCallback{
        void OnSavingFinishedSuccessfully(Stack<DatabaseReference> savedObjectsReferences);
        void OnSavingError(DatabaseError databaseError);
    }
}
