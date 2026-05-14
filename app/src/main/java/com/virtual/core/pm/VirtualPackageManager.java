package com.virtual.core.pm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.VirtualLog;

import java.util.ArrayList;
import java.util.List;

public class VirtualPackageManager {

    private static final String TAG = "VirtualPackageManager";
    private final VirtualCore core;
    private final PackageManager originalPm;

    public VirtualPackageManager(VirtualCore core) {
        this.core = core;
        this.originalPm = core.getContext().getPackageManager();
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        try {
            VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
            if (vPkg != null) {
                PackageInfo realPkg = originalPm.getPackageInfo(vPkg.sourcePackage, flags);
                return spoofPackageInfo(realPkg, vPkg);
            }
            return originalPm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            VirtualLog.e(TAG, "Package not found: " + packageName, e);
            return null;
        }
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        try {
            VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
            if (vPkg != null) {
                ApplicationInfo realInfo = originalPm.getApplicationInfo(vPkg.sourcePackage, flags);
                return spoofApplicationInfo(realInfo, vPkg);
            }
            return originalPm.getApplicationInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            VirtualLog.e(TAG, "Application not found: " + packageName, e);
            return null;
        }
    }

    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        List<ApplicationInfo> result = new ArrayList<>();
        try {
            result.addAll(originalPm.getInstalledApplications(flags));
            return result;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to get installed applications", e);
            return result;
        }
    }

    public List<ApplicationInfo> getInstalledApplications(int flags) {
        return getInstalledApplications(flags, 0);
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
            return spoof;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to spoof package info", e);
            return pkgInfo;
        }
    }

    private ApplicationInfo spoofApplicationInfo(ApplicationInfo info, VirtualPackage vPkg) {
        if (info == null) return null;

        try {
            ApplicationInfo spoof = new ApplicationInfo(info);
            spoof.packageName = vPkg.packageName;
            spoof.uid = vPkg.userId * 100000;
            return spoof;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to spoof application info", e);
            return info;
        }
    }
}