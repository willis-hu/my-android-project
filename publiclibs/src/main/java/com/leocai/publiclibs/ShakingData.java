package com.leocai.publiclibs;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import com.dislab.leocai.spacesync.utils.MatrixUtils;
import com.leocai.publiclibs.multidecicealign.GpsLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;


/**
 * Remenber to sync format when update members
 * Created by leocai on 15-12-31.
 * 传感器数据封装
 */
public class ShakingData implements Serializable, Cloneable {



    private static final long serialVersionUID = -6091530420906090649L;
    private static final String TAG = "ShakingData";

    /**
     * 索引号，暂时没用
     */
    private int index;
    /**
     * 线性加速度
     */
    private double linearAccData[];
    /**
     * 陀螺仪
     */
    private double gyrData[];
    /**
     * 时间戳
     */
    private long timeStamp;
    /**
     * 与上次数据的事件差
     */
    private double dt;
    /**
     * 重力加速度数据
     */
    private double[] gravityAccData;
    /**
     * 合加速度
     */
    private double resultantAccData;
    /**
     * 全局加速度，有问题
     */
    private double[] convertedData;
    /**
     * 磁力计
     */
    private double[] magnetData;
    /**
     * 原始加速度
     */
    private double[] accData;
    /**
     * 旋转矩阵，可能有问题
     */
    private float[] rotationMatrix = new float[9];
    /**
     * 倾斜矩阵
     */
    private float[] inclimentMatrix = new float[9];

    private double lightData;

    private double pressureData;

    private double orientationData;

    private float[] usingAccData = new float[3];
    private float[] usingMagData = new float[3];

/*    private LocationManager myLocationManager;
    private GpsLocation myGpsLocation = new GpsLocation(myLocationManager);*/
    private StringBuffer satelliteInfo;
    private int satelliteNum;



    public ShakingData(double[] linearAccData, double[] gyrData, int index, double dt) {
        this.linearAccData = linearAccData;
        this.gyrData = gyrData;
        this.index = index;
        this.dt = dt;
    }

    /**
     * 初始化，并写ｃｓｖ头
     * @param dataLine
     */
//    依次读取dataLine里的数据，存到对应的传感器数据数组中
    public ShakingData(String dataLine) {
        String vals[] = dataLine.split(",");
        int cuIndex = 0;
        String v = null;
        accData = new double[3];
        for (int i = 0; i < 3; i++) {
            v = vals[cuIndex++];
            if(v.length()>0) accData[i] = Double.parseDouble(v);
//            将数字类型的字符串转变成double类型
            else accData[i] = 0;
        }
        linearAccData = new double[3];
        for (int i = 0; i < 3; i++) {
            v = vals[cuIndex++];
            if(v.length()>0) linearAccData[i] = Double.parseDouble(v);
            else linearAccData[i] = 0;
        }
        gravityAccData = new double[3];
        for (int i = 0; i < 3; i++) {
            v = vals[cuIndex++];
            if(v.length()>0) gravityAccData[i] = Double.parseDouble(v);
            else gravityAccData[i] = 0;
        }
        gyrData = new double[3];
        for (int i = 0; i < 3; i++) {
            v = vals[cuIndex++];
            if(v.length()>0) gyrData[i] = Double.parseDouble(v);
            else gyrData[i] = 0;
        }
        magnetData = new double[3];
        for (int i = 0; i < 3; i++) {
            v = vals[cuIndex++];
            if(v.length()>0) magnetData[i] = Double.parseDouble(v);
            else magnetData[i] = 0;
        }
        convertedData = new double[3];
        for (int i = 0; i < 3; i++) {
            v = vals[cuIndex++];
            if(v.length()>0) convertedData[i] = Double.parseDouble(v);
            else convertedData[i] = 0;
        }
        v = vals[cuIndex++];
        if(v.length()>0) lightData = Double.parseDouble(v);
        else lightData = 0;

        v = vals[cuIndex++];
        if(v.length()>0) pressureData= Double.parseDouble(v);
        else pressureData = 0;

        v = vals[cuIndex++];
        if(v.length()>0) orientationData= Double.parseDouble(v);
        else orientationData = 0;

        v = vals[cuIndex++];
        if(v.length()>0) resultantAccData= Double.parseDouble(v);
        else resultantAccData = 0;

        v = vals[cuIndex++];
        if(v.length()>0) timeStamp=  Long.parseLong(v);
        else timeStamp = 0;

        v = vals[cuIndex++];
        if(v.length()>0) dt=   Double.parseDouble(v);
        else dt = 0;
    }

