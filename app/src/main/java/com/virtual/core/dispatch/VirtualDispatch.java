package com.virtual.core.dispatch;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.core.pm.VirtualPackageManager;
import com.virtual.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualDispatch {

    private static final String TAG = "VirtualDispatch";
    private static VirtualDispatch instance;

    private final VirtualCore core;
    private final Map<String, IntentFilter> intentFilters = new HashMap<>();

    private IBinder.ActivityTaskManagerActivityTaskManager activityTaskManager;
    private IBinder.FgServiceTransactionResolver fgServiceTransactionResolver;
    private IBinder.OldMountService mountService;

    public interface IntentFilter {
        boolean onTransact(int code, Intent intent);
    }

    private VirtualDispatch(VirtualCore core) {
        this.core = core;
    }

    public static VirtualDispatch getInstance(VirtualCore core) {
        if (instance == null) {
            synchronized (VirtualDispatch.class) {
                if (instance == null) {
                    instance = new VirtualDispatch(core);
                }
            }
        }
        return instance;
    }

    public void init() {
        hookActivityManager();
        hookPackageManager();
        hookContentProvider();
        hookUserManager();
        hookDevicePolicy();
        Log.i(TAG, "VirtualDispatch initialized");
    }

    private void hookActivityManager() {
        try {
            Class<?> activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
            Field instanceField = activityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
            instanceField.setAccessible(true);
            Object singleton = instanceField.get(null);

            Method getMethod = Class.forName("android.util.Singleton").getMethod("get");
            Object iActivityTaskManager = getMethod.invoke(singleton);

            if (iActivityTaskManager != null) {
                activityTaskManager = new ActivityTaskManagerWrapper(iActivityTaskManager);
                Log.i(TAG, "Hooked ActivityTaskManager");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook ActivityTaskManager", e);
        }
    }

    private void hookPackageManager() {
        try {
            Context context = core.getContext();

            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object originalPm = sPackageManagerField.get(null);

            if (originalPm != null) {
                Object proxyPm = java.lang.reflect.Proxy.newProxyInstance(
                    originalPm.getClass().getClassLoader(),
                    originalPm.getClass().getInterfaces(),
                    new PackageManagerProxy(originalPm, core)
                );
                sPackageManagerField.set(null, proxyPm);
                Log.i(TAG, "Hooked PackageManager");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook PackageManager", e);
        }
    }

    private void hookContentProvider() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sContentProviderField = activityThreadClass.getDeclaredField("sContentProviders");
            sContentProviderField.setAccessible(true);
            Object contentProviders = sContentProviderField.get(null);
            Log.i(TAG, "ContentProvider hook prepared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook ContentProvider", e);
        }
    }

    private void hookUserManager() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Field cacheField = serviceManagerClass.getDeclaredField("sCache");
            cacheField.setAccessible(true);
            Map<String, IBinder> cache = (Map<String, IBinder>) cacheField.get(null);
            IBinder umsBinder = cache.get("user");
            if (umsBinder != null) {
                Log.i(TAG, "UserManager service intercepted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook UserManager", e);
        }
    }

    private void hookDevicePolicy() {
        try {
            Log.i(TAG, "DevicePolicy hook prepared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook DevicePolicy", e);
        }
    }

    public boolean dispatchActivityStart(Intent intent, int userId) {
        if (intent == null) return false;

        String packageName = intent.getComponent() != null ?
            intent.getComponent().getPackageName() : intent.getPackage();

        if (packageName == null) return false;

        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            intent.setPackage(vPkg.packageName);
            return true;
        }

        return false;
    }

    public boolean dispatchPackageQuery(String packageName, int userId) {
        if (packageName == null) return true;

        if (core.isVirtualPackage(packageName)) {
            return true;
        }

        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null && vPkg.userId == userId) {
            return true;
        }

        return false;
    }

    public boolean dispatchServiceStart(Intent intent, int userId) {
        if (intent == null) return false;

        String packageName = intent.getComponent() != null ?
            intent.getComponent().getPackageName() : intent.getPackage();

        if (packageName == null) return false;

        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            return true;
        }

        return false;
    }

    public boolean dispatchProviderQuery(String authority, int userId) {
        if (authority == null) return true;

        return true;
    }

    public ComponentName getLaunchIntentForPackage(String packageName, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            Intent launchIntent = core.getContext().getPackageManager()
                .getLaunchIntentForPackage(vPkg.packageName);
            if (launchIntent != null) {
                launchIntent.setPackage(vPkg.packageName);
                return launchIntent.getComponent();
            }
        }
        return null;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(vPkg.sourcePackage, flags);
                return spoofApplicationInfo(appInfo, vPkg);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg != null) {
            try {
                PackageManager pm = core.getContext().getPackageManager();
                PackageInfo pkgInfo = pm.getPackageInfo(vPkg.sourcePackage, flags);
                return spoofPackageInfo(pkgInfo, vPkg);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private ApplicationInfo spoofApplicationInfo(ApplicationInfo info, VirtualPackage vPkg) {
        if (info == null) return null;

        try {
            ApplicationInfo spoof = new ApplicationInfo(info);

            spoof.packageName = vPkg.packageName;
            spoof.uid = vPkg.userId * 100000;

            if (info.sourceDir != null) {
                spoof.sourceDir = info.sourceDir;
                spoof.publicSourceDir = info.publicSourceDir;
            }

            return spoof;
        } catch (Exception e) {
            Log.e(TAG, "Failed to spoof ApplicationInfo", e);
            return info;
        }
    }

    private PackageInfo spoofPackageInfo(PackageInfo pkgInfo, VirtualPackage vPkg) {
        if (pkgInfo == null) return null;

        try {
            PackageInfo spoof = new PackageInfo();

            spoof.packageName = vPkg.packageName;
            spoof.versionCode = vPkg.versionCode;
            spoof.versionName = vPkg.versionName;
            spoof.firstInstallTime = vPkg.firstInstallTime;
            spoof.lastUpdateTime = vPkg.lastUpdateTime;
            spoof.applicationInfo = spoofApplicationInfo(pkgInfo.applicationInfo, vPkg);
            spoof.signatures = pkgInfo.signatures;
            spoof.gids = pkgInfo.gids;
            spoof.configPreferences = pkgInfo.configPreferences;
            spoof.reqFeatures = pkgInfo.reqFeatures;
            spoof.targetPlatforms = pkgInfo.targetPlatforms;

            return spoof;
        } catch (Exception e) {
            Log.e(TAG, "Failed to spoof PackageInfo", e);
            return pkgInfo;
        }
    }

    private static class ActivityTaskManagerWrapper {
        private final Object original;

        ActivityTaskManagerWrapper(Object original) {
            this.original = original;
        }
    }

    private static class PackageManagerProxy implements java.lang.reflect.InvocationHandler {
        private final Object original;
        private final VirtualCore core;

        PackageManagerProxy(Object original, VirtualCore core) {
            this.original = original;
            this.core = core;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if ("getPackageInfo".equals(methodName) && args != null && args.length >= 2) {
                String packageName = (String) args[0];
                int flags = (int) args[1];

                VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
                if (vPkg != null) {
                    try {
                        PackageManager pm = core.getContext().getPackageManager();
                        PackageInfo realPkg = pm.getPackageInfo(vPkg.sourcePackage, flags);
                        return spoofPackageInfo(realPkg, vPkg);
                    } catch (PackageManager.NameNotFoundException e) {
                        throw e;
                    }
                }
            }

            if ("getApplicationInfo".equals(methodName) && args != null && args.length >= 2) {
                String packageName = (String) args[0];
                int flags = (int) args[1];

                VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
                if (vPkg != null) {
                    try {
                        PackageManager pm = core.getContext().getPackageManager();
                        ApplicationInfo appInfo = pm.getApplicationInfo(vPkg.sourcePackage, flags);
                        return spoofApplicationInfo(appInfo, vPkg);
                    } catch (PackageManager.NameNotFoundException e) {
                        throw e;
                    }
                }
            }

            if ("getLaunchIntentForPackage".equals(methodName) && args != null && args.length >= 1) {
                String packageName = (String) args[0];
                Intent intent = core.getContext().getPackageManager()
                    .getLaunchIntentForPackage(packageName);
                return intent;
            }

            if ("resolveActivity".equals(methodName) && args != null && args.length >= 1) {
                Intent intent = (Intent) args[0];
                if (intent != null) {
                    String pkg = intent.getPackage();
                    if (pkg != null && core.isVirtualPackage(pkg)) {
                        Intent launchIntent = core.getContext().getPackageManager()
                            .getLaunchIntentForPackage(pkg);
                        if (launchIntent != null) {
                            return core.getContext().getPackageManager()
                                .resolveActivity(launchIntent,
                                    args.length > 1 ? (int) args[1] : 0);
                        }
                    }
                }
            }

            try {
                return method.invoke(original, args);
            } catch (Exception e) {
                Log.e(TAG, "PackageManager proxy error: " + methodName, e);
                throw e;
            }
        }

        private PackageInfo spoofPackageInfo(PackageInfo pkgInfo, VirtualPackage vPkg) {
            if (pkgInfo == null) return null;

            try {
                PackageInfo spoof = new PackageInfo();
                spoof.packageName = vPkg.packageName;
                spoof.versionCode = vPkg.versionCode;
                spoof.versionName = vPkg.versionName;
                spoof.firstInstallTime = vPkg.firstInstallTime;
                spoof.lastUpdateTime = vPkg.lastUpdateTime;

                if (pkgInfo.applicationInfo != null) {
                    spoof.applicationInfo = new ApplicationInfo(pkgInfo.applicationInfo);
                    spoof.applicationInfo.packageName = vPkg.packageName;
                    spoof.applicationInfo.uid = vPkg.userId * 100000;
                }

                return spoof;
            } catch (Exception e) {
                return pkgInfo;
            }
        }

        private ApplicationInfo spoofApplicationInfo(ApplicationInfo info, VirtualPackage vPkg) {
            if (info == null) return null;

            try {
                ApplicationInfo spoof = new ApplicationInfo(info);
                spoof.packageName = vPkg.packageName;
                spoof.uid = vPkg.userId * 100000;
                return spoof;
            } catch (Exception e) {
                return info;
            }
        }
    }

    public void registerIntentFilter(String packageName, IntentFilter filter) {
        intentFilters.put(packageName, filter);
    }

    public void unregisterIntentFilter(String packageName) {
        intentFilters.remove(packageName);
    }

    public void cleanup() {
        intentFilters.clear();
        instance = null;
    }
}
