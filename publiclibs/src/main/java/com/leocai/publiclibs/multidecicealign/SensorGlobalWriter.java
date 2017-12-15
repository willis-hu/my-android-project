package com.leocai.publiclibs.multidecicealign;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;

import com.leocai.publiclibs.PublicConstants;
import com.leocai.publiclibs.ShakingData;
import com.androidhiddencamera.HiddenCameraFragment;
import com.androidhiddencamera.HiddenCameraService;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;

/**
 * 监听传感器数据，用于将传感器数据写到文件中
 * 启动一个线程，一个传感器周期写一次数据
 *
 * Created by leocai on 15-12-21.
 */
public class SensorGlobalWriter extends Observable implements SensorEventListener {
    private static final String TAG = "SensorDataWriter";

    private long preTimestamp;

    private double[] gravity = new double[3];//TODO 初始值
    private double[] linear_acceleration = new double[3];

    protected ShakingData cuShakingData = new ShakingData();
    private volatile boolean stop;
//    private int capture_time = 0;

    private OutputStream outputStream;
    private FileWriter fileWriter;

    private LocationManager myLocationManager;
//    protected GpsLocation myGpsLocation = new GpsLocation(myLocationManager);
    protected GpsLocation myGpsLocation;
//    private start_photo mStartPhoto = new start_photo();
//    private boolean fileNameSet;
    private String masterAdress;


//    新建文件，将文件头和缓冲带的数据写入到文件中
    public SensorGlobalWriter(String fileName) {
        cuShakingData.setLinearAccData(null);
        cuShakingData.setGyrData(null);
        cuShakingData.setDt(0);
        cuShakingData.setIndex(0);
        cuShakingData.setTimeStamp(0);
        try {
//            fileWriter = new FileWriter(new File(fileName));
            fileWriter = new FileWriter(new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    +File.separator+"DataCollector",fileName+".csv"));
            fileWriter.write(cuShakingData.getCSVHead());
            fileWriter.flush();

//            将缓冲带中的数据写入到文件中
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

//    设置文件名
    public void setFileName(String fileName) throws IOException {
        try {
//            fileWriter = new FileWriter(new File(fileName));
//            String filename = Environment.getExternalStorageDirectory().toString()+'/'+fileName;
//            File f = new File(filename);
//            fileNameSet = true;
//            Log.d(TAG,Environment.getDataDirectory().toString());
            fileWriter = new FileWriter(new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    +File.separator+"DataCollector",fileName+".csv"));
            fileWriter.write(cuShakingData.getCSVHead());
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SensorGlobalWriter() {
        cuShakingData.setLinearAccData(null);
        cuShakingData.setGyrData(null);
        cuShakingData.setDt(0);
        cuShakingData.setIndex(0);
        cuShakingData.setTimeStamp(0);
    }

    public SensorGlobalWriter(Context context){
        this.myLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        this.myGpsLocation = new GpsLocation(myLocationManager);
        myGpsLocation.setShakingData(cuShakingData);
//        这里完成SensorGlobalWriter和GpsLocation之间cushakingData的同步
        myGpsLocation.setMyContext(context);
        myGpsLocation.isOpenGps();
        myGpsLocation.formListenerGetLocation();
        myGpsLocation.getLocation();
        myGpsLocation.getGpsStatus();
//        这里实现获取gps数据
        myGpsLocation.getStatusListener();

        cuShakingData.setLinearAccData(null);
        cuShakingData.setGyrData(null);
        cuShakingData.setDt(0);
        cuShakingData.setIndex(0);
        cuShakingData.setTimeStamp(0);

    }

    public void startDetection() {
        stop = false;
        new Thread(new Runnable() {

                //            匿名内部类
                @Override
                public void run() {

//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                    while (!stop) {
//                        这里添加getSatelliteInfo为空时不执行
                        if (cuShakingData.getLatitude()[1] == 0.0) continue;
//                    Log.d(TAG,"dection");
//                    cuShakingData.transform();
//                    观察者模式，
                        notifyObservers(cuShakingData);
//                    通知所有的observe数据发生了变化，使所有的observe调用update
                        setChanged();
//                    用来设置一个内部标志位注明数据发生了变化
                        try {
//                           这里添加控制，控制拍照间隔,试一下把控制添加到最外层activity中

                            Thread.sleep(PublicConstants.SENSOPR_PERIOD);
//                        一个传感器周期获取一次数据
                            StringBuffer info = new StringBuffer();
                            info.append(cuShakingData.getCSV());
                            /*if (myGpsLocation != null) {
                                info.append(",gpsInfo" + myGpsLocation.getSatelliteInfo());
                                info.append(",gpsNum," + myGpsLocation.getSatellliteNum());
                                Log.i(TAG,"this show myGpsLocation is not null");
                            }*/
                            /*capture_time++;
                            if(capture_time > 3){
                                if(mStartPhoto.PhotoTake == 0){
                                    mStartPhoto.take_photo();
                                }
                                if(mStartPhoto.PhotoTake == 1){
                                    mStartPhoto.stop_photo();
                                }
                                capture_time = 0;
                            }*/
//                        传感器不停变动，但是获取数据是一个传感器周期内获取一次

                            Log.d(TAG, info.toString());
                            if(fileWriter != null)  fileWriter.write(info.toString());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
    }

    public void start_take_picture(){

    }

    public void close() {
        try {
            stop = true;
            fileWriter.close();
            myGpsLocation.stopListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        传感器数据监听，如果传感器数据发生变化，则记录,使用cushaking封装封装记录传感器数据
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            cuShakingData.setAccData(new double[]{event.values[0],event.values[1],event.values[2]});
//            cuShakingData.setTimeStamp(new Date().getTime());
            if (preTimestamp != 0)
                cuShakingData.setDt(1.0 * (event.timestamp - preTimestamp) / 1000000000);
            preTimestamp = event.timestamp;
            Log.d(TAG, "" + event.timestamp);
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            cuShakingData.setGyrData(new double[]{event.values[0], event.values[1], event.values[2]});
            Log.d(TAG, "" + event.timestamp);
        } else if(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            cuShakingData.setMagnetData(new double[]{event.values[0], event.values[1], event.values[2]});
        } else if(sensor.getType() == Sensor.TYPE_GRAVITY){
            cuShakingData.setGravityAccData(new double[]{event.values[0], event.values[1], event.values[2]});
        } else if(sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            cuShakingData.setLinearAccData(new double[]{event.values[0], event.values[1], event.values[2]});
        } else if(sensor.getType() == Sensor.TYPE_LIGHT){
            cuShakingData.setLightData(event.values[0]);
        } else if(sensor.getType() == Sensor.TYPE_PRESSURE){
            cuShakingData.setPressureData(event.values[0]);
        }
//        cuShakingData.setSatelliteNum(this.myGpsLocation.getSatellliteNum());
//        cuShakingData.setSatelliteInfo(this.myGpsLocation.getSatelliteInfo());

        cuShakingData.setOrienData();
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public void setMasterAdress(String masterAdress){
        this.masterAdress = masterAdress;
        myGpsLocation.setMasterAdress(masterAdress);
    }

    public String getGps(){
        return cuShakingData.getSatelliteInfo();
    }

    public int getSatelliteNum(){return cuShakingData.getSatelliteNum();}

    public double[] getLatitude(){
        return cuShakingData.getLatitude();
    }

    public int[] getGpsSnr(){return cuShakingData.getGpsSnr();}
    public int[] getGpsPrn(){return cuShakingData.getGpsPrn();}
    public int[] getGpsAzimuth(){return cuShakingData.getGpsAzimuth();}
    public int[] getGpsElevation(){return cuShakingData.getGpsElevation();}
}
