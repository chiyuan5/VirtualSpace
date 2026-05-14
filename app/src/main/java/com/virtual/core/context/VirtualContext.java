package com.virtual.core.context;

import android.app.Application;
import android.app.ResourcesManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.os.Build;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class VirtualContext {

    private static final String TAG = "VirtualContext";

    public static Context createVirtualContext(Context hostContext, VirtualPackage pkg) {
        try {
            Context virtualContext;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                virtualContext = createContextApi30(hostContext, pkg);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                virtualContext = createContextApi24(hostContext, pkg);
            } else {
                virtualContext = createContextApiBelow24(hostContext, pkg);
            }
            
            Log.i(TAG, "Created virtual context for: " + pkg.packageName);
            return virtualContext;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create virtual context", e);
            return hostContext;
        }
    }

    private static Context createContextApi30(Context hostContext, VirtualPackage pkg) {
        try {
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            Constructor<?> constructor = contextImplClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            ContextImpl contextImpl = (ContextImpl) constructor.newInstance();
            
            contextImpl.init(hostContext.getOuterContext(), null);
            contextImpl.setResources(createVirtualResources(hostContext, pkg));
            
            return contextImpl;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create context for API 30+", e);
            return hostContext;
        }
    }

    private static Context createContextApi24(Context hostContext, VirtualPackage pkg) {
        try {
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            
            Method createAppContextMethod = contextImplClass.getDeclaredMethod(
                "createAppContext", 
                contextImplClass, 
                Context.class
            );
            createAppContextMethod.setAccessible(true);
            
            Object contextImpl = createAppContextMethod.invoke(
                null, 
                hostContext.getApplicationContext(), 
                hostContext
            );
            
            return (Context) contextImpl;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create context for API 24+", e);
            return hostContext;
        }
    }

    private static Context createContextApiBelow24(Context hostContext, VirtualPackage pkg) {
        try {
            Object mainThread = android.app.ActivityThread.currentActivityThread();
            
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            Method createAppContextMethod = contextImplClass.getDeclaredMethod(
                "createAppContext",
                Class.forName("android.app.ActivityThread"),
                Context.class
            );
            createAppContextMethod.setAccessible(true);
            
            Object contextImpl = createAppContextMethod.invoke(
                null, 
                mainThread, 
                hostContext
            );
            
            return (Context) contextImpl;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create context for API <24", e);
            return hostContext;
        }
    }

    private static Resources createVirtualResources(Context hostContext, VirtualPackage pkg) {
        try {
            Resources hostResources = hostContext.getResources();
            Resources.Theme theme = hostResources.newTheme();
            
            String apkPath = pkg.apkPath;
            
            android.content.res.AssetManager assets = null;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                assets = android.content.res.AssetManager.class.newInstance();
                Method addAssetPathMethod = android.content.res.AssetManager.class.getDeclaredMethod(
                    "addAssetPath", 
                    String.class
                );
                addAssetPathMethod.setAccessible(true);
                addAssetPathMethod.invoke(assets, apkPath);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    Method addAssetPathAsSharedLibrary = android.content.res.AssetManager.class.getDeclaredMethod(
                        "addAssetPathAsSharedLibrary",
                        String.class
                    );
                    addAssetPathAsSharedLibrary.setAccessible(true);
                    try {
                        addAssetPathAsSharedLibrary.invoke(assets, apkPath);
                    } catch (Exception e) {
                    }
                }
            }
            
            CompatibilityInfo compatInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
            
            Constructor<?> resourcesConstructor = Resources.class.getConstructor(
                android.content.res.AssetManager.class,
                DisplayMetrics.class,
                DisplayMetrics.class,
                Configuration.class,
                CompatibilityInfo.class,
                IBinder.class
            );
            resourcesConstructor.setAccessible(true);
            
            Resources resources = (Resources) resourcesConstructor.newInstance(
                assets,
                hostResources.getDisplayMetrics(),
                hostResources.getDisplayMetrics(),
                hostResources.getConfiguration(),
                compatInfo,
                null
            );
            
            return resources;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create virtual resources", e);
            return hostContext.getResources();
        }
    }

    private static class DisplayMetrics {
        float density = 3.0f;
        float densityDpi = 480;
        float scaledDensity = 3.0f;
        int widthPixels = 1080;
        int heightPixels = 1920;
    }

    private static class Configuration {
        int screenWidthDp = 360;
        int screenHeightDp = 640;
        int smallestScreenWidthDp = 360;
    }

    private static class IBinder {
    }
}
