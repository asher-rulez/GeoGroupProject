package Fragments;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Utils.SharedPreferencesUtil;
import novitskyvitaly.geogroupproject.R;

public class SettingsFragment extends Fragment implements View.OnClickListener {

    ISettingsFragmentInteraction mListener;

    RelativeLayout rl_btn_set_frequency;
    TextView tv_frequency_title;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        InitButtonSelectFrequency();
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.SetupMainToolbarTitle(getString(R.string.toolbar_title_fragment_settings));
        mListener.SetMainToolbarGoToMapVisible(true);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_btn_settings_loc_reports_frequency:
                ShowChooseFrequencyDialog();
                break;
        }
    }

    //region loc report frequency

    private void InitButtonSelectFrequency() {
        rl_btn_set_frequency = (RelativeLayout) getView().findViewById(R.id.rl_btn_settings_loc_reports_frequency);
        rl_btn_set_frequency.setOnClickListener(this);
        tv_frequency_title = (TextView) getView().findViewById(R.id.tv_settings_report_frequency);
        int frequency = SharedPreferencesUtil.GetLocationRefreshFrequency(getContext());
        HashMap<String, Integer> frequencyMap = getFrequencyOptions();
        tv_frequency_title.setText(getFrequencyNameByValue(frequencyMap, frequency));
    }

    private void ShowChooseFrequencyDialog() {
        final Context ctx = getContext();
        final Dialog frequencyDialog = new Dialog(ctx);
        frequencyDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        frequencyDialog.setContentView(R.layout.select_report_frequency_dialog);
        ListView lv_options = (ListView) frequencyDialog.findViewById(R.id.lv_report_frequency_options);

        Set<Integer> frequencies = getFrequencyOptionsInverted().keySet();
        ArrayList<Integer> frequenciesArrayList = new ArrayList<>();
        frequenciesArrayList.addAll(frequencies);
        Collections.sort(frequenciesArrayList);
        final List<String> frequencyTitles = new ArrayList<>();
        for(Integer frequency : frequenciesArrayList)
                frequencyTitles.add(getFrequencyOptionsInverted().get(frequency));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.select_report_frequency_item, R.id.tv_frequency_item, frequencyTitles);
        lv_options.setAdapter(adapter);
        lv_options.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                tv_frequency_title.setText(frequencyTitles.get(i));
                frequencyDialog.dismiss();
                SharedPreferencesUtil.SetLocationRefreshFrequency(getContext(), getFrequencyOptions().get(frequencyTitles.get(i)));
            }
        });
        frequencyDialog.show();
    }

    private static HashMap<String, Integer> getFrequencyOptions() {
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

    private static HashMap<Integer, String> getFrequencyOptionsInverted() {
        HashMap<Integer, String> result = new HashMap<>();
        result.put(1000, "1 second");
        result.put(3000, "3 seconds");
        result.put(5000, "5 seconds");
        result.put(10000, "10 seconds");
        result.put(20000, "20 seconds");
        result.put(30000, "30 seconds");
        result.put(1 * 60000, "1 minute");
        result.put(2 * 60000, "2 minutes");
        result.put(5 * 60000, "5 minutes");
        result.put(10 * 60000, "10 minutes");
        result.put(30 * 60000, "30 minutes");
        result.put(60 * 60000, "1 hour");
        return result;
    }

    private static String getFrequencyNameByValue(HashMap<String, Integer> map, int value) {
        String result = "";
        for (String name : map.keySet()) {
            if (map.get(name) == value) {
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
