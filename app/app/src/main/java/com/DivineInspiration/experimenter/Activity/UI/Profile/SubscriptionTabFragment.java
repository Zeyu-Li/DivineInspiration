package com.DivineInspiration.experimenter.Activity.UI.Profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.DivineInspiration.experimenter.Controller.ExperimentManager;
import com.DivineInspiration.experimenter.Controller.UserManager;
import com.DivineInspiration.experimenter.Model.Experiment;
import com.DivineInspiration.experimenter.R;
import androidx.recyclerview.widget.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionTabFragment extends Fragment{

    private ExperimentAdapter adapter;
    ArrayList<Experiment> subExps = new ArrayList<>();

    /**
     * On create
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Log.d("MESSAGE", "onCreateView");
        return inflater.inflate(R.layout.experiment_list, container, false);

    }

    /**
     * On view
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set experiment adapter
        adapter = new ExperimentAdapter(subExps);

        // recycler list

        RecyclerView recycler = view.findViewById(R.id.experimentList);
        recycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        // get subbed users
        ExperimentManager.getInstance().queryUserSubs(UserManager.getInstance().getLocalUser().getUserId(), new ExperimentManager.OnExperimentListReadyListener() {

            @Override
            public void onExperimentsReady(List<Experiment> experiments) {
                subExps.clear();
                subExps.addAll(experiments);
                adapter.notifyDataSetChanged();
            }
        });
    }
}
