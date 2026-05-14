package com.virtual.core.entity;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VirtualApp implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String packageName;
    public String appName;
    public int userId;
    public List<String> clonedPackages = new ArrayList<>();
    public long createdTime;
    public boolean isActive;
    
    public String fakeDeviceId;
    public String fakeAndroidId;
    public String fakePackageName;
    
    public VirtualApp() {
        this.createdTime = System.currentTimeMillis();
        this.isActive = true;
    }
    
    public String getDataDir() {
        return "/data/data/" + packageName + "_" + userId;
    }
}
