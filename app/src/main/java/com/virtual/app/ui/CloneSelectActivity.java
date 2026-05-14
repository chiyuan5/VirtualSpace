package com.virtual.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.virtual.app.R;
import com.virtual.app.adapter.AppAdapter;
import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.util.VirtualLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloneSelectActivity extends AppCompatActivity {

    private static final String TAG = "CloneSelectActivity";
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    private RecyclerView recyclerView;
    private LinearProgressIndicator progressBar;
    private ExtendedFloatingActionButton fabClone;

    private AppAdapter adapter;
    private String targetPackage;
    private VirtualCore core;

    public static void startForPackage(Context context, String packageName) {
        Intent intent = new Intent(context, CloneSelectActivity.class);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clone_select);

        targetPackage = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        core = VirtualCore.get();

        VirtualLog.d(TAG, "Target package: " + targetPackage);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFab();

        if (targetPackage != null) {
            createCloneForPackage(targetPackage);
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        fabClone = findViewById(R.id.fabClone);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.btn_create_clone);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupFab() {
        fabClone.setOnClickListener(v -> {
            if (targetPackage != null) {
                performClone();
            }
        });
    }

    private void createCloneForPackage(String packageName) {
        VirtualLog.d(TAG, "Creating clone for: " + packageName);

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (fabClone != null) {
            fabClone.setVisibility(View.GONE);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                PackageManager pm = getPackageManager();
                String appName = packageName;
                try {
                    appName = pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString();
                    VirtualLog.d(TAG, "App name: " + appName);
                } catch (Exception e) {
                    VirtualLog.e(TAG, "Failed to get app name", e);
                }

                VirtualLog.d(TAG, "Calling core.createVirtualApp...");
                VirtualApp virtualApp = core.createVirtualApp(packageName, appName);

                VirtualLog.d(TAG, "Virtual app created: " + (virtualApp != null));

                runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (virtualApp != null) {
                        VirtualLog.d(TAG, "Clone success, showing toast");
                        Toast.makeText(this, getString(R.string.toast_clone_success) + ": " + appName, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        VirtualLog.d(TAG, "Clone failed, showing toast");
                        Toast.makeText(this, R.string.toast_clone_failed, Toast.LENGTH_SHORT).show();
                        if (fabClone != null) {
                            fabClone.setVisibility(View.VISIBLE);
                        }
                    }
                });

            } catch (Exception e) {
                VirtualLog.e(TAG, "Failed to clone package", e);
                runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, getString(R.string.toast_clone_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (fabClone != null) {
                        fabClone.setVisibility(View.VISIBLE);
                    }
                });
            } finally {
                executor.shutdown();
            }
        });
    }

    private void performClone() {
        createCloneForPackage(targetPackage);
    }
}
