package com.virtual.core.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VirtualPackageManager {

    private static final String TAG = "VirtualPackageManager";

    private final VirtualCore core;
    private Object originalPm;
    private IBinder packageManagerBinder;

    public VirtualPackageManager(VirtualCore core) {
        this.core = core;
        initOriginalPackageManager();
    }

    private void initOriginalPackageManager() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);

            if (activityThread != null) {
                Field mPMField = activityThreadClass.getDeclaredField("mPM");
                mPMField.setAccessible(true);
                originalPm = mPMField.get(activityThread);
                Log.i(TAG, "Got original PackageManager");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get original PackageManager", e);
        }
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                PackageInfo sourceInfo = pm.getPackageInfo(vPkg.sourcePackage, flags);
                
                PackageInfo cloneInfo = new PackageInfo();
                cloneInfo.packageName = vPkg.packageName;
                cloneInfo.versionCode = vPkg.versionCode;
                cloneInfo.versionName = vPkg.versionName;
                cloneInfo.applicationInfo = sourceInfo.applicationInfo;
                cloneInfo.applicationInfo.packageName = vPkg.packageName;
                cloneInfo.firstInstallTime = vPkg.firstInstallTime;
                cloneInfo.lastUpdateTime = vPkg.lastUpdateTime;
                cloneInfo.signatures = vPkg.signatures;
                cloneInfo.sharedUserId = null;
                cloneInfo.installRequest = null;
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    cloneInfo.getSigningInfo();
                }
                
                return cloneInfo;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package not found: " + packageName);
            }
        }
        
        return null;
    }

    public PackageInfo getPackageInfo(String packageName, int flags) {
        return getPackageInfo(packageName, flags, 0);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                ApplicationInfo sourceInfo = pm.getApplicationInfo(vPkg.sourcePackage, flags);
                
                ApplicationInfo cloneInfo = new ApplicationInfo();
                cloneInfo.packageName = vPkg.packageName;
                cloneInfo.className = sourceInfo.className;
                cloneInfo.sourceDir = vPkg.apkPath;
                cloneInfo.dataDir = "/data/data/" + vPkg.packageName;
                cloneInfo.uid = vPkg.userId * 100000;
                cloneInfo.flags = sourceInfo.flags;
                cloneInfo.enabled = sourceInfo.enabled;
                cloneInfo.theme = sourceInfo.theme;
                cloneInfo.labelRes = sourceInfo.labelRes;
                cloneInfo.icon = sourceInfo.icon;
                cloneInfo.metaData = sourceInfo.metaData;
                
                return cloneInfo;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Application not found: " + packageName);
            }
        }
        
        return null;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags) {
        return getApplicationInfo(packageName, flags, 0);
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        List<PackageInfo> result = new ArrayList<>();
        
        for (VirtualPackage vPkg : core.getStorage().getAllPackages().values()) {
            PackageInfo info = getPackageInfo(vPkg.packageName, flags, userId);
            if (info != null) {
                result.add(info);
            }
        }
        
        return result;
    }

    public List<PackageInfo> getInstalledPackages(int flags) {
        return getInstalledPackages(flags, 0);
    }

    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        List<ApplicationInfo> result = new ArrayList<>();
        
        for (VirtualPackage vPkg : core.getStorage().getAllPackages().values()) {
            ApplicationInfo info = getApplicationInfo(vPkg.packageName, flags, userId);
            if (info != null) {
                result.add(info);
            }
        }
        
        return result;
    }

    public List<ApplicationInfo> getInstalledApplications(int flags) {
        return getInstalledApplications(flags, 0);
    }

    public boolean isPackageInstalled(String packageName) {
        return core.isVirtualPackage(packageName);
    }

    public int checkPermission(String permName, String pkgName, int userId) {
        return PackageManager.PERMISSION_GRANTED;
    }

    public String[] getPackagesForUid(int uid) {
        List<String> packages = new ArrayList<>();
        int baseUid = (uid / 100000) * 100000;
        
        for (VirtualPackage vPkg : core.getStorage().getAllPackages().values()) {
            if (vPkg.userId * 100000 == baseUid) {
                packages.add(vPkg.packageName);
            }
        }
        
        return packages.toArray(new String[0]);
    }

    public int getPackageUid(String packageName, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            return vPkg.userId * 100000;
        }
        return -1;
    }

    public int getPackageUid(String packageName) {
        return getPackageUid(packageName, 0);
    }

    public Intent getLaunchIntentForPackage(String packageName) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                PackageInfo pkgInfo = pm.getPackageArchiveInfo(vPkg.apkPath, 
                    PackageManager.GET_ACTIVITIES);
                
                if (pkgInfo != null && pkgInfo.activities != null && pkgInfo.activities.length > 0) {
                    for (ActivityInfo activity : pkgInfo.activities) {
                        if (activity.name.contains("MainActivity") || 
                            activity.name.contains("LauncherActivity") ||
                            (activity.exported && activity.enabled)) {
                            
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName(vPkg.packageName, activity.name));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            return intent;
                        }
                    }
                    
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName(vPkg.packageName, pkgInfo.activities[0].name));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return intent;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get launch intent for: " + packageName, e);
            }
        }
        
        return null;
    }
}
