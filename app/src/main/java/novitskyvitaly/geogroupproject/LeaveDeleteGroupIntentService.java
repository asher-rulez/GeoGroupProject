package novitskyvitaly.geogroupproject;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import DataModel.Group;
import DataModel.GroupCommonEvent;
import DataModel.UserLocationReport;
import DataModel.UserStatusUpdates;
import DataModel.UserToGroupAssignment;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;

public class LeaveDeleteGroupIntentService extends IntentService {
    private static final String ACTION_DELETE = "IntentServices.action.FOO";
    private static final String ACTION_LEAVE = "IntentServices.action.BAZ";

    private static final String EXTRA_PARAM_GROUP_KEY = "group_key";

    public LeaveDeleteGroupIntentService() {
        super("LeaveDeleteGroupIntentService");
    }

    public static void startActionDelete(Context context, String groupKey) {
        Intent intent = new Intent(context, LeaveDeleteGroupIntentService.class);
        intent.setAction(ACTION_DELETE);
        intent.putExtra(EXTRA_PARAM_GROUP_KEY, groupKey);
        context.startService(intent);
    }

    public static void startActionLeave(Context context, String groupKey) {
        Intent intent = new Intent(context, LeaveDeleteGroupIntentService.class);
        intent.setAction(ACTION_LEAVE);
        intent.putExtra(EXTRA_PARAM_GROUP_KEY, groupKey);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DELETE.equals(action)) {
                final String groupKey = intent.getStringExtra(EXTRA_PARAM_GROUP_KEY);
                handleActionDelete(groupKey);
            } else if (ACTION_LEAVE.equals(action)) {
                final String groupKey = intent.getStringExtra(EXTRA_PARAM_GROUP_KEY);
                handleActionLeave(groupKey);
            }
        }
    }

    private void handleActionDelete(final String groupKey) {
        Query utgaQuery = FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.firebase_user_to_group_assignment))
                .orderByChild(UserToGroupAssignment.UTGA_KEY_GROUP_ID)
                .equalTo(groupKey);
        utgaQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren())
                    for(DataSnapshot ds : dataSnapshot.getChildren())
                        ds.getRef().setValue(null, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null)
                                    databaseError.toException().printStackTrace();
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });

        Query userQuery
                = FirebaseUtil.GetQueryForSingleUserByUserProfileID(getApplicationContext(),
                        SharedPreferencesUtil.GetMyProfileID(getApplicationContext()));
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        String key = ds.getKey();
                        FirebaseDatabase.getInstance().getReference()
                                .child(getString(R.string.firebase_child_users))
                                .child(key)
                                .child(getString(R.string.firebase_location_reports))
                                .child(groupKey)
                                .setValue(null, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if(databaseError != null)
                                            databaseError.toException().printStackTrace();
                                    }
                                });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });

//        Query locationUpdatesQuery = FirebaseDatabase.getInstance().getReference()
//                .child(getString(R.string.firebase_location_reports))
//                .orderByChild(UserLocationReport.USER_LOCATION_REPORT_KEY_GROUP_ID)
//                .equalTo(groupKey);
//        locationUpdatesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                if(dataSnapshot.hasChildren())
//                    for(DataSnapshot ds : dataSnapshot.getChildren()){
//                        ds.getRef().setValue(null, new DatabaseReference.CompletionListener() {
//                            @Override
//                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//                                if(databaseError != null)
//                                    databaseError.toException().printStackTrace();
//                            }
//                        });
//                    }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                databaseError.toException().printStackTrace();
//            }
//        });

        Query groupEventsQuery = FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.firebase_group_common_events))
                .orderByChild(GroupCommonEvent.GROUP_COMMON_EVENT_KEY_GROUP_ID)
                .equalTo(groupKey);
        groupEventsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren())
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        ds.getRef().setValue(null, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null)
                                    databaseError.toException().printStackTrace();
                            }
                        });
                    }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });

        Query userStatusesQuery = FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.firebase_user_status_update))
                .orderByChild(UserStatusUpdates.USER_STATUS_UPDATES_KEY_GROUP_ID)
                .equalTo(groupKey);
        userStatusesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren())
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        ds.getRef().setValue(null, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null)
                                    databaseError.toException().printStackTrace();
                            }
                        });
                    }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });

        Query groupQuery = FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.firebase_child_groups))
                .orderByChild(Group.GROUP_KEY_GENERATED_ID)
                .equalTo(groupKey);
        groupQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren())
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        ds.getRef().setValue(null, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null)
                                    databaseError.toException().printStackTrace();
                            }
                        });
                    }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    private DatabaseReference getUTGAReference(){
        return FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.firebase_user_to_group_assignment));
    }

    private void handleActionLeave(String groupKey) {

    }
}
