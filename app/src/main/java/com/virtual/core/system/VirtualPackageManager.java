package com.virtual.core.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.util.VirtualLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualPackageManager {

    private static final String TAG = "VirtualPackageManager";
    private static VirtualPackageManager instance;

    private final Context context;
    private final VirtualCore core;
    private final Map<String, VirtualApp> virtualApps = new HashMap<>();

    public static VirtualPackageManager getInstance(Context context) {
        if (instance == null) {
            synchronized (VirtualPackageManager.class) {
                if (instance == null) {
                    instance = new VirtualPackageManager(context);
                }
            }
        }
        return instance;
    }

    private VirtualPackageManager(Context context) {
        this.context = context.getApplicationContext();
        this.core = VirtualCore.get();
        VirtualLog.d(TAG, "VirtualPackageManager initialized");
    }

    public void registerVirtualApp(VirtualApp app) {
        if (app != null && app.packageName != null) {
            virtualApps.put(app.packageName, app);
            VirtualLog.d(TAG, "Registered virtual app: " + app.packageName + " -> " + app.fakePackageName);
        }
    }

    public void unregisterVirtualApp(String packageName) {
        virtualApps.remove(packageName);
        VirtualLog.d(TAG, "Unregistered virtual app: " + packageName);
    }

    public boolean isVirtualPackage(String packageName) {
        if (packageName == null) return false;
        if (virtualApps.containsKey(packageName)) return true;
        for (VirtualApp app : virtualApps.values()) {
            if (packageName.equals(app.fakePackageName)) return true;
        }
        return false;
    }

    public VirtualApp getVirtualApp(String packageName) {
        if (packageName == null) return null;
        VirtualApp app = virtualApps.get(packageName);
        if (app != null) return app;
        for (VirtualApp vApp : virtualApps.values()) {
            if (packageName.equals(vApp.fakePackageName)) return vApp;
        }
        return null;
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        try {
            VirtualApp virtualApp = getVirtualApp(packageName);
            if (virtualApp != null) {
                return createVirtualPackageInfo(virtualApp, flags);
            }
            return null;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting package info for: " + packageName, e);
            return null;
        }
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        try {
            VirtualApp virtualApp = getVirtualApp(packageName);
            if (virtualApp != null) {
                return createVirtualApplicationInfo(virtualApp);
            }
            return null;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting application info for: " + packageName, e);
            return null;
        }
    }

    public Intent getLaunchIntentForPackage(String packageName, int userId) {
        try {
            VirtualApp virtualApp = getVirtualApp(packageName);
            if (virtualApp == null) return null;

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(virtualApp.fakePackageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            List<ResolveInfo> activities = queryIntentActivities(intent, 0, null, userId);
            if (activities != null && !activities.isEmpty()) {
                ResolveInfo ri = activities.get(0);
                intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                return intent;
            }
            return intent;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting launch intent for: " + packageName, e);
            return null;
        }
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags, String resolvedType, int userId) {
        try {
            List<ResolveInfo> result = new ArrayList<>();

            android.content.pm.PackageManager pm = context.getPackageManager();
            List<android.content.pm.ResolveInfo> activities = pm.queryIntentActivities(intent, flags);

            for (android.content.pm.ResolveInfo ri : activities) {
                if (ri.activityInfo != null) {
                    ResolveInfo vri = convertToVirtualResolveInfo(ri, userId);
                    if (vri != null) {
                        result.add(vri);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error querying intent activities", e);
            return Collections.emptyList();
        }
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int flags, String resolvedType, int userId) {
        try {
            List<ResolveInfo> result = new ArrayList<>();
            android.content.pm.PackageManager pm = context.getPackageManager();
            List<android.content.pm.ResolveInfo> services = pm.queryIntentServices(intent, flags);

            for (android.content.pm.ResolveInfo ri : services) {
                if (ri.serviceInfo != null) {
                    ResolveInfo vri = convertToVirtualResolveInfo(ri, userId);
                    if (vri != null) {
                        result.add(vri);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error querying intent services", e);
            return Collections.emptyList();
        }
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, String resolvedType, int userId) {
        try {
            List<ResolveInfo> result = new ArrayList<>();
            android.content.pm.PackageManager pm = context.getPackageManager();
            List<android.content.pm.ResolveInfo> receivers = pm.queryBroadcastReceivers(intent, flags);

            for (android.content.pm.ResolveInfo ri : receivers) {
                if (ri.activityInfo != null) {
                    ResolveInfo vri = convertToVirtualResolveInfo(ri, userId);
                    if (vri != null) {
                        result.add(vri);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error querying broadcast receivers", e);
            return Collections.emptyList();
        }
    }

    public ResolveInfo resolveActivity(Intent intent, int flags, String resolvedType, int userId) {
        List<ResolveInfo> activities = queryIntentActivities(intent, flags, resolvedType, userId);
        if (activities != null && !activities.isEmpty()) {
            return activities.get(0);
        }
        return null;
    }

    public ResolveInfo resolveService(Intent intent, int flags, String resolvedType, int userId) {
        List<ResolveInfo> services = queryIntentServices(intent, flags, resolvedType, userId);
        if (services != null && !services.isEmpty()) {
            return services.get(0);
        }
        return null;
    }

    public ProviderInfo resolveContentProvider(String authority, int flags, int userId) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            List<android.content.pm.ProviderInfo> providers = pm.queryContentProviders(null, 0, 0);

            for (android.content.pm.ProviderInfo pi : providers) {
                if (authority.equals(pi.authority)) {
                    ProviderInfo vpi = new ProviderInfo();
                    vpi.authority = pi.authority;
                    vpi.packageName = pi.packageName;
                    vpi.name = pi.name;
                    vpi.processName = pi.processName;
                    vpi.applicationInfo = getApplicationInfo(pi.packageName, 0, userId);
                    return vpi;
                }
            }
            return null;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error resolving content provider: " + authority, e);
            return null;
        }
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ActivityInfo ai = pm.getActivityInfo(component, flags);

            VirtualApp virtualApp = getVirtualApp(component.getPackageName());
            if (virtualApp != null) {
                return createVirtualActivityInfo(ai, virtualApp);
            }
            return null;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting activity info for: " + component, e);
            return null;
        }
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ServiceInfo si = pm.getServiceInfo(component, flags);

            VirtualApp virtualApp = getVirtualApp(component.getPackageName());
            if (virtualApp != null) {
                return createVirtualServiceInfo(si, virtualApp);
            }
            return null;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting service info for: " + component, e);
            return null;
        }
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ProviderInfo pi = pm.getProviderInfo(component, flags);

            VirtualApp virtualApp = getVirtualApp(component.getPackageName());
            if (virtualApp != null) {
                return createVirtualProviderInfo(pi, virtualApp);
            }
            return null;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error getting provider info for: " + component, e);
            return null;
        }
    }

    public boolean isInstalled(String packageName, int userId) {
        return isVirtualPackage(packageName);
    }

    private PackageInfo createVirtualPackageInfo(VirtualApp app, int flags) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            PackageInfo original = pm.getPackageArchiveInfo(getOriginalApkPath(app.packageName), 0);

            if (original == null) {
                original = pm.getPackageInfo(app.packageName, flags);
            }

            if (original != null) {
                PackageInfo vp = new PackageInfo();
                vp.packageName = app.fakePackageName;
                vp.versionCode = original.versionCode;
                vp.versionName = original.versionName;
                vp.applicationInfo = createVirtualApplicationInfo(app);
                vp.firstInstallTime = System.currentTimeMillis();
                vp.lastUpdateTime = System.currentTimeMillis();
                return vp;
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error creating virtual package info", e);
        }
        return null;
    }

    private ApplicationInfo createVirtualApplicationInfo(VirtualApp app) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            ApplicationInfo original = pm.getApplicationInfo(app.packageName, 0);

            ApplicationInfo ai = new ApplicationInfo(original);
            ai.packageName = app.fakePackageName;
            ai.sourceDir = getVirtualDataDir(app) + "/base.apk";
            ai.publicSourceDir = ai.sourceDir;
            ai.dataDir = getVirtualDataDir(app);
            ai.nativeLibraryDir = getVirtualLibDir(app);

            File dataDir = new File(ai.dataDir);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            File libDir = new File(ai.nativeLibraryDir);
            if (!libDir.exists()) {
                libDir.mkdirs();
            }

            return ai;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error creating virtual application info", e);
            return null;
        }
    }

    private ActivityInfo createVirtualActivityInfo(android.content.pm.ActivityInfo original, VirtualApp app) {
        ActivityInfo ai = new ActivityInfo(original);
        ai.packageName = app.fakePackageName;
        ai.applicationInfo = createVirtualApplicationInfo(app);
        return ai;
    }

    private ServiceInfo createVirtualServiceInfo(android.content.pm.ServiceInfo original, VirtualApp app) {
        ServiceInfo si = new ServiceInfo(original);
        si.packageName = app.fakePackageName;
        si.applicationInfo = createVirtualApplicationInfo(app);
        return si;
    }

    private ProviderInfo createVirtualProviderInfo(android.content.pm.ProviderInfo original, VirtualApp app) {
        ProviderInfo pi = new ProviderInfo(original);
        pi.packageName = app.fakePackageName;
        pi.applicationInfo = createVirtualApplicationInfo(app);
        return pi;
    }

    private ResolveInfo convertToVirtualResolveInfo(android.content.pm.ResolveInfo original, int userId) {
        try {
            VirtualApp virtualApp = getVirtualApp(original.activityInfo.packageName);
            if (virtualApp == null) {
                virtualApp = getVirtualApp(original.serviceInfo != null ? original.serviceInfo.packageName : original.activityInfo.packageName);
            }

            ResolveInfo ri = new ResolveInfo();
            ri.activityInfo = original.activityInfo;
            ri.priority = original.priority;
            ri.preferredOrder = original.preferredOrder;
            ri.match = original.match;
            ri.specificIndex = original.specificIndex;
            ri.labelRes = original.labelRes;
            ri.nonLocalizedLabel = original.nonLocalizedLabel;
            ri.icon = original.icon;
            ri.configurationDescription = original.configurationDescription;

            if (virtualApp != null && ri.activityInfo != null) {
                ri.activityInfo = createVirtualActivityInfo(ri.activityInfo, virtualApp);
            }

            return ri;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error converting resolve info", e);
            return null;
        }
    }

    private String getVirtualDataDir(VirtualApp app) {
        return context.getFilesDir().getAbsolutePath() + "/virtual/" + app.userId + "/" + app.packageName;
    }

    private String getVirtualLibDir(VirtualApp app) {
        return context.getFilesDir().getAbsolutePath() + "/virtual/" + app.userId + "/" + app.packageName + "/lib";
    }

    private String getOriginalApkPath(String packageName) {
        File dataApp = new File("/data/app");
        if (dataApp.exists()) {
            File[] files = dataApp.listFiles();
            if (files != null) {
                for (File dir : files) {
                    if (dir.isDirectory() && dir.getName().contains(packageName)) {
                        File baseApk = new File(dir, "base.apk");
                        if (baseApk.exists()) {
                            return baseApk.getAbsolutePath();
                        }
                    }
                }
            }
        }
        return null;
    }
}