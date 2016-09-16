package Fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import DataModel.Group;
import Utils.FirebaseUtil;
import Utils.SharedPreferencesUtil;
import novitskyvitaly.geogroupproject.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class GroupFragment extends Fragment implements View.OnClickListener {
    private final static String MY_TAG = "geog_group";

    IGroupFragmentInteraction mListener;

    Group group;
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
        cb_is_track = (CheckBox)view.findViewById(R.id.cb_group_fragment_track);
        rl_members = (RelativeLayout)view.findViewById(R.id.rl_btn_group_fragment_members);
        rl_members.setOnClickListener(this);
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

    private void reloadGroup(String groupKey){
        FirebaseUtil.GetSingleGroupReferenceByGroupKey(getContext(), groupKey, new FirebaseUtil.IFirebaseInitListenersCallback() {
            @Override
            public void OnSingleGroupResolved(Group group) {
                setGroup(group);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_send_group_data:
                break;
            case R.id.rl_btn_group_fragment_track:
                break;
            case R.id.rl_btn_group_fragment_members:
                break;
            case R.id.btn_group_fragment_messages:
                break;
            case R.id.btn_group_fragment_events:
                break;
            case R.id.btn_group_fragment_leave_delete:
                break;
        }
    }

    public interface IGroupFragmentInteraction extends ICommonFragmentInteraction{

    }

}
