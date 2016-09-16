package Fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import Adapters.GroupsListRecyclerViewAdapter;
import novitskyvitaly.geogroupproject.R;

public class GroupsListFragment extends Fragment {

    IGroupsListFragmentInteraction mListener;

    private static View view;
    private RecyclerView rv_groups_list;
    GroupsListRecyclerViewAdapter adapter;

    public GroupsListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_groups_list, container, false);
        rv_groups_list = (RecyclerView)view.findViewById(R.id.rv_groups_list);
        initRecyclerView();

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
        //rv_groups_list.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(rv_groups_list.getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv_groups_list.setLayoutManager(llm);
        adapter = new GroupsListRecyclerViewAdapter(getContext());
        rv_groups_list.setAdapter(adapter);
    }

    public interface IGroupsListFragmentInteraction extends ICommonFragmentInteraction {
        void MakeFabsVisibleForGroupsList();
    }

}
