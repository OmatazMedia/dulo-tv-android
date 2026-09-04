package com.dulotv.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String TARGET_URL = "https://dulo.gd/";
    private boolean virtualCursorEnabled = false;
    private int cursorX = 960;
    private int cursorY = 540;
    private static final int CURSOR_STEP = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        setupWebView();
        webView.loadUrl(TARGET_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        webView.addJavascriptInterface(new VirtualCursorInterface(), "TVCursor");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (virtualCursorEnabled) {
                    injectVirtualCursor();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                view.loadUrl("file:///android_asset/error.html");
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.proceed();
            }
        });

        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
    }

    private void injectVirtualCursor() {
        String js = "javascript:(function() {" +
            "if (window.__tvCursorInjected) return; window.__tvCursorInjected = true;" +
            "var cursor = document.createElement('div');" +
            "cursor.id = 'tv-virtual-cursor';" +
            "cursor.style.cssText = 'position:fixed;width:24px;height:24px;border-radius:50%;" +
            "background:rgba(233,69,96,0.9);border:3px solid #fff;pointer-events:none;z-index:2147483647;" +
            "transform:translate(-50%,-50%);transition:left 0.1s,top 0.1s;display:none;" +
            "box-shadow:0 0 10px rgba(233,69,96,0.8);';" +
            "document.body.appendChild(cursor);" +
            "window.TVCursor = {" +
            "  show: function(x,y) { cursor.style.left=x+'px'; cursor.style.top=y+'px'; cursor.style.display='block'; }," +
            "  move: function(x,y) { cursor.style.left=x+'px'; cursor.style.top=y+'px'; }," +
            "  hide: function() { cursor.style.display='none'; }," +
            "  click: function() { var el=document.elementFromPoint(cursor.offsetLeft,cursor.offsetTop); if(el) el.click(); }" +
            "};" +
            "})()";
        webView.evaluateJavascript(js, null);
        showCursor();
    }

    private void showCursor() {
        webView.evaluateJavascript("javascript:window.TVCursor && window.TVCursor.show(" + cursorX + "," + cursorY + ");", null);
    }

    private void moveCursor(int dx, int dy) {
        cursorX = Math.max(0, Math.min(1920, cursorX + dx));
        cursorY = Math.max(0, Math.min(1080, cursorY + dy));
        webView.evaluateJavascript("javascript:window.TVCursor && window.TVCursor.move(" + cursorX + "," + cursorY + ");", null);
    }

    private void clickCursor() {
        webView.evaluateJavascript("javascript:window.TVCursor && window.TVCursor.click();", null);
    }

    public class VirtualCursorInterface {
        @JavascriptInterface
        public void toggleCursor() {
            runOnUiThread(() -> {
                virtualCursorEnabled = !virtualCursorEnabled;
                if (virtualCursorEnabled) {
                    injectVirtualCursor();
                } else {
                    webView.evaluateJavascript("javascript:window.TVCursor && window.TVCursor.hide();", null);
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (virtualCursorEnabled) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveCursor(0, -CURSOR_STEP);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveCursor(0, CURSOR_STEP);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    moveCursor(-CURSOR_STEP, 0);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    moveCursor(CURSOR_STEP, 0);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    clickCursor();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                        return true;
                    }
                    virtualCursorEnabled = false;
                    webView.evaluateJavascript("javascript:window.TVCursor && window.TVCursor.hide();", null);
                    return true;
            }
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                virtualCursorEnabled = true;
                injectVirtualCursor();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}