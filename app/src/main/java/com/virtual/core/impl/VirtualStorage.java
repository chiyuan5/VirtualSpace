package com.virtual.core.impl;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.core.entity.VirtualPackage;
import com.virtual.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class VirtualStorage {

    private static final String TAG = "VirtualStorage";
    private static final String STORAGE_DIR = "virtual_space";
    private static final String APPS_FILE = "virtual_apps.dat";
    private static final String PACKAGES_FILE = "virtual_packages.dat";

    private final Context context;
    private final File storageDir;
    private final File appsFile;
    private final File packagesFile;

    private final Map<String, VirtualApp> appsCache = new HashMap<>();
    private final Map<String, VirtualPackage> packagesCache = new HashMap<>();

    public VirtualStorage(Context context) {
        this.context = context;
        this.storageDir = new File(context.getFilesDir(), STORAGE_DIR);
        this.appsFile = new File(storageDir, APPS_FILE);
        this.packagesFile = new File(storageDir, PACKAGES_FILE);
    }

    public void init() {
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        loadAll();
        Log.d(TAG, "VirtualStorage initialized");
    }

    public void saveAll() {
        saveApps();
        savePackages();
    }

    @SuppressWarnings("unchecked")
    private void loadAll() {
        loadApps();
        loadPackages();
    }

    @SuppressWarnings("unchecked")
    private void loadApps() {
        if (!appsFile.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(appsFile))) {
            Map<String, VirtualApp> loaded = (Map<String, VirtualApp>) ois.readObject();
            if (loaded != null) {
                appsCache.putAll(loaded);
                for (VirtualApp app : loaded.values()) {
                    VirtualCore.get().getAllVirtualApps().add(app);
                }
            }
            Log.d(TAG, "Loaded " + appsCache.size() + " virtual apps");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load apps", e);
        }
    }

    private void saveApps() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(appsFile))) {
            oos.writeObject(new HashMap<>(appsCache));
            Log.d(TAG, "Saved " + appsCache.size() + " virtual apps");
        } catch (IOException e) {
            Log.e(TAG, "Failed to save apps", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPackages() {
        if (!packagesFile.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(packagesFile))) {
            Map<String, VirtualPackage> loaded = (Map<String, VirtualPackage>) ois.readObject();
            if (loaded != null) {
                packagesCache.putAll(loaded);
            }
            Log.d(TAG, "Loaded " + packagesCache.size() + " virtual packages");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load packages", e);
        }
    }

    private void savePackages() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(packagesFile))) {
            oos.writeObject(new HashMap<>(packagesCache));
            Log.d(TAG, "Saved " + packagesCache.size() + " virtual packages");
        } catch (IOException e) {
            Log.e(TAG, "Failed to save packages", e);
        }
    }

    public void saveApp(VirtualApp app) {
        if (app != null && app.packageName != null) {
            appsCache.put(app.packageName, app);
            saveApps();
        }
    }

    public void deleteApp(String packageName) {
        appsCache.remove(packageName);
        saveApps();
        
        // Remove associated packages
        for (String key : packagesCache.keySet()) {
            VirtualPackage pkg = packagesCache.get(key);
            if (pkg != null && pkg.sourcePackage.equals(packageName)) {
                packagesCache.remove(key);
            }
        }
        savePackages();
    }

    public void savePackage(VirtualPackage pkg) {
        if (pkg != null && pkg.packageName != null) {
            String key = pkg.sourcePackage + "_" + pkg.userId;
            packagesCache.put(key, pkg);
            savePackages();
        }
    }

    public VirtualApp getApp(String packageName) {
        return appsCache.get(packageName);
    }

    public VirtualPackage getPackage(String sourcePackage, int userId) {
        String key = sourcePackage + "_" + userId;
        return packagesCache.get(key);
    }

    public Map<String, VirtualApp> getAllApps() {
        return new HashMap<>(appsCache);
    }

    public Map<String, VirtualPackage> getAllPackages() {
        return new HashMap<>(packagesCache);
    }
}
