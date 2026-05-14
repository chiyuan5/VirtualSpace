package com.virtual.core.service;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.VirtualLog;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class ServiceBroker {

    private static final String TAG = "ServiceBroker";
    private static ServiceBroker instance;

    private final VirtualCore core;
    private final Context context;

    private final Map<String, IBinder> originalServices = new HashMap<>();
    private final Map<String, IBinder> proxyServices = new HashMap<>();

    public interface ServiceInterceptor {
        Object intercept(int code, Parcel data, Parcel reply, int flags) throws RemoteException;
    }

    private ServiceBroker(Context context) {
        this.context = context;
        this.core = VirtualCore.get();
    }

    public static ServiceBroker getInstance(Context context) {
        if (instance == null) {
            synchronized (ServiceBroker.class) {
                if (instance == null) {
                    instance = new ServiceBroker(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void init() {
        hookSystemServices();
        replaceServiceManager();
        VirtualLog.i(TAG, "ServiceBroker initialized");
    }

    private void hookSystemServices() {
        try {
            hookActivityManagerService();
            hookPackageManagerService();
            hookUserManagerService();
            hookAccountManagerService();
            hookDevicePolicyService();
            hookNotificationManagerService();
            hookTelephonyService();
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook system services", e);
        }
    }

    private void hookActivityManagerService() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder originalAMS = (IBinder) getServiceMethod.invoke(null, "activity");
            if (originalAMS != null) {
                originalServices.put("activity", originalAMS);

                IBinder proxyAMS = createAMSProxy(originalAMS);
                proxyServices.put("activity", proxyAMS);

                replaceServiceInCache("activity", proxyAMS);
                VirtualLog.i(TAG, "ActivityManagerService hooked");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook AMS", e);
        }
    }

    private void hookPackageManagerService() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder originalPMS = (IBinder) getServiceMethod.invoke(null, "package");
            if (originalPMS != null) {
                originalServices.put("package", originalPMS);

                IBinder proxyPMS = createPMSProxy(originalPMS);
                proxyServices.put("package", proxyPMS);

                replaceServiceInCache("package", proxyPMS);
                VirtualLog.i(TAG, "PackageManagerService hooked");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook PMS", e);
        }
    }

    private void hookUserManagerService() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder originalUMS = (IBinder) getServiceMethod.invoke(null, "user");
            if (originalUMS != null) {
                originalServices.put("user", originalUMS);
                VirtualLog.i(TAG, "UserManagerService available");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook UMS", e);
        }
    }

    private void hookAccountManagerService() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder original = (IBinder) getServiceMethod.invoke(null, "account");
            if (original != null) {
                originalServices.put("account", original);
                VirtualLog.i(TAG, "AccountManagerService available");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook AccountManager", e);
        }
    }

    private void hookDevicePolicyService() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder original = (IBinder) getServiceMethod.invoke(null, "device_policy");
            if (original != null) {
                originalServices.put("device_policy", original);
                VirtualLog.i(TAG, "DevicePolicyService available");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook DevicePolicy", e);
        }
    }

    private void hookNotificationManagerService() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder original = (IBinder) getServiceMethod.invoke(null, "notification");
            if (original != null) {
                originalServices.put("notification", original);
                VirtualLog.i(TAG, "NotificationManagerService available");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook NotificationManager", e);
        }
    }

    private void hookTelephonyService() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);

            IBinder original = (IBinder) getServiceMethod.invoke(null, "phone");
            if (original != null) {
                originalServices.put("phone", original);
                VirtualLog.i(TAG, "TelephonyService available");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to hook Telephony", e);
        }
    }

    private void replaceServiceManager() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method systemMainMethod = activityThreadClass.getDeclaredMethod("systemMain");
            Object activityThread = systemMainMethod.invoke(null);

            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            Field iActivityManagerField = activityThreadClass.getDeclaredField("mIActivityManager");
            iActivityManagerField.setAccessible(true);

            IBinder currentAMS = (IBinder) iActivityManagerField.get(activityThread);
            if (currentAMS != null) {
                IBinder proxyAMS = createAMSProxy(currentAMS);
                iActivityManagerField.set(activityThread, proxyAMS);
                VirtualLog.i(TAG, "ActivityThread IActivityManager replaced");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to replace ServiceManager", e);
        }
    }

    private void replaceServiceInCache(String name, IBinder binder) {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Field cacheField = serviceManagerClass.getDeclaredField("sCache");
            cacheField.setAccessible(true);
            Map<String, IBinder> cache = (Map<String, IBinder>) cacheField.get(null);
            cache.put(name, binder);
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to replace service in cache: " + name, e);
        }
    }

    private IBinder createAMSProxy(final IBinder original) {
        return Proxy.newProxyBinder(original.getClass().getClassLoader(),
            new IBinder() {
                @Override
                public boolean pingBinder() {
                    return original.pingBinder();
                }

                @Override
                public boolean isBinderAlive() {
                    return original.isBinderAlive();
                }

                @Override
                public IInterface queryLocalInterface(String descriptor) {
                    return original.queryLocalInterface(descriptor);
                }

                @Override
                public void dump(FileDescriptor fd, String[] args) throws RemoteException {
                    original.dump(fd, args);
                }

                @Override
                public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
                    original.dumpAsync(fd, args);
                }

                @Override
                public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    if (interceptAMS(code, data, reply, flags)) {
                        return true;
                    }
                    return original.transact(code, data, reply, flags);
                }
            });
    }

    private IBinder createPMSProxy(final IBinder original) {
        return Proxy.newProxyBinder(original.getClass().getClassLoader(),
            new IBinder() {
                @Override
                public boolean pingBinder() {
                    return original.pingBinder();
                }

                @Override
                public boolean isBinderAlive() {
                    return original.isBinderAlive();
                }

                @Override
                public IInterface queryLocalInterface(String descriptor) {
                    return original.queryLocalInterface(descriptor);
                }

                @Override
                public void dump(FileDescriptor fd, String[] args) throws RemoteException {
                    original.dump(fd, args);
                }

                @Override
                public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
                    original.dumpAsync(fd, args);
                }

                @Override
                public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    if (interceptPMS(code, data, reply, flags)) {
                        return true;
                    }
                    return original.transact(code, data, reply, flags);
                }
            });
    }

    private boolean interceptAMS(int code, Parcel data, Parcel reply, int flags) {
        try {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();

            VirtualApp app = getVirtualAppForUid(callingUid);
            if (app == null) {
                return false;
            }

            switch (code) {
                case 3:
                case 4:
                    handleStartActivity(data, app);
                    break;
                case 5:
                case 6:
                    handleStopActivity(data, app);
                    break;
                case 7:
                    handleResumeActivity(data, app);
                    break;
                case 8:
                    handlePauseActivity(data, app);
                    break;
                case 9:
                    handleStopActivity(data, app);
                    break;
                default:
                    break;
            }

            return false;
        } catch (Exception e) {
            VirtualLog.e(TAG, "AMS intercept error", e);
            return false;
        }
    }

    private boolean interceptPMS(int code, Parcel data, Parcel reply, int flags) {
        try {
            return false;
        } catch (Exception e) {
            VirtualLog.e(TAG, "PMS intercept error", e);
            return false;
        }
    }

    private void handleStartActivity(Parcel data, VirtualApp app) {
        try {
            data.setDataPosition(0);
        } catch (Exception e) {
            VirtualLog.e(TAG, "handleStartActivity error", e);
        }
    }

    private void handleStopActivity(Parcel data, VirtualApp app) {
        try {
            data.setDataPosition(0);
        } catch (Exception e) {
            VirtualLog.e(TAG, "handleStopActivity error", e);
        }
    }

    private void handleResumeActivity(Parcel data, VirtualApp app) {
        try {
            data.setDataPosition(0);
        } catch (Exception e) {
            VirtualLog.e(TAG, "handleResumeActivity error", e);
        }
    }

    private void handlePauseActivity(Parcel data, VirtualApp app) {
        try {
            data.setDataPosition(0);
        } catch (Exception e) {
            VirtualLog.e(TAG, "handlePauseActivity error", e);
        }
    }

    private VirtualApp getVirtualAppForUid(int uid) {
        int userId = uid / 100000;
        for (VirtualApp app : core.getAllVirtualApps()) {
            if (app.userId == userId) {
                return app;
            }
        }
        return null;
    }

    public IBinder getOriginalService(String name) {
        return originalServices.get(name);
    }

    public IBinder getService(String name) {
        return proxyServices.containsKey(name) ? proxyServices.get(name) : originalServices.get(name);
    }

    public void cleanup() {
        originalServices.clear();
        proxyServices.clear();
        instance = null;
    }

    private static abstract class FileDescriptor {
    }
}
