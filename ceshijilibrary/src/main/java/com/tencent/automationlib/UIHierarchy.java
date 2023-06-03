package com.tencent.automationlib;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class UIHierarchy {

    private static String windowManagerString;
    private static Class<?> windowManager = null;
    private static String windowManagerClassName;

    private static final List<String> DECOR_VIEWS = Arrays.asList(
            "com.android.internal.policy.DecorView",
            "com.android.internal.policy.PhoneWindow$DecorView",
            "com.android.internal.policy.impl.MultiPhoneWindow$MultiPhoneDecorView",
            "com.android.internal.policy.impl.PhoneWindow$DecorView");

    //app内部获取RootView方式
    public static View[] getWindowDecorViews(){

        if (AndroidVersion.isJellyBeanMr1()) {
            windowManagerString = "sDefaultWindowManager";
        } else if (AndroidVersion.isHoneycomb()) {
            windowManagerString = "sWindowManager";
        } else {
            windowManagerString = "mWindowManager";
        }
        if (AndroidVersion.isJellyBeanMr1()) {
            windowManagerClassName = "android.view.WindowManagerGlobal";
        } else {
            windowManagerClassName = "android.view.WindowManagerImpl";
        }try {
            windowManager = Class.forName(windowManagerClassName);
        }catch (Exception e){
            e.printStackTrace();
        }

        Field viewsField;
        Field instanceField;
        try {
            viewsField = windowManager.getDeclaredField("mViews");
            instanceField = windowManager.getDeclaredField(windowManagerString);
            viewsField.setAccessible(true);
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            View[] result;
            if (AndroidVersion.isKitKat()) {
                Object views = viewsField.get(instance);
                if (views != null) {
                    result = ((ArrayList<View>) views).toArray(new View[0]);
                } else {
                    result = null;
                }
            } else {
                result = (View[]) viewsField.get(instance);
            }
            return result;
        } catch (Throwable e) {
            Log.e(TAG, "Error occurred", e);
        }
        return null;
    }

    private static boolean isDecorView(View view) {
        if (view == null) {
            return false;
        }
        final String clazzName = view.getClass().getName();
        if (!TextUtils.isEmpty(clazzName)) {
            return DECOR_VIEWS.contains(clazzName)
                    || clazzName.endsWith("ScrollView")
                    || clazzName.endsWith("ViewPager");
        }
        return false;
    }

    public static View getRecentDecorView(View[] views) {
        if (views == null) {
            return null;
        }

        final View[] decorViews = new View[views.length];
        int i = 0;
        for (View view : views) {
            if (isDecorView(view)) {
                decorViews[i] = view;
                i++;
            }
        }
        return getRecentContainer(decorViews);
    }

    private static View getRecentContainer(View[] views) {
        View container = null;
        long drawingTime = 0;

        if (views == null) {
            return null;
        }

        for (View view : views) {
            if (view != null && view.isShown() && view.hasWindowFocus() && view.getDrawingTime() > drawingTime) {
                container = view;
                drawingTime = view.getDrawingTime();
            }
        }
        return container;
    }


    // 遍历控件树生成JSON格式字符串
    public static String generateHierarchyJson(View view) throws JSONException {
        JSONObject rootJson = new JSONObject();
        try {
            rootJson.put("id", System.identityHashCode(view));
            rootJson.put("class", view.getClass().getName());
            rootJson.put("text", view instanceof TextView ? ((TextView) view).getText().toString() : "");
            rootJson.put("ContentDescription", view.getContentDescription() != null ? view.getContentDescription().toString() : "");
            rootJson.put("visible", view.getVisibility());
            rootJson.put("isVisibleToUser", isVisibleToUser(view));
            rootJson.put("coord",view.getX() + " " + view.getY());
            rootJson.put("dimensions", view.getWidth() + " " + view.getHeight());
            rootJson.put("isClickable", view.isClickable());
            rootJson.put("isFocusable", view.isFocusable());
            rootJson.put("isLongClickable", view.isLongClickable());
            rootJson.put("isSelected", view.isSelected());
            rootJson.put("canScroll", view.canScrollHorizontally(-1)+" "+view.canScrollHorizontally(1)+" "+view.canScrollVertically(-1)+" "+view.canScrollVertically(1));
            rootJson.put("children", generateChildrenJson(view));
            if(view instanceof WebView){
                rootJson.put("wvDom",JsBridge.getInstance().getWebViewDOM());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rootJson.toString();
    }

    // 嵌套遍历所有View，最终生成JSON字符串
    private static JSONArray generateChildrenJson(View view) throws JSONException {
        JSONArray childrenJson = new JSONArray();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            JSONObject childJson = new JSONObject();
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                try {
                    childJson.put("id", System.identityHashCode(child));
                    childJson.put("class", child.getClass().getName());
                    childJson.put("text", child instanceof TextView ? ((TextView) child).getText().toString() : "");
                    childJson.put("ContentDescription",child.getContentDescription() != null ? child.getContentDescription().toString() : "");
                    childJson.put("visible", child.getVisibility());
                    childJson.put("isVisibleToUser", isVisibleToUser(view));
                    childJson.put("coord", child.getX() + " " + child.getY());
                    childJson.put("dimensions", child.getWidth() + " " + child.getHeight());
                    childJson.put("isClickable", child.isClickable());
                    childJson.put("isFocusable", child.isFocusable());
                    childJson.put("isLongClickable", child.isLongClickable());
                    childJson.put("isSelected", child.isSelected());
                    childJson.put("canScroll", child.canScrollHorizontally(-1)+" "+child.canScrollHorizontally(1)+" "+child.canScrollVertically(-1)+" "+child.canScrollVertically(1));
                    childJson.put("children", generateChildrenJson(child));
                    if(child instanceof WebView){
                        childJson.put("wvDom",JsBridge.getInstance().getWebViewDOM());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                childrenJson.put(childJson);
            }
        }
        return childrenJson;
    }

    public static String getPackageName() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentApplicationMethod = activityThreadClass.getMethod("currentApplication");
            Object currentApplication = currentApplicationMethod.invoke(null);
            Method getPackageNameMethod = currentApplication.getClass().getMethod("getPackageName");
            return (String) getPackageNameMethod.invoke(currentApplication);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 对指定的界面元素是否用户可见.
     *
     * @param view 界面元素
     * @return 可见返回true，否则返回false
     */
    public static boolean isVisibleToUser(View view) {
        try {
            final int[] xyView = new int[2];
            final int[] xyParent = new int[2];

            if (view == null) {
                return false;
            }

            final float viewHeight = view.getHeight();
            final View parent = getScrollOrListParent(view);
            view.getLocationOnScreen(xyView);

            if (parent == null) {
                xyParent[1] = 0;
            } else {
                parent.getLocationOnScreen(xyParent);
            }

            return !((xyView[1] + (viewHeight / 2.0f) > getScrollListWindowHeight(parent)) || (xyView[1] + (viewHeight / 2.0f) < xyParent[1]));
        } catch (Throwable t) {
            return false;
        }
    }


    private static View getScrollOrListParent(View view) {
        if (!(view instanceof android.widget.AbsListView) && !(view instanceof android.widget.ScrollView) && !(view instanceof WebView)) {
            try {
                return getScrollOrListParent((View) view.getParent());
            } catch (Throwable e) {
                return null;
            }
        } else {
            return view;
        }
    }

    private static float getScrollListWindowHeight(View parent) {
        final int[] xyParent = new int[2];
        final float windowHeight;

        if (parent == null) {
            windowHeight = getDisplayHeight(getApplicationContext());
        } else {
            parent.getLocationOnScreen(xyParent);
            windowHeight = xyParent[1] + parent.getHeight();
        }
        return windowHeight;
    }

    public static int getDisplayHeight(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }

    public static Context getApplicationContext(){
        Context context = null;
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method method = activityThreadClass.getMethod("currentApplication");
            context = (Context) method.invoke(null, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }

    // Hook WebView的控件ID
    private static void hookWebView(WebView webView) {
        try {
            Class<?> clazz = Class.forName("android.webkit.WebView");
            Method method = clazz.getDeclaredMethod("getWebViewProvider");
            method.setAccessible(true);
            Object webViewProvider = method.invoke(webView);
            if (webViewProvider != null) {
                Class<?> webViewProviderClass = Class.forName("android.webkit.WebViewProvider");
                Field field = webViewProviderClass.getDeclaredField("mContentsClientBridge");
                field.setAccessible(true);
                Object contentsClientBridge = field.get(webViewProvider);
                if (contentsClientBridge != null) {
                    Class<?> contentsClientBridgeClass = Class.forName("android.webkit.WebViewClient$WebViewClientCompatImpl$ContentsClientBridge");
                    Method method1 = contentsClientBridgeClass.getDeclaredMethod("getWebView");
                    method1.setAccessible(true);
                    WebView webView1 = (WebView) method1.invoke(contentsClientBridge);
                    if (webView1 != null) {
                        webView1.setWebContentsDebuggingEnabled(true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

