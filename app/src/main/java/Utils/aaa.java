package Utils;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import DataModel.Group;
import DataModel.User;
import DataModel.UserToGroupAssignment;
import novitskyvitaly.geogroupproject.R;

/**
 * Created by Asher on 19.09.2016.
 */
public class aaa {
    String MY_TAG = "aaa";

    public class FbDbEventsHandlerThread extends HandlerThread implements Handler.Callback {

        //region variables - runtime objects and event listeners

        Query myGroupsQuery;

        HashMap<String, Group> myGroupsDictionary;
        HashMap<String, User> usersDictionary;
        HashMap<String, Query> usersByGroupKeyQueries;

        ChildEventListener myGroupsAssignmentsListener;
        ChildEventListener userAssignmentsToMyGroupsListener;
        ChildEventListener commonEventsOfMyGroupsListener;
        ChildEventListener userStatusUpdatesListener;

        //endregion

        boolean flagShouldStart;

        Context context;
        private Handler innerHandler, callbackHandler;

        public FbDbEventsHandlerThread(String name) {
            super(name);
        }

        public FbDbEventsHandlerThread(String name, int priority){
            super(name, priority);
        }

        @Override
        protected void onLooperPrepared() {
            innerHandler = new Handler(getLooper(), this);
            if(flagShouldStart) innerHandler.sendEmptyMessage(0);
        }

        public void StartListeningToFbDb(){
            if(innerHandler != null)
                innerHandler.sendEmptyMessage(0);
            else flagShouldStart = true;
        }

        public void SetCallback(Handler cb){
            callbackHandler = cb;
        }

        public void setContext(Context ctx){
            context = ctx;
        }

        @Override
        public boolean handleMessage(Message message) {
            StartTrackingFirebaseDatabase();
            return false;
        }

        private void StartTrackingFirebaseDatabase() {
            myGroupsQuery = FirebaseUtil.GetMyGroupsQuery(context);
            myGroupsQuery.addChildEventListener(getMyGroupsAssignmentsListener());
        }

        private void StopListeners() {
            if (myGroupsQuery != null)
                myGroupsQuery.removeEventListener(getMyGroupsAssignmentsListener());
            for (Query q : getUsersByGroupKeyQueries().values())
                q.removeEventListener(getUserAssignmentsToMyGroupsListener());
        }

        public synchronized HashMap<String, Group> getMyGroupsDictionary() {
            if (myGroupsDictionary == null)
                myGroupsDictionary = new HashMap<>();
            return myGroupsDictionary;
        }

        public synchronized HashMap<String, User> getUsersDictionary() {
            if (usersDictionary == null)
                usersDictionary = new HashMap<>();
            return usersDictionary;
        }

        public synchronized HashMap<String, Query> getUsersByGroupKeyQueries() {
            if (usersByGroupKeyQueries == null)
                usersByGroupKeyQueries = new HashMap<>();
            return usersByGroupKeyQueries;
        }

