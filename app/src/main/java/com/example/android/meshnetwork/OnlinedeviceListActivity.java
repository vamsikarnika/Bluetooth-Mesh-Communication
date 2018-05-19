package com.example.android.meshnetwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Vamsi Karnika on 1/25/2018.
 */

public class OnlinedeviceListActivity extends AppCompatActivity {

    private ListView mListView;
    private static ArrayList<String> onlineDevices;
    private ArrayAdapter<String> mOnlineDevicesAdapter;
    private LSA mLsa;
    private SPF mSpf;
    private ArrayList<String> onlineDeviceId;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.listonlinedevices);

        mLsa = new LSA();
        mLsa.start();

        mSpf = new SPF(10);
        mSpf.start();

        mListView = (ListView)(findViewById(R.id.onlinedeviceslist));
        mOnlineDevicesAdapter = new ArrayAdapter<String>(this,R.layout.device_name);
        mListView.setAdapter(mOnlineDevicesAdapter);
        mListView.setOnItemClickListener(mOnlineDeviceClickListener);
        onlineDeviceId = new ArrayList<String>();

        String LinkState = ServerClass.getLsa();
        String[] Onlinenodes = LinkState.split("-");

        for(int i = 0; i < Onlinenodes.length;i++){
            String[] data = Onlinenodes[i].split(" ");
            mOnlineDevicesAdapter.add(data[1]);
            onlineDeviceId.add(data[0]);
        }

        IntentFilter filter = new IntentFilter("com.example.android.LIST_UPDATED");
        registerReceiver(NewOnlineDevice,filter);
    }

    private final BroadcastReceiver NewOnlineDevice = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("com.example.android.LIST_UPDATED")){

                String textMessage = intent.getExtras().getString("text");
                String Idinfo = intent.getExtras().getString("IdValue");
                Log.d("NewOnlineDevice",textMessage);

                //onlineDevices.add(textMessage);
                mOnlineDevicesAdapter.add(textMessage);
                onlineDeviceId.add(Idinfo);
            }
        }
    };

    private AdapterView.OnItemClickListener mOnlineDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(getApplicationContext(),MessagingActivity.class);

            String nameinfo = ((TextView)view).getText().toString();
            String nodeinfo = onlineDeviceId.get(position);
            intent.putExtra("DeviceName",nameinfo);
            intent.putExtra("DeviceId",nodeinfo);
            startActivity(intent);
        }
    };
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(NewOnlineDevice);
    }

}
