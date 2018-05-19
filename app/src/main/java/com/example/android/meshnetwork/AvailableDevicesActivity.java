package com.example.android.meshnetwork;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.UUID;

/**
 * Created by Vamsi Karnika on 2/10/2018.
 */

public class AvailableDevicesActivity extends Activity{

    private static final String TAG = "AvailableDevicesActivity";

    private BluetoothAdapter mBTAdapter;

    private ArrayAdapter<String> mDevicesAdapter;
    public static final UUID HELLO_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        Button scanButton = (Button)findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        mDevicesAdapter = new ArrayAdapter<String>(this,R.layout.device_name);
        ListView DevicesListView = (ListView) (findViewById(R.id.new_devices));
        DevicesListView.setAdapter(mDevicesAdapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver,filter);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();


    }

    protected void onDestroy(){
        super.onDestroy();
        if(mBTAdapter != null){
            mBTAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    private void doDiscovery(){


        setTitle(R.string.scanning);

        findViewById(R.id.new_devices).setVisibility(View.VISIBLE);

        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
        }

        mBTAdapter.startDiscovery();
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                mDevicesAdapter.add(device.getName());
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                setTitle(R.string.select_device);
                if (mDevicesAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mDevicesAdapter.add(noDevices);
                }
            }
        }
    };

}
