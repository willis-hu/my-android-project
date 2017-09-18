package com.leocai.multidevicesalign;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.security.PublicKey;

/**
 * Created by HuQigen on 2017/9/13.
 */

public class MessageGet{
    private String TAG = "MessageGet";
    private Socket msocket;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private String ip;
    private String data;
    private boolean socketStatus = false;
    private Handler handler;
    private boolean buttonStart;


    public boolean connect(String masterAdress) {
//        ip = "10.103.246.66";//这里设置服务器端ip地址
        ip = masterAdress;
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                if (!socketStatus) {
                    try {
                        msocket = new Socket(ip, 8989);//尝试连接到服务器端
                        if (msocket == null) {
                        } else {
                            Log.i(TAG,"socket is connected");
                            socketStatus = true;
//                            连接成功后socket状态变为true
                        }
                        outputStream = msocket.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
        try {
            thread.sleep(1000);//保证连接完成，返回为true
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return socketStatus;

    }

    public void send(StringBuffer satelliteInfo){
        data = satelliteInfo.toString();
        if (data == null){
            Log.d(TAG,"卫星数据暂时为空");
        }else {
            data = data + '\n';
        }

        final Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                if (socketStatus){
                    try{
                        outputStream.write(data.getBytes("utf-8"));
                        outputStream.flush();
                        Log.i(TAG,"satelliteInfo has been sent");
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

//     从服务器端接收信息
    public boolean receive() throws IOException{

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    String response = new String();
                    inputStream = msocket.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    bufferedReader = new BufferedReader(inputStreamReader);
                    response = bufferedReader.readLine();
                    Log.i(TAG,"we received " +response);
                    if (response == "start" && buttonStart == false){
                        buttonStart = true;
                    }else if (response == "start" && buttonStart == true){
                        buttonStart = false;
                    }else {
                    }

                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        try {
            thread.sleep(1000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return buttonStart;
    }

    public void close() throws IOException {
        try {
            outputStream.close();
            msocket.close();
            Log.i(TAG,"socket is closed");
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}
