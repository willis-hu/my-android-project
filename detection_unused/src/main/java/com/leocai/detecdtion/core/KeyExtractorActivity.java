package com.leocai.detecdtion.core;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.leocai.detecdtion.R;
import com.leocai.detecdtion.ShakeBufferView;
import com.leocai.publiclibs.PublicConstants;
import com.leocai.publiclibs.ShakeDetector;

import java.util.Observable;
import java.util.Observer;

public class KeyExtractorActivity extends AppCompatActivity implements Observer {

    private TextView tvLog;

    ShakeBufferView shakeBufferView;

    ShakeDatasStore shakeDatasStore = new ShakeDatasStore();

    ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_core_main);
//        ShakeBufferView shakeBufferView = (ShakeBufferView)findViewById(R.id.buffer_view);
        shakeBufferView = (ShakeBufferView) findViewById(R.id.shakebufferview);

        findViewById(R.id.btn_slave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Slave slave = new Slave(PublicConstants.MASTER_ADDRESS);
                shakeDetector = new ShakeDetector();
                shakeDetector.addObserver(shakeBufferView);
                shakeDetector.addObserver(shakeDatasStore);
                shakeDatasStore.setFileName("SlaveShakingDatas");
                slave.setShakeDetector(shakeDetector);
                slave.addObserver(KeyExtractorActivity.this);
            }
        });
        findViewById(R.id.btn_master).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Master master = new Master();
                shakeDetector = new ShakeDetector();
                shakeDetector.addObserver(shakeBufferView);
                shakeDetector.addObserver(shakeDatasStore);
                shakeDatasStore.setFileName("MasterShakingDatas");
                master.setShakeDetector(shakeDetector);
                master.addObserver(KeyExtractorActivity.this);
            }
        });

        tvLog = (TextView) findViewById(R.id.tv_log);

//        slave = new MySensorListener(shakeBufferView);
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_core_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable observable, final Object data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLog.setText((CharSequence) data);
            }
        });
    }
}
