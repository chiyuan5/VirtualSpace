package com.virtual.hook;

import android.os.Build;

import com.virtual.util.Log;

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
            Log.w(TAG, "Already hooked");
            return;
        }

        try {
            hookAMS();
            hookPMS();
            hookContentProvider();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hookUserManager();
            }
            
            isHooked = true;
            Log.i(TAG, "HookManager initialized for Android " + Build.VERSION.SDK_INT);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize HookManager", e);
        }
    }

    private void hookAMS() {
        try {
            HookInfo hook = new HookInfo();
            hook.targetClass = "android.app.ActivityManagerNative";
            hook.targetMethod = "getDefault";
            hook.isStatic = true;
            
            hooks.put("AMS", hook);
            Log.i(TAG, "AMS hook registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook AMS", e);
        }
    }

    private void hookPMS() {
        try {
            HookInfo hook = new HookInfo();
            hook.targetClass = "android.app.ActivityThread";
            hook.targetMethod = "getPackageManager";
            hook.isStatic = true;
            
            hooks.put("PMS", hook);
            Log.i(TAG, "PMS hook registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook PMS", e);
        }
    }

    private void hookContentProvider() {
        try {
            HookInfo hook = new HookInfo();
            hook.targetClass = "android.app.ActivityThread";
            hook.targetMethod = "installContentProviders";
            hook.isStatic = false;
            
            hooks.put("CP", hook);
            Log.i(TAG, "ContentProvider hook registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook ContentProvider", e);
        }
    }

    private void hookUserManager() {
        try {
            HookInfo hook = new HookInfo();
            hook.targetClass = "android.os.UserManager";
            hook.targetMethod = "getUserHandle";
            hook.isStatic = false;
            
            hooks.put("UM", hook);
            Log.i(TAG, "UserManager hook registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook UserManager", e);
        }
    }

    public void hookMethod(Class<?> clazz, String methodName, HookCallback callback) {
        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    Log.i(TAG, "Hooked method: " + clazz.getName() + "." + methodName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook method: " + methodName, e);
        }
    }

    public void unhookAll() {
        hooks.clear();
        isHooked = false;
        Log.i(TAG, "All hooks removed");
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
