package com.virtual.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.virtual.core.VirtualCore;
import com.virtual.core.dispatch.VirtualDispatch;
import com.virtual.core.service.ServiceBroker;
import com.virtual.hook.HookManager;
import com.virtual.util.Log;

public class VirtualApplication extends Application {

    private static final String TAG = "VirtualApp";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "VirtualSpace starting...");
        Log.i(TAG, "Android Version: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");

        try {
            VirtualCore.get().init(this);

            if (VirtualCore.get().isSupportVersion()) {
                HookManager.getInstance().init();
                VirtualDispatch.getInstance(VirtualCore.get()).init();
                ServiceBroker.getInstance(this).init();
                Log.i(TAG, "Virtualization engine initialized");
            } else {
                Log.w(TAG, "Android version not supported, limited functionality");
            }

            Log.i(TAG, "VirtualSpace ready");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize VirtualSpace", e);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        VirtualDispatch.getInstance(VirtualCore.get()).cleanup();
        ServiceBroker.getInstance(this).cleanup();
    }
}
