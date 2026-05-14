package com.virtual.hook;

import android.content.Context;

import com.virtual.core.VirtualCore;
import com.virtual.util.VirtualLog;

public class MethodProxy {

    private static final String TAG = "MethodProxy";

    public static Context getSystemContext() {
        try {
            VirtualLog.i(TAG, "Getting system context");
            return null;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to get system context", e);
            return null;
        }
    }

    public static int getUserId() {
        return 0;
    }

    public static boolean isVirtualApp(String packageName) {
        if (packageName == null) return false;
        
        VirtualCore core = VirtualCore.get();
        return core != null && core.isVirtualPackage(packageName);
    }

    public static void onActivityCreated(String packageName) {
        VirtualLog.i(TAG, "Activity created: " + packageName);
    }

    public static void onActivityDestroyed(String packageName) {
        VirtualLog.i(TAG, "Activity destroyed: " + packageName);
    }

    public static void onServiceCreated(String packageName) {
        VirtualLog.i(TAG, "Service created: " + packageName);
    }

    public static void onProviderCreated(String packageName) {
        VirtualLog.i(TAG, "Provider created: " + packageName);
    }
}