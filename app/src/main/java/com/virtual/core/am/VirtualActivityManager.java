package com.virtual.core.am;

import com.virtual.util.VirtualLog;

public class VirtualActivityManager {

    private static final String TAG = "VirtualActivityManager";
    private static VirtualActivityManager instance;
    private final VirtualCore core;

    private VirtualActivityManager(VirtualCore core) {
        this.core = core;
    }

    public static VirtualActivityManager getInstance(VirtualCore core) {
        if (instance == null) {
            synchronized (VirtualActivityManager.class) {
                if (instance == null) {
                    instance = new VirtualActivityManager(core);
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

    private static class VirtualCore {
        public static VirtualCore get() {
            return com.virtual.core.VirtualCore.get();
        }
    }
}