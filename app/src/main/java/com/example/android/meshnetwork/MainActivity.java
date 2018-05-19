package com.example.android.meshnetwork;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "MainActivity";
    public static final UUID HELLO_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static BluetoothAdapter mBTAdapter ;
    public static String mesh = "MeshNetwork";
    public static ArrayList<BluetoothDevice> AvailableDevices;
    public static ArrayList<BluetoothDevice> AllDevices;
    public static ArrayList<BluetoothDevice> mNewBTDevices;
    private ArrayAdapter<String> mNewDevicesAdapter;
    public static int node_id = 1;
    //private AcceptThread mAcceptThread;
    private HelperClass helperClass;
    private ServerClass serverClass;
    private ArrayAdapter<String> mPairedDevicesAdapter;
    public static String device_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_list);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        //Setting device name and ID
        setNodeID();setNodeName();

        //mAcceptThread = new AcceptThread(mBTAdapter,getBaseContext(),HELLO_UUID);
        helperClass = new HelperClass();

        serverClass = new ServerClass(mBTAdapter,getBaseContext(),HELLO_UUID);
        serverClass.start();

        mNewDevicesAdapter = new ArrayAdapter<String>(this,R.layout.device_name);
        mPairedDevicesAdapter = new ArrayAdapter<String>(this,R.layout.device_name);

        ListView mPairedDevicesListView = (ListView)findViewById(R.id.paired_devices);
        mPairedDevicesListView.setAdapter(mPairedDevicesAdapter);

        ListView mNewDevicesListVIew = (ListView)(findViewById(R.id.new_devices));
        mNewDevicesListVIew.setAdapter(mNewDevicesAdapter);
        mNewDevicesListVIew.setOnItemClickListener(mNewDeviceClickListener);

        AvailableDevices = new ArrayList<BluetoothDevice>() ;
        AllDevices = new ArrayList<BluetoothDevice>();

        mNewBTDevices = new ArrayList<BluetoothDevice>();

        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        if(mBTAdapter == null){

            Log.d("Android: ","Bluetooth is missing");

        }

        if(!mBTAdapter.isEnabled()){

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableBtIntent , REQUEST_ENABLE_BT);

        }

        Button scan = (Button)findViewById(R.id.button_scan);
        scan.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                doDiscovery();
            }
        });

        Button visible = (Button)findViewById(R.id.button_setDiscovrable);
        visible.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
              enableDiscovery();
            }
        });

        Button online = (Button)findViewById(R.id.button_online);
        online.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                goOnline();
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter);

        helperClass.start();
        //mAcceptThread.start();
    }

    private void doDiscovery(){

        Log.d(TAG,"Discovering devices");
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
        }

        mBTAdapter.startDiscovery();
        (findViewById(R.id.button_scan)).setEnabled(false);
    }

    private void enableDiscovery(){

        Log.d(TAG,"enableDiscovery");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        startActivity(discoverableIntent);

    }
    private void goOnline(){

        Log.d(TAG,"Enabling Discovery");
        Intent intent = new Intent(getApplicationContext(),OnlinedeviceListActivity.class);
        startActivity(intent);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceAddress = device.getAddress();
                boolean newDevice = true;

                for(int device_i = 0; device_i < AllDevices.size();device_i++){
                    if(AllDevices.get(device_i).getAddress().equals(deviceAddress)){
                        newDevice = false;
                        break;
                    }
                }

                if(!newDevice) return;

                AllDevices.add(device);
                String deviceHardwareAddress = device.getAddress();
                Log.d("Android:", device.getName() + " " + deviceHardwareAddress);

                if(device.getBondState() == BluetoothDevice.BOND_BONDED){

                    if(findViewById(R.id.title_paired_devices).getVisibility() == View.GONE)
                        findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);

                    AvailableDevices.add(device);
                    mPairedDevicesAdapter.add(device.getName());
                }
                else {
                    /*if(mNewDevicesAdapter.getCount() == 1) {
                        if (mNewDevicesAdapter.getItem(0).equals(getResources().getText(R.string.none_found).toString())) {
                            mNewDevicesAdapter.remove(getResources().getText(R.string.none_found).toString());
                        }
                    }*/
                    mNewDevicesAdapter.add(device.getName());
                    mNewBTDevices.add(device);
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                (findViewById(R.id.button_scan)).setEnabled(true);
                /*if(mNewDevicesAdapter.getCount() == 0){
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesAdapter.add(noDevices);
                }*/
            }
        }
    };

    private final Handler handler = new Handler(){

        public void handleMessage(android.os.Message msg){

            Log.d(TAG,"Handler Message.");

            /*else{
                Log.d("Android:","Message type not found.");
            }*/
        }
    };



    private  AdapterView.OnItemClickListener mNewDeviceClickListener = new AdapterView.OnItemClickListener(){

        public void onItemClick(AdapterView<?> av,View v,int arg1,long arg2){
            //Stop Discovery because It's costly process
            mBTAdapter.cancelDiscovery();

            int view_position = av.getPositionForView(v);

            BluetoothDevice pairTo = mNewBTDevices.get(view_position);
            if(pairTo.getBondState() == BluetoothDevice.BOND_BONDED){
                Toast.makeText(getApplicationContext(),"Already Paired",Toast.LENGTH_SHORT).show();
            }
            else {
                PairDevice(pairTo,view_position);
                Log.d("OnItemClick", "Pairing Finished.");

            }
        }
    };

    private void PairDevice(BluetoothDevice device,int position){
        try{
            Log.d("PairDevice","Starting Pairing....");
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device,(Class[]) null);
        }catch (Exception e){
            Log.e("PairDevice",e.getMessage());
            return;
        }
        mNewBTDevices.remove(position);
        findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
        mNewDevicesAdapter.remove(device.getName());
        AvailableDevices.add(device);
        mPairedDevicesAdapter.add(device.getName());
    }

    private void setNodeID(){
        String hardwareAddress = mBTAdapter.getAddress();
        int generatedid = hardwareAddress.charAt(hardwareAddress.length()-1)*hardwareAddress.charAt(hardwareAddress.length()-2);
        generatedid = generatedid%100;
        node_id = generatedid;
        Log.d("NodeID:",String.valueOf(node_id));
    }

    private void setNodeName(){
        String name = mBTAdapter.getName();
        device_name = "";
        for(int i = 0; i < name.length();i++){
            if(name.charAt(i) == ' '){
                device_name+= '_';
            }
            else{
                device_name+= name.charAt(i);
            }
        }
    }

    public void onDestroy(){
        super.onDestroy();
        //mAcceptThread.cancel();
        serverClass.cancel();
        helperClass.cancel();
        serverClass.stop();
        mBTAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
    }

}
