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
import com.leocai.publiclibs.multidecicealign.MySensorManager;
import com.leocai.publiclibs.multidecicealign.SensorGlobalWriter;
import com.leocai.publiclibs.multidecicealign.SensorSokectWriter;
import com.leocai.publiclibs.multidecicealign.StartCallBack;
import com.leocai.publiclibs.multidecicealign.StopCallBack;
import com.androidhiddencamera.HiddenCameraFragment;
import com.leocai.multidevicesalign.DemoCamService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private static final String PREF_FREQUNCY_KEY = "frequncy";
    private static final String PREF_FILENAME_KEY = "filename";
    private static final String PREF_CSV_SWITCH_KEY = "csvswitch";


    private static final int STOPPED = 0;
    private static final int FILE_INITED = 1;
    private static final int STARTING = 2;

    TextView tv_log;

//    BleServer bleServer;   记录主从机器的连接状态
//    BleClient bleClient;

    MySensorManager mySensorManager;
    private int currentState;

//    Button btnMaster;
//    Button btnClient;
    Button btnStart;

    EditText etFileName;
    EditText edt_masterAddress;
    EditText edt_frequency;
    private String fileName;
    private String masterAddress;
    private int frequency;

//    这里更改了下初始化方法
    private SensorGlobalWriter csvWriter;
    private SensorSokectWriter socketWriter = new SensorSokectWriter();
    private Switch writeCSVSwitch;

    private HiddenCameraFragment mHiddenCameraFragment;

    private int countNum = 1000;

    Intent intent;
    Handler handler;
    Runnable runnable;
    Runnable runRemove;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_sync);
//        csvWriter = new SensorGlobalWriter(this);

        tv_log = (TextView) findViewById(R.id.tv_log);
//        btnMaster = (Button) findViewById(R.id.btn_master);
//        btnClient = (Button) findViewById(R.id.btn_client);
        btnStart = (Button) findViewById(R.id.btn_start);
        etFileName = (EditText) findViewById(R.id.et_filename);
        edt_masterAddress = (EditText) findViewById(R.id.edt_masterAddress);
        edt_frequency = (EditText) findViewById(R.id.edt_sensor_frequency);
        writeCSVSwitch = (Switch) findViewById(R.id.switch_writecsv);
        init();
//        这是在开始运行时拍一次照片
//        startService(new Intent(BleSyncActivity.this,DemoCamService.class));

        intent = new Intent(BleSyncActivity.this,DemoCamService.class);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                startService(intent);
                Log.i(TAG,"runnable executed:Thread number is" + countNum--);
                handler.postDelayed(this,5000);
            }
        };
        runRemove = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"runRemove executed");
                handler.removeCallbacks(runnable);
            }
        };
    }

    public void init(){
//        进行界面的默认设置，读取手机文件，设置默认的显示内容
//        btnMaster.setEnabled(true);
//        btnClient.setEnabled(true);
        btnStart.setEnabled(true);
//        masterBtnAction();
//        clientBtnAction();
        startBtnAction();
//        利用sharedpreference存储文件名、频率、目的地址等信息，利用read**设置默认频率、文件名等
        masterAddress = readMasterAddress();
        edt_masterAddress.setText(masterAddress);
        frequency = readFrequncy();
        edt_frequency.setText(frequency+"");
        etFileName.setText(readFileName());
        etFileName.setEnabled(true);
        btnStart.setText("START");
//        测试一下获取的地址是什么
//        tv_log.setText(Environment.getExternalStorageDirectory().toString());
        tv_log.setText("");
        currentState = STOPPED;
        writeCSVSwitch.setChecked(readCSVSwitch());
    }

//    这一段是自己写的，试一下
    private void startBtnAction(){
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                switch (currentState){
                    case STOPPED:
                        String fileName = etFileName.getText().toString();
//                        看一下是不是文件名问题
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
                        saveCSVSwitch(writeCSVSwitch.isChecked());
                        mySensorManager = new MySensorManager(BleSyncActivity.this);
                        /*if(writeCSVSwitch.isChecked()){
                            mySensorManager.setGlobalWriter(csvWriter);
                        }else {
                            mySensorManager.setGlobalWriter(socketWriter);
                        }*/
                        try {
                            mySensorManager.setFileName(fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d("mySensorManager","a mistake using mysensorManager");
                        }
                        mySensorManager.setFrequency(frequency);
                        mySensorManager.startSensor();
                        mySensorManager.startDetection();

                        handler.postDelayed(runnable,5000);

                        ((Button) v).setText("STOP");
                        currentState = STARTING;
                        etFileName.setEnabled(false);
                        showLog("Sensor Listener Running");
                        break;
                    case STARTING:
                        mySensorManager.stop();
                        currentState = STOPPED;
                        handler.postDelayed(runRemove,1000);

                        stopService(intent);
                        ((Button) v).setText("START");
                        etFileName.setEnabled(true);
                        showLog("Sensor Listener Stopped");
                }
            }
        });
    }