    public double[] getAccData() {
        return accData;
    }

    public void setAccData(double[] accData) {
        this.accData = accData;
    }

    public ShakingData(byte[] sdBuffer) {
//        从sdBuffer里面读各个传感器的数据
//字节数组输入流
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(sdBuffer);
//        数据输入流，用来装饰其他输入流
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        this.linearAccData = new double[3];
        this.gravityAccData = new double[3];
        this.gyrData = new double[3];
        try {
            this.index = dataInputStream.readInt();
//            一次读取四个字节，即读一个int数
            for (int i = 0; i < 3; i++) {
//                依次读取三个传感器的数据
                this.linearAccData[i] = dataInputStream.readDouble();
                this.gyrData[i] = dataInputStream.readDouble();
                this.gravityAccData[i] = dataInputStream.readDouble();
//                从sdBuffer中读取各个传感器数组的数据
            }
            this.lightData = dataInputStream.readDouble();
            this.pressureData = dataInputStream.readDouble();
            this.orientationData = dataInputStream.readDouble();
            this.resultantAccData = dataInputStream.readDouble();
            this.timeStamp = dataInputStream.readLong();
            this.dt = dataInputStream.readDouble();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ShakingData(ShakingData shakingData) {
        copy(shakingData);
    }

    /**
     * 将各个数组中原有的数据转换成字节
     * @return
     */
    public byte[] getBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataInputStream = new DataOutputStream(outputStream);
        try {
            dataInputStream.writeInt(this.index);
            for (int i = 0; i < 3; i++) {
                dataInputStream.writeDouble(this.linearAccData[i]);
                dataInputStream.writeDouble(this.gyrData[i]);
                dataInputStream.writeDouble(this.gravityAccData[i]);
            }
            dataInputStream.writeDouble(this.lightData);
            dataInputStream.writeDouble(this.pressureData);
            dataInputStream.writeDouble(this.orientationData);
            dataInputStream.writeDouble(this.resultantAccData);
            dataInputStream.writeLong(this.timeStamp);
            dataInputStream.writeDouble(this.dt);
            dataInputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();

    }

    public ShakingData() {
        Random random = new Random(System.currentTimeMillis());
        linearAccData = new double[]{random.nextDouble() * 10, random.nextDouble() * 10, random.nextDouble() * 10};
        gravityAccData = new double[]{random.nextDouble() * 10, random.nextDouble() * 10, random.nextDouble() * 10};
        convertedData = new double[]{random.nextDouble() * 10, random.nextDouble() * 10, random.nextDouble() * 10};
        gyrData = new double[]{random.nextDouble() * 5, random.nextDouble() * 5, random.nextDouble() * 5};
        lightData = random.nextDouble();
        pressureData = random.nextDouble();
        orientationData = random.nextDouble();
        index = random.nextInt(100);
        timeStamp = random.nextLong();
        dt = random.nextDouble();
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public double[] getLinearAccData() {
        return linearAccData;
    }

    public void setLinearAccData(double[] linearAccData) {
        this.linearAccData = linearAccData;
//        if (linearAccData == null) return;
//        this.resultantAccData = Math.sqrt(Math.pow(linearAccData[0], 2)
//                + Math.pow(linearAccData[1], 2)
//                + Math.pow(linearAccData[2], 2));
    }

    public double[] getGyrData() {
        return gyrData;
    }

    public void setGyrData(double[] gyrData) {
        this.gyrData = gyrData;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getDt() {
        return dt;
    }

    public void setDt(double dt) {
        this.dt = dt;
    }

    public void setLightData(double lightData){
        this.lightData = lightData;
    }

    public double getLightData(){
        return lightData;
    }

    public double getPressureData(){
        return pressureData;
    }

    public void setPressureData(double pressureData){
        this.pressureData = pressureData;
    }

    public double getOrienData(){
        return orientationData;
    }

    public void setOrienData() {
        if (this.accData != null && this.magnetData != null) {
            float[] values = new float[3];
            float[] R = new float[9];

            for (int i = 0; i < 3; i++) {
                usingAccData[i] = (float) this.accData[i];
                usingMagData[i] = (float) this.magnetData[i];
            }
            SensorManager.getRotationMatrix(R, null, usingAccData, usingMagData);
            SensorManager.getOrientation(R, values);
            values[0] = (float) Math.toDegrees(values[0]);
            this.orientationData = values[0];
        }
    }


//    将ShakingData类的toString方法重写，返回的是一个长数组，保存所有传感器数据
    @Override
    public String toString() {
        return "ShakingData{" +
                "linearAccData=" + Arrays.toString(linearAccData) +
                ", gyrData=" + Arrays.toString(gyrData) +
                ", dt=" + dt +
                ", gravityAccData=" + Arrays.toString(gravityAccData) +
                ", resultantAccData=" + resultantAccData +
                ", lightData=" + lightData +
                ", pressureData=" + pressureData +
                ", orientationData=" + orientationData +
                ", convertedData=" + Arrays.toString(convertedData) +
                '}';
    }

    public void setGravityAccData(double[] gravityAccData) {
        this.gravityAccData = gravityAccData;
    }

    public double[] getGravityAccData() {
        return gravityAccData;
    }

    public double getResultantAccData() {
        return resultantAccData;
    }

    public void setResultantAccData(double resultantAccData) {
        this.resultantAccData = resultantAccData;
    }

    @Override
    public ShakingData clone() throws CloneNotSupportedException {
        return (ShakingData) super.clone();
    }

    public void copy(ShakingData cuShakingData) {
        this.index = cuShakingData.index;
        this.gyrData = cuShakingData.gyrData;
        this.linearAccData = Arrays.copyOf(cuShakingData.linearAccData, 3);
        this.gravityAccData = Arrays.copyOf(cuShakingData.gravityAccData, 3);
        this.timeStamp = cuShakingData.timeStamp;
        this.dt = cuShakingData.dt;
        this.resultantAccData = cuShakingData.resultantAccData;
        this.lightData = cuShakingData.lightData;
        this.pressureData = cuShakingData.pressureData;
        this.orientationData = cuShakingData.orientationData;
    }

    public void setConvertedData(double[] convertedData) {
        this.convertedData = convertedData;
    }

    public double[] getConvertedData() {
        return convertedData;
    }

//返回值是Acc1,Acc2,Acc3,LinearACC1……
//    作用应当是产生csv的第一行
    public String getCSVHead() {
        StringBuilder info = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            info.append("Acc");
            info.append(i);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            info.append("LinearAcc");
            info.append(i);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            info.append("Gravity");
            info.append(i);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            info.append("Gyro");
            info.append(i);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            info.append("MagnetData");
            info.append(i);
            info.append(",");
        }
        info.append("LightData");
        info.append(",");
        info.append("PressureData");
        info.append(",");
        info.append("OrientationData");
        info.append(",");
//        for (int i = 0; i < 3; i++) {
//            info.append("ConvertedData");
//            info.append(i);
//            info.append(",");
//        }
//        info.append("ResultantAcc");
//        info.append(",");
        info.append("Timestamp");
        info.append(",");
        info.append("dt");
        info.append(",");
        info.append("GpsInfo");
        info.append("\n");
        return info.toString();
    }

//    产生csv的剩余行，即记录传感器数据
    public String getCSV() {
        StringBuilder info = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if(accData!=null)
            info.append(accData[i]);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            if(linearAccData!=null)
            info.append(this.linearAccData[i]);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            if(gravityAccData!=null)
            info.append(this.gravityAccData[i]);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            if(gyrData!=null)
            info.append(this.gyrData[i]);
            info.append(",");
        }
        for (int i = 0; i < 3; i++) {
            if(magnetData !=null)
                info.append(this.magnetData[i]);
            info.append(",");
        }
        info.append(this.lightData);
        info.append(",");
        info.append(this.pressureData);
        info.append(",");
        info.append(this.orientationData);
        info.append(",");
//        for (int i = 0; i < 3; i++) {
//            if(convertedData!=null)
//                info.append(this.convertedData[i]);
//            info.append(",");
//        }
//        info.append(resultantAccData);
//        info.append(",");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        info.append(sdf.format(new Date(System.currentTimeMillis())));
        info.append(",");
        info.append(this.dt);
        /*if (myGpsLocation != null) {
            info.append(",");
            info.append(myGpsLocation.getSatelliteInfo());
            info.append(",");
            info.append(myGpsLocation.getSatellliteNum());
            if (myGpsLocation.getSatellliteNum() != 0)
                Log.i(TAG,"satelliteNum is not 0");
//            Log.i(TAG,"myGpsLocation is not null");
        }*/
        info.append(",");
        info.append(satelliteInfo);
        info.append(",");
        info.append(satelliteNum);
        if(satelliteNum != 0)   Log.i(TAG,"Satelllite num is not 0");

        info.append("\n");

        return info.toString();
    }

    public void setMagnetData(double[] magnetData) {
        this.magnetData = magnetData;
    }

    public double[] getMagnetData() {
        return magnetData;
    }

    /**
     * 全局坐标转换
     */
//    应该是返回手机方向吧，这个不清楚，需要再请教一下。
    public void transform() {
        if(gravityAccData == null || magnetData == null) return;
        SensorManager.getRotationMatrix(rotationMatrix, inclimentMatrix,
                new float[]{(float) gravityAccData[0], (float) gravityAccData[1], (float) gravityAccData[2]},
                new float[]{(float) magnetData[0], (float) magnetData[1], (float) magnetData[2]});
        double[][] tempData = MatrixUtils.multiply(new double[][]{
                {rotationMatrix[0], rotationMatrix[1], rotationMatrix[2]},
                {rotationMatrix[3], rotationMatrix[4], rotationMatrix[5]},
                {rotationMatrix[6], rotationMatrix[7], rotationMatrix[8]}

        }, MatrixUtils.convertVectorToMatrix(linearAccData));

        convertedData = MatrixUtils.convertMatrixToVector(tempData);
        Log.d(TAG, Arrays.toString(convertedData));

    }

    /*public void useGpsLocation(Context context){
        myLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        myGpsLocation = new GpsLocation(myLocationManager);
        myGpsLocation.setMyContext(context);
        myGpsLocation.isOpenGps();
        myGpsLocation.formListenerGetLocation();
        myGpsLocation.getGpsStatus();
        myGpsLocation.getStatusListener();
    }*/

    public void setSatelliteInfo(StringBuffer satelliteinfo){
        satelliteInfo = satelliteinfo;
//        Log.i(TAG,"satelliteInfo are " + satelliteinfo.toString());
    }

    public void setSatelliteNum(int satellitenum){
        satelliteNum = satellitenum;
    }

    public StringBuffer getSatelliteInfo(){
        return satelliteInfo;
    }

    public int getSatelliteNum(){
        return satelliteNum;
    }
}
