package Fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

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

    //region loc report frequency

    private static HashMap<String, Integer> getFrequencyOptions(){
        HashMap<String, Integer> result = new HashMap<>();
        result.put("1 second", 1000);
        result.put("3 seconds", 3000);
        result.put("5 seconds", 5000);
        result.put("10 seconds", 10000);
        result.put("20 seconds", 20000);
        result.put("30 seconds", 30000);
        result.put("1 minute", 1 * 60000);
        result.put("2 minutes", 2 * 60000);
        result.put("5 minutes", 5 * 60000);
        result.put("10 minutes", 10 * 60000);
        result.put("30 minutes", 30 * 60000);
        result.put("1 hour", 60 * 60000);
        return result;
    }

    private static String getFrequencyNameByValue(HashMap<String, Integer> map, int value){
        String result = "";
        for(String name : map.keySet()){
            if(map.get(name) == value) {
                result = name;
                break;
            }
        }
        return result;
    }

    //endregion

    public interface ISettingsFragmentInteraction extends ICommonFragmentInteraction {

    }

}
