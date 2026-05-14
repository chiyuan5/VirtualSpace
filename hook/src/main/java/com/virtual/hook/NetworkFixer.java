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
                networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                return networkInfo;
            } else {
                return createNetworkInfoViaReflection();
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create mock NetworkInfo", e);
            return null;
        }
    }

    private static NetworkInfo createNetworkInfoViaReflection() {
        try {
            Class&lt;?&gt; networkInfoClass = Class.forName("android.net.NetworkInfo");
            Constructor&lt;?&gt; constructor = networkInfoClass.getDeclaredConstructor(
                int.class, int.class, String.class, String.class);
            constructor.setAccessible(true);

            NetworkInfo networkInfo = (NetworkInfo) constructor.newInstance(
                ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");

            Method setDetailedState = networkInfoClass.getDeclaredMethod(
                "setDetailedState", NetworkInfo.DetailedState.class, String.class, String.class);
            setDetailedState.setAccessible(true);
            setDetailedState.invoke(networkInfo, NetworkInfo.DetailedState.CONNECTED, null, null);

            return networkInfo;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create NetworkInfo via reflection", e);
            return null;
        }
    }

    public static Object createMockNetworkCapabilities() {
        try {
            Class&lt;?&gt; ncClass = Class.forName("android.net.NetworkCapabilities");
            Constructor&lt;?&gt; constructor = ncClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object nc = constructor.newInstance();

            try {
                Method addTransportType = ncClass.getMethod("addTransportType", int.class);
                addTransportType.setAccessible(true);
                addTransportType.invoke(nc, 1);
                addTransportType.invoke(nc, 0);
            } catch (Exception e) {
                VirtualLog.w(TAG, "Could not add transport types", e);
            }

            try {
                Method addCapability = ncClass.getMethod("addCapability", int.class);
                addCapability.setAccessible(true);
                addCapability.invoke(nc, 12);
                addCapability.invoke(nc, 16);
                addCapability.invoke(nc, 11);
                addCapability.invoke(nc, 2);
            } catch (Exception e) {
                VirtualLog.w(TAG, "Could not add capabilities", e);
            }

            return nc;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create NetworkCapabilities", e);
            return null;
        }
    }

    public static Object createMockLinkProperties() {
        try {
            Class&lt;?&gt; lpClass = Class.forName("android.net.LinkProperties");
            Constructor&lt;?&gt; constructor = lpClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object linkProperties = constructor.newInstance();

            try {
                List&lt;Object&gt; dnsServers = new ArrayList&lt;&gt;();
                Class&lt;?&gt; inetAddressClass = Class.forName("java.net.InetAddress");
                Method getByName = inetAddressClass.getMethod("getByName", String.class);
                dnsServers.add(getByName.invoke(null, "8.8.8.8"));
                dnsServers.add(getByName.invoke(null, "8.8.4.4"));

                Method setDnsServers = lpClass.getMethod("setDnsServers", List.class);
                setDnsServers.setAccessible(true);
                setDnsServers.invoke(linkProperties, dnsServers);
            } catch (Exception e) {
                VirtualLog.w(TAG, "Could not set DNS servers", e);
            }

            return linkProperties;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to create LinkProperties", e);
            return null;
        }
    }

    public static List&lt;String&gt; getDnsServers() {
        List&lt;String&gt; dnsServers = new ArrayList&lt;&gt;();
        dnsServers.add("8.8.8.8");
        dnsServers.add("8.8.4.4");
        dnsServers.add("1.1.1.1");
        dnsServers.add("1.0.0.1");
        return dnsServers;
    }
}