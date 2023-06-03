package com.tencent.automationlib;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.Keep;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

public class JsBridge {
    private static final String TAG = "JsBridge";
    private static volatile JsBridge jsBridge;

    private String injectJs;
    private String vmValue;

    public static JsBridge getInstance() {
        if (jsBridge == null) {
            synchronized (JsBridge.class) {
                if (jsBridge == null) {
                    jsBridge = new JsBridge();
                }
            }
        }
        return jsBridge;
    }

    public void injectJs(Object web) {
        if (TextUtils.isEmpty(injectJs)) {
            getJs();
        }
        if (web instanceof WebView) {
            final WebView webView = (WebView) web;
            webView.getSettings().setDomStorageEnabled(true);
            // 将Js注入进去
            webView.evaluateJavascript(injectJs, value -> {
                // value 就是该页面的 DOM 树，可以将其转换为 JSON 格式
                vmValue = StringEscapeUtils.unescapeJava(value);
                Log.e(TAG,"get wv receive: " + vmValue);
            });
        } else if (web instanceof com.tencent.smtt.sdk.WebView ) {
            final com.tencent.smtt.sdk.WebView webView = (com.tencent.smtt.sdk.WebView) web;
            webView.getSettings().setDomStorageEnabled(true);
            webView.evaluateJavascript(injectJs, value -> {
                vmValue = StringEscapeUtils.unescapeJava(value);
                Log.e(TAG,"get x5wv receive: " + vmValue);
            });
        } else {
          // todo: other WebView
        }
    }

    public String getWebViewDOM(){
        return vmValue;
    }

    // 示例，这里注入的是hook的js
    private void getJs() {
        injectJs = "javascript:(function() {"
                + "return document.getElementsByTagName('html')[0].outerHTML;"
                + "})();";
    }

    // 这里接收下Js传入的数据
    @JavascriptInterface
    @Keep
    public void receiverDomInfo(String dom) {
        Log.e(TAG, "receiverDomInfo: " + dom);
    }
}
