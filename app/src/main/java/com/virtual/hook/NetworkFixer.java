package com.virtual.hook;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.virtual.util.VirtualLog;

public class NetworkFixer {
    private static final String TAG = "NetworkFixer";
    private static boolean initialized = false;

    public static void init(Context context) {
        if (initialized) return;
        initialized = true;

        try {
            VirtualLog.d(TAG, "Initializing network fixes for Android " + Build.VERSION.SDK_INT);

            ensureNetworkAccess();

            VirtualLog.d(TAG, "Network fixes initialized successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to initialize network fixes", e);
        }
    }

    private static void ensureNetworkAccess() {
        try {
            System.setProperty("network.available", "true");
            System.setProperty("network.connected", "true");
            VirtualLog.d(TAG, "Network access properties set");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to set network properties", e);
        }
    }

    public static NetworkInfo createMockNetworkInfo() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                NetworkInfo networkInfo = new NetworkInfo(
                    ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
                return networkInfo;
            } else {
                return createNetworkInfoViaReflection();
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create mock NetworkInfo", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static NetworkInfo createNetworkInfoViaReflection() {
        try {
            Class<?> networkInfoClass = Class.forName("android.net.NetworkInfo");
            Constructor<?> constructor = networkInfoClass.getDeclaredConstructor(
                int.class, int.class, String.class, String.class);
            constructor.setAccessible(true);

            NetworkInfo networkInfo = (NetworkInfo) constructor.newInstance(
                ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");

            return networkInfo;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create NetworkInfo via reflection", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Object createMockNetworkCapabilities() {
        try {
            Class<?> ncClass = Class.forName("android.net.NetworkCapabilities");
            Constructor<?> constructor = ncClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object nc = constructor.newInstance();
            return nc;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create NetworkCapabilities", e);
            return null;
        }
    }

    public static Object createMockLinkProperties() {
        try {
            Class<?> lpClass = Class.forName("android.net.LinkProperties");
            Constructor<?> constructor = lpClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object linkProperties = constructor.newInstance();
            return linkProperties;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create LinkProperties", e);
            return null;
        }
    }

    public static List<String> getDnsServers() {
        List<String> dnsServers = new ArrayList<>();
        dnsServers.add("8.8.8.8");
        dnsServers.add("8.8.4.4");
        dnsServers.add("1.1.1.1");
        dnsServers.add("1.0.0.1");
        return dnsServers;
    }
}
