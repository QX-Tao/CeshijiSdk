package com.tencent.automationlib;

public class Automation {
    public static void enable(int configCode){
        SdkServer sdkServer = new SdkServer(9000);
        Thread thread = new Thread(() -> {
            try {
                sdkServer.startListen();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
}
