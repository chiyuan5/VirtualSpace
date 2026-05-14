package com.virtual.app.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.virtual.app.R;
import com.virtual.app.adapter.AppAdapter;
import com.virtual.util.VirtualLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstalledAppsFragment extends Fragment {

    private static final String TAG = "InstalledAppsFragment";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyView;
    private ProgressBar progressBar;

    private AppAdapter adapter;
    private List<PackageInfo> installedApps = new ArrayList<>();
    private String currentFilter = "";

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

        loadInstalledApps();
    }

    private void setupRecyclerView() {
        adapter = new AppAdapter(installedApps, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((position, packageInfo) -> {
            openAppDetails(packageInfo);
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadInstalledApps);
            swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        }
    }

    private void loadInstalledApps() {
        if (getContext() == null) return;

        if (progressBar != null) {
            if (installedApps.isEmpty()) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }

        new LoadAppsTask(requireContext()).execute();
    }

    private void openAppDetails(PackageInfo packageInfo) {
        CloneSelectActivity.startForPackage(requireContext(), packageInfo.packageName);
    }

    public void filter(String query) {
        currentFilter = query != null ? query.toLowerCase() : "";
        if (adapter != null) {
            adapter.getFilter().filter(currentFilter);
        }
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<PackageInfo>> {

        private final Context context;

        LoadAppsTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected List<PackageInfo> doInBackground(Void... voids) {
            List<PackageInfo> apps = new ArrayList<>();

            try {
                PackageManager pm = context.getPackageManager();
                List<PackageInfo> packages = pm.getInstalledPackages(0);

                for (PackageInfo pkg : packages) {
                    if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        apps.add(pkg);
                    }
                }

                Collections.sort(apps, (p1, p2) -> {
                    String name1 = p1.applicationInfo.loadLabel(pm).toString();
                    String name2 = p2.applicationInfo.loadLabel(pm).toString();
                    return name1.compareToIgnoreCase(name2);
                });

            } catch (Exception e) {
                VirtualLog.e(TAG, "Error loading apps", e);
            }

            return apps;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> result) {
            if (!isAdded() || getContext() == null) return;

            installedApps.clear();
            installedApps.addAll(result);

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }

            if (emptyView != null) {
                if (installedApps.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.GONE);
                }
            }
        }
    }
}
