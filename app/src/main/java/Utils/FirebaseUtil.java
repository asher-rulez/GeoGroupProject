package Utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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

    public static void CheckAuthForActionCode(final Context ctx, final int actionCode, final IFirebaseCheckAuthCallback callbackListener){
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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
                                SharedPreferencesUtil.SaveProfileIDInSharedPreferences(ctx, user1.getProfileID());
                                callbackListener.onCheckAuthorizationCompleted(actionCode, true, nickname);
                            }
                        }
                    } else callbackListener.onCheckAuthorizationCompleted(actionCode, false, null);
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

    public static void GetActiveGroups(Context ctx, final IFirebaseGetDataCheckCallback callbackListener){
        //todo: here should be method that gets current User profileID from SP
        String profileID = CommonUtil.GetAndroidID(ctx);

        Query qMyUTGAs
                = FirebaseDatabase.getInstance().getReference()
                .child(ctx.getString(R.string.firebase_user_to_group_assignment))
                .orderByChild(UserToGroupAssignment.UTGA_KEY_USER_PROFILE_ID)
                .equalTo(profileID);
        qMyUTGAs.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChildren()){
                    Log.i(MY_TAG, "no active groups found");
                    callbackListener.onGetActiveGroupsCompleted(new ArrayList<Group>());
                    return;
                }
                Set<String> groupKeys = new HashSet<String>();
                for(DataSnapshot dsUTGA : dataSnapshot.getChildren()){
                    UserToGroupAssignment utga = dsUTGA.getValue(UserToGroupAssignment.class);
                    if(utga != null)
                        groupKeys.add(utga.getGroupID());
                }
                if(groupKeys.size() == 0){
                    Log.i(MY_TAG, "resolving group keys from fbdb went wrong");
                    callbackListener.onGetActiveGroupsCompleted(new ArrayList<Group>());
                    return;
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void GetUserNamesAndReferencesByGroupKeys(Context ctx, Set<String> groupKeys, ArrayList<Group> groupsResult, IFirebaseGetDataCheckCallback callbackListener){
        if(groupKeys == null || groupKeys.size() == 0)
            callbackListener.onGetActiveGroupsCompleted(groupsResult);
        //Query utgaByGroupKeyQuery = FirebaseDatabase.getInstance().getReference().child(ctx.getString(R.string.firebase_user_to_group_assignment)).orderByChild(UserToGroupAssignment.UTGA_KEY_GROUP_ID).
    }

    public static Query GetMyGroupsQuery(Context ctx){
        return FirebaseDatabase.getInstance().getReference()
                .child(ctx.getString(R.string.firebase_user_to_group_assignment))
                .orderByChild(UserToGroupAssignment.UTGA_KEY_USER_PROFILE_ID)
                .equalTo(SharedPreferencesUtil.GetMyProfileID(ctx));
    }

    public static void GetSingleGroupReferenceByGroupKey(final Context ctx, final String groupKey, final IFirebaseInitListenersCallback callbackListener){
        Query singleGroupQuery = GetQueryForSingleGroupByGroupKey(ctx, groupKey);
        singleGroupQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    int i = 0;
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        if(i > 0){
                            Log.e(MY_TAG, "unexpected amount of groups got by one key!");
                            return;
                        }
                        Group group = ds.getValue(Group.class);
                        group.setKey(ds.getKey());
                        group.setSelfReference(FirebaseDatabase.getInstance().getReference()
                                .child(ctx.getString(R.string.firebase_child_groups)).child(group.getKey()));
                        group.setAssignedUsersReference(GetUsersOfGroupQuery(ctx, groupKey));
                        callbackListener.OnSingleGroupResolved(group);
                        i++;
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    public static Query GetQueryForSingleGroupByGroupKey(Context ctx, String groupKey){
        return FirebaseDatabase.getInstance().getReference()
                .child(ctx.getString(R.string.firebase_child_groups))
                .orderByChild(Group.GROUP_KEY_GENERATED_ID)
                .equalTo(groupKey);
    }

    public static Query GetUsersOfGroupQuery(Context ctx, String groupKey){
        return FirebaseDatabase.getInstance().getReference()
                .child(ctx.getString(R.string.firebase_user_to_group_assignment))
                .orderByChild(UserToGroupAssignment.UTGA_KEY_GROUP_ID)
                .equalTo(groupKey);
    }

    public static Query GetQueryForSingleUserByUserProfileID(Context ctx, String userProfileID){
        return FirebaseDatabase.getInstance().getReference()
                .child(ctx.getString(R.string.firebase_child_users))
                .orderByChild(User.USER_KEY_PROFILEID)
                .equalTo(userProfileID);
    }

    //region Callback interfaces

    public interface IFirebaseCheckAuthCallback{
        //todo: this callback should update nickname and user profileID in SP
        void onCheckAuthorizationCompleted(int actionCode, boolean isAuthorized, String nickName);
    }

    public interface IFirebaseGetDataCheckCallback {
        void onGetActiveGroupsCompleted(ArrayList<Group> groupsResult);

    }

    public interface IFirebaseSaveArrayOfObjectsCallback{
        void OnSavingFinishedSuccessfully(Stack<DatabaseReference> savedObjectsReferences);
        void OnSavingError(DatabaseError databaseError);
    }

    public interface IFirebaseInitListenersCallback{
        void OnSingleGroupResolved(Group group);
    }

    //endregion
}
