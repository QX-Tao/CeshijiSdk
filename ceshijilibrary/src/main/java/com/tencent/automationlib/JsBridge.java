package com.tencent.automationlib;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.Keep;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class JsBridge {
    private static final String TAG = "JsBridge";
    private static volatile JsBridge jsBridge;

    private String injectJs;
    private JSONObject json;

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
                try{
                    Log.e(TAG,"get receive: " + value);
                    json = new JSONObject(value);
                } catch (JSONException e){
                    e.printStackTrace();
                }
            });
        } else {
            // todo: may be X5WebView, UCWebView...
        }
    }

    public JSONObject getWebViewJson(){
        return json;
    }

    // 示例，这里注入的是hook的js
    private void getJs() {
        injectJs = "javascript:(function() {"
                + "var str=JSON.stringify(document.getElementsByTagName('html')[0].outerHTML);"
                + "var decodedStr=unescape(str);"
                + "return decodedStr;"
                + "})();";
    }


    // 这里接收下Js传入的数据
    @JavascriptInterface
    @Keep
    public void receiverDomInfo(String dom) {
        Log.e(TAG, "receiverDomInfo: " + dom);
    }
}
