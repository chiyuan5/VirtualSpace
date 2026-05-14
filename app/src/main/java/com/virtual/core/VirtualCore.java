package com.virtual.core;

import android.content.Context;
import android.os.Build;

import com.virtual.core.entity.VirtualApp;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.core.fs.VirtualFileSystem;
import com.virtual.core.impl.VirtualStorage;
import com.virtual.core.system.VirtualActivityManager;
import com.virtual.core.system.VirtualPackageManager;
import com.virtual.hook.BinderHook;
import com.virtual.hook.NetworkFixer;
import com.virtual.hook.WebViewFixer;
import com.virtual.util.VirtualLog;

import java.io.File;
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

    private VirtualPackageManager packageManager;
    private VirtualActivityManager activityManager;
    private VirtualFileSystem fileSystem;
    private BinderHook binderHook;

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
        
        VirtualLog.i(TAG, "Initializing VirtualCore...");
        VirtualLog.i(TAG, "Android Version: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");

        this.storage = new VirtualStorage(this.context);
        this.storage.init();

        this.fileSystem = VirtualFileSystem.getInstance(this.context);
        this.packageManager = VirtualPackageManager.getInstance(this.context);
        this.activityManager = VirtualActivityManager.getInstance(this.context);
        
        loadVirtualApps();
        
        for (VirtualApp app : virtualApps.values()) {
            packageManager.registerVirtualApp(app);
            fileSystem.initializeVirtualEnvironment(app);
        }
        
        try {
            WebViewFixer.init(this.context);
            VirtualLog.d(TAG, "WebView fixes initialized");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to init WebView fixes", e);
        }
        
        try {
            NetworkFixer.init(this.context);
            VirtualLog.d(TAG, "Network fixes initialized");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to init Network fixes", e);
        }
        
        try {
            this.binderHook = BinderHook.getInstance(this.context);
            VirtualLog.d(TAG, "BinderHook instance created");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create BinderHook", e);
        }
        
        isInitialized = true;
        VirtualLog.i(TAG, "VirtualCore initialized successfully");
        VirtualLog.i(TAG, "Loaded " + virtualApps.size() + " virtual apps");
    }

    public Context getContext() {
        return context;
    }

    public boolean isSupportVersion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private void loadVirtualApps() {
        List<VirtualApp> apps = storage.loadVirtualApps();
        for (VirtualApp app : apps) {
            virtualApps.put(app.packageName, app);
        }
        VirtualLog.i(TAG, "Loaded " + apps.size() + " virtual apps from storage");
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
        
        if (packageManager != null) {
            packageManager.registerVirtualApp(app);
        }
        
        if (fileSystem != null) {
            fileSystem.initializeVirtualEnvironment(app);
            fileSystem.copyOriginalDataToVirtual(packageName, app);
        }
        
        storage.saveVirtualApps(new ArrayList<>(virtualApps.values()));
        
        VirtualLog.i(TAG, "Created virtual app: " + appName + " (userId=" + userId + ")");
        return app;
    }

    public boolean removeVirtualApp(String packageName) {
        VirtualApp app = virtualApps.remove(packageName);
        if (app != null) {
            if (packageManager != null) {
                packageManager.unregisterVirtualApp(packageName);
            }
            
            if (fileSystem != null) {
                fileSystem.deleteVirtualEnvironment(app);
            }
            
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
        if (packageName == null) return false;
        if (virtualApps.containsKey(packageName)) return true;
        for (VirtualApp app : virtualApps.values()) {
            if (packageName.equals(app.fakePackageName)) return true;
        }
        return virtualPackages.containsKey(packageName);
    }

    public void addVirtualPackage(VirtualPackage vPkg) {
        virtualPackages.put(vPkg.packageName, vPkg);
    }

    public VirtualPackageManager getPackageManager() {
        return packageManager;
    }

    public VirtualActivityManager getActivityManager() {
        return activityManager;
    }

    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    public BinderHook getBinderHook() {
        return binderHook;
    }

    public void initBinderHook() {
        if (binderHook != null && !isBinderHookInitialized()) {
            try {
                binderHook.init();
                VirtualLog.d(TAG, "BinderHook initialized");
            } catch (Exception e) {
                VirtualLog.e(TAG, "Failed to init BinderHook", e);
            }
        }
    }

    private boolean isBinderHookInitialized() {
        return binderHook != null;
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
        return "35" + String.format("%013d", System.currentTimeMillis() % 100000000000L);
    }

    private String generateFakeAndroidId() {
        return String.format("%08x", (int) (System.currentTimeMillis() & 0xFFFFFFFF));
    }

    public void cleanup() {
        for (VirtualApp app : virtualApps.values()) {
            if (fileSystem != null) {
                fileSystem.deleteVirtualEnvironment(app);
            }
        }
        
        virtualApps.clear();
        virtualPackages.clear();
        isInitialized = false;
        VirtualLog.i(TAG, "VirtualCore cleaned up");
    }
}
