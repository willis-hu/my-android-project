package com.leocai.multidevicesalign;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.leocai.publiclibs.ConnectedCallBack;
import com.leocai.publiclibs.connection.BleServer;
import com.leocai.publiclibs.multidecicealign.BleClient;
import com.leocai.publiclibs.multidecicealign.FileInitCallBack;
import com.leocai.publiclibs.multidecicealign.MessageSend;
import com.leocai.publiclibs.multidecicealign.MySensorManager;
import com.leocai.publiclibs.multidecicealign.SensorGlobalWriter;
import com.leocai.publiclibs.multidecicealign.SensorSokectWriter;
import com.leocai.publiclibs.multidecicealign.StartCallBack;
import com.leocai.publiclibs.multidecicealign.StopCallBack;
import com.androidhiddencamera.HiddenCameraFragment;
import com.leocai.multidevicesalign.DemoCamService;
import com.leocai.publiclibs.multidecicealign.MessageSend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

/**
 * 主机从计公用的activity
 * 主机先输入文件名，按master，
 * 从机按client，主机显示连接并发送文件名给从机，从机初始化。
 * 主机发送开始命令给从机，从机开始。
 * 主机发送结束命令给从机，从机结束
 */
public class BleSyncActivity extends AppCompatActivity implements Observer {

    private static final String TAG = "BleSyncActivity";
    private static final String PREF_ADDRESS_KEY = "master_address";
    private static final String PREFS_NAME = "pref";
    private static final String PREF_FREQUNCY_KEY = "frequency";
    private static final String CAMERA_FREQUENCY_KEY = "camera_frequency";
    private static final String PREF_FILENAME_KEY = "filename";
    private static final String PREF_CSV_SWITCH_KEY = "csvswitch";
    private final StringBuffer messageSend2Wifi = new StringBuffer("still connect");


//  变量表示startSensor是否开始。
    private int currentState;
    private static final int STOPPED = 0;
    private static final int FILE_INITED = 1;
    private static final int STARTING = 2;

    private boolean connected = false;//记录无线连接状态

    MySensorManager mySensorManager;


    Button btnStart;
    Button btnConnect;//两个按钮用于连接服务器和开始采集
    TextView tv_log;//用于显示当前状态
    EditText etFileName;
    EditText edt_masterAddress;
    EditText edt_frequency;
//    EditText edt_camera_frequency;//采集数据需要的参数
    private String masterAddress;
    private int frequency;
//    private int camera_frequency;
    private int countNum = 0;//记录拍照次数，可以在log中看到。


/*    private SensorGlobalWriter csvWriter;
    private SensorSokectWriter socketWriter = new SensorSokectWriter();
    已废弃
    */
    private Switch writeCSVSwitch;

    Intent intent;
    Handler handler;
    Runnable runnable;
    Runnable runRemove;//用于拍照控制
    Runnable keepWifiConnect;

//    Handler handler_connect;
//    Runnable runConnect;//用来监控服务器端发送的消息

    private Socket msocket;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private char[] data = new char[5];//无线控制相关，data为缓冲区



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_sync);

        tv_log = (TextView) findViewById(R.id.tv_log);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        etFileName = (EditText) findViewById(R.id.et_filename);
        edt_masterAddress = (EditText) findViewById(R.id.edt_masterAddress);
        edt_frequency = (EditText) findViewById(R.id.edt_sensor_frequency);
//        edt_camera_frequency = (EditText)findViewById(R.id.edt_camera_frequency);
        writeCSVSwitch = (Switch) findViewById(R.id.switch_writecsv);
        init();

        intent = new Intent(BleSyncActivity.this,DemoCamService.class);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                startService(intent);
                Log.i("CameraService","we have take picture : " + countNum++);
