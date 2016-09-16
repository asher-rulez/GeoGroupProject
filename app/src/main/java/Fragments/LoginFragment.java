package Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import DataModel.User;
import Utils.CommonUtil;
import Utils.SharedPreferencesUtil;
import Utils.UIUtil;
import novitskyvitaly.geogroupproject.MainActivity;
import novitskyvitaly.geogroupproject.R;

public class LoginFragment extends Fragment implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String MY_TAG = "geog_login";

    public static final int ACCOUNT_TYPE_NICKNAME = 1;
    public static final int ACCOUNT_TYPE_GOOGLE = 2;
    public static final int ACCOUNT_TYPE_FACEBOOK = 3;

    //region sign in constants

    public static final int REQUEST_CODE_GOOGLE_SIGNIN = 9001;
    public static final int REQUEST_CODE_FACEBOOK_SIGNIN = 64206;

    //endregion

    private OnLoginFragmentInteractionListener mListener;

    private int afterLoginAction;

    Button btn_use_nickname;
    SignInButton btn_google_signin;
    LoginButton btn_facebook_login;

    GoogleApiClient googleApiClient;
    GoogleSignInOptions googleSignInOptions;

    CallbackManager callbackManager;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
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

        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = GetAuthStateListener();

        btn_use_nickname = (Button)getView().findViewById(R.id.btn_use_nickname);
        btn_use_nickname.setOnClickListener(this);

        initGoogleSignIn();
        btn_google_signin = (SignInButton) getView().findViewById(R.id.btn_google_signin);
        btn_google_signin.setSize(SignInButton.SIZE_WIDE);
        btn_google_signin.setScopes(googleSignInOptions.getScopeArray());
        btn_google_signin.setOnClickListener(this);
        ViewGroup.LayoutParams lp = btn_google_signin.getLayoutParams();

        initFacebookLogin(lp);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_login));
        mListener.SetMainToolbarGoToMapVisible(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        googleApiClient.stopAutoManage(getActivity());
        googleApiClient.disconnect();
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
            case R.id.btn_google_signin:
                GoogleSignIn();
                break;
        }
    }

    //endregion

    //region Google sign in

    private void initGoogleSignIn(){
        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();
    }

    private void GoogleSignIn(){
        Intent googleSignInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(googleSignInIntent, REQUEST_CODE_GOOGLE_SIGNIN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_GOOGLE_SIGNIN:
                if(data == null) return;
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if(result.isSuccess()){
                    GoogleSignInAccount account = result.getSignInAccount();
                    firebaseAuthWithGoogle(account);
                }else {
                    Log.e(MY_TAG, "google signin fail");
                }
                break;
            case REQUEST_CODE_FACEBOOK_SIGNIN:
                callbackManager.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account){
        if(account != null)
            CheckUserInFirebaseBySocialProfileID(account.getDisplayName(), account.getId(), ACCOUNT_TYPE_GOOGLE);
//        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
//        firebaseAuth.signInWithCredential(credential)
//                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if(task.isSuccessful()){
//                            SaveUserInFirebase(account.getDisplayName(), account.getId(), ACCOUNT_TYPE_GOOGLE);
//                        }else
//                            Log.e(MY_TAG, "firebase auth onComplete fail");
//                    }
//                });
    }

    private FirebaseAuth.AuthStateListener GetAuthStateListener(){
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if(firebaseUser != null){
                    Log.i(MY_TAG, "firebase auth success");
                }
                else {
                    Log.e(MY_TAG, "firebase auth fail");
                }
            }
        };
    }

    //endregion

    //region Facebook login

    private void initFacebookLogin(ViewGroup.LayoutParams lp){
        btn_facebook_login = (LoginButton) getView().findViewById(R.id.btn_facebook_login);
        btn_facebook_login.setReadPermissions("email", "public_profile");
        btn_facebook_login.setFragment(this);

        float fbIconScale = 1.45F;
        Drawable drawable = getContext().getResources().getDrawable(
                com.facebook.R.drawable.com_facebook_button_icon);
        drawable.setBounds(0, 0, (int)(drawable.getIntrinsicWidth()*fbIconScale),
                (int)(drawable.getIntrinsicHeight()*fbIconScale));
        btn_facebook_login.setCompoundDrawables(drawable, null, null, null);
        btn_facebook_login.setCompoundDrawablePadding(getContext().getResources().
                getDimensionPixelSize(R.dimen.fb_margin_override_textpadding));
        btn_facebook_login.setPadding(
                getContext().getResources().getDimensionPixelSize(
                        R.dimen.fb_margin_override_lr),
                getContext().getResources().getDimensionPixelSize(
                        R.dimen.fb_margin_override_top),
                0,
                getContext().getResources().getDimensionPixelSize(
                        R.dimen.fb_margin_override_bottom));

        callbackManager = CallbackManager.Factory.create();

        // Callback registration
        btn_facebook_login.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if(loginResult != null){
                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                                Profile profile = Profile.getCurrentProfile();
                            CheckUserInFirebaseBySocialProfileID(profile.getId(), profile.getFirstName() + " " + profile.getLastName(), ACCOUNT_TYPE_FACEBOOK);
                        }
                    });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "id,name");
                    request.setParameters(parameters);
                    request.executeAsync();
                }
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException exception) {
                exception.printStackTrace();
            }
        });

        LoginManager.getInstance().logOut();
    }

    //endregion

    //region dialogs

    private void ShowSelectNicknameDialog(){
        final Context ctx = getContext();
        final Dialog nicknameDialog = new Dialog(ctx);
        nicknameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nicknameDialog.setContentView(R.layout.select_nickname_dialog);
        final EditText et_nickname = (EditText)nicknameDialog.findViewById(R.id.et_select_nickname);
        final TextInputLayout til_nickname = (TextInputLayout) nicknameDialog.findViewById(R.id.til_nickname);
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
                til_nickname.setHint(editable.length() > 2 ? "" : getString(R.string.validation_message_nickname_too_short));
                btn_nickname_ok.setEnabled(editable.length() > 2);
            }
        });

        btn_nickname_ok.setEnabled(false);
        btn_nickname_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!et_nickname.getText().toString().matches(getString(R.string.regex_digits_letters_more_than_3))){
                    UIUtil.SetEditTextIsValid(ctx, et_nickname, false);
                    til_nickname.setHint(getString(R.string.validation_message_nickname_symbols));
                    return;
                }
                UIUtil.SetEditTextIsValid(ctx, et_nickname, true);
                CheckUserInFirebaseBySocialProfileID(et_nickname.getText().toString(), CommonUtil.GetAndroidID(getContext()), ACCOUNT_TYPE_NICKNAME);
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

    private void CheckUserInFirebaseBySocialProfileID(final String profileId, final String userName, final int accountTypeID){
        Query currentUserQuery
                = FirebaseDatabase.getInstance().getReference().child(getContext().getString(R.string.firebase_child_users))
                .orderByChild(User.USER_KEY_PROFILEID).equalTo(profileId);
        currentUserQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        User user1 = ds.getValue(User.class);
                        if (user1.getProfileID().equals(profileId)) {
                            String nickname = user1.getUsername();
                            SharedPreferencesUtil.SaveNicknameInSharedPreferences(getContext(), userName);
                            SharedPreferencesUtil.SaveProfileIDInSharedPreferences(getContext(), profileId);
                            mListener.onLoginMade(afterLoginAction);
                        }
                    }
                } else {
                    SaveUserInFirebase(userName, profileId, accountTypeID);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    private void SaveUserInFirebase(final String profileID, final String nickname, final int accountTypeID){
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
                    user.setProfileID(profileID);
                    user.setProfileTypeID(accountTypeID);
                    user.setFcmToken(FirebaseInstanceId.getInstance().getToken());
                    fdRef.push().setValue(user, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError != null){
                                databaseError.toException().printStackTrace();
                                Snackbar.make(getView(), getString(R.string.error_message_failed_to_save_user_by_nickname), Snackbar.LENGTH_SHORT).show();
                            } else {
                                SharedPreferencesUtil.SaveNicknameInSharedPreferences(getContext(), nickname);
                                SharedPreferencesUtil.SaveProfileIDInSharedPreferences(getContext(), profileID);
                                mListener.onLoginMade(afterLoginAction);
                            }
                        }
                    });

                } else {
                    user.setUsername(nickname);
                    fdRef.child(user.getKey()).setValue(user, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError != null){
                                databaseError.toException().printStackTrace();
                                Snackbar.make(getView(), getString(R.string.error_message_failed_to_save_user_by_nickname), Snackbar.LENGTH_SHORT).show();
                            } else {
                                SharedPreferencesUtil.SaveNicknameInSharedPreferences(getContext(), nickname);
                                mListener.onLoginMade(afterLoginAction);
                            }
                        }
                    });
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //endregion

    public interface OnLoginFragmentInteractionListener extends ICommonFragmentInteraction {
        void onLoginMade(int afterLoginAction);
    }
}
