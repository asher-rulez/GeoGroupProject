package Fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import novitskyvitaly.geogroupproject.R;

public class SettingsFragment extends Fragment {

    ISettingsFragmentInteraction mListener;

    public SettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ISettingsFragmentInteraction) {
            mListener = (ISettingsFragmentInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_settings));
        mListener.SetMainToolbarGoToMapVisible(true);
    }

    public interface ISettingsFragmentInteraction extends ICommonFragmentInteraction {

    }

}
