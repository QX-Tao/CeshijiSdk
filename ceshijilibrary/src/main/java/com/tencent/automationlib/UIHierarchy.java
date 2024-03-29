package com.tencent.automationlib;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
    private static  Method method;

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
        } try {
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
                    || clazzName.endsWith("PopupDecorView")
                    || clazzName.endsWith("ScrollView")
                    || clazzName.endsWith("ViewPager");
        }
        return false;
    }

    public static View getRecentDecorView(View[] views) throws JSONException, IOException {
        if (views == null) {
            return null;
        }

//        for (int i = 0; i<views.length; i++){
//            if(views[i] != null){
//                String nr= UIHierarchy.generateHierarchyJson(views[i]);
//                File outPath = views[i].getContext().getExternalFilesDir("/Tencent/ceshiji");
//                String SDPath = outPath.getAbsolutePath();
//                File accessibleSaveFile = new File(SDPath + i);
//                RandomAccessFile accessRaf = new RandomAccessFile(accessibleSaveFile, "rwd");
//                accessRaf.seek(accessibleSaveFile.length());
//                accessRaf.write(nr.getBytes());
//                accessRaf.close();
//            }
//        }

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

    private static View getRecentContainer(View[] views) throws IOException, JSONException {
        View container = null;
        long drawingTime = 0;

        if (views == null) {
            Log.d("TAG getRecentContainer","views null");
            return null;
        }



        for (View view : views) {
            if (view != null) {
                boolean isShown = view.isShown();
                boolean hasWindowFocus = view.hasWindowFocus();
                boolean isFocused = view.isFocused();
                boolean isVisibleToUser = isVisibleToUser(view);
                boolean drawBigger = view.getDrawingTime() > drawingTime;
                Log.d("TAG VIEW NOT NULL",String.valueOf(isShown) + hasWindowFocus + isFocused + isVisibleToUser + drawBigger);
                Log.d("TAG VIEW NOT NULL VIEW",view.toString());
                if (isShown && hasWindowFocus) {
                    container = view;
//                    drawingTime = view.getDrawingTime();
                }
            } else {
                Log.d("TAG VIEW","getRecentContainer view is null");
            }
        }
        return container;
    }

    // 遍历控件树生成JSON格式字符串
    public static JSONObject generateHierarchyJson(View view) {
        if (view == null) return null;
        if (view.getVisibility() == View.INVISIBLE || view.getVisibility() == View.GONE) return null;
        JSONObject rootJson = new JSONObject();
        try {
            rootJson.put("isImportantForAccessibility",view.isImportantForAccessibility());
            String resourceId = null;
            try {
                resourceId = view.getContext().getResources().getResourceName(view.getId());
            } catch (Resources.NotFoundException ignored){
            } finally {
                if(resourceId != null) rootJson.put("resourceId", resourceId);
            }
            rootJson.put("class", view.getAccessibilityClassName());
            rootJson.put("text", view instanceof TextView ? ((TextView) view).getText().toString() : "");
            rootJson.put("contentDescription", view.getContentDescription() != null ? view.getContentDescription().toString() : "");
            rootJson.put("canScroll", view.canScrollHorizontally(-1) || view.canScrollHorizontally(1) || view.canScrollVertically(-1) || view.canScrollVertically(1));
            rootJson.put("isClickable", view.isClickable());
            rootJson.put("isLongClickable", view.isLongClickable());
            rootJson.put("isFocusable", view.isFocusable());
            rootJson.put("isFocused", view.isFocused());
            rootJson.put("isVisibleToUser", isVisibleToUser(view));
            rootJson.put("isSelected", view.isSelected());
            rootJson.put("isEnabled", view.isEnabled());
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            rootJson.put("boundsInScreen", "("+(location[0])+", "+(location[1])+" - "+(view.getWidth()+location[0])+", "+(view.getHeight()+location[1])+")");
            if(view instanceof WebView){
                rootJson.put("wvDom",JsBridge.getInstance().getWebViewDOM());
            }

            JSONArray children = new JSONArray();
            if (view instanceof ViewGroup && ((ViewGroup) view).getChildCount() > 0) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++){
                    ViewGroup viewGroup = (ViewGroup) view;
                    View child = viewGroup.getChildAt(i);
                    JSONObject childJson = generateHierarchyJson(child);
                    if (childJson != null && childJson.length() > 0){
                        children.put(childJson);
                    }
                }
            }
            if (children.length() > 0) {
                rootJson.put("children", children);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rootJson;
    }


    public static String getPackageName() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentApplicationMethod = activityThreadClass.getMethod("currentApplication");
            Object currentApplication = currentApplicationMethod.invoke(null);
            if (currentApplication != null) {
                Method getPackageNameMethod = currentApplication.getClass().getMethod("getPackageName");
                return (String) getPackageNameMethod.invoke(currentApplication);
            } else Log.e("TAG getPackageName", "currentApplication is null");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isVisibleToUser(View view){
        try {
            boolean isVisible = (boolean) method.invoke(view);
            return isVisible;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void initAccessibleIsVisibleToUser() {
        try {
            method = View.class.getDeclaredMethod("isVisibleToUser");
            method.setAccessible(true);
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 对指定的界面元素是否用户可见.
     *
     * @param view 界面元素
     * @return 可见返回true，否则返回false
     */
    public static boolean isVisibleToUser1(View view) {
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
}

