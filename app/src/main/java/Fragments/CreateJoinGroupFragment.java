package Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.opengl.ETC1;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import DataModel.Group;
import DataModel.IFirebaseSavable;
import DataModel.UserStatusUpdates;
import DataModel.UserToGroupAssignment;
import Utils.CommonUtil;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;
import Utils.UIUtil;
import novitskyvitaly.geogroupproject.MainActivity;
import novitskyvitaly.geogroupproject.R;

public class CreateJoinGroupFragment extends Fragment implements View.OnClickListener {
    private static final String MY_TAG = "geog_createJoin";

    private int ActionCode;

    TextView tv_title;
    EditText et_group_name_id;
    TextInputLayout til_group_name_id;
    TextView tv_group_name_validation;
    EditText et_password;
    TextInputLayout til_password;
    TextView tv_password_validation;
    Button btn_ok;
    Button btn_cancel;

    private OnCreateJoinGroupInteractionListener mListener;

    public CreateJoinGroupFragment() {
        // Required empty public constructor
    }

    public void SetAction(int actionCode) {
        ActionCode = actionCode;
    }

    //region fragment overrides

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_join_group, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCreateJoinGroupInteractionListener) {
            mListener = (OnCreateJoinGroupInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        InitControls();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ClearFields();
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.makeFabsInvisible();
        switch (ActionCode){
            case MainActivity.ACTION_CODE_FOR_CREATE_GROUP:
                mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_create_group));
                break;
            case MainActivity.ACTION_CODE_FOR_JOIN_GROUP:
                mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_join_group));
                break;
        }
        mListener.SetMainToolbarGoToMapVisible(true);
    }

    //endregion

    //region controls init

    private void InitControls() {
        tv_title = (TextView) getView().findViewById(R.id.tv_create_join_group_fragment_title);
        et_group_name_id = (EditText) getView().findViewById(R.id.et_group_name_or_id);
        til_group_name_id = (TextInputLayout)getView().findViewById(R.id.til_group_name_or_id);
        tv_group_name_validation = (TextView)getView().findViewById(R.id.tv_group_name_validation);
        et_password = (EditText) getView().findViewById(R.id.et_group_password);
        til_password = (TextInputLayout)getView().findViewById(R.id.til_group_password);
        tv_password_validation = (TextView)getView().findViewById(R.id.tv_password_validation);
        btn_ok = (Button) getView().findViewById(R.id.btn_group_ok);
        btn_cancel = (Button) getView().findViewById(R.id.btn_group_cancel);

        switch (ActionCode){
            case MainActivity.ACTION_CODE_FOR_CREATE_GROUP:
                tv_title.setText(getString(R.string.create_group_title));
                et_group_name_id.setInputType(InputType.TYPE_CLASS_TEXT);
                til_group_name_id.setHint(getString(R.string.select_group_name_hint));
                til_password.setHint(getString(R.string.select_new_group_password_hint));
                btn_ok.setText(getString(R.string.group_btn_create));
                break;
            case MainActivity.ACTION_CODE_FOR_JOIN_GROUP:
                tv_title.setText(getString(R.string.join_group_title));
                et_group_name_id.setInputType(InputType.TYPE_CLASS_NUMBER);
                til_group_name_id.setHint(getString(R.string.enter_existing_group_name_hint));
                til_password.setHint(getString(R.string.enter_existing_group_password_hint));
                btn_ok.setText(getString(R.string.group_btn_join));
                break;
        }

        btn_ok.setEnabled(false);
        btn_ok.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        ClearFields();

        et_group_name_id.addTextChangedListener(nameTextWatcher);
        et_password.addTextChangedListener(passwordTextWatcher);
}

    public void ClearFields(){
//        et_group_name_id.removeTextChangedListener(nameTextWatcher);
//        et_password.removeTextChangedListener(passwordTextWatcher);
        et_group_name_id.setText(null);
        et_password.setText(null);
        tv_group_name_validation.setText(null);
        tv_password_validation.setText(null);
//        et_group_name_id.addTextChangedListener(nameTextWatcher);
//        et_password.addTextChangedListener(passwordTextWatcher);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_group_cancel:
                if(mListener != null)
                    mListener.onCancelCreateJoinGroup();
                break;
            case R.id.btn_group_ok:
                if(!et_group_name_id.getText().toString().matches(getString(R.string.regex_digits_letters_more_than_3))){
                    UIUtil.SetEditTextIsValid(getContext(), et_group_name_id, false);
                    tv_group_name_validation.setText(getString(R.string.validation_message_nickname_symbols));
                    return;
                }
                final String groupName = et_group_name_id.getText().toString();
                switch (ActionCode){
                    case MainActivity.ACTION_CODE_FOR_CREATE_GROUP:
                        final DatabaseReference fdRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.firebase_child_groups));
                        Query myGroupsQuery = fdRef.orderByChild(Group.GROUP_KEY_OWNER_PROFILE_ID).equalTo(SharedPreferencesUtil.GetMyProfileID(getContext()));
                        myGroupsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Set<String> groupNames = new HashSet<String>();
                                for(DataSnapshot ds : dataSnapshot.getChildren()){
                                    Group gr = ds.getValue(Group.class);
                                    groupNames.add(gr.getName());
                                }
                                if(groupNames.contains(groupName)){
                                    UIUtil.SetEditTextIsValid(getContext(), et_group_name_id, false);
                                    tv_group_name_validation.setText(getString(R.string.validation_message_group_name_unique));
                                    return;
                                }
                                GenerateGroupIDAndCheckUniqueness();
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
                        break;
                    case MainActivity.ACTION_CODE_FOR_JOIN_GROUP:
                        final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.firebase_child_groups));
                        Query groupsQuery = groupsRef.orderByChild(Group.GROUP_KEY_GENERATED_ID).equalTo(groupName);
                        groupsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(final DataSnapshot dataSnapshot1) {
                                //check if group exists
                                if(!dataSnapshot1.hasChildren()){
                                    UIUtil.SetEditTextIsValid(getContext(), et_group_name_id, false);
                                    tv_group_name_validation.setText(getString(R.string.validation_message_no_such_group));
                                    return;
                                }
                                //check if user already assigned to group
                                final DatabaseReference utgaRef
                                        = FirebaseDatabase.getInstance().getReference().child(getString(R.string.firebase_user_to_group_assignment));
                                Query myAssignmentsQuery = utgaRef
                                        .orderByChild(UserToGroupAssignment.UTGA_KEY_USER_PROFILE_ID)
                                        .equalTo(SharedPreferencesUtil.GetMyProfileID(getContext()));
                                myAssignmentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot2) {
                                        if(dataSnapshot2.hasChildren()){
                                            for(DataSnapshot ds : dataSnapshot2.getChildren()){
                                                UserToGroupAssignment tmp = ds.getValue(UserToGroupAssignment.class);
                                                if(tmp.getGroupID().equals(groupName)){
                                                    UIUtil.SetEditTextIsValid(getContext(), et_group_name_id, false);
                                                    tv_group_name_validation.setText(getString(R.string.validation_message_already_assigned));
                                                    return;
                                                }
                                            }
                                        }
                                        //check password match
                                        for(DataSnapshot ds : dataSnapshot1.getChildren()){
                                            Group tmp = ds.getValue(Group.class);
                                            if(!tmp.getPassword().equals(et_password.getText().toString())) {
                                                UIUtil.SetEditTextIsValid(getContext(), et_password, false);
                                                tv_password_validation.setText(getString(R.string.password_incorrect));
                                                return;
                                            }
                                        }
                                        UserToGroupAssignment utga = new UserToGroupAssignment();
                                        utga.setGroupID(groupName);
                                        utga.setUserProfileID(SharedPreferencesUtil.GetMyProfileID(getContext()));
                                        utga.setIsTracking(true);
                                        utga.setLastReportedUnixTime(new Date().getTime());
                                        LatLng latLng = SharedPreferencesUtil.GetLastLocationLatLng(getContext());
                                        if(latLng != null){
                                            utga.setLastReportedLatitude(latLng.latitude);
                                            utga.setLastReportedLongitude(latLng.longitude);
                                        }

                                        UserStatusUpdates usu = new UserStatusUpdates();
                                        usu.setMessage(getContext().getString(R.string.status_update_joined_group));
                                        usu.setUserProfileID(SharedPreferencesUtil.GetMyProfileID(getContext()));
                                        usu.setGroupID(groupName);
                                        usu.setStatusUpdateTypeID(UserStatusUpdates.USER_STATUS_UPDATE_TYPE_JOINED_GROUP);
                                        usu.setCreateUnixTime(new Date().getTime());

                                        ArrayList<IFirebaseSavable> savables = new ArrayList<IFirebaseSavable>();
                                        savables.add(utga);
                                        savables.add(usu);
                                        FirebaseUtil.SaveDataArrayToFirebase(getContext(), savables, new Stack<DatabaseReference>(), new FirebaseUtil.IFirebaseSaveArrayOfObjectsCallback() {
                                            @Override
                                            public void OnSavingFinishedSuccessfully(Stack<DatabaseReference> savedObjectsReferences) {
                                                ClearFields();
                                                if(mListener != null)
                                                    mListener.onSuccessCreateJoinGroup(null, null, false);
                                            }

                                            @Override
                                            public void OnSavingError(DatabaseError databaseError) {
                                                if(databaseError != null)
                                                    databaseError.toException().printStackTrace();
                                            }
                                        });
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) { }
                                });
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
                        break;
                }
                UIUtil.SetEditTextIsValid(getContext(), et_group_name_id, true);
                UIUtil.SetEditTextIsValid(getContext(), et_password, true);
                break;
        }
    }

    private void GenerateGroupIDAndCheckUniqueness(){
        final String generatedGroupID = GenerateGroupID();
        FirebaseDatabase.getInstance().getReference()
                .child(getContext().getString(R.string.firebase_child_groups))
                .orderByChild(Group.GROUP_KEY_GENERATED_ID)
                .equalTo(generatedGroupID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.hasChildren())
                            SaveNewGroup(generatedGroupID);
                        else GenerateGroupIDAndCheckUniqueness();
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                });
    }

    private void SaveNewGroup(String generatedGroupID){
        final Group group = new Group();
        group.setGeneratedID(generatedGroupID);
        group.setName(et_group_name_id.getText().toString());
        group.setPassword(et_password.getText().toString());
        group.setOwnerProfileID(SharedPreferencesUtil.GetMyProfileID(getContext()));

        UserToGroupAssignment utga = new UserToGroupAssignment();
        utga.setGroupID(generatedGroupID);
        utga.setUserProfileID(SharedPreferencesUtil.GetMyProfileID(getContext()));
        utga.setIsTracking(true);
        utga.setLastReportedUnixTime(new Date().getTime());
        LatLng latLng = SharedPreferencesUtil.GetLastLocationLatLng(getContext());
        if(latLng != null){
            utga.setLastReportedLatitude(latLng.latitude);
            utga.setLastReportedLongitude(latLng.longitude);
        }

        UserStatusUpdates usu = new UserStatusUpdates();
        usu.setGroupID(generatedGroupID);
        usu.setUserProfileID(SharedPreferencesUtil.GetMyProfileID(getContext()));
        usu.setCreateUnixTime(new Date().getTime());
        usu.setStatusUpdateTypeID(UserStatusUpdates.USER_STATUS_UPDATE_TYPE_CREATED_GROUP);
        usu.setMessage(getContext().getString(R.string.status_update_created_group));

        ArrayList<IFirebaseSavable> savables = new ArrayList<>();
        savables.add(group);
        savables.add(utga);
        savables.add(usu);

        FirebaseUtil.SaveDataArrayToFirebase(getContext(), savables, new Stack<DatabaseReference>(), new FirebaseUtil.IFirebaseSaveArrayOfObjectsCallback() {
            @Override
            public void OnSavingFinishedSuccessfully(Stack<DatabaseReference> savedObjectsReferences) {
                OnSavingGroupSucceededShowDialog(group);
            }
            @Override
            public void OnSavingError(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    private void OnSavingGroupSucceededShowDialog(final Group group){
        final Dialog groupResultDialog = new Dialog(getContext());
        groupResultDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        groupResultDialog.setContentView(R.layout.group_saved_info_dialog);
        TextView tv_group_id = (TextView)groupResultDialog.findViewById(R.id.tv_group_saved_result_id);
        tv_group_id.setText(group.getGeneratedID());
        TextView tv_group_password = (TextView)groupResultDialog.findViewById(R.id.tv_group_saved_result_password);
        tv_group_password.setText(group.getPassword());

        Button btn_group_res_send = (Button)groupResultDialog.findViewById(R.id.btn_group_saved_result_send);
        btn_group_res_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mListener != null)
                    mListener.onSuccessCreateJoinGroup(group.getGeneratedID(), group.getPassword(), true);
                groupResultDialog.setOnDismissListener(null);
                groupResultDialog.dismiss();
            }
        });

        Button btn_group_res_continue = (Button)groupResultDialog.findViewById(R.id.btn_group_saved_result_continue);
        btn_group_res_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mListener != null)
                    mListener.onSuccessCreateJoinGroup(null, null, false);
                groupResultDialog.setOnDismissListener(null);
                groupResultDialog.dismiss();
            }
        });

        groupResultDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(mListener != null)
                    mListener.onSuccessCreateJoinGroup(null, null, false);
            }
        });

        groupResultDialog.show();
        ClearFields();

        //todo: show dialog with id and password, option to send
    }

    TextWatcher nameTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void afterTextChanged(Editable editable) {
            UIUtil.RemoveValidationFromEditText(getContext(), et_group_name_id);
            tv_group_name_validation.setText(
                    editable.length() > 2 || editable.length() == 0
                            ? "" : getString(R.string.validation_message_group_name_length));
            btn_ok.setEnabled(editable.length() > 2 && et_password.getText().length() > 2);
        }
    };

    TextWatcher passwordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void afterTextChanged(Editable editable) {
            UIUtil.RemoveValidationFromEditText(getContext(), et_password);
            tv_password_validation.setText(
                    editable.length() > 2 || editable.length() == 0
                            ? "" : getString(R.string.validation_message_password_length));
            btn_ok.setEnabled(editable.length() > 2 && et_group_name_id.getText().length() > 2);
        }
    };

    //endregion

    //region groupID generator

    private String GenerateGroupID(){
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for(int i = 0; i < 7; i++){
            sb.append(String.valueOf(rnd.nextInt(9)));
        }
        return sb.toString();
    }

    //endregion

    //region activity interaction

    public interface OnCreateJoinGroupInteractionListener extends ICommonFragmentInteraction {
        void onCancelCreateJoinGroup();
        void onSuccessCreateJoinGroup(String groupID, String groupPassword, boolean ifSendData);
    }

    //endregion
}
