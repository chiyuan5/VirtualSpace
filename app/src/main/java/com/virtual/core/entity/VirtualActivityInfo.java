package com.virtual.core.entity;

public class VirtualActivityInfo {
    public String packageName;
    public String activityName;
    public String className;
    public int launchMode;
    public int flags;
    public String theme;
    
    public VirtualActivityInfo() {}
    
    public VirtualActivityInfo(String packageName, String activityName) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.className = activityName;
    }
}
