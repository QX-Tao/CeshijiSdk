package com.tencent.automationlib;

import android.os.Build;

public class AndroidVersion {

    /**
     * 版本是否在3.2.x之后（API 13)
     *
     * @return 如果当前版本大于等于13返回true，否则返回false
     */
    public static boolean isHoneycomb(){
        return Build.VERSION.SDK_INT >= 13;
    }

    /**
     * 版本是否在4.2之后（API 17)
     *
     * @return 如果当前版本大于等于17返回true，否则返回false
     */
    public static boolean isJellyBeanMr1(){
        return Build.VERSION.SDK_INT >= 17;
    }

    /**
     * 版本是否在4.4之后（API 19）
     *
     * @return 如果当前版本大于等于19返回true，否则返回false
     */
    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= 19;
    }


}