        public ChildEventListener getMyGroupsAssignmentsListener() {
            if (myGroupsAssignmentsListener == null)
                myGroupsAssignmentsListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                        if (utga == null) {
                            Log.e(MY_TAG, "utga null: getMyGroupsAssignmentsListener onChildAdded");
                            return;
                        }
                        if (getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                            Log.e(MY_TAG, "group already exists: getMyGroupsAssignmentsListener onChildAdded");
                            Query usersByGroupQ = getUsersByGroupKeyQueries().get(utga.getGroupID());
                            if (usersByGroupQ == null) {
                                usersByGroupQ = FirebaseUtil.GetUsersOfGroupQuery(getApplicationContext(), utga.getGroupID());
                                getUsersByGroupKeyQueries().put(utga.getGroupID(), usersByGroupQ);
                            }
                            usersByGroupQ.removeEventListener(getUserAssignmentsToMyGroupsListener());
                            usersByGroupQ.addChildEventListener(getUserAssignmentsToMyGroupsListener());
                            //return;
                        } else {
                            Log.i(MY_TAG, "got group for myGroups: " + utga.getGroupID());
                            FirebaseUtil.GetQueryForSingleGroupByGroupKey(getApplicationContext(), utga.getGroupID())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            int i = 0;
                                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                                if (i > 0) {
                                                    Log.e(MY_TAG, "unexpected amount of groups got by one key!");
                                                    return;
                                                }
                                                Group group1 = ds.getValue(Group.class);
                                                group1.setKey(ds.getKey());
                                                group1.setSelfReference(FirebaseDatabase.getInstance().getReference()
                                                        .child(getApplicationContext().getString(R.string.firebase_child_groups))
                                                        .child(group1.getKey()));
                                                getMyGroupsDictionary().put(group1.getGeneratedID(), group1);
                                                Query usersByGroupQuery = FirebaseUtil.GetUsersOfGroupQuery(getApplicationContext(), group1.getGeneratedID());
                                                usersByGroupQuery.addChildEventListener(getUserAssignmentsToMyGroupsListener());
                                                getUsersByGroupKeyQueries().put(group1.getGeneratedID(), usersByGroupQuery);
                                                NotifyGroupAdded(group1);
                                                i++;
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            databaseError.toException().printStackTrace();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        Log.i(MY_TAG, "getMyGroupsAssignmentsListener onChildChanged - got update about my UTGA updated");
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                        if (utga == null) {
                            Log.e(MY_TAG, "group null: getMyGroupsAssignmentsListener onChildRemoved");
                            return;
                        }
                        if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                            Log.e(MY_TAG, "group doesn't exist in myGroups: getMyGroupsAssignmentsListener onChildRemoved");
                            return;
                        }
                        Log.i(MY_TAG, "group removed: " + utga.getGroupID());
                        Group group = getMyGroupsDictionary().get(utga.getGroupID());
                        getMyGroupsDictionary().remove(utga.getGroupID());
                        NotifyGroupRemoved(group);
                        Query q = getUsersByGroupKeyQueries().get(group.getGeneratedID());
                        if (q == null) {
                            Log.e(MY_TAG, "users by group key query not found");
                            return;
                        }
                        q.removeEventListener(getUserAssignmentsToMyGroupsListener());
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                };
            return myGroupsAssignmentsListener;
        }

