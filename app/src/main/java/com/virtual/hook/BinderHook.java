package com.virtual.hook;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.core.fs.VirtualFileSystem;
import com.virtual.core.system.VirtualActivityManager;
import com.virtual.core.system.VirtualPackageManager;
import com.virtual.util.VirtualLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BinderHook {

    private static final String TAG = "BinderHook";
    private static BinderHook instance;

    private final Context context;
    private final VirtualCore core;
    private final Map<String, IBinder> originalServices = new HashMap<>();
    private boolean isInitialized = false;

    public static BinderHook getInstance(Context context) {
        if (instance == null) {
            synchronized (BinderHook.class) {
                if (instance == null) {
                    instance = new BinderHook(context);
                }
            }
        }
        return instance;
    }

    private BinderHook(Context context) {
        this.context = context.getApplicationContext();
        this.core = VirtualCore.get();
        VirtualLog.d(TAG, "BinderHook instance created");
    }

    public void init() {
        if (isInitialized) {
            VirtualLog.w(TAG, "Already initialized");
            return;
        }

        try {
            VirtualLog.d(TAG, "Initializing Binder Hook system...");

            hookActivityManager();
            hookPackageManager();
            hookContentResolver();

            isInitialized = true;
            VirtualLog.i(TAG, "Binder Hook system initialized successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to initialize Binder Hook system", e);
        }
    }

    private void hookActivityManager() {
        try {
            VirtualLog.d(TAG, "Hooking ActivityManager...");

            hookService("activity", new ActivityManagerHook());
            hookService("activity_task", new ActivityManagerHook());

            VirtualLog.d(TAG, "ActivityManager hooked successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook ActivityManager", e);
        }
    }

    private void hookPackageManager() {
        try {
            VirtualLog.d(TAG, "Hooking PackageManager...");

            hookService("package", new PackageManagerHook());

            VirtualLog.d(TAG, "PackageManager hooked successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook PackageManager", e);
        }
    }

    private void hookContentResolver() {
        try {
            VirtualLog.d(TAG, "Hooking ContentResolver...");

            hookService("content", new ContentResolverHook());

            VirtualLog.d(TAG, "ContentResolver hooked successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook ContentResolver", e);
        }
    }

    private void hookService(String serviceName, ServiceHook hook) {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder originalBinder = (IBinder) getServiceMethod.invoke(null, serviceName);
            if (originalBinder != null) {
                originalServices.put(serviceName, originalBinder);
                VirtualLog.d(TAG, "Got original service: " + serviceName);

                try {
                    Field sCacheField = serviceManagerClass.getDeclaredField("sCache");
                    sCacheField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, IBinder> cache = (Map<String, IBinder>) sCacheField.get(null);
                    IBinder proxyBinder = hook.asBinder();
                    if (proxyBinder != null) {
                        cache.put(serviceName, proxyBinder);
                        VirtualLog.d(TAG, "Replaced service in cache: " + serviceName);
                    }
                } catch (Exception e) {
                    VirtualLog.w(TAG, "Could not replace service in cache, hook may not work", e);
                }
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook service: " + serviceName, e);
        }
    }

    public IBinder getOriginalService(String serviceName) {
        return originalServices.get(serviceName);
    }

    public boolean isVirtualPackage(String packageName) {
        if (core == null) return false;
        return core.isVirtualPackage(packageName);
    }

    public VirtualApp getVirtualApp(String packageName) {
        if (core == null) return null;
        for (VirtualApp app : core.getAllVirtualApps()) {
            if (app.packageName.equals(packageName) || app.fakePackageName.equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    public abstract static class ServiceHook implements android.os.IInterface {
        protected final IBinder mRemote;

        public ServiceHook(IBinder remote) {
            this.mRemote = remote;
        }

        @Override
        public IBinder asBinder() {
            return mRemote;
        }
    }

    public class ActivityManagerHook extends ServiceHook {
        public ActivityManagerHook() {
            super(null);
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) {
            try {
                VirtualLog.d(TAG, "ActivityManager onTransact: " + code);
                return super.onTransact(code, data, reply, flags);
            } catch (Exception e) {
                VirtualLog.e(TAG, "Error in ActivityManager hook", e);
                return false;
            }
        }
    }

    public class PackageManagerHook extends ServiceHook {
        public PackageManagerHook() {
            super(null);
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) {
            try {
                VirtualLog.d(TAG, "PackageManager onTransact: " + code);
                return super.onTransact(code, data, reply, flags);
            } catch (Exception e) {
                VirtualLog.e(TAG, "Error in PackageManager hook", e);
                return false;
            }
        }
    }

    public class ContentResolverHook extends ServiceHook {
        public ContentResolverHook() {
            super(null);
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) {
            try {
                VirtualLog.d(TAG, "ContentResolver onTransact: " + code);
                return super.onTransact(code, data, reply, flags);
            } catch (Exception e) {
                VirtualLog.e(TAG, "Error in ContentResolver hook", e);
                return false;
            }
        }
    }
}
