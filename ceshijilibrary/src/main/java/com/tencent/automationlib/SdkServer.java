package com.tencent.automationlib;

import android.util.Log;
import android.view.View;

import org.json.JSONException;

import java.io.IOException;
import java.net.Socket;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;

// 服务端
public class SdkServer {
    private final ServerSocket serverSocket;

    public SdkServer(int port){
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String handleMessage(String message) throws JSONException, IOException {
        // TODO: 处理来自无障碍服务App的消息
        if("开始收集".equals(message)){
            View[] windowDecorViews = UIHierarchy.getWindowDecorViews();
            if( windowDecorViews == null){
                Log.d("TAG windowDecorViews", "windowDecorViews == null");
                return null;
            }
            View view = UIHierarchy.getRecentDecorView(windowDecorViews);
            if(view == null) return null;
//            long startT = System.currentTimeMillis();
//            try {
//                UIHierarchy.generateHierarchyJson(view);
//            } finally {
//                Log.d("SDK cost time", (System.currentTimeMillis() - startT) + " ms");
//            }
            return UIHierarchy.generateHierarchyJson(view).toString();
        }
        return null;
    }

    public void startListen() {
        Socket clientSocket = null;
        BufferedReader in = null;
        try {
            while (true){
                System.out.println("等待客户端连接...");
                clientSocket = serverSocket.accept();
                System.out.println("客户端已连接");
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientMsg = in.readLine();
                Log.d("TAG clientMsg", String.valueOf(clientMsg));
                System.out.println("接收到来自客户端的消息：" + clientMsg);
                if (clientMsg.length()> 0) {
                    String ans = handleMessage(clientMsg);
                    Log.d("TAG SDK JSON", String.valueOf(ans));
                    String packageName = UIHierarchy.getPackageName();
                    Log.d("TAG SDK PKG", String.valueOf(packageName));
                    if (ans == null){
                        Log.e("TAG IN SDK", "SDK JSON NULL");
                        ans = "error while getting sdk json: ans is null";
                    }
                    if (packageName == null){
                        Log.e("TAG IN SDK", "PKG NULL");
                        packageName = "null";
                    }
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println(packageName);
                    out.println(ans);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }finally {
            try {
                in.close();
                clientSocket.close();
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


