package com.virtual.core.loader;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.core.context.VirtualContext;
import com.virtual.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualAppLoader {

    private static final String TAG = "VirtualAppLoader";

    private final VirtualCore core;
    private final Map<String, LoadedApk> loadedApks = new HashMap<>();
    private Object activityThread;
    private Context systemContext;

    public VirtualAppLoader(VirtualCore core) {
        this.core = core;
    }

    public void init() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            activityThread = currentActivityThread.invoke(null);

            if (activityThread != null) {
                Field mSystemContextField = activityThreadClass.getDeclaredField("mSystemContext");
                mSystemContextField.setAccessible(true);
                systemContext = (Context) mSystemContextField.get(activityThread);
            }

            Log.i(TAG, "VirtualAppLoader initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize VirtualAppLoader", e);
        }
    }

    public Context loadApp(String packageName) {
        VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
        if (vPkg == null) {
            Log.e(TAG, "Package not found: " + packageName);
            return null;
        }

        try {
            LoadedApk apk = loadedApks.get(packageName);
            if (apk == null) {
                apk = createLoadedApk(vPkg);
                loadedApks.put(packageName, apk);
            }

            Context virtualContext = VirtualContext.createVirtualContext(systemContext, vPkg);
            return virtualContext;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load app: " + packageName, e);
            return null;
        }
    }

    private LoadedApk createLoadedApk(VirtualPackage vPkg) throws Exception {
        LoadedApk apk = new LoadedApk();
        apk.packageName = vPkg.packageName;
        apk.apkPath = vPkg.apkPath;
        apk.appInfo = createApplicationInfo(vPkg);
        apk.music = createMusicManager(apk);

        return apk;
    }

    private ApplicationInfo createApplicationInfo(VirtualPackage vPkg) throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = vPkg.packageName;
        appInfo.sourceDir = vPkg.apkPath;
        appInfo.publicSourceDir = vPkg.apkPath;
        appInfo.dataDir = "/data/data/" + vPkg.packageName;
        appInfo.uid = vPkg.userId * 100000;
        appInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        appInfo.enabled = true;

        return appInfo;
    }

    private Object createMusicManager(LoadedApk apk) throws Exception {
        Class<?> LoadedApkClass = Class.forName("android.app.LoadedApk");
        Constructor<?> constructor = LoadedApkClass.getDeclaredConstructor(
            ActivityThread.class, 
            ApplicationInfo.class, 
            CompatibilityInfo.class,
            ContainerActivity.class
        );
        constructor.setAccessible(true);

        return constructor.newInstance(
            activityThread,
            apk.appInfo,
            CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO,
            null
        );
    }

    public Application loadApplication(LoadedApk apk, Context context) {
        try {
            if (apk.application != null) {
                return apk.application;
            }

            Class<?> LoadedApkClass = Class.forName("android.app.LoadedApk");
            Method makeApplicationMethod = LoadedApkClass.getDeclaredMethod(
                "makeApplication", 
                boolean.class, 
                Instrumentation.class
            );
            makeApplicationMethod.setAccessible(true);

            apk.application = (Application) makeApplicationMethod.invoke(
                apk.music, 
                false, 
                null
            );

            Log.i(TAG, "Loaded application: " + apk.packageName);
            return apk.application;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load application", e);
            return null;
        }
    }

    public void startActivity(Context context, Intent intent) {
        try {
            ComponentName component = intent.getComponent();
            if (component == null) {
                return;
            }

            String packageName = component.getPackageName();
            VirtualPackage vPkg = core.getVirtualPackageByName(packageName);
            if (vPkg == null) {
                Log.e(TAG, "Package not found for activity: " + packageName);
                return;
            }

            String targetActivity = findLaunchActivity(vPkg);
            if (targetActivity != null) {
                ComponentName newComponent = new ComponentName(packageName, targetActivity);
                intent.setComponent(newComponent);

                Class<?> instrumentationClass = Class.forName("android.app.Instrumentation");
                Method execStartActivityMethod = instrumentationClass.getDeclaredMethod(
                    "execStartActivity",
                    Context.class,
                    IBinder.class,
                    IBinder.class,
                    Activity.class,
                    Intent.class,
                    int.class
                );
                execStartActivityMethod.setAccessible(true);

                execStartActivityMethod.invoke(
                    null,
                    context,
                    null,
                    null,
                    null,
                    intent,
                    0
                );

                Log.i(TAG, "Started activity: " + newComponent.flattenToShortString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start activity", e);
        }
    }

    private String findLaunchActivity(VirtualPackage vPkg) {
        try {
            PackageManager pm = core.getContext().getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(
                vPkg.apkPath,
                PackageManager.GET_ACTIVITIES
            );

            if (pkgInfo != null && pkgInfo.activities != null) {
                for (ActivityInfo activity : pkgInfo.activities) {
                    if (activity.exported && activity.enabled) {
                        if (activity.name.contains("MainActivity") ||
                            activity.name.contains("LauncherActivity") ||
                            activity.name.contains("SplashActivity")) {
                            return activity.name;
                        }
                    }
                }
                return pkgInfo.activities[0].name;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to find launch activity", e);
        }
        return null;
    }

    public static class LoadedApk {
        public String packageName;
        public String apkPath;
        public ApplicationInfo appInfo;
        public Application application;
        public Object music;
    }

    private static class CompatibilityInfo {
        public static CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo();
    }

    private static class ContainerActivity extends android.app.Activity {
    }

    private static class Instrumentation {
    }
}
