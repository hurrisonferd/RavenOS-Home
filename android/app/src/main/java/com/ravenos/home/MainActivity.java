package com.ravenos.home;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private static final String VERSION_NAME = "0.2.0";
    private static final String LIVE_URL = "https://hurrisonferd.github.io/jarvis/ravenos-home.html";
    private static final String LIVE_HOST = "hurrisonferd.github.io";
    private static final String LIVE_PATH_PREFIX = "/jarvis/";
    private static final String OFFLINE_URL = "file:///android_asset/index.html";
    private static final String CHANNEL_ID = "ravenos_home_resident";
    private static final int REQUEST_NOTIFICATIONS = 4202;

    private WebView webView;
    private boolean usingOfflineFallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowSetup.apply(this);
        ensureNotificationChannel();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7, 5, 13));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        webView.addJavascriptInterface(new RavenHomeBridge(), "RavenHome");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("file".equalsIgnoreCase(scheme)) return false;
                if (isRavenPagesUri(uri)) return false;
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame() && !usingOfflineFallback) {
                    usingOfflineFallback = true;
                    view.loadUrl(OFFLINE_URL);
                }
            }
        });

        setContentView(webView);
        webView.loadUrl(LIVE_URL);
    }

    private void ensureNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "RavenOS Home resident",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Owner-invoked local RavenOS Home resident notifications.");
        manager.createNotificationChannel(channel);
    }

    private boolean notificationsGranted() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean appEnabled = manager.areNotificationsEnabled();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return appEnabled && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return appEnabled;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        showResidentPing("RavenOS Home", "Resident notification lane is online.");
    }

    private void showResidentPing(String title, String body) {
        if (!notificationsGranted()) return;

        Intent launch = new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(
            this,
            4202,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.ravenos.home.R.drawable.ic_ravenos)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(4202, notification);
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    private boolean isRavenPagesUri(Uri uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
            && LIVE_HOST.equalsIgnoreCase(uri.getHost())
            && uri.getPath() != null
            && uri.getPath().startsWith(LIVE_PATH_PREFIX);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private final class RavenHomeBridge {
        @JavascriptInterface
        public String getVersion() {
            return VERSION_NAME;
        }

        @JavascriptInterface
        public boolean isNativeAndroid() {
            return true;
        }

        @JavascriptInterface
        public boolean notificationsGranted() {
            return MainActivity.this.notificationsGranted();
        }

        @JavascriptInterface
        public void requestNotifications() {
            runOnUiThread(MainActivity.this::requestNotificationPermission);
        }

        @JavascriptInterface
        public void testNotification() {
            runOnUiThread(() -> showResidentPing("RavenOS Home", "⚛ Resident ping. The house is awake."));
        }

        @JavascriptInterface
        public void openNotificationSettings() {
            runOnUiThread(MainActivity.this::openNotificationSettings);
        }
    }

    private static final class WindowSetup {
        private static void apply(Activity activity) {
            activity.getWindow().setStatusBarColor(Color.rgb(7, 5, 13));
            activity.getWindow().setNavigationBarColor(Color.rgb(7, 5, 13));
        }
    }
}
