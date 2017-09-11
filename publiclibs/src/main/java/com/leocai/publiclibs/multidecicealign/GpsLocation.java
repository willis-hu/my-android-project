package com.leocai.publiclibs.multidecicealign;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.net.Socket;

import com.leocai.publiclibs.ShakingData;
/**
 * Created by willi on 2017-06-19.
 */

public class GpsLocation {
    private final String TAG = "GpsLocation";

    private StringBuffer satelliteInfo;
    private int satellliteNum;

    private Context myContext;
    private GpsLocation instance;
    private Activity myActivity;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private Socket msocket;
    private PrintStream moutput;
    private OutputStream outputStream = null;
    private String ip;
    private String data;
    private boolean socketStatus = false;

    private GpsStatus.Listener listener;


    protected ShakingData cushakingData = new ShakingData();

    public void setShakingData(ShakingData shakingData) {
        this.cushakingData = shakingData;
    }


    public void setMyContext(Context context) {
        myContext = context;
    }

    public GpsLocation(Context context) {
        satelliteInfo = new StringBuffer();
//        locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }

    public GpsLocation(LocationManager thisLocationManager) {
        satelliteInfo = new StringBuffer();
        locationManager = thisLocationManager;
    }


    public void isOpenGps() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(myActivity);
            dialog.setMessage("GPS未打开，是否打开?");
            dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    // 设置完成后返回到原来的界面
                    myActivity.startActivityForResult(intent, 0);
                }
            });
            dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    public void formListenerGetLocation() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "纬度；" + location.getLatitude());
                Log.i(TAG, "经度：" + location.getLongitude());
                Log.i(TAG, "海拔：" + location.getAltitude());
                Log.i(TAG, "时间：" + location.getTime());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i(TAG, "Gps 状态变化");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.i(TAG, "Gps 状态不可用");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i(TAG, "Gps 状态可用");
            }
        };
        if (ActivityCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Log.i(TAG, "纬度：" + location.getLatitude());
        Log.i(TAG, "经度：" + location.getLongitude());
        Log.i(TAG, "海拔：" + location.getAltitude());
        Log.i(TAG, "时间：" + location.getTime());
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    public StringBuffer getStatusListener() {
        listener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                Log.i("debug","this is getStatusListener");
                satelliteInfo.setLength(0);
                satellliteNum = 0;
                if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
                    Log.i(TAG, "第一次定位");
                } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                    if (ActivityCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    GpsStatus gpsStauts = locationManager.getGpsStatus(null);
                    int maxSatellites = gpsStauts.getMaxSatellites();
                    Iterator<GpsSatellite> it = gpsStauts.getSatellites().iterator();//创建一个迭代器保存所有卫星
                    int count = 0;
                    while (it.hasNext() && count <= maxSatellites) {
                        count++;
                        GpsSatellite s = it.next();
                        satelliteInfo.append("," + s.getSnr());
//                        这里显示具体的gps信息
                        getGpsStatelliteInfo(s);
                        Log.i(TAG, satelliteInfo.toString());

                    }
                    satellliteNum = count;

                    cushakingData.setSatelliteInfo(satelliteInfo);
                    cushakingData.setSatelliteNum(satellliteNum);

                    Log.i(TAG, "搜索到：" + count + "颗卫星");
                } else if (event == GpsStatus.GPS_EVENT_STARTED) {
                    Log.i(TAG, "定位启动");
                } else if (event == GpsStatus.GPS_EVENT_STOPPED) {
                    Log.i(TAG, "定位关闭");
                }
            }
        };
        locationManager.addGpsStatusListener(listener);
        return satelliteInfo;
    }

    public StringBuffer showSateliteInfo() {
        return satelliteInfo;
    }

    public List<GpsSatellite> getGpsStatus() {
        List<GpsSatellite> result = new ArrayList<GpsSatellite>();
        if (ActivityCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.i(TAG, "Line 179 permission denied");
            return null;
        }
        GpsStatus gpsStatus = locationManager.getGpsStatus(null); // 取当前状态
        //获取默认最大卫星数
        int maxSatellites = gpsStatus.getMaxSatellites();
        //获取第一次定位时间（启动到第一次定位）
        int costTime = gpsStatus.getTimeToFirstFix();
        Log.i(TAG, "第一次定位时间:" + costTime);
        //获取卫星
        Iterable<GpsSatellite> iterable = gpsStatus.getSatellites();
        //一般再次转换成Iterator
        Iterator<GpsSatellite> itrator = iterable.iterator();
        int count = 0;
        while (itrator.hasNext() && count <= maxSatellites) {
            count++;
            GpsSatellite s = itrator.next();
            result.add(s);
        }
        return result;
    }

    public void getGpsStatelliteInfo(GpsSatellite gpssatellite) {

        //卫星的方位角，浮点型数据
        Log.i(TAG, "卫星的方位角：" + gpssatellite.getAzimuth());
        //卫星的高度，浮点型数据
        Log.i(TAG, "卫星的高度：" + gpssatellite.getElevation());
        //卫星的伪随机噪声码，整形数据
        Log.i(TAG, "卫星的伪随机噪声码：" + gpssatellite.getPrn());
        //卫星的信噪比，浮点型数据
        Log.i(TAG, "卫星的信噪比：" + gpssatellite.getSnr());
        //卫星是否有年历表，布尔型数据
        Log.i(TAG, "卫星是否有年历表：" + gpssatellite.hasAlmanac());
        //卫星是否有星历表，布尔型数据
        Log.i(TAG, "卫星是否有星历表：" + gpssatellite.hasEphemeris());
        //卫星是否被用于近期的GPS修正计算
        Log.i(TAG, "卫星是否被用于近期的GPS修正计算：" + gpssatellite.hasAlmanac());
    }

    public StringBuffer getSatelliteInfo() {
        return satelliteInfo;
    }

    public int getSatellliteNum() {
        return satellliteNum;
    }

    public void stopListener(){
        locationManager.removeUpdates(locationListener);
        locationManager.removeGpsStatusListener(listener);
//        试一下通过locationManager置空，关闭gps数据监听
    }

    /*public void connect(View view) {
        ip = "10.3.8.211";//这里设置服务器端ip地址
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                if (!socketStatus) {
                    try {
                        msocket = new Socket(ip, 8000);//尝试连接到服务器端
                        if (msocket == null) {
                        } else {
                            socketStatus = true;
                        }
                        outputStream = msocket.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();

    }

    public void send(View view){
        data = satelliteInfo.toString();
        if (data == null){
            Log.d(TAG,"卫星数据暂时为空");
        }else {
            data = data + '\0';
        }

        final Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                if (socketStatus){
                    try{
                        outputStream.write(data.getBytes());
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }*/
}
