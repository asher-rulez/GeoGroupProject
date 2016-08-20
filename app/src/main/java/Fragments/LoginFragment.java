package Fragments;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import DataModel.User;
import Utils.CommonUtil;
import Utils.SharedPreferencesUtil;
import Utils.UIUtil;
import novitskyvitaly.geogroupproject.R;

public class LoginFragment extends Fragment implements View.OnClickListener {

    private OnLoginFragmentInteractionListener mListener;

    private int afterLoginAction;

    Button btn_use_nickname;

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onLoginFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginFragmentInteractionListener) {
            mListener = (OnLoginFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        btn_use_nickname = (Button)getView().findViewById(R.id.btn_use_nickname);
        btn_use_nickname.setOnClickListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void SetAfterLoginAction(int actionCode){
        afterLoginAction = actionCode;
    }

    //region OnClicks

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_use_nickname:
                ShowSelectNicknameDialog();
                break;
        }
    }

    //endregion

    //region dialogs

    private void ShowSelectNicknameDialog(){
        final Context ctx = getContext();
        final Dialog nicknameDialog = new Dialog(ctx);
        nicknameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nicknameDialog.setContentView(R.layout.select_nickname_dialog);
        final EditText et_nickname = (EditText)nicknameDialog.findViewById(R.id.et_select_nickname);
        final TextView tv_nickname_validation = (TextView)nicknameDialog.findViewById(R.id.tv_select_nickname_validation);
        final Button btn_nickname_ok = (Button)nicknameDialog.findViewById(R.id.btn_select_nickname_ok);
        final Button btn_nickname_cancel = (Button)nicknameDialog.findViewById(R.id.btn_select_nickname_cancel);

        et_nickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                UIUtil.RemoveValidationFromEditText(ctx, et_nickname);
                tv_nickname_validation.setText(editable.length() > 2 ? "" : getString(R.string.validation_message_nickname_too_short));
                btn_nickname_ok.setEnabled(editable.length() > 2);
            }
        });

        btn_nickname_ok.setEnabled(false);
        btn_nickname_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!et_nickname.getText().toString().matches(getString(R.string.regex_digits_letters_more_than_3))){
                    UIUtil.SetEditTextIsValid(ctx, et_nickname, false);
                    tv_nickname_validation.setText(getString(R.string.validation_message_nickname_symbols));
                    return;
                }
                UIUtil.SetEditTextIsValid(ctx, et_nickname, true);
                SaveUserByNickname(et_nickname.getText().toString());
                nicknameDialog.dismiss();
            }
        });

        btn_nickname_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nicknameDialog.dismiss();
            }
        });

        nicknameDialog.show();
    }

    //endregion

    //region firebase

    private void SaveUserByNickname(final String nickname){
        final DatabaseReference fdRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.firebase_child_users));
        Query currentUserQuery = fdRef.orderByChild(User.USER_KEY_PROFILEID).equalTo(CommonUtil.GetAndroidID(getContext()));
        currentUserQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = null;
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    User current = ds.getValue(User.class);
                    if(current != null && current.getProfileID().equals(CommonUtil.GetAndroidID(getContext()))){
                        user = current;
                        user.setKey(ds.getKey());
                    }
                }
                if(user == null){
                    user = new User();
                    user.setUsername(nickname);
                    user.setProfileID(CommonUtil.GetAndroidID(getContext()));
                    user.setProfileTypeID(1);
                    fdRef.push().setValue(user);
                    SharedPreferencesUtil.SaveNicknameInSharedPreferences(getContext(), nickname);
                    SharedPreferencesUtil.SaveProfileIDInSharedPreferences(getContext(), CommonUtil.GetAndroidID(getContext()));
                    mListener.onLoginMade(afterLoginAction);
                } else {
                    user.setUsername(nickname);
                    fdRef.child(user.getKey()).setValue(user);
                    SharedPreferencesUtil.SaveNicknameInSharedPreferences(getContext(), nickname);
                    mListener.onLoginMade(afterLoginAction);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

//        User user = new User();
//        user.setUsername(nickname);
//        user.setProfileID(CommonUtil.GetAndroidID(getContext()));
//        user.setProfileTypeID(1);
//        fdRef.push().setValue(user);
//        SharedPreferencesUtil.SaveNicknameInSharedPreferences(getContext(), nickname);
//        SharedPreferencesUtil.SaveProfileIDInSharedPreferences(getContext(), CommonUtil.GetAndroidID(getContext()));
//        mListener.onLoginMade(afterLoginAction);
    }

    //endregion

    public interface OnLoginFragmentInteractionListener {
        // TODO: Update argument type and name
        void onLoginFragmentInteraction(Uri uri);
        void onLoginMade(int afterLoginAction);
    }
}
