package com.tencent.automationlib;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
//            Logger.INSTANCE.exception(TAG, e);
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
    public static String generateHierarchyJson(View view) {
        JSONObject rootJson = new JSONObject();
        try {
            rootJson.put("id", System.identityHashCode(view));
            rootJson.put("class", view.getClass().getName());
            rootJson.put("text", view instanceof TextView ? ((TextView) view).getText().toString() : "");
            rootJson.put("ContentDescription", view.getContentDescription() != null ? view.getContentDescription().toString() : "");
            rootJson.put("visible", view.getVisibility());
            rootJson.put("coord",view.getX() + " " + view.getY());
            rootJson.put("dimensions", view.getWidth() + " " + view.getHeight());
            rootJson.put("isClickable", view.isClickable());
            rootJson.put("isFocusable", view.isFocusable());
            rootJson.put("isLongClickable", view.isLongClickable());

            rootJson.put("isSeclected", view.isSelected());
            rootJson.put("canScroll", view.canScrollHorizontally(-1)+" "+view.canScrollHorizontally(1)+" "+view.canScrollVertically(-1)+" "+view.canScrollVertically(1));
            rootJson.put("children", generateChildrenJson(view));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rootJson.toString();
    }

    // 嵌套遍历所有View，最终生成JSON字符串
    private static JSONArray generateChildrenJson(View view) {
        JSONArray childrenJson = new JSONArray();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                JSONObject childJson = new JSONObject();
                try {
                    childJson.put("id", System.identityHashCode(child));
                    childJson.put("class", child.getClass().getName());
                    childJson.put("text", child instanceof TextView ? ((TextView) child).getText().toString() : "");
                    childJson.put("ContentDescription",child.getContentDescription() != null ? child.getContentDescription().toString() : "");
                    childJson.put("visible", child.getVisibility());
                    childJson.put("coord", child.getX() + " " + child.getY());
                    childJson.put("dimensions", child.getWidth() + " " + child.getHeight());
                    childJson.put("isClickable", child.isClickable());
                    childJson.put("isFocusable", child.isFocusable());
                    childJson.put("isLongClickable", child.isLongClickable());
                    childJson.put("isSeclected", child.isSelected());
                    childJson.put("canScroll", child.canScrollHorizontally(-1)+" "+child.canScrollHorizontally(1)+" "+child.canScrollVertically(-1)+" "+child.canScrollVertically(1));
                    childJson.put("children", generateChildrenJson(child));
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

}

