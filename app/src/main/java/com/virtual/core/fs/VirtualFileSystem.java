package com.virtual.core.fs;

import android.content.Context;
import android.os.Build;

import com.virtual.core.VirtualCore;
import com.virtual.core.entity.VirtualApp;
import com.virtual.util.VirtualLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class VirtualFileSystem {

    private static final String TAG = "VirtualFileSystem";
    private static VirtualFileSystem instance;

    private final Context context;
    private final VirtualCore core;
    private final Map<String, FileRedirection> redirections = new HashMap<>();

    public static VirtualFileSystem getInstance(Context context) {
        if (instance == null) {
            synchronized (VirtualFileSystem.class) {
                if (instance == null) {
                    instance = new VirtualFileSystem(context);
                }
            }
        }
        return instance;
    }

    private VirtualFileSystem(Context context) {
        this.context = context.getApplicationContext();
        this.core = VirtualCore.get();
        VirtualLog.d(TAG, "VirtualFileSystem initialized");
    }

    public void initializeVirtualEnvironment(VirtualApp app) {
        try {
            String baseDir = getVirtualBaseDir(app);
            File base = new File(baseDir);
            if (!base.exists()) {
                base.mkdirs();
            }

            createVirtualSubDirs(app);

            VirtualLog.d(TAG, "Initialized virtual environment for: " + app.packageName);
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error initializing virtual environment for: " + app.packageName, e);
        }
    }

    private void createVirtualSubDirs(VirtualApp app) {
        String baseDir = getVirtualBaseDir(app);
        String[] subDirs = {
            "", "/shared_prefs", "/databases", "/files", "/cache", "/lib", "/code_cache"
        };

        for (String subDir : subDirs) {
            File dir = new File(baseDir + subDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    public String getVirtualBaseDir(VirtualApp app) {
        return context.getFilesDir().getAbsolutePath() + "/virtual/" + app.userId + "/" + app.packageName;
    }

    public String getVirtualDataDir(VirtualApp app) {
        return getVirtualBaseDir(app);
    }

    public String getVirtualFilesDir(VirtualApp app) {
        return getVirtualBaseDir(app) + "/files";
    }

    public String getVirtualCacheDir(VirtualApp app) {
        return getVirtualBaseDir(app) + "/cache";
    }

    public String getVirtualSharedPrefsDir(VirtualApp app) {
        return getVirtualBaseDir(app) + "/shared_prefs";
    }

    public String getVirtualDatabaseDir(VirtualApp app) {
        return getVirtualBaseDir(app) + "/databases";
    }

    public String getVirtualLibDir(VirtualApp app) {
        return getVirtualBaseDir(app) + "/lib";
    }

    public String getVirtualCodeCacheDir(VirtualApp app) {
        return getVirtualBaseDir(app) + "/code_cache";
    }

    public void deleteVirtualEnvironment(VirtualApp app) {
        try {
            String baseDir = getVirtualBaseDir(app);
            File base = new File(baseDir);
            if (base.exists()) {
                deleteDir(base);
                VirtualLog.d(TAG, "Deleted virtual environment for: " + app.packageName);
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error deleting virtual environment for: " + app.packageName, e);
        }
    }

    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }
        dir.delete();
    }

    public File getVirtualFile(String originalPath, VirtualApp app) {
        if (originalPath == null) return null;

        String basePath = "/data/data/" + app.packageName;
        if (originalPath.startsWith(basePath)) {
            String relativePath = originalPath.substring(basePath.length());
            return new File(getVirtualBaseDir(app) + relativePath);
        }

        basePath = "/data/user/0/" + app.packageName;
        if (originalPath.startsWith(basePath)) {
            String relativePath = originalPath.substring(basePath.length());
            return new File(getVirtualBaseDir(app) + relativePath);
        }

        return new File(originalPath);
    }

    public String redirectPath(String originalPath, VirtualApp app) {
        if (originalPath == null) return null;

        String basePath = "/data/data/" + app.packageName;
        if (originalPath.startsWith(basePath)) {
            String relativePath = originalPath.substring(basePath.length());
            return getVirtualBaseDir(app) + relativePath;
        }

        basePath = "/data/user/0/" + app.packageName;
        if (originalPath.startsWith(basePath)) {
            String relativePath = originalPath.substring(basePath.length());
            return getVirtualBaseDir(app) + relativePath;
        }

        return originalPath;
    }

    public boolean copyOriginalDataToVirtual(String packageName, VirtualApp app) {
        try {
            String originalDataDir = "/data/data/" + packageName;
            String virtualDataDir = getVirtualBaseDir(app);

            File originalDir = new File(originalDataDir);
            File virtualDir = new File(virtualDataDir);

            if (!originalDir.exists()) {
                VirtualLog.d(TAG, "Original data dir does not exist: " + originalDataDir);
                return false;
            }

            if (!virtualDir.exists()) {
                virtualDir.mkdirs();
            }

            copyDirectory(originalDir, virtualDir);
            VirtualLog.d(TAG, "Copied original data to virtual environment for: " + packageName);
            return true;
        } catch (Exception e) {
            VirtualLog.e(TAG, "Error copying data for: " + packageName, e);
            return false;
        }
    }

    private void copyDirectory(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists()) {
                dst.mkdirs();
            }

            String[] files = src.list();
            if (files != null) {
                for (String file : files) {
                    copyDirectory(new File(src, file), new File(dst, file));
                }
            }
        } else {
            if (!dst.getParentFile().exists()) {
                dst.getParentFile().mkdirs();
            }

            try (InputStream in = new FileInputStream(src);
                 OutputStream out = new FileOutputStream(dst)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }

    public long getVirtualDataSize(VirtualApp app) {
        String baseDir = getVirtualBaseDir(app);
        File dir = new File(baseDir);
        if (!dir.exists()) return 0;
        return getFolderSize(dir);
    }

    private long getFolderSize(File folder) {
        long size = 0;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += getFolderSize(file);
                }
            }
        } else {
            size = folder.length();
        }
        return size;
    }

    public void registerRedirection(String original, String virtual) {
        redirections.put(original, new FileRedirection(original, virtual));
        VirtualLog.d(TAG, "Registered redirection: " + original + " -> " + virtual);
    }

    public void unregisterRedirection(String original) {
        redirections.remove(original);
    }

    public FileRedirection getRedirection(String original) {
        return redirections.get(original);
    }

    public static class FileRedirection {
        public final String originalPath;
        public final String virtualPath;

        public FileRedirection(String original, String virtual) {
            this.originalPath = original;
            this.virtualPath = virtual;
        }
    }
}
