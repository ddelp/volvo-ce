package com.ddelp.volvoce.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ddelp.volvoce.R;
import com.ddelp.volvoce.WorksiteViewActivity;
import com.ddelp.volvoce.helpers.DatabaseHelper;
import com.ddelp.volvoce.helpers.WorksiteListAdapter;
import com.ddelp.volvoce.objects.WorksiteSummary;

import java.util.ArrayList;

public class WorksiteListFragment extends Fragment{

    private static final String TAG = WorksiteListFragment.class.getSimpleName();

    private WorksiteListAdapter worksiteListAdapter;
    private DatabaseHelper databaseHelper;
    private ArrayList<WorksiteSummary> worksiteSummaries;

    public WorksiteListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_worksite_list, container, false);

        // Load worksites into ListView
        worksiteSummaries = new ArrayList<>();
        databaseHelper = DatabaseHelper.getInstance(getActivity());
        ArrayList<String> worksites = databaseHelper.getWorksiteList();
        try {
            for(String worksiteName : worksites) {
                WorksiteSummary worksiteSummary = new WorksiteSummary(R.drawable.worksite1, worksiteName);
                if(worksiteName.contentEquals("worksite1")) {
                    worksiteSummary.imageID = R.drawable.worksite1;
                } else if(worksiteName.contentEquals("worksite2")) {
                    worksiteSummary.imageID = R.drawable.worksite2;
                } else if(worksiteName.contentEquals("worksite3")) {
                    worksiteSummary.imageID = R.drawable.worksite3;
                }
                worksiteSummaries.add(worksiteSummary);
            }
            ListView rideHistoryView = (ListView) rootView.findViewById(R.id.worksite_list_view);
            worksiteListAdapter = new WorksiteListAdapter(getActivity(), R.layout.worksite_list_row, worksiteSummaries);
            rideHistoryView.setAdapter(worksiteListAdapter);
            Log.i(TAG, "ListView adapter set");
            rideHistoryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
                    openWorksite(pos);
                }
            });
        } catch (Exception e) {
            Log.i(TAG, "Error generating worksite select list");
        }
        return rootView;
    }

       void openWorksite(int pos) {
           Log.i(TAG, getString(R.string.app_name) + ": Starting worksite view activity");
           String worksiteName = worksiteSummaries.get(pos).name;
           Intent mainIntent = new Intent(getActivity(), WorksiteViewActivity.class);
           mainIntent.putExtra("worksite_name",worksiteName);
           getActivity().startActivity(mainIntent);
    }
}





