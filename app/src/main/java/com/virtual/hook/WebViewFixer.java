package com.virtual.hook;

import android.content.Context;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebSettings;

import java.io.File;
import java.lang.reflect.Method;

import com.virtual.util.VirtualLog;

public class WebViewFixer {
    private static final String TAG = "WebViewFixer";
    private static boolean initialized = false;

    public static void init(Context context) {
        if (initialized) return;
        initialized = true;

        try {
            VirtualLog.d(TAG, "Initializing WebView fixes for Android " + Build.VERSION.SDK_INT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fixAndroid13WebView(context);
            }

            VirtualLog.d(TAG, "WebView fixes initialized successfully");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to initialize WebView fixes", e);
        }
    }

    private static void fixAndroid13WebView(Context context) {
        try {
            VirtualLog.d(TAG, "Applying Android 13+ WebView fixes");

            System.setProperty("webview.disable_updates", "true");
            System.setProperty("webview.force_system_webview", "true");

            fixWebViewDataDirectory(context);

            disableSafeBrowsing();

            enableNetworkAccess();

        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to apply Android 13+ WebView fixes", e);
        }
    }

    private static void fixWebViewDataDirectory(Context context) {
        try {
            String packageName = context.getPackageName();
            String userId = String.valueOf(android.os.Process.myUid());
            String uniqueDir = context.getApplicationInfo().dataDir + "/webview_" + userId;

            File dir = new File(uniqueDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            System.setProperty("webview.data.dir", uniqueDir);
            System.setProperty("webview.cache.dir", uniqueDir + "/cache");
            System.setProperty("webview.cookies.dir", uniqueDir + "/cookies");

            VirtualLog.d(TAG, "WebView data directory set to: " + uniqueDir);

            try {
                Method setDataDirectorySuffix = WebView.class.getMethod("setDataDirectorySuffix", String.class);
                setDataDirectorySuffix.invoke(null, "webview_" + userId);
                VirtualLog.d(TAG, "WebView data directory suffix set");
            } catch (Exception e) {
                VirtualLog.w(TAG, "Failed to set WebView data directory suffix", e);
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to fix WebView data directory", e);
        }
    }

    private static void disableSafeBrowsing() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                System.setProperty("webview.safebrowsing.enabled", "false");
            }
            VirtualLog.d(TAG, "Safe Browsing disabled for WebView");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to disable Safe Browsing", e);
        }
    }

    private static void enableNetworkAccess() {
        try {
            System.setProperty("webview.network.enabled", "true");
            System.setProperty("webview.network.available", "true");
            VirtualLog.d(TAG, "WebView network access enabled");
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to enable WebView network access", e);
        }
    }

    public static void configureWebViewSettings(WebView webView) {
        if (webView == null) return;

        try {
            WebSettings settings = webView.getSettings();
            if (settings != null) {
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setDatabaseEnabled(true);
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);

                settings.setBlockNetworkLoads(false);
                settings.setBlockNetworkImage(false);

                settings.setAllowFileAccess(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    settings.setAllowFileAccessFromFileURLs(true);
                    settings.setAllowUniversalAccessFromFileURLs(true);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.setSafeBrowsingEnabled(false);
                }

                settings.setAllowContentAccess(true);

                try {
                    webView.setNetworkAvailable(true);
                } catch (Exception e) {
                }

                VirtualLog.d(TAG, "WebView settings configured for network access");
            }
        } catch (Exception e) {
            VirtualLog.e(TAG, "Failed to configure WebView settings", e);
        }
    }
}
