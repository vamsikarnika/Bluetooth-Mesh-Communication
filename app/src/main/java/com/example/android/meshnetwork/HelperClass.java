package com.example.android.meshnetwork;

import android.bluetooth.BluetoothDevice;
import android.nfc.Tag;
import android.os.*;
import android.util.Log;
import android.util.Pair;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

import static com.example.android.meshnetwork.MainActivity.HELLO_UUID;

/**
 * Created by Vamsi Karnika on 3/23/2018.
 */

public class HelperClass extends Thread {
    private static MyBluetoothService myBluetoothService ;
    private static String TAG = "MeshCom/HelperClass";
    private volatile boolean gotoNextPacket;
    private volatile int packet_no;
    private volatile boolean isRunning;
    private Pair<String,BluetoothDevice> current_packet;
    private volatile int attempts = 0;
    private static BlockingQueue<Pair<String,BluetoothDevice>> packet_queue = new LinkedBlockingDeque<Pair<String,BluetoothDevice>>();

    public HelperClass(){

        isRunning = true;
        gotoNextPacket = false;
        packet_no = 0;
        myBluetoothService = new MyBluetoothService(handler);
    }
    private final Handler handler = new Handler(){

        public void handleMessage(android.os.Message message){

            if(message.what == 1){
                Log.d(TAG,"Packet " + String.valueOf(packet_no) + " is successfully sent.");
                attempts = 0;
                gotoNextPacket = true;
            }
            else {
                Log.d(TAG,"Packet " + String.valueOf(packet_no) + " sending failed.");
                attempts++;
                if(attempts > 2){
                    gotoNextPacket = true;
                    attempts = 0;
                }
            }

        }
    };

    public static void insert(Pair<String,BluetoothDevice> stringBluetoothDevicePair){
        try {
            packet_queue.put(stringBluetoothDevicePair);
            Log.d(TAG,"A new packet is added to the queue, Receiver : " + stringBluetoothDevicePair.second.getName());
        }catch (Exception e){Log.d(TAG,"Error in putting packets in queue.");}
    }

    private Pair<String,BluetoothDevice> getPacket(){
        if(gotoNextPacket){
            try {
                gotoNextPacket = false;
                current_packet = packet_queue.take();
                packet_no++;
                return current_packet;
            }
            catch (Exception e){Log.d(TAG,"Error in getPacket");return null;}
        }
        else{
            return current_packet;
        }

    }
    public void run(){
        try {
            current_packet = packet_queue.take();
        }catch (Exception e){}
        while(isRunning){

            Pair<String, BluetoothDevice> packet = null;
            try {
                Log.d(TAG,"Fetching the packet.");
                packet = getPacket();
                Log.d(TAG,"Packet "+ String.valueOf(packet_no) + " is ready for sending.");
            }catch (Exception e){Log.d(TAG,"Error in retrieving packet from queue");}

            MyBluetoothService.ConnectThread connectThread = myBluetoothService.new ConnectThread(packet.second, packet.first, HELLO_UUID);
            connectThread.start();
            Log.d(TAG, "Packet " + String.valueOf(packet_no) + " is being sent with trial:" + String.valueOf(attempts) );

            try{sleep(1000);}catch (Exception e){}


        }
    }

    public void cancel(){
        isRunning = false;
        packet_queue.clear();
    }
}
