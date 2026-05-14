package com.virtual.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.virtual.core.VirtualCore;
import com.virtual.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Boot completed, starting VirtualService");

            try {
                Intent serviceIntent = new Intent(context, com.virtual.app.service.VirtualService.class);
                context.startService(serviceIntent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to start VirtualService", e);
            }
        }
    }
}
