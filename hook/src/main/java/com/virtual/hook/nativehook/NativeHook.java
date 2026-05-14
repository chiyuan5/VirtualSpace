package com.virtual.hook.nativehook;

import android.os.Build;
import android.util.Log;

public class NativeHook {
    
    private static final boolean SUPPORTED = true;
    private static NativeHook instance;
    private long nativeHandle = 0;
    
    static {
        try {
            System.loadLibrary("virtualhook");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
    
    private NativeHook() {
        initNative();
    }
    
    public static NativeHook getInstance() {
        if (instance == null) {
            synchronized (NativeHook.class) {
                if (instance == null) {
                    instance = new NativeHook();
                }
            }
        }
        return instance;
    }
    
    private native void initNative();
    
    public native long hookMethod(String library, String symbol, long newMethod);
    
    public native long hookNativeMethod(long targetAddr, long newAddr);
    
    public native long getModuleBase(String moduleName);
    
    public native long getSymbol(String moduleName, String symbolName);
    
    public native boolean unhook(String library, String symbol, long oldAddr);
    
    public long hookAMS(String libName, String symbol, long newMethod) {
        return hookMethod(libName, symbol, newMethod);
    }
    
    public long hookPMS(String libName, String symbol, long newMethod) {
        return hookMethod(libName, symbol, newMethod);
    }
    
    public long hookIActivityTaskManager() {
        return hookMethod("libandroid_runtime.so", "android_os_BinderProxy transact", 
            getTransactHook());
    }
    
    public long getTransactHook() {
        return 0;
    }
    
    public static boolean isSupported() {
        return SUPPORTED && Build.VERSION.SDK_INT >= 28;
    }

    public long onNativeHook(String library, String symbol, long oldAddr, long newAddr) {
        Log.i("NativeHook", "Hooked: " + symbol + " in " + library);
        return oldAddr;
    }
}