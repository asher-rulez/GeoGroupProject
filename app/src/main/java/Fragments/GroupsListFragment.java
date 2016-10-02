package Fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import Adapters.GroupsListRecyclerViewAdapter;
import novitskyvitaly.geogroupproject.R;

public class GroupsListFragment extends Fragment implements GroupsListRecyclerViewAdapter.IGroupsListRecyclerViewInteraction {

    IGroupsListFragmentInteraction mListener;

    private static View view;
    private RecyclerView rv_groups_list;
    GroupsListRecyclerViewAdapter adapter;
    ProgressBar pb_loading_groups;
    TextView tv_groups_not_found;

    public GroupsListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_groups_list, container, false);
        rv_groups_list = (RecyclerView)view.findViewById(R.id.rv_groups_list);
        initRecyclerView();
        pb_loading_groups = (ProgressBar)view.findViewById(R.id.pb_loading_groups);
        tv_groups_not_found = (TextView)view.findViewById(R.id.tv_groups_list_not_found);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IGroupsListFragmentInteraction) {
            mListener = (IGroupsListFragmentInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.MakeFabsVisibleForGroupsList();
        mListener.SetMainToolbarGoToMapVisible(true);
        mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_groups_list));
    }

    private void initRecyclerView() {
        rv_groups_list.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(rv_groups_list.getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv_groups_list.setLayoutManager(llm);
        adapter = new GroupsListRecyclerViewAdapter(getContext(), this);
        rv_groups_list.setAdapter(adapter);
    }

    @Override
    public void onGroupSelected(String groupKey) {
        mListener.OnGroupSelectedFromList(groupKey);
    }

    @Override
    public void onFirstGroupLoaded() {
        pb_loading_groups.setVisibility(ProgressBar.GONE);
        tv_groups_not_found.setVisibility(View.GONE);
    }

    @Override
    public void onFoundNoGroups() {
        pb_loading_groups.setVisibility(ProgressBar.GONE);
        tv_groups_not_found.setVisibility(View.VISIBLE);
    }

    public interface IGroupsListFragmentInteraction extends ICommonFragmentInteraction {
        void MakeFabsVisibleForGroupsList();
        void OnGroupSelectedFromList(String groupKey);
    }

}