/*    private void startBtnAction() {
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (bleServer == null) {
//                    toastError("Not Connected Yet");
//                    return;
//                }
                switch (currentState) {
                    case STOPPED:
                        String fileName = etFileName.getText().toString();
                        if (fileName.equals("")) {
                            toastError("Please input fileName first");
                            return;
                        }
                        saveFileName(fileName);
//                        bleServer.sendFileCommands(fileName);
                        ((Button) v).setText("START");
                        currentState = FILE_INITED;
                        etFileName.setEnabled(false);
                        break;
                    case FILE_INITED:
//                        bleServer.sendStartCommands();
                        ((Button) v).setText("STOP");
                        currentState = STARTING;
                        break;
                    case STARTING:
//                        bleServer.sendStopCommands();
                        ((Button) v).setText("Need Reset");
                        ((Button) v).setEnabled(false);
                        currentState = STOPPED;
                        etFileName.setEnabled(true);
                        break;
                }

                masterAddress = edt_masterAddress.getText().toString();
                if(masterAddress == null || masterAddress.equals("")){
                    toastError("Please input master address first");
                    return;
                }

                saveMasterAddress(masterAddress);
                showLog("Start Sensor Listener");
                btnStart.setEnabled(false);
                mySensorManager = new MySensorManager(BleSyncActivity.this);
                if(writeCSVSwitch.isChecked()){
                    mySensorManager.setGlobalWriter(csvWriter);
                }else
                    mySensorManager.setGlobalWriter(socketWriter);
                saveCSVSwitch(writeCSVSwitch.isChecked());
                frequency = Integer.parseInt(edt_frequency.getText().toString());
                saveFrequncy(frequency);
                以下两句是设置存储fileWriter对应的文件名，可能出错。
                fileName = etFileName.getText().toString();
                try {
                    mySensorManager.setFileName(fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }*//*
                mySensorManager.setFrequency(frequency);
                mySensorManager.startSensor();
                mySensorManager.startDetection();
            }
        });
    }*/



//    private void clientBtnAction() {
//        findViewById(R.id.btn_client).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!isBlutoothOpened()) {
//                    toastError("Blutooth Not Open");
//                    return;
//                }
//                masterAddress = edt_masterAddress.getText().toString();
//                if (masterAddress == null || masterAddress.equals("")) {
//                    toastError("Please input master address first");
//                    return;
//                }
//                saveMasterAddress(masterAddress);
//    存下主机地址
//                showLog("Client");
//                btnMaster.setEnabled(false);
//                btnStart.setEnabled(false);
//                btnClient.setEnabled(false);
//    点击客户端后三个按钮均不可点击
//                mySensorManager = new MySensorManager(BleSyncActivity.this);
//                if(writeCSVSwitch.isChecked())
//                    mySensorManager.setGlobalWriter(csvWriter);
//                else
//                    mySensorManager.setGlobalWriter(socketWriter);
//    设置传感器数据存储方式，本地存储或者socket传输
//                saveCSVSwitch(writeCSVSwitch.isChecked());
//                frequency = Integer.parseInt(edt_frequency.getText().toString());
//                saveFrequncy(frequency);
//    设置传感器监听频率，注册传感器监听
//                mySensorManager.setFrequency(frequency);
//                mySensorManager.startSensor();
//
//                bleClient = new BleClient(masterAddress);
//    应该是这里，在客户端点击client，创建一个蓝牙连接，然后连接到主机。
//                bleClient.connect(new ConnectedCallBack() {
//                    @Override
//                    public void onConnected(InputStream in) {
//                        showLog("Connected");
//                    }
//                }, new FileInitCallBack(){
//                    @Override
//    接受主机发送的初始化完成命令，之后锁定文件名输入功能
//                    public void onFileReceived(InputStream in) {
//                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
//                        try {
//                            final String fileName = br.readLine();
//                            mySensorManager.setFileName(fileName);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    etFileName.setText(fileName);
//                                    etFileName.setEnabled(false);
//                                }
//                            });
//                            showLog("FILE INITED");
//                        } catch (IOException e) {
//                            showLog(e.getMessage());
//                        }
//                    }
//                }, new StartCallBack() {
//                    @Override
//                    public void onStart() {
//                        showLog("STATING");
//                        mySensorManager.startDetection();
//                    }
//                }, new StopCallBack() {
//                    @Override
//                    public void onStop() {
//                        showLog("STOPPED");
//                        mySensorManager.stop();
//                    }
//                });
//            }
//        });
//    }

//    private void masterBtnAction() {
//        findViewById(R.id.btn_master).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!isBlutoothOpened()) {
//                    toastError("Blutooth Not Open");
//                    return;
//                }
//
//                showLog("Master");
//                bleServer = new BleServer();
//                bleServer.addObserver(BleSyncActivity.this);
//                bleServer.listen();
//                btnClient.setEnabled(false);
//                btnMaster.setEnabled(false);
//            }
//        });
//    }

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
//        if (mySensorManager != null)
//            mySensorManager.stop();
        Log.d(TAG, "onStop");
//        Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show();
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
        stopService(new Intent(BleSyncActivity.this,DemoCamService.class));
        Log.d(TAG, "onDestroy");
        if (mySensorManager != null)
            mySensorManager.stop();
//        Toast.makeText(this, "onDestroy", Toast.LENGTH_SHORT).show();

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
}
