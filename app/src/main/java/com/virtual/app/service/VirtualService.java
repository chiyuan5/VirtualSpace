package com.virtual.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.virtual.core.VirtualCore;
import com.virtual.core.dispatch.VirtualDispatch;
import com.virtual.core.service.ServiceBroker;
import com.virtual.util.Log;

public class VirtualService extends Service {

    private static final String TAG = "VirtualService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "VirtualService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "VirtualService started");

        try {
            if (!VirtualCore.get().isInitialized()) {
                VirtualCore.get().init(this);
            }

            if (VirtualCore.get().isSupportVersion()) {
                VirtualDispatch.getInstance(VirtualCore.get()).init();
                ServiceBroker.getInstance(this).init();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize virtual service", e);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "VirtualService destroyed");
    }
}
