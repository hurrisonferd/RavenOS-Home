package com.ravenos.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private static final String LIVE_URL = "https://hurrisonferd.github.io/jarvis/ravenos-home.html";
    private static final String LIVE_HOST = "hurrisonferd.github.io";
    private static final String LIVE_PATH_PREFIX = "/jarvis/";
    private static final String OFFLINE_URL = "file:///android_asset/index.html";

    private WebView webView;
    private boolean usingOfflineFallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(7, 5, 13));
        window.setNavigationBarColor(Color.rgb(7, 5, 13));

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
}
