package com.tencent.automationlib;

import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.net.Socket;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.Arrays;

// 服务端
public class SdkServer {
    private ServerSocket serverSocket;

    public SdkServer(int port){
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String handleMessage(String message) {
        // TODO: 处理来自无障碍服务App的消息
        if("开始收集".equals(message)){
            View[] windowDecorViews = UIHierarchy.getWindowDecorViews();
            String viewsString = UIHierarchy.generateHierarchyJson(windowDecorViews[0]);
            Log.d("TAG", "JSON: " + viewsString);
            return viewsString;
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
                System.out.println("接收到来自客户端的消息：" + clientMsg);
                if (clientMsg.length()> 0) {
                    String ans = handleMessage(clientMsg);
                    String packageName = UIHierarchy.getPackageName();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println(packageName);

                    out.println(ans);

                }
            }
        } catch (IOException e) {
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


