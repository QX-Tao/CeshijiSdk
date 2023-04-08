package com.tencent.automationlib;

import static android.content.ContentValues.TAG;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;


public class UIHierarchy {

    private static String windowManagerString;
    private static Class<?> windowManager = null;
    private static String windowManagerClassName;

//    private UIHierarchy() throws ClassNotFoundException {
//        if (AndroidVersion.isJellyBeanMr1()) {
//            windowManagerString = "sDefaultWindowManager";
//        } else if (AndroidVersion.isHoneycomb()) {
//            windowManagerString = "sWindowManager";
//        } else {
//            windowManagerString = "mWindowManager";
//        }
//        if (AndroidVersion.isJellyBeanMr1()) {
//            windowManagerClassName = "android.view.WindowManagerGlobal";
//        } else {
//            windowManagerClassName = "android.view.WindowManagerImpl";
//        }
//        windowManager = Class.forName(windowManagerClassName);
//    }

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


    // 遍历控件树生成JSON格式字符串
    public static String generateHierarchyJson(View view) {
        JSONObject rootJson = new JSONObject();
        try {
            rootJson.put("id", System.identityHashCode(view));
            rootJson.put("class", view.getClass().getName());
            rootJson.put("text", view instanceof TextView ? ((TextView) view).getText().toString() : "");
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
                    childJson.put("children", generateChildrenJson(child));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                childrenJson.put(childJson);
            }
        }
        return childrenJson;
    }


}

