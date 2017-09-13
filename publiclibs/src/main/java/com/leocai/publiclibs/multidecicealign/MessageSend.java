package com.leocai.publiclibs.multidecicealign;

import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Created by HuQigen on 2017/9/13.
 */

public class MessageSend {

    private String TAG = "MessageSend";
    private Socket msocket;
    private PrintStream moutput;
    private OutputStream outputStream = null;
    private String ip;
    private String data;
    private boolean socketStatus = false;



    public boolean connect() {
        ip = "10.103.246.66";//这里设置服务器端ip地址
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
