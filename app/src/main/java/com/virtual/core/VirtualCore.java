package com.virtual.core;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;

import com.virtual.core.entity.VirtualApp;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.core.impl.VirtualStorage;
import com.virtual.core.pm.VirtualPackageManager;
import com.virtual.core.am.VirtualActivityManager;
import com.virtual.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualCore {

    private static final String TAG = "VirtualCore";
    private static VirtualCore instance;

    private Context context;
    private VirtualStorage storage;
    private VirtualPackageManager packageManager;
    private VirtualActivityManager activityManager;
    
    private final Map<String, VirtualApp> virtualApps = new ConcurrentHashMap<>();
    private final Map<String, VirtualPackage> virtualPackages = new ConcurrentHashMap<>();
    private final Map<String, IBinder> services = new ConcurrentHashMap<>();
    
    private boolean isInitialized = false;

    public static VirtualCore get() {
        if (instance == null) {
            synchronized (VirtualCore.class) {
                if (instance == null) {
                    instance = new VirtualCore();
                }
            }
        }
        return instance;
    }

    public void init(Context ctx) {
        if (isInitialized) return;
        
        this.context = ctx.getApplicationContext();
        this.storage = new VirtualStorage(ctx);
        this.storage.init();
        
        this.packageManager = new VirtualPackageManager(this);
        this.activityManager = new VirtualActivityManager(this);
        
        loadVirtualApps();
        
        isInitialized = true;
        Log.i(TAG, "VirtualCore initialized for Android " + Build.VERSION.SDK_INT);
    }

    private void loadVirtualApps() {
        Map<String, VirtualApp> apps = storage.getAllApps();
        virtualApps.putAll(apps);
        
        Map<String, VirtualPackage> packages = storage.getAllPackages();
        virtualPackages.putAll(packages);
        
        Log.i(TAG, "Loaded " + virtualApps.size() + " virtual apps, " + virtualPackages.size() + " packages");
    }

    public Context getContext() {
        return context;
    }

    public VirtualPackageManager getPackageManager() {
        return packageManager;
    }

    public VirtualActivityManager getActivityManager() {
        return activityManager;
    }

    public VirtualStorage getStorage() {
        return storage;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isVirtualProcess() {
        int uid = Process.myUid();
        return uid >= 10000;
    }

    public VirtualApp createVirtualApp(String packageName, String appName) {
        if (virtualApps.containsKey(packageName)) {
            return virtualApps.get(packageName);
        }

        VirtualApp app = new VirtualApp();
        app.packageName = packageName;
        app.appName = appName;
        app.userId = allocateUserId();
        app.clonedPackages = new ArrayList<>();
        app.createdTime = System.currentTimeMillis();
        app.isActive = true;
        
        app.fakeDeviceId = generateFakeDeviceId();
        app.fakeAndroidId = generateFakeAndroidId();
        app.fakePackageName = packageName + ".clone" + app.userId;

        virtualApps.put(packageName, app);
        storage.saveApp(app);

        Log.i(TAG, "Created virtual app: " + packageName + " (userId=" + app.userId + ")");
        return app;
    }

    public boolean removeVirtualApp(String packageName) {
        VirtualApp app = virtualApps.remove(packageName);
        if (app != null) {
            for (String pkg : app.clonedPackages) {
                String key = pkg + "_" + app.userId;
                virtualPackages.remove(key);
            }
            storage.deleteApp(packageName);
            Log.i(TAG, "Removed virtual app: " + packageName);
            return true;
        }
        return false;
    }

    public VirtualApp getVirtualApp(String packageName) {
        return virtualApps.get(packageName);
    }

    public List<VirtualApp> getAllVirtualApps() {
        return new ArrayList<>(virtualApps.values());
    }

    public VirtualPackage clonePackage(String sourcePackage, int userId) {
        String key = sourcePackage + "_" + userId;
        
        if (virtualPackages.containsKey(key)) {
            return virtualPackages.get(key);
        }

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo sourceInfo = pm.getPackageInfo(sourcePackage, 0);

            VirtualPackage vPkg = new VirtualPackage();
            vPkg.sourcePackage = sourcePackage;
            vPkg.packageName = sourcePackage + ".clone" + userId;
            vPkg.appName = sourceInfo.applicationInfo.loadLabel(pm) + " (Clone)";
            vPkg.versionCode = sourceInfo.versionCode;
            vPkg.versionName = sourceInfo.versionName;
            vPkg.userId = userId;
            vPkg.apkPath = sourceInfo.applicationInfo.sourceDir;
            vPkg.installedTime = System.currentTimeMillis();
            vPkg.firstInstallTime = sourceInfo.firstInstallTime;
            vPkg.lastUpdateTime = sourceInfo.lastUpdateTime;
            vPkg.signatures = sourceInfo.signatures;

            virtualPackages.put(key, vPkg);
            storage.savePackage(vPkg);

            VirtualApp app = getVirtualAppByUserId(userId);
            if (app != null && !app.clonedPackages.contains(sourcePackage)) {
                app.clonedPackages.add(sourcePackage);
            }

            Log.i(TAG, "Cloned package: " + sourcePackage + " -> " + vPkg.packageName);
            return vPkg;

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to clone package: " + sourcePackage, e);
            return null;
        }
    }

    public VirtualPackage getVirtualPackage(String sourcePackage, int userId) {
        String key = sourcePackage + "_" + userId;
        return virtualPackages.get(key);
    }

    public VirtualPackage getVirtualPackageByName(String packageName) {
        for (VirtualPackage pkg : virtualPackages.values()) {
            if (pkg.packageName.equals(packageName)) {
                return pkg;
            }
        }
        return null;
    }

    public boolean isVirtualPackage(String packageName) {
        return getVirtualPackageByName(packageName) != null;
    }

    public String getSourcePackage(String virtualPackage) {
        VirtualPackage pkg = getVirtualPackageByName(virtualPackage);
        return pkg != null ? pkg.sourcePackage : null;
    }

    public VirtualApp getVirtualAppByUserId(int userId) {
        for (VirtualApp app : virtualApps.values()) {
            if (app.userId == userId) {
                return app;
            }
        }
        return null;
    }

    private int allocateUserId() {
        int baseId = 10000;
        for (VirtualApp app : virtualApps.values()) {
            if (app.userId >= baseId) {
                baseId = app.userId + 1;
            }
        }
        return baseId;
    }

    public void registerService(String name, IBinder service) {
        services.put(name, service);
    }

    public IBinder getService(String name) {
        return services.get(name);
    }

    private String generateFakeDeviceId() {
        return "15" + String.format("%014d", (long)(Math.random() * 100000000000000L));
    }

    private String generateFakeAndroidId() {
        return String.format("%016x", (long)(Math.random() * 0xFFFFFFFFFFFFL));
    }

    public boolean isSupportVersion() {
        int sdk = Build.VERSION.SDK_INT;
        return sdk >= 28 && sdk <= 35;
    }
    
    public String getSupportVersionRange() {
        return "Android 9 (Pie) - Android 15";
    }
}