//                handler.postDelayed(this,camera_frequency);
            }
        };
        runRemove = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"runRemove executed");
                stopService(intent);
                handler.removeCallbacks(runnable);
            }
        };
        keepWifiConnect = new Runnable() {
            @Override
            public void run() {
                /*if(msocket.isClosed()&&connected){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnConnect.performClick();
                        }
                    });
                }*/
                send(messageSend2Wifi);
                handler.postDelayed(this,2000);
            }
        };
    }

    public void init(){
//        进行界面的默认设置，读取手机文件，设置默认的显示内容
        btnStart.setEnabled(true);
        btnConnect.setEnabled(true);

        connectBtnAction();//连接服务器
        startBtnAction();//开始采集



//        利用sharedpreference存储文件名、频率、目的地址等信息，利用read**设置默认频率、文件名等
        masterAddress = readMasterAddress();
        edt_masterAddress.setText(masterAddress);
        frequency = readFrequncy();
        edt_frequency.setText(frequency+"");
//        camera_frequency = readCameraFrequency();
//        edt_camera_frequency.setText(camera_frequency+"");
        etFileName.setText(readFileName());
        etFileName.setEnabled(true);
        btnStart.setText("START");

        tv_log.setText("");
        currentState = STOPPED;
        writeCSVSwitch.setChecked(readCSVSwitch());
    }

//  start按钮操作
    private void startBtnAction(){
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                switch (currentState){
                    case STOPPED:
                        String fileName = new String();
                        if(connected){
//                            如果是已连接wifi状态，则获取当前时间来作为文件名
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                            Date curDate = new Date(System.currentTimeMillis());
                            fileName = formatter.format(curDate);
                            etFileName.setText(fileName);
                            handler.removeCallbacks(keepWifiConnect);
                        }
                        else {
                            fileName = etFileName.getText().toString();
                        }
                        Log.d(TAG,fileName);
                        masterAddress = edt_masterAddress.getText().toString();
                        frequency = Integer.parseInt(edt_frequency.getText().toString());
                        if(fileName.equals("")){
                            toastError("Please input filename first");
                            return;
                        }
                        saveFileName(fileName);
                        saveMasterAddress(masterAddress);
                        saveFrequncy(frequency);
//                        saveCameraFrequency(camera_frequency);
                        saveCSVSwitch(writeCSVSwitch.isChecked());
                        mySensorManager = new MySensorManager(BleSyncActivity.this);
                        try {
                            mySensorManager.setFileName(fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d("mySensorManager","a mistake using mySensorManager");
                        }

                        mySensorManager.setFrequency(frequency);
                        mySensorManager.setMasterAdress(masterAddress);
                        mySensorManager.startSensor();
                        mySensorManager.startDetection();

//                      开始调用拍照service
//                        handler.postDelayed(runnable,camera_frequency);

                        ((Button) v).setText("STOP");
                        currentState = STARTING;
                        etFileName.setEnabled(false);
                        edt_masterAddress.setEnabled(false);
                        edt_frequency.setEnabled(false);
//                        edt_camera_frequency.setEnabled(false);
                        showLog("Sensor Listener Running");
                        break;
                    case STARTING:
                        if(connected){
                            handler.postDelayed(keepWifiConnect,2000);
                        }
                        mySensorManager.stop();
                        currentState = STOPPED;
                        handler.postDelayed(runRemove,1000);
                        stopService(intent);
                        ((Button) v).setText("START");
                        etFileName.setEnabled(true);
                        edt_masterAddress.setEnabled(true);
                        edt_frequency.setEnabled(true);
//                        edt_camera_frequency.setEnabled(true);
                        showLog("Sensor Listener Stopped");
                }
            }
        });
    }

    private void connectBtnAction(){
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                masterAddress = edt_masterAddress.getText().toString();
                saveMasterAddress(masterAddress);
                if (!connected) {
                    if (connect(masterAddress)) {
                        tv_log.setText("Connected");
                        btnConnect.setText("Disconnect");
//                      创建线程监听服务器端是否有start消息
                        final Thread thread = new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                while (connected) {
                                    try {
                                        data = new char[5];
                                        Log.i(TAG, "socket is still connected");
                                        bufferedReader.read(data);
                                        Log.i(TAG, "we received " + String.valueOf(data));
                                        if (String.valueOf(data).equals("start")) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
//                                                  接收到start消息，模拟点击start按钮
                                                    btnStart.performClick();
                                                }
                                            });
                                        }else if(String.valueOf(data).equals("shutt")){
                                            handler.post(runnable);
                                        }
                                        else {
                                            Log.i(TAG, "something wrong received");
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };
                        thread.start();
                    }
                    else{
                        Toast.makeText(BleSyncActivity.this,"connect server error,",Toast.LENGTH_SHORT).show();
                    }
                    handler.postDelayed(keepWifiConnect,2000);
                }
                else {
                    try {
                        msocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    connected = false;
                    btnConnect.setText("Connect");
                    tv_log.setText("Disconnected");
                    try{
                        inputStream.close();
                        inputStreamReader.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    handler.removeCallbacks(keepWifiConnect);
                }
                }
            });
    }


    private void toastError(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BleSyncActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isBlutoothOpened() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 存取主机地址
     *
     * @param masterAddress
     */
    private void saveMasterAddress(String masterAddress) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ADDRESS_KEY, masterAddress);
        editor.apply();
    }

    private String readFileName() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getString(PREF_FILENAME_KEY, "114.212.85.124");
    }
    private void saveFileName(String fileName){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_FILENAME_KEY, fileName);
        editor.apply();
    }


    private String readMasterAddress() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getString(PREF_ADDRESS_KEY, "50:A7:2B:7F:B7:2F");
    }

    private void saveFrequncy(int frequency){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_FREQUNCY_KEY, frequency);
        editor.apply();
    }

    private int readFrequncy() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getInt(PREF_FREQUNCY_KEY, 50);
    }

    private void saveCameraFrequency(int camera_frequency){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(CAMERA_FREQUENCY_KEY,camera_frequency);
        editor.apply();
    }

    private int readCameraFrequency(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
        return settings.getInt(CAMERA_FREQUENCY_KEY,5000);
    }

    private void saveCSVSwitch(boolean sw) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_CSV_SWITCH_KEY, sw);
        editor.apply();
    }

    private Boolean readCSVSwitch() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean(PREF_CSV_SWITCH_KEY, true);
    }



    private void showLog(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_log.setText(info);
                Log.d(TAG, info);
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
//        Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
//        Toast.makeText(this, "onPause", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
//        Toast.makeText(this, "onStart", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connected = false;
        try {
            msocket.close();
            outputStream.close();
            inputStream.close();
            inputStreamReader.close();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopService(new Intent(BleSyncActivity.this,DemoCamService.class));
//        handler_connect.removeCallbacks(runConnect);
        Log.d(TAG, "onDestroy");
        if (mySensorManager != null)
            mySensorManager.stop();
        Toast.makeText(this, "onDestroy", Toast.LENGTH_SHORT).show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ble_sync, menu);
        return true;
    }

    @Override
