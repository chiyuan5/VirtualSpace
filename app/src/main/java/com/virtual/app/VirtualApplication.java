package com.virtual.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.virtual.core.VirtualCore;
import com.virtual.core.dispatch.VirtualDispatch;
import com.virtual.core.service.ServiceBroker;
import com.virtual.hook.HookManager;
import com.virtual.util.VirtualLog;

public class VirtualApplication extends Application {

    private static final String TAG = "VirtualApp";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        VirtualLog.i(TAG, "VirtualSpace starting...");
        VirtualLog.i(TAG, "Android Version: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");

        try {
            VirtualCore.get().init(this);

            if (VirtualCore.get().isSupportVersion()) {
                HookManager.get().init();
                VirtualDispatch.getInstance(VirtualCore.get()).init();
                ServiceBroker.getInstance(this).init();
                VirtualLog.i(TAG, "Virtualization engine initialized");
            } else {
                VirtualLog.w(TAG, "Android version not supported, limited functionality");
            }

            VirtualLog.i(TAG, "VirtualSpace ready");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to initialize VirtualSpace", e);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        VirtualDispatch.getInstance(VirtualCore.get()).cleanup();
        ServiceBroker.getInstance(this).cleanup();
    }
}