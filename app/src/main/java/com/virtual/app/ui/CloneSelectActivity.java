package com.virtual.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.virtual.app.R;
import com.virtual.app.adapter.AppAdapter;
import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloneSelectActivity extends AppCompatActivity {

    private static final String TAG = "CloneSelectActivity";
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    private RecyclerView recyclerView;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabClone;

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
        progressBar.setVisibility(View.VISIBLE);
        fabClone.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                VirtualApp existingApp = core.getVirtualApp(packageName);
                if (existingApp == null) {
                    core.createVirtualApp(packageName, "");
                }

                VirtualPackage vPkg = null;
                for (VirtualApp app : core.getAllVirtualApps()) {
                    if (app.packageName.equals(packageName)) {
                        vPkg = core.clonePackage(packageName, app.userId);
                        break;
                    }
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (vPkg != null) {
                        Toast.makeText(this, R.string.toast_clone_success, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.toast_clone_failed, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to clone package", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.toast_clone_failed, Toast.LENGTH_SHORT).show();
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