//    设计点击菜单的相应事件
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if(mySensorManager!=null){
                mySensorManager.stop();
                mySensorManager.close();
            }
            /*if(bleServer!=null)
            bleServer.close();
            if(bleClient!=null)
            bleClient.close();*/
            init();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
//    设置上方的log显示内容
    public void update(Observable observable, final Object data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int numOfClinet = (int) data;
                tv_log.setText(numOfClinet + " clients connected");
                if(numOfClinet >= 1){
                    btnStart.setText("INIT FILE");
                    btnStart.setEnabled(true);
                }
            }
        });
    }

    public boolean connect(final String masterAddress){
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                if (!connected) {
                    try {
                        msocket = new Socket(masterAddress, 5000);//尝试连接到服务器端
                        if (msocket == null) {
                            Log.i(TAG,"socket connect error");
                        } else {
                            Log.i(TAG,"socket is connected");
                            connected = true;
//                            连接成功后socket状态变为true
                            inputStream = msocket.getInputStream();
                            inputStreamReader = new InputStreamReader(inputStream,"utf-8");
                            bufferedReader = new BufferedReader(inputStreamReader);
                        }
                        outputStream = msocket.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i(TAG,"perhaps wrong with connecting");
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
        return connected;
    }

    public void send(final StringBuffer satelliteInfo){

        final Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                if (connected){
                    try{
                        outputStream.write(satelliteInfo.toString().getBytes("utf-8"));
                        outputStream.flush();
//                        flush()完成数据发送
                        Log.i(TAG,"message has been sent");
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

}
