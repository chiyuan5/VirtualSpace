package com.virtual.util;

public class VirtualLog {
    private static final String TAG = "VirtualSpace";
    private static boolean DEBUG = true;

    public static void d(String tag, String msg) {
        if (DEBUG) {
            android.util.VirtualLog.d(TAG + "/" + tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        android.util.VirtualLog.i(TAG + "/" + tag, msg);
    }

    public static void w(String tag, String msg) {
        android.util.VirtualLog.w(TAG + "/" + tag, msg);
    }

    public static void e(String tag, String msg) {
        android.util.VirtualLog.e(TAG + "/" + tag, msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        android.util.VirtualLog.e(TAG + "/" + tag, msg, t);
    }

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }
}