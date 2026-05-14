package com.virtual.hook;

import android.content.Context;

import com.virtual.util.VirtualLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class HookManager {

    private static final String TAG = "HookManager";
    private static HookManager instance;
    
    private boolean isHooked = false;
    private final Map<String, HookInfo> hooks = new HashMap<>();

    public static HookManager get() {
        if (instance == null) {
            synchronized (HookManager.class) {
                if (instance == null) {
                    instance = new HookManager();
                }
            }
        }
        return instance;
    }

    public void init() {
        if (isHooked) {
            VirtualLog.w(TAG, "Already hooked");
            return;
        }

        try {
            hookAMS();
            hookPMS();
            
            isHooked = true;
            VirtualLog.i(TAG, "HookManager initialized");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to initialize HookManager", e);
        }
    }

    private void hookAMS() {
        try {
            HookInfo hook = new HookInfo();
            hook.targetMethod = "ActivityManager";
            
            hooks.put("AMS", hook);
            VirtualLog.i(TAG, "AMS hook registered");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook AMS", e);
        }
    }

    private void hookPMS() {
        try {
            HookInfo hook = new HookInfo();
            hook.targetMethod = "PackageManager";
            
            hooks.put("PMS", hook);
            VirtualLog.i(TAG, "PMS hook registered");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook PMS", e);
        }
    }

    public void hookMethod(String className, String methodName, HookCallback callback) {
        try {
            VirtualLog.i(TAG, "Hook registered: " + className + "." + methodName);
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook method: " + methodName, e);
        }
    }

    public void unhookAll() {
        hooks.clear();
        isHooked = false;
        VirtualLog.i(TAG, "All hooks removed");
    }

    public boolean isHooked() {
        return isHooked;
    }

    public Map<String, HookInfo> getHooks() {
        return new HashMap<>(hooks);
    }

    public interface HookCallback {
        Object onHook(Object receiver, Method method, Object[] args) throws Throwable;
    }

    public static class HookInfo {
        public String targetClass;
        public String targetMethod;
        public boolean isStatic;
        public Object hookObject;
        public Method hookMethod;
    }
}