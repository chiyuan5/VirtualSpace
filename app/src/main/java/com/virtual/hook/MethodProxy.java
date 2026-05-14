package com.virtual.hook;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import com.virtual.core.VirtualCore;
import com.virtual.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MethodProxy {

    private static final String TAG = "MethodProxy";

    public static IBinder queryLocalInterface(Object obj, String descriptor) {
        if (obj == null) {
            return null;
        }

        try {
            Field field = obj.getClass().getDeclaredField("mDescriptor");
            field.setAccessible(true);
            String mDescriptor = (String) field.get(obj);

            if (mDescriptor != null && mDescriptor.equals(descriptor)) {
                Log.d(TAG, "Intercepted queryLocalInterface for: " + descriptor);
            }
        } catch (Exception e) {
        }

        return null;
    }

    public static PackageManager getPackageManager(Object obj) {
        try {
            Field field = ActivityThread.class.getDeclaredField("mSystemContext");
            field.setAccessible(true);
            Context context = (Context) field.get(null);

            if (context != null) {
                return context.getPackageManager();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get PackageManager", e);
        }

        return null;
    }

    public static int getCallingUid(Object obj, Object[] args) {
        try {
            if (args != null && args.length > 0) {
                Object BinderObj = args[0];
                Class<?> BinderClass = Class.forName("android.os.Binder");
                Method getCallingUidMethod = BinderClass.getDeclaredMethod("getCallingUid");
                return (int) getCallingUidMethod.invoke(null);
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public static String getPackageName(Object obj, Object[] args) {
        try {
            if (args != null && args.length > 0 && args[0] != null) {
                if (args[0] instanceof String) {
                    return (String) args[0];
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static int getUserId(Object obj, Object[] args) {
        try {
            if (args != null && args.length > 0) {
                if (args[0] instanceof Integer) {
                    return (int) args[0];
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public static Object onTransact(Object obj, int code, Object data, Object reply, int flags) {
        try {
            Log.d(TAG, "onTransact called with code: " + code);
        } catch (Exception e) {
        }
        return null;
    }

    public static String getDeviceId(int userId) {
        VirtualApp app = VirtualCore.get().getVirtualAppByUserId(userId);
        if (app != null && app.fakeDeviceId != null) {
            return app.fakeDeviceId;
        }
        return null;
    }

    public static String getAndroidId(int userId) {
        VirtualApp app = VirtualCore.get().getVirtualAppByUserId(userId);
        if (app != null && app.fakeAndroidId != null) {
            return app.fakeAndroidId;
        }
        return null;
    }
}
