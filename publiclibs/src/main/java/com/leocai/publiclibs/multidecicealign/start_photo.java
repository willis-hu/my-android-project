package com.leocai.publiclibs.multidecicealign;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.androidhiddencamera.*;


public class start_photo extends AppCompatActivity {

    public int PhotoTake =0;
    private HiddenCameraFragment mHiddenCameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }

    public void take_photo(){
        if (mHiddenCameraFragment != null) {    //Remove fragment from container if present
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(mHiddenCameraFragment)
                    .commit();
            mHiddenCameraFragment = null;
        }
        startService(new Intent(start_photo.this, DemoCamService.class));
//        PhotoTake = 1表示已经拍照完成，0表示service已经关闭
        PhotoTake =1;
    }

    public void stop_photo(){
        stopService(new Intent(start_photo.this,DemoCamService.class));
        PhotoTake =0;
    }

}
