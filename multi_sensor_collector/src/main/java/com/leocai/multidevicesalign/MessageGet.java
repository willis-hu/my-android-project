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
    private String response;
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

    public boolean receive() throws IOException{

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    inputStream = msocket.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    bufferedReader = new BufferedReader(inputStreamReader);
                    response = bufferedReader.readLine();
                    switch (response){
                        case "0":buttonStart = false;break;//为0表示关闭监听
                        case "1":buttonStart = true;break;//为1表示开启监听
                    }

                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        };
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
