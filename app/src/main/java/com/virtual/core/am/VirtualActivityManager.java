package com.virtual.core.am;

import com.virtual.util.VirtualLog;

public class VirtualActivityManager {

    private static final String TAG = "VirtualActivityManager";
    private static VirtualActivityManager instance;

    private VirtualActivityManager() {
    }

    public static VirtualActivityManager getInstance() {
        if (instance == null) {
            synchronized (VirtualActivityManager.class) {
                if (instance == null) {
                    instance = new VirtualActivityManager();
                }
            }
        }
        return instance;
    }

    public void init() {
        VirtualLog.i(TAG, "VirtualActivityManager initialized");
    }

    public boolean startActivity(String packageName, int userId) {
        VirtualLog.i(TAG, "Starting activity: " + packageName);
        return true;
    }

    public boolean finishActivity(String packageName, int userId) {
        VirtualLog.i(TAG, "Finishing activity: " + packageName);
        return true;
    }

    public int getUidForPackage(String packageName, int userId) {
        return userId * 100000;
    }

    public void cleanup() {
        instance = null;
    }
}