package com.virtual.app.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.virtual.app.R;
import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CloneDetailActivity extends AppCompatActivity {

    private static final String TAG = "CloneDetailActivity";
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    private VirtualCore core;
    private VirtualApp currentApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clone_detail);

        core = VirtualCore.get();

        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        if (packageName != null) {
            for (VirtualApp app : core.getAllVirtualApps()) {
                if (app.packageName.equals(packageName)) {
                    currentApp = app;
                    break;
                }
            }
        }

        initViews();
        setupToolbar();
        displayAppDetails();
    }

    private void initViews() {
        if (currentApp == null) {
            finish();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.clone_info_title);
        }
    }

    private void displayAppDetails() {
        if (currentApp == null) {
            return;
        }

        ImageView appIcon = findViewById(R.id.appIcon);
        TextView appNameView = findViewById(R.id.appName);
        TextView packageNameView = findViewById(R.id.packageName);
        TextView userIdView = findViewById(R.id.userId);
        TextView installTimeView = findViewById(R.id.installTime);
        TextView cloneCountView = findViewById(R.id.cloneCount);
        TextView fakeDeviceIdView = findViewById(R.id.fakeDeviceId);
        TextView fakeAndroidIdView = findViewById(R.id.fakeAndroidId);

        try {
            appNameView.setText(currentApp.appName);
            packageNameView.setText(currentApp.packageName);
            userIdView.setText(String.valueOf(currentApp.userId));
            cloneCountView.setText(String.valueOf(currentApp.clonedPackages.size()));
            fakeDeviceIdView.setText(currentApp.fakeDeviceId);
            fakeAndroidIdView.setText(currentApp.fakeAndroidId);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            installTimeView.setText(sdf.format(new Date()));

            android.content.pm.ApplicationInfo appInfo = getPackageManager()
                .getApplicationInfo(currentApp.packageName, 0);
            if (appIcon != null) {
                appIcon.setImageDrawable(appInfo.loadIcon(getPackageManager()));
            }
        } catch (Exception e) {
            appNameView.setText(currentApp.packageName);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}