package com.virtual.core;

import android.content.Context;

import com.virtual.core.entity.VirtualApp;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.core.impl.VirtualStorage;
import com.virtual.util.VirtualLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualCore {

    private static final String TAG = "VirtualCore";
    private static VirtualCore instance;

    private Context context;
    private VirtualStorage storage;
    
    private final Map<String, VirtualApp> virtualApps = new ConcurrentHashMap<>();
    private final Map<String, VirtualPackage> virtualPackages = new ConcurrentHashMap<>();
    
    private boolean isInitialized = false;

    private VirtualCore() {}

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
        if (isInitialized) {
            VirtualLog.w(TAG, "Already initialized");
            return;
        }

        this.context = ctx.getApplicationContext();
        this.storage = new VirtualStorage(this.context);
        this.storage.init();
        loadVirtualApps();
        
        isInitialized = true;
        VirtualLog.i(TAG, "VirtualCore initialized");
    }

    public Context getContext() {
        return context;
    }

    public boolean isSupportVersion() {
        return true;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private void loadVirtualApps() {
        List<VirtualApp> apps = storage.loadVirtualApps();
        for (VirtualApp app : apps) {
            virtualApps.put(app.packageName, app);
        }
        VirtualLog.i(TAG, "Loaded " + apps.size() + " virtual apps");
    }

    public VirtualApp createVirtualApp(String packageName, String appName) {
        if (virtualApps.containsKey(packageName)) {
            VirtualLog.w(TAG, "Virtual app already exists: " + packageName);
            return virtualApps.get(packageName);
        }

        int userId = getNextUserId();
        VirtualApp app = new VirtualApp();
        app.packageName = packageName;
        app.appName = appName;
        app.userId = userId;
        app.fakePackageName = packageName + ".clone" + userId;
        app.fakeDeviceId = generateFakeDeviceId();
        app.fakeAndroidId = generateFakeAndroidId();
        app.isActive = true;

        virtualApps.put(packageName, app);
        storage.saveVirtualApps(new ArrayList<>(virtualApps.values()));
        
        VirtualLog.i(TAG, "Created virtual app: " + appName);
        return app;
    }

    public boolean removeVirtualApp(String packageName) {
        VirtualApp app = virtualApps.remove(packageName);
        if (app != null) {
            storage.saveVirtualApps(new ArrayList<>(virtualApps.values()));
            VirtualLog.i(TAG, "Removed virtual app: " + packageName);
            return true;
        }
        return false;
    }

    public List<VirtualApp> getAllVirtualApps() {
        return new ArrayList<>(virtualApps.values());
    }

    public VirtualApp getVirtualAppByUserId(int userId) {
        for (VirtualApp app : virtualApps.values()) {
            if (app.userId == userId) {
                return app;
            }
        }
        return null;
    }

    public VirtualPackage getVirtualPackageByName(String packageName) {
        return virtualPackages.get(packageName);
    }

    public boolean isVirtualPackage(String packageName) {
        return virtualApps.containsKey(packageName) || virtualPackages.containsKey(packageName);
    }

    public void addVirtualPackage(VirtualPackage vPkg) {
        virtualPackages.put(vPkg.packageName, vPkg);
    }

    private int getNextUserId() {
        int maxId = 0;
        for (VirtualApp app : virtualApps.values()) {
            if (app.userId > maxId) {
                maxId = app.userId;
            }
        }
        return maxId + 1;
    }

    private String generateFakeDeviceId() {
        return "fake_device_" + System.currentTimeMillis();
    }

    private String generateFakeAndroidId() {
        return String.format("%08x", (int) System.currentTimeMillis());
    }

    public void cleanup() {
        virtualApps.clear();
        virtualPackages.clear();
        isInitialized = false;
        VirtualLog.i(TAG, "VirtualCore cleaned up");
    }
}