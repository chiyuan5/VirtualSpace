package com.virtual.core.context;

import android.content.Context;
import android.content.res.Resources;

import com.virtual.util.VirtualLog;

public class VirtualContext {

    private static final String TAG = "VirtualContext";

    private final Context baseContext;
    private final String packageName;

    public VirtualContext(Context baseContext, String packageName) {
        this.baseContext = baseContext;
        this.packageName = packageName;
        VirtualLog.i(TAG, "Created virtual context for: " + packageName);
    }

    public Context getBaseContext() {
        return baseContext;
    }

    public String getPackageName() {
        return packageName;
    }

    public Context createPackageContext(String targetPackage, int flags) {
        try {
            return baseContext.createPackageContext(targetPackage, flags);
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create package context", e);
            return baseContext;
        }
    }

    public ClassLoader getClassLoader() {
        return baseContext.getClassLoader();
    }

    public Resources getResources() {
        return baseContext.getResources();
    }

    public Object getSystemService(String name) {
        return baseContext.getSystemService(name);
    }
}