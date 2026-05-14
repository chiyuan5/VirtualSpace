package com.virtual.app.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.virtual.app.R;
import com.virtual.app.adapter.ClonedAppAdapter;
import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;

import java.util.ArrayList;
import java.util.List;

public class ClonedAppsFragment extends Fragment {

    private static final String TAG = "ClonedAppsFragment";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyView;
    private ProgressBar progressBar;

    private ClonedAppAdapter adapter;
    private List<VirtualApp> clonedApps = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);

        setupRecyclerView();
        setupSwipeRefresh();

        loadClonedApps();
    }

    private void setupRecyclerView() {
        adapter = new ClonedAppAdapter(clonedApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((position, app) -> {
            openCloneDetail(app);
        });

        adapter.setOnOpenClickListener(new ClonedAppAdapter.OnActionClickListener() {
            @Override
            public void onOpenClick(int position, VirtualApp app) {
                openClone(app);
            }

            @Override
            public void onDeleteClick(int position, VirtualApp app) {
            }
        });

        adapter.setOnDeleteClickListener(new ClonedAppAdapter.OnActionClickListener() {
            @Override
            public void onOpenClick(int position, VirtualApp app) {
            }

            @Override
            public void onDeleteClick(int position, VirtualApp app) {
                confirmDelete(app);
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadClonedApps);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
    }

    private void loadClonedApps() {
        if (clonedApps.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        emptyView.setVisibility(View.GONE);

        new LoadClonedAppsTask().execute();
    }

    public void refresh() {
        loadClonedApps();
    }

    private void openCloneDetail(VirtualApp app) {
        Intent intent = new Intent(requireContext(), CloneDetailActivity.class);
        intent.putExtra("package_name", app.packageName);
        startActivity(intent);
    }

    private void openClone(VirtualApp app) {
        try {
            Intent launchIntent = requireContext().getPackageManager()
                .getLaunchIntentForPackage(app.fakePackageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                requireContext().startActivity(launchIntent);
            }
        } catch (Exception e) {
        }
    }

    private void confirmDelete(VirtualApp app) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                deleteClone(app);
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void deleteClone(VirtualApp app) {
        VirtualCore core = VirtualCore.get();
        boolean success = core.removeVirtualApp(app.packageName);

        if (success) {
            loadClonedApps();
        }
    }

    public void filter(String query) {
    }

    private class LoadClonedAppsTask extends AsyncTask<Void, Void, List<VirtualApp>> {

        @Override
        protected List<VirtualApp> doInBackground(Void... voids) {
            try {
                return VirtualCore.get().getAllVirtualApps();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        @Override
        protected void onPostExecute(List<VirtualApp> result) {
            if (!isAdded() || getContext() == null) return;

            clonedApps.clear();
            clonedApps.addAll(result);

            adapter.notifyDataSetChanged();

            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);

            if (clonedApps.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }
}