package com.virtual.core.am;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualActivityManager {

    private static final String TAG = "VirtualActivityManager";

    private final VirtualCore core;
    private IActivityManager originalAm;
    
    private final Map<String, ActivityRecord> activityStack = new HashMap<>();
    private int activityIdCounter = 0;

    public VirtualActivityManager(VirtualCore core) {
        this.core = core;
        initOriginalActivityManager();
    }

    private void initOriginalActivityManager() {
        try {
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);
            Object gDefault = gDefaultField.get(null);
            
            if (gDefault != null) {
                Field mInstanceField = gDefault.getClass().getDeclaredField("mInstance");
                mInstanceField.setAccessible(true);
                originalAm = (IActivityManager) mInstanceField.get(gDefault);
                Log.i(TAG, "Got original ActivityManager");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get original ActivityManager", e);
        }
    }

    public ComponentName startActivity(Intent intent, int userId) {
        String packageName = intent.getComponent() != null ? 
            intent.getComponent().getPackageName() : intent.getPackage();
        
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                PackageInfo pkgInfo = pm.getPackageArchiveInfo(vPkg.apkPath, 
                    PackageManager.GET_ACTIVITIES);
                
                if (pkgInfo != null && pkgInfo.activities != null && pkgInfo.activities.length > 0) {
                    ActivityInfo targetActivity = null;
                    
                    for (ActivityInfo activity : pkgInfo.activities) {
                        if (activity.exported && activity.enabled) {
                            targetActivity = activity;
                            break;
                        }
                    }
                    
                    if (targetActivity == null) {
                        targetActivity = pkgInfo.activities[0];
                    }
                    
                    ComponentName component = new ComponentName(vPkg.packageName, targetActivity.name);
                    intent.setComponent(component);
                    
                    Log.i(TAG, "Starting virtual activity: " + component.flattenToShortString());
                    
                    return startVirtualActivity(intent, component, userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start virtual activity", e);
            }
        }
        
        return null;
    }

    private ComponentName startVirtualActivity(Intent intent, ComponentName component, int userId) {
        String activityKey = component.flattenToShortString() + "_" + (activityIdCounter++);
        
        ActivityRecord record = new ActivityRecord();
        record.intent = intent;
        record.component = component;
        record.userId = userId;
        record.startTime = System.currentTimeMillis();
        
        activityStack.put(activityKey, record);
        
        return component;
    }

    public boolean finishActivity(ComponentName component) {
        String keyToRemove = null;
        for (Map.Entry<String, ActivityRecord> entry : activityStack.entrySet()) {
            if (entry.getValue().component.equals(component)) {
                keyToRemove = entry.getKey();
                break;
            }
        }
        
        if (keyToRemove != null) {
            activityStack.remove(keyToRemove);
            Log.i(TAG, "Finished virtual activity: " + component.flattenToShortString());
            return true;
        }
        
        return false;
    }

    public List<ActivityManager.RunningTaskInfo> getRunningTasks(int maxNum) {
        List<ActivityManager.RunningTaskInfo> tasks = new ArrayList<>();
        
        ActivityManager am = (ActivityManager) core.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            try {
                List<ActivityManager.RunningTaskInfo> originalTasks = am.getRunningTasks(maxNum);
                if (originalTasks != null) {
                    tasks.addAll(originalTasks);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get running tasks", e);
            }
        }
        
        return tasks;
    }

    public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() {
        List<ActivityManager.RunningAppProcessInfo> processes = new ArrayList<>();
        
        ActivityManager am = (ActivityManager) core.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            try {
                List<ActivityManager.RunningAppProcessInfo> originalProcesses = am.getRunningAppProcesses();
                if (originalProcesses != null) {
                    processes.addAll(originalProcesses);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get running app processes", e);
            }
        }
        
        return processes;
    }

    public int getCurrentUserId() {
        try {
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Method getCurrentUserMethod = activityManagerNativeClass.getDeclaredMethod("getCurrentUser");
            Object userInfo = getCurrentUserMethod.invoke(null);
            
            if (userInfo != null) {
                Field idField = userInfo.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                return idField.getInt(userInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current user id", e);
        }
        
        return 0;
    }

    public void broadcastIntent(Intent intent, int userId) {
        Log.i(TAG, "Broadcasting virtual intent: " + intent.getAction());
    }

    private static class ActivityRecord {
        Intent intent;
        ComponentName component;
        int userId;
        long startTime;
    }
}
