package com.virtual.core.dispatch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.VirtualLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class VirtualDispatch {

    private static final String TAG = "VirtualDispatch";
    private static VirtualDispatch instance;

    private final VirtualCore core;
    private final Map<String, IntentFilter> intentFilters = new HashMap<>();

    public interface IntentFilter {
        boolean onTransact(int code, Intent intent);
    }

    private VirtualDispatch(VirtualCore core) {
        this.core = core;
    }

    public static VirtualDispatch getInstance(VirtualCore core) {
        if (instance == null) {
            synchronized (VirtualDispatch.class) {
                if (instance == null) {
                    instance = new VirtualDispatch(core);
                }
            }
        }
        return instance;
    }

    public void init() {
        hookPackageManager();
        VirtualLog.i(TAG, "VirtualDispatch initialized");
    }

    private void hookPackageManager() {
        try {
            Context context = core.getContext();
            VirtualLog.i(TAG, "PackageManager hook prepared");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook PackageManager", e);
        }
    }

    public boolean dispatchActivityStart(Intent intent, int userId) {
        if (intent == null) return false;

        String packageName = intent.getComponent() != null ?
            intent.getComponent().getPackageName() : intent.getPackage();

        if (packageName == null) return false;

        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            intent.setPackage(vPkg.packageName);
            return true;
        }

        return false;
    }

    public boolean dispatchPackageQuery(String packageName, int userId) {
        if (packageName == null) return true;

        if (core.isVirtualPackage(packageName)) {
            return true;
        }

        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null && vPkg.userId == userId) {
            return true;
        }

        return false;
    }

    public ComponentName getLaunchIntentForPackage(String packageName, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            Intent launchIntent = core.getContext().getPackageManager()
                .getLaunchIntentForPackage(vPkg.packageName);
            if (launchIntent != null) {
                launchIntent.setPackage(vPkg.packageName);
                return launchIntent.getComponent();
            }
        }
        return null;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(vPkg.sourcePackage, flags);
                return spoofApplicationInfo(appInfo, vPkg);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                PackageInfo pkgInfo = pm.getPackageInfo(vPkg.sourcePackage, flags);
                return spoofPackageInfo(pkgInfo, vPkg);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private ApplicationInfo spoofApplicationInfo(ApplicationInfo info, VirtualPackage vPkg) {
        if (info == null) return null;

        try {
            ApplicationInfo spoof = new ApplicationInfo(info);

            spoof.packageName = vPkg.packageName;
            spoof.uid = vPkg.userId * 100000;

            if (info.sourceDir != null) {
                spoof.sourceDir = info.sourceDir;
                spoof.publicSourceDir = info.publicSourceDir;
            }

            return spoof;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to spoof ApplicationInfo", e);
            return info;
        }
    }

    private PackageInfo spoofPackageInfo(PackageInfo pkgInfo, VirtualPackage vPkg) {
        if (pkgInfo == null) return null;

        try {
            PackageInfo spoof = new PackageInfo();

            spoof.packageName = vPkg.packageName;
            spoof.versionCode = vPkg.versionCode;
            spoof.versionName = vPkg.versionName;
            spoof.firstInstallTime = vPkg.firstInstallTime;
            spoof.lastUpdateTime = vPkg.lastUpdateTime;
            spoof.applicationInfo = spoofApplicationInfo(pkgInfo.applicationInfo, vPkg);
            spoof.signatures = pkgInfo.signatures;
            spoof.gids = pkgInfo.gids;

            return spoof;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to spoof PackageInfo", e);
            return pkgInfo;
        }
    }

    public void registerIntentFilter(String packageName, IntentFilter filter) {
        intentFilters.put(packageName, filter);
    }

    public void unregisterIntentFilter(String packageName) {
        intentFilters.remove(packageName);
    }

    public void cleanup() {
        intentFilters.clear();
        instance = null;
    }
}