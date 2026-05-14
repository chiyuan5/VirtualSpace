package com.virtual.core.system;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.util.VirtualLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualActivityManager {

    private static final String TAG = "VirtualActivityManager";
    private static VirtualActivityManager instance;

    private final Context context;
    private final VirtualCore core;
    private final VirtualPackageManager packageManager;
    private final Map<String, Integer> processUsers = new HashMap<>();

    public static VirtualActivityManager getInstance(Context context) {
        if (instance == null) {
            synchronized (VirtualActivityManager.class) {
                if (instance == null) {
                    instance = new VirtualActivityManager(context);
                }
            }
        }
        return instance;
    }

    private VirtualActivityManager(Context context) {
        this.context = context.getApplicationContext();
        this.core = VirtualCore.get();
        this.packageManager = VirtualPackageManager.getInstance(context);
        VirtualLog.d(TAG, "VirtualActivityManager initialized");
    }

    public int getUserIdForProcess(String processName) {
        if (processUsers.containsKey(processName)) {
            return processUsers.get(processName);
        }
        return 0;
    }

    public void registerProcess(String processName, int userId) {
        processUsers.put(processName, userId);
        VirtualLog.d(TAG, "Registered process: " + processName + " -> user " + userId);
    }

    public boolean startActivity(Intent intent, int userId) {
        try {
            String packageName = intent.getComponent() != null ? 
                intent.getComponent().getPackageName() : intent.getPackage();

            if (packageName == null) return false;

            VirtualApp virtualApp = packageManager.getVirtualApp(packageName);
            if (virtualApp == null) {
                VirtualLog.w(TAG, "Not a virtual package: " + packageName);
                return false;
            }

            Intent virtualIntent = new Intent(intent);
            virtualIntent.setPackage(virtualApp.fakePackageName);
            virtualIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            virtualIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            context.startActivity(virtualIntent);
            VirtualLog.d(TAG, "Started virtual activity: " + packageName + " -> " + virtualApp.fakePackageName);
            return true;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error starting activity", e);
            return false;
        }
    }

    public boolean startService(Intent intent, int userId) {
        try {
            String packageName = intent.getComponent() != null ?
                intent.getComponent().getPackageName() : intent.getPackage();

            if (packageName == null) return false;

            VirtualApp virtualApp = packageManager.getVirtualApp(packageName);
            if (virtualApp == null) {
                VirtualLog.w(TAG, "Not a virtual package: " + packageName);
                return false;
            }

            Intent virtualIntent = new Intent(intent);
            virtualIntent.setPackage(virtualApp.fakePackageName);

            context.startService(virtualIntent);
            VirtualLog.d(TAG, "Started virtual service: " + packageName + " -> " + virtualApp.fakePackageName);
            return true;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error starting service", e);
            return false;
        }
    }

    public boolean stopService(Intent intent, int userId) {
        try {
            String packageName = intent.getComponent() != null ?
                intent.getComponent().getPackageName() : intent.getPackage();

            if (packageName == null) return false;

            VirtualApp virtualApp = packageManager.getVirtualApp(packageName);
            if (virtualApp == null) {
                VirtualLog.w(TAG, "Not a virtual package: " + packageName);
                return false;
            }

            Intent virtualIntent = new Intent(intent);
            virtualIntent.setPackage(virtualApp.fakePackageName);

            return context.stopService(virtualIntent);
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error stopping service", e);
            return false;
        }
    }

    public int getPidForProcess(String processName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return -1;

        try {
            List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                for (RunningAppProcessInfo info : processes) {
                    if (info.processName.equals(processName)) {
                        return info.pid;
                    }
                }
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting pid for process: " + processName, e);
        }
        return -1;
    }

    public List<RunningAppProcessInfo> getRunningVirtualProcesses(int userId) {
        List<RunningAppProcessInfo> result = new ArrayList<>();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return result;

        try {
            List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                for (RunningAppProcessInfo info : processes) {
                    for (VirtualApp app : core.getAllVirtualApps()) {
                        if (info.processName.contains(app.packageName) || 
                            info.processName.contains(app.fakePackageName)) {
                            RunningAppProcessInfo vInfo = new RunningAppProcessInfo();
                            vInfo.processName = info.processName.replace(app.packageName, app.fakePackageName);
                            vInfo.pid = info.pid;
                            vInfo.uid = info.uid;
                            vInfo.importance = info.importance;
                            vInfo.importanceReasonCode = info.importanceReasonCode;
                            vInfo.importanceReasonPid = info.importanceReasonPid;
                            vInfo.importanceReasonComponent = info.importanceReasonComponent;
                            vInfo.lastTrimLevel = info.lastTrimLevel;
                            vInfo.lru = info.lru;
                            vInfo.adjType = info.adjType;
                            vInfo.adjTypeCode = info.adjTypeCode;
                            vInfo.adjSource = info.adjSource;
                            vInfo.adjTarget = info.adjTarget;
                            vInfo.setAdjType(info.adjType);
                            result.add(vInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting running virtual processes", e);
        }
        return result;
    }

    public List<RunningServiceInfo> getRunningVirtualServices(int userId) {
        List<RunningServiceInfo> result = new ArrayList<>();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return result;

        try {
            List<RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
            if (services != null) {
                for (RunningServiceInfo info : services) {
                    for (VirtualApp app : core.getAllVirtualApps()) {
                        if (info.process.contains(app.packageName) ||
                            info.process.contains(app.fakePackageName)) {
                            RunningServiceInfo vInfo = new RunningServiceInfo();
                            vInfo.service = info.service;
                            vInfo.process = info.process.replace(app.packageName, app.fakePackageName);
                            vInfo.pid = info.pid;
                            vInfo.foreground = info.foreground;
                            vInfo.activeSince = info.activeSince;
                            vInfo.clientCount = info.clientCount;
                            vInfoCrashCount = info.crashCount;
                            vInfo.app = info.app;
                            result.add(vInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting running virtual services", e);
        }
        return result;
    }

    public boolean killProcess(String processName, int userId) {
        try {
            int pid = getPidForProcess(processName);
            if (pid > 0) {
                android.os.Process.killProcess(pid);
                VirtualLog.d(TAG, "Killed process: " + processName);
                return true;
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error killing process: " + processName, e);
        }
        return false;
    }

    public void clearAppData(String packageName, int userId) {
        try {
            VirtualApp app = packageManager.getVirtualApp(packageName);
            if (app == null) return;

            String dataDir = context.getFilesDir().getAbsolutePath() + "/virtual/" + app.userId + "/" + app.packageName;
            java.io.File dataFile = new java.io.File(dataDir);
            if (dataFile.exists()) {
                deleteDir(dataFile);
                VirtualLog.d(TAG, "Cleared app data for: " + packageName);
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error clearing app data for: " + packageName, e);
        }
    }

    private void deleteDir(java.io.File dir) {
        if (dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteDir(file);
                }
            }
        }
        dir.delete();
    }

    public boolean isProcessRunning(String processName) {
        return getPidForProcess(processName) > 0;
    }
}