        public ChildEventListener getUserAssignmentsToMyGroupsListener() {
            if (userAssignmentsToMyGroupsListener == null)
                userAssignmentsToMyGroupsListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                        onUTGAAdded(utga);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                        onUTGAUpdated(utga);
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        final UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                        if (utga == null) {
                            Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                            return;
                        }
                        if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                            Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                            return;
                        }
                        getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().remove(utga.getUserProfileID());
                        if (!utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext()))){
                            User user = getUsersDictionary().get(utga.getUserProfileID());
                            Group group = getMyGroupsDictionary().get(utga.getGroupID());
                            NotifyUserLeftGroupInternal(utga, user, group);
                        }
                        if (!CheckIfThereAreGroupsWithUsers())
                            SharedPreferencesUtil.SetShouldStopService(getApplicationContext(), true);
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                };
            return userAssignmentsToMyGroupsListener;
        }

        private void NotifyUserLeftGroupInternal(UserToGroupAssignment utga, User user, Group group){
        }

        private void NotifyGroupAdded(Group group) {
        }

        private void NotifyGroupRemoved(Group group) {
            Log.i(MY_TAG, "notified about group removed");
        }


        private void onUTGAAdded(UserToGroupAssignment utga) {
            if (utga == null) {
                Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext())))
                return; // my own assignment
            notifyStartService();
            Log.i(MY_TAG, "got user (" + utga.getUserProfileID() + ") joined group: " + utga.getGroupID());
            if (getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().containsKey(utga.getUserProfileID())) {
                Log.e(MY_TAG, "user assignment already exists in this group");
                onUTGAUpdated(utga);
                return;
            }
            HandleUserJoinedGroup(utga);
        }

        private void notifyStartService(){}

        private void onUTGAUpdated(UserToGroupAssignment utga) {
            if (utga == null) {
                Log.e(MY_TAG, "utga null: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (!getMyGroupsDictionary().containsKey(utga.getGroupID())) {
                Log.e(MY_TAG, "group not found: getUserAssignmentsToMyGroupsListener onChildAdded");
                return;
            }
            if (utga.getUserProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getApplicationContext())))
                return; // my own assignment
            if (!getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().containsKey(utga.getUserProfileID())) {
                HandleUserJoinedGroup(utga);
            } else {
                getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().remove(utga.getUserProfileID());
                getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().put(utga.getUserProfileID(), utga);
                NotifyUserUpdatedLocation(utga);
            }
        }

        private boolean CheckIfThereAreGroupsWithUsers() {
            for (String groupKey : getMyGroupsDictionary().keySet()) {
                if (getMyGroupsDictionary().get(groupKey).getUserAssignments().size() > 0)
                    return true;
            }
            return false;
        }

        private void HandleUserJoinedGroup(final UserToGroupAssignment utga) {
            getMyGroupsDictionary().get(utga.getGroupID()).getUserAssignments().put(utga.getUserProfileID(), utga);
            if (getUsersDictionary().containsKey(utga.getUserProfileID())){
                User user = getUsersDictionary().get(utga.getUserProfileID());
                Group group = getMyGroupsDictionary().get(utga.getGroupID());
                NotifyUserJoinedGroupInternal(utga, user, group);
            }
            else {
                FirebaseUtil.GetQueryForSingleUserByUserProfileID(getApplicationContext(), utga.getUserProfileID())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                int i = 0;
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    if (i > 0) {
                                        Log.e(MY_TAG, "unexpected amount of users got by one profileID!");
                                        return;
                                    }
                                    User user = ds.getValue(User.class);
                                    user.setKey(ds.getKey());
                                    getUsersDictionary().put(user.getProfileID(), user);
                                    Group group = getMyGroupsDictionary().get(utga.getGroupID());
                                    NotifyUserJoinedGroupInternal(utga, user, group);
                                    i++;
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                databaseError.toException().printStackTrace();
                            }
                        });
            }
        }

        private void NotifyUserJoinedGroupInternal(UserToGroupAssignment utga, User user, Group group){}

        private void NotifyUserUpdatedLocation(final UserToGroupAssignment utga) {
            Log.i(MY_TAG, "notified user updated location");
            final User user = getUsersDictionary().get(utga.getUserProfileID());
            if(user == null){
                FirebaseUtil.GetQueryForSingleUserByUserProfileID(getApplicationContext(), utga.getUserProfileID())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                int i = 0;
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    if (i > 0) {
                                        Log.e(MY_TAG, "unexpected amount of users got by one profileID!");
                                        return;
                                    }
                                    final User user1 = ds.getValue(User.class);
                                    user1.setKey(ds.getKey());
                                    getUsersDictionary().put(user1.getProfileID(), user1);
                                    i++;
                                    Group group = getMyGroupsDictionary().get(utga.getGroupID());
                                    if(group == null){
                                        FirebaseUtil.GetQueryForSingleGroupByGroupKey(getApplicationContext(), utga.getGroupID())
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        int j = 0;
                                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                                            if (j > 0) {
                                                                Log.e(MY_TAG, "unexpected amount of groups got by one key!");
                                                                return;
                                                            }
                                                            Group group = ds.getValue(Group.class);
                                                            group.setKey(ds.getKey());
                                                            getMyGroupsDictionary().put(group.getGeneratedID(), group);
                                                            notifyUserChangedLocation(user, group, utga);
                                                            j++;
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        databaseError.toException().printStackTrace();
                                                    }
                                                });
                                    } else{
                                        if(utga.getLastReportedLatitude() != null && utga.getLastReportedLongitude() != null)
                                            notifyUserChangedLocation(user, group, utga);
                                    }

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                databaseError.toException().printStackTrace();
                            }
                        });
            } else {
                Group group = getMyGroupsDictionary().get(utga.getGroupID());
                if(group == null){
                    FirebaseUtil.GetQueryForSingleGroupByGroupKey(getApplicationContext(), utga.getGroupID())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    int j = 0;
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        if (j > 0) {
                                            Log.e(MY_TAG, "unexpected amount of groups got by one key!");
                                            return;
                                        }
                                        Group group = ds.getValue(Group.class);
                                        group.setKey(ds.getKey());
                                        getMyGroupsDictionary().put(group.getGeneratedID(), group);
                                        notifyUserChangedLocation(user, group, utga);
                                        j++;
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    databaseError.toException().printStackTrace();
                                }
                            });
                } else{
                    if(utga.getLastReportedLatitude() != null && utga.getLastReportedLongitude() != null)
                        notifyUserChangedLocation(user, group, utga);
                }

            }
        }

        private void notifyUserChangedLocation(User user, Group group, UserToGroupAssignment utga){
        }

        public ChildEventListener getCommonEventsOfMyGroupsListener() {
            if (commonEventsOfMyGroupsListener == null)
                commonEventsOfMyGroupsListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                };
            return commonEventsOfMyGroupsListener;
        }

        public ChildEventListener getUserStatusUpdatesListener() {
            if (userStatusUpdatesListener == null)
                userStatusUpdatesListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                };
            return userStatusUpdatesListener;
        }

        private Context getApplicationContext(){return null;}

    }

}
