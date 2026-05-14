package com.virtual.core.system;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

public interface IVirtualPackageManager {

    ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) throws RemoteException;

    ActivityInfo getReceiverInfo(ComponentName componentName, int flags, int userId) throws RemoteException;

    ActivityInfo getActivityInfoForUser(ComponentName component, int flags, int userId) throws RemoteException;

    ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) throws RemoteException;

    ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;

    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) throws RemoteException;

    Intent getLaunchIntentForPackage(String packageName, int userId) throws RemoteException;

    List<ResolveInfo> queryIntentActivities(Intent intent, int flags, String resolvedType, int userId) throws RemoteException;

    List<ResolveInfo> queryIntentServices(Intent intent, int flags, String resolvedType, int userId) throws RemoteException;

    List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, String resolvedType, int userId) throws RemoteException;

    List<ProviderInfo> queryContentProviders(String processName, int uid, int flags, int userId) throws RemoteException;

    List<ApplicationInfo> getInstalledApplications(int flags, int userId) throws RemoteException;

    List<PackageInfo> getInstalledPackages(int flags, int userId) throws RemoteException;

    ResolveInfo resolveService(Intent intent, int flags, String resolvedType, int userId) throws RemoteException;

    ResolveInfo resolveActivity(Intent intent, int flags, String resolvedType, int userId) throws RemoteException;

    ProviderInfo resolveContentProvider(String authority, int flags, int userId) throws RemoteException;

    boolean isInstalled(String packageName, int userId) throws RemoteException;

    void installPackageAsUser(String file, int userId) throws RemoteException;

    void uninstallPackage(String packageName, int userId) throws RemoteException;

    void clearPackage(String packageName, int userId) throws RemoteException;

    void stopPackage(String packageName, int userId) throws RemoteException;
}