package com.virtual.core.impl;

import android.content.Context;
import android.content.SharedPreferences;

import com.virtual.core.entity.VirtualApp;
import com.virtual.util.VirtualLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class VirtualStorage {

    private static final String TAG = "VirtualStorage";
    private static final String PREFS_NAME = "virtual_prefs";
    private static final String VIRTUAL_APPS_FILE = "virtual_apps.dat";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final File dataDir;

    public VirtualStorage(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dataDir = new File(context.getFilesDir(), "virtual");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public void init() {
        VirtualLog.i(TAG, "VirtualStorage initialized");
    }

    public List<VirtualApp> loadVirtualApps() {
        List<VirtualApp> apps = new ArrayList<>();
        try {
            File file = new File(dataDir, VIRTUAL_APPS_FILE);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                apps = (List<VirtualApp>) ois.readObject();
                ois.close();
                fis.close();
                VirtualLog.i(TAG, "Loaded " + apps.size() + " virtual apps");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to load virtual apps", e);
        }
        return apps;
    }

    public void saveVirtualApps(List<VirtualApp> apps) {
        try {
            File file = new File(dataDir, VIRTUAL_APPS_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(apps);
            oos.close();
            fos.close();
            VirtualLog.i(TAG, "Saved " + apps.size() + " virtual apps");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to save virtual apps", e);
        }
    }

    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
}