package novitskyvitaly.geogroupproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.BatteryManager;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;

import DataModel.UserLocationReport;
import DataModel.UserStatusUpdates;
import DataModel.UserToGroupAssignment;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;

public class BatteryStateChangesReceiver extends BroadcastReceiver {
    private final static String MY_TAG = "geog_battReceiver";

    public BatteryStateChangesReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!LocationListenerService.IsServiceRunning)
            return;
        if(intent.getAction().equals(Intent.ACTION_BATTERY_LOW)){
            SendStatusUpdateBatteryLowToFirebase(context);
        }
    }

    private void SendStatusUpdateBatteryLowToFirebase(final Context ctx) {
        final Query myAssignmentsToGroups = FirebaseUtil.GetMyGroupsQuery(ctx);
        myAssignmentsToGroups.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ds) {
                if (!ds.hasChildren()) {
                    Log.e(MY_TAG, "got no group assignments");
                    return;
                }
                for (DataSnapshot dataSnapshot : ds.getChildren()) {
                    UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    if (utga == null) {
                        Log.e(MY_TAG, "got null utga from query");
                        return;
                    }
                    UserStatusUpdates usu = new UserStatusUpdates();
                    usu.setMessage(ctx.getString(R.string.status_update_battery_low));
                    usu.setCreateUnixTime(new Date().getTime());
                    usu.setStatusUpdateTypeID(UserStatusUpdates.USER_STATUS_UPDATE_TYPE_BATTERY_LOW);
                    usu.setGroupID(utga.getGroupID());
                    usu.setUserProfileID(utga.getUserProfileID());
                    FirebaseDatabase.getInstance().getReference()
                            .child(ctx.getString(R.string.firebase_user_status_update))
                            .child(dataSnapshot.getKey())
                            .setValue(usu, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError != null)
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

}
