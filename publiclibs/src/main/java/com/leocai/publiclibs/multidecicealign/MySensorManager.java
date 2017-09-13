package com.leocai.publiclibs.multidecicealign;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.leocai.publiclibs.PublicConstants;

import java.io.IOException;
import java.util.Observer;

/**
 * 传感器管理类
 * 用于启动传感器，启动监听，传递事件给观察者
 * Created by leocai on 16-1-15.
 */
public class MySensorManager {
    private SensorManager mSensorManager;
    private Sensor mSensorAcc;
    private Sensor mSensorGYR;
    private Sensor mSensorMAG;
    private Sensor mSensorGravity;
    private Sensor mSensorLinear;
    private Sensor mSensorLight;
    private Sensor mSensorPressure;
    /**
     * 用于写文件的类
     */
    SensorGlobalWriter sensorGlobalWriter;

    private int frequency;
    private String masterAdress;

    public MySensorManager(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorGYR = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorMAG = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorGlobalWriter = new SensorGlobalWriter(context);
    }

    /**
     * 设置写本地文件还是socket通信
     * @param globalWriter
     * SensorSocketWriter 写socket
     * SensorGlobalWriter 写文件
     */
    public void setGlobalWriter(SensorGlobalWriter globalWriter){
        sensorGlobalWriter = globalWriter;
    }

    public void startSensor() {
        if(frequency==0) frequency = (int) (1000/ PublicConstants.SENSOPR_PERIOD);
        mSensorManager.registerListener(sensorGlobalWriter, mSensorAcc, (int) (1000 / frequency * 1000)); // 根据频率调整
        mSensorManager.registerListener(sensorGlobalWriter, mSensorGYR, (int) (1000 / frequency * 1000));
        mSensorManager.registerListener(sensorGlobalWriter, mSensorMAG, (int) (1000 / frequency * 1000));
        mSensorManager.registerListener(sensorGlobalWriter, mSensorGravity, (int) (1000 / frequency * 1000));
        mSensorManager.registerListener(sensorGlobalWriter, mSensorLinear, (int) (1000 / frequency * 1000));
        mSensorManager.registerListener(sensorGlobalWriter, mSensorLight,(int)(1000 / frequency * 1000));
        mSensorManager.registerListener(sensorGlobalWriter, mSensorPressure,(int)(1000 / frequency * 1000));
    }

    public void startDetection() {
        sensorGlobalWriter.startDetection();
    }

    public void stop() {
        sensorGlobalWriter.close();
    }

    public void close(){
        mSensorManager.unregisterListener(sensorGlobalWriter, mSensorAcc);
        mSensorManager.unregisterListener(sensorGlobalWriter, mSensorGYR);
        mSensorManager.unregisterListener(sensorGlobalWriter, mSensorMAG);
        mSensorManager.unregisterListener(sensorGlobalWriter, mSensorGravity);
        mSensorManager.unregisterListener(sensorGlobalWriter, mSensorLinear);
        mSensorManager.unregisterListener(sensorGlobalWriter, mSensorLight);
        mSensorManager.unregisterListener(sensorGlobalWriter, mSensorPressure);
        sensorGlobalWriter.close();
    }

    /**
     * 设置文件名或ip地址
     * @param fileName
     */
    public void setFileName(String fileName) throws IOException {
        sensorGlobalWriter.setFileName(fileName);
    }

    public void setMasterAdress(String masterAdress){
        this.masterAdress = masterAdress;
        sensorGlobalWriter.setMasterAdress(masterAdress);
    }

    public void addObserver(Observer observer) {
        sensorGlobalWriter.addObserver(observer);
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
