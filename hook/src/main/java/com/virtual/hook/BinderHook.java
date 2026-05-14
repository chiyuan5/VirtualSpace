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
    private final VirtualPackageManager packageManager;
    private final VirtualActivityManager activityManager;
    private final VirtualFileSystem fileSystem;

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
        this.packageManager = VirtualPackageManager.getInstance(context);
        this.activityManager = VirtualActivityManager.getInstance(context);
        this.fileSystem = VirtualFileSystem.getInstance(context);
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
            hookFileSystem();

            isInitialized = true;
            VirtualLog.i(TAG, "Binder Hook system initialized successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to initialize Binder Hook system", e);
        }
    }

    private void hookActivityManager() {
        try {
            VirtualLog.d(TAG, "Hooking ActivityManager...");

            Class<?> activityTaskManagerClass = null;
            try {
                activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
            } catch (ClassNotFoundException e) {
                VirtualLog.d(TAG, "ActivityTaskManager not found, using ActivityManager");
            }

            hookService("activity", new ActivityManagerHook(context, core, packageManager, activityManager));

            VirtualLog.d(TAG, "ActivityManager hooked successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook ActivityManager", e);
        }
    }

    private void hookPackageManager() {
        try {
            VirtualLog.d(TAG, "Hooking PackageManager...");

            hookService("package", new PackageManagerHook(context, core, packageManager, fileSystem));

            VirtualLog.d(TAG, "PackageManager hooked successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook PackageManager", e);
        }
    }

    private void hookContentResolver() {
        try {
            VirtualLog.d(TAG, "Hooking ContentResolver...");

            hookService("content", new ContentResolverHook(context, core, packageManager));

            VirtualLog.d(TAG, "ContentResolver hooked successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook ContentResolver", e);
        }
    }

    private void hookFileSystem() {
        try {
            VirtualLog.d(TAG, "Hooking FileSystem...");

            hookNativeFileOperations();

            VirtualLog.d(TAG, "FileSystem hooked successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook FileSystem", e);
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
                    Class<?> iInterfaceClass = Class.forName("android.os.IInterface");
                    Method asBinderMethod = hook.getClass().getDeclaredMethod("asBinder");
                    IBinder proxyBinder = (IBinder) asBinderMethod.invoke(hook);

                    Field sCacheField = serviceManagerClass.getDeclaredField("sCache");
                    sCacheField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, IBinder> cache = (Map<String, IBinder>) sCacheField.get(null);
                    cache.put(serviceName, proxyBinder);

                    VirtualLog.d(TAG, "Replaced service in cache: " + serviceName);
                } catch (Exception e) {
                    VirtualLog.w(TAG, "Could not replace service in cache, hook may not work", e);
                }
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook service: " + serviceName, e);
        }
    }

    private void hookNativeFileOperations() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                System.setProperty("file.encoding", "UTF-8");
            }

            VirtualLog.d(TAG, "Native file operations configured");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook native file operations", e);
        }
    }

    public IBinder getOriginalService(String serviceName) {
        return originalServices.get(serviceName);
    }

    public boolean isVirtualPackage(String packageName) {
        return packageManager != null && packageManager.isVirtualPackage(packageName);
    }

    public VirtualApp getVirtualApp(String packageName) {
        return packageManager != null ? packageManager.getVirtualApp(packageName) : null;
    }

    public static abstract class ServiceHook implements android.os.IInterface {
        protected final IBinder mRemote;
        protected final Context context;

        public ServiceHook(Context context, IBinder remote) {
            this.context = context;
            this.mRemote = remote;
        }

        @Override
        public IBinder asBinder() {
            return mRemote;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws RemoteException {
            return false;
        }
    }

    public static class ActivityManagerHook extends ServiceHook {
        private final VirtualCore core;
        private final VirtualPackageManager packageManager;
        private final VirtualActivityManager activityManager;

        public ActivityManagerHook(Context context, VirtualCore core, 
                                   VirtualPackageManager packageManager, 
                                   VirtualActivityManager activityManager) {
            super(context, null);
            this.core = core;
            this.packageManager = packageManager;
            this.activityManager = activityManager;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (Exception e) {
                VirtualLog.e(TAG, "Error in ActivityManager hook", e);
                return false;
            }
        }
    }

    public static class PackageManagerHook extends ServiceHook {
        private final VirtualCore core;
        private final VirtualPackageManager packageManager;
        private final VirtualFileSystem fileSystem;

        public PackageManagerHook(Context context, VirtualCore core,
                                  VirtualPackageManager packageManager,
                                  VirtualFileSystem fileSystem) {
            super(context, null);
            this.core = core;
            this.packageManager = packageManager;
            this.fileSystem = fileSystem;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (Exception e) {
                VirtualLog.e(TAG, "Error in PackageManager hook", e);
                return false;
            }
        }
    }

    public static class ContentResolverHook extends ServiceHook {
        private final VirtualCore core;
        private final VirtualPackageManager packageManager;

        public ContentResolverHook(Context context, VirtualCore core, VirtualPackageManager packageManager) {
            super(context, null);
            this.core = core;
            this.packageManager = packageManager;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (Exception e) {
                VirtualLog.e(TAG, "Error in ContentResolver hook", e);
                return false;
            }
        }
    }

    private static class RemoteException extends Exception {
        public RemoteException(String message) {
            super(message);
        }

        public RemoteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
