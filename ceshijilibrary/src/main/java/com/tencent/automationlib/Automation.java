package com.tencent.automationlib;

import android.app.Application;

public class Automation {
    public static void enable(int configCode){
        SdkServer sdkServer = new SdkServer(9000);

        Thread thread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sdkServer.startListen();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        thread.start();
    }

    public static void setupApplication(Application app) {

    }
}
