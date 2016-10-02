package Fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import DataModel.Group;
import DataModel.UserToGroupAssignment;
import novitskyvitaly.geogroupproject.LeaveDeleteGroupIntentService;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;
import Utils.UIUtil;
import novitskyvitaly.geogroupproject.R;

public class GroupFragment extends Fragment implements View.OnClickListener {
    private final static String MY_TAG = "geog_group";

    IGroupFragmentInteraction mListener;

    Group group;
    UserToGroupAssignment userToGroupAssignment;
    String groupKey;

    TextView tv_group_key;
    TextView tv_password;
    Button btn_share_group;
    RelativeLayout rl_track;
    CheckBox cb_is_track;
    RelativeLayout rl_members;
    TextView tv_group_fragment_members;
    Button btn_messages;
    Button btn_events;
    Button btn_leave_delete_group;

    ProgressDialog progressDialog;

    public GroupFragment() {
        // Required empty public constructor
    }

    public void setGroupKey(String groupKey){
        this.groupKey = groupKey;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        tv_group_key = (TextView)view.findViewById(R.id.tv_group_fragment_group_key);
        tv_password = (TextView)view.findViewById(R.id.tv_group_fragment_group_password);
        btn_share_group = (Button) view.findViewById(R.id.btn_send_group_data);
        btn_share_group.setOnClickListener(this);
        rl_track = (RelativeLayout)view.findViewById(R.id.rl_btn_group_fragment_track);
        rl_track.setOnClickListener(this);
        rl_track.setEnabled(false);
        cb_is_track = (CheckBox)view.findViewById(R.id.cb_group_fragment_track);
        cb_is_track.setEnabled(false);
        rl_members = (RelativeLayout)view.findViewById(R.id.rl_btn_group_fragment_members);
        rl_members.setOnClickListener(this);
        rl_members.setEnabled(false);
        tv_group_fragment_members = (TextView)view.findViewById(R.id.tv_group_fragment_members);
        btn_messages = (Button)view.findViewById(R.id.btn_group_fragment_messages);
        btn_messages.setOnClickListener(this);
        btn_events = (Button)view.findViewById(R.id.btn_group_fragment_events);
        btn_events.setOnClickListener(this);
        btn_leave_delete_group = (Button)view.findViewById(R.id.btn_group_fragment_leave_delete);
        btn_leave_delete_group.setOnClickListener(this);

        reloadGroup(groupKey);

        return view;
    }

    private void setGroup(Group group){
        this.group = group;
        tv_group_key.setText(group.getGeneratedID());
        tv_password.setText(group.getPassword());
        //cb_is_track.setChecked(group.);
        //tv_group_fragment_members.setText
        btn_leave_delete_group.setText(SharedPreferencesUtil.GetMyProfileID(getContext()).equals(group.getOwnerProfileID())
                                            ? getString(R.string.group_fragment_delete)
                                            : getString(R.string.group_fragment_leave));
        mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_group).replace("{0}", group.getName()));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IGroupFragmentInteraction) {
            mListener = (IGroupFragmentInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.makeFabsInvisible();
        mListener.SetMainToolbarGoToMapVisible(true);
    }

    private void reloadGroup(final String groupKey){
        FirebaseUtil.GetSingleGroupReferenceByGroupKey(getContext(), groupKey, new FirebaseUtil.IFirebaseInitListenersCallback() {
            @Override
            public void OnSingleGroupResolved(Group group) {
                setGroup(group);
            }
        });
        FirebaseUtil.GetMyGroupsQuery(getContext()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChildren()){
                            for(DataSnapshot ds : dataSnapshot.getChildren()){
                                UserToGroupAssignment utga = ds.getValue(UserToGroupAssignment.class);
                                String str = groupKey;
                                if(utga.getGroupID().equals(str)){
                                    utga.setKey(ds.getKey());
                                    SetUserToGroupAssignment(utga);
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                });
        FirebaseUtil.GetUsersOfGroupQuery(getContext(), groupKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = 0;
                if(dataSnapshot.hasChildren()){
                    for(DataSnapshot ds : dataSnapshot.getChildren())
                        count++;
                }
                tv_group_fragment_members.setText(String.valueOf(count));
                rl_members.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    private void SetUserToGroupAssignment(UserToGroupAssignment userToGroupAssignment){
        this.userToGroupAssignment = userToGroupAssignment;
        cb_is_track.setChecked(this.userToGroupAssignment.getIsTracking());
        cb_is_track.setEnabled(true);
        rl_track.setEnabled(true);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_send_group_data:
                mListener.sendGroupJoinData(group.getGeneratedID(), group.getPassword());
                break;
            case R.id.rl_btn_group_fragment_track:
                SwitchIsTracking();
                break;
            case R.id.rl_btn_group_fragment_members:
                break;
            case R.id.btn_group_fragment_messages:
                break;
            case R.id.btn_group_fragment_events:
                break;
            case R.id.btn_group_fragment_leave_delete:
                ConfirmLeaveDeleteGroup();
                break;
        }
    }

    private void ConfirmLeaveDeleteGroup(){
        final boolean isMyGroup = group.getOwnerProfileID().equals(SharedPreferencesUtil.GetMyProfileID(getContext()));
        new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.message_confirm_leave_delete_group)
                        .replace("{0}", isMyGroup
                                ? getString(R.string.message_to_delete) : getString(R.string.message_to_leave)))
                .setPositiveButton(getString(R.string.common_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(progressDialog != null){
                            progressDialog.dismiss();
                        }
                        LeaveDeleteGroup(group.getGeneratedID(), isMyGroup);
                        dialogInterface.dismiss();
                        progressDialog = UIUtil.ShowProgressDialog(getContext(),
                                getString(isMyGroup ? R.string.progress_delete_group : R.string.progress_leave_group));
                    }
                })
                .setNegativeButton(getString(R.string.common_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void LeaveDeleteGroup(final String groupKey, final boolean isMyGroup){
//        FirebaseDatabase.getInstance().getReference().child(getString(R.string.firebase_location_reports)).setValue(null);
        DatabaseReference utgaReference
                = FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.firebase_user_to_group_assignment))
                .child(userToGroupAssignment.getKey());
        utgaReference.setValue(null, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError != null)
                    databaseError.toException().printStackTrace();
                else {
                    if (isMyGroup)
                        LeaveDeleteGroupIntentService.startActionDelete(getContext(), groupKey);
                    else
                        LeaveDeleteGroupIntentService.startActionLeave(getContext(), groupKey);
                }
                if(progressDialog != null)
                    progressDialog.dismiss();
                mListener.backToGroupsOnGroupLeftDeleted();
            }
        });
    }

    private void SwitchIsTracking(){
        final boolean prevValue = userToGroupAssignment.getIsTracking();
        userToGroupAssignment.setIsTracking(!prevValue);
        DatabaseReference dref = FirebaseDatabase.getInstance()
                .getReference(getString(R.string.firebase_user_to_group_assignment))
                .child(userToGroupAssignment.getKey());
        dref.setValue(userToGroupAssignment, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError != null){
                    databaseError.toException().printStackTrace();
                    userToGroupAssignment.setIsTracking(prevValue);
                }
                else
                    cb_is_track.setChecked(!prevValue);
            }
        });
    }

    public interface IGroupFragmentInteraction extends ICommonFragmentInteraction{
        void sendGroupJoinData(String groupKey, String groupPassword);
        void backToGroupsOnGroupLeftDeleted();
    }

}
