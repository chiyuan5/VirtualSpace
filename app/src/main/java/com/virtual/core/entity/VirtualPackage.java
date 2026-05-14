package com.virtual.core.entity;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class VirtualPackage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String sourcePackage;
    public String packageName;
    public String appName;
    public int versionCode;
    public String versionName;
    public int userId;
    public String apkPath;
    public transient Drawable icon;
    public long installedTime;
    public long firstInstallTime;
    public long lastUpdateTime;
    
    public VirtualPackage() {
        this.installedTime = System.currentTimeMillis();
        this.firstInstallTime = System.currentTimeMillis();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public String getPackageName() {
        return packageName != null ? packageName : sourcePackage;
    }
}