package com.virtual.app.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;
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
            currentApp = core.getVirtualApp(packageName);
        }

        initViews();
        setupToolbar();
        displayAppDetails();
    }

    private void initViews() {
        SwitchMaterial switchSpoofDevice = findViewById(R.id.switchSpoofDevice);
        SwitchMaterial switchSpoofLocation = findViewById(R.id.switchSpoofLocation);
        SwitchMaterial switchAutoStart = findViewById(R.id.switchAutoStart);

        if (switchSpoofDevice != null) {
            switchSpoofDevice.setChecked(true);
        }
        if (switchSpoofLocation != null) {
            switchSpoofLocation.setChecked(false);
        }
        if (switchAutoStart != null) {
            switchAutoStart.setChecked(false);
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
        TextView appName = findViewById(R.id.appName);
        TextView packageName = findViewById(R.id.packageName);
        TextView userId = findViewById(R.id.userId);
        TextView installTime = findViewById(R.id.installTime);
        TextView cloneCount = findViewById(R.id.cloneCount);
        TextView fakeDeviceId = findViewById(R.id.fakeDeviceId);
        TextView fakeAndroidId = findViewById(R.id.fakeAndroidId);

        try {
            appName.setText(currentApp.appName);
            packageName.setText(currentApp.packageName);
            userId.setText(String.valueOf(currentApp.userId));
            cloneCount.setText(String.valueOf(currentApp.clonedPackages.size()));
            fakeDeviceId.setText(currentApp.fakeDeviceId);
            fakeAndroidId.setText(currentApp.fakeAndroidId);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            installTime.setText(sdf.format(new Date(currentApp.createdTime)));

            android.content.pm.ApplicationInfo appInfo = getPackageManager()
                .getApplicationInfo(currentApp.packageName, 0);
            if (appIcon != null) {
                appIcon.setImageDrawable(appInfo.loadIcon(getPackageManager()));
            }
        } catch (Exception e) {
            appName.setText(currentApp.packageName);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
