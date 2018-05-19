package com.example.android.meshnetwork;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Exchanger;

import static com.example.android.meshnetwork.MainActivity.device_name;
import static com.example.android.meshnetwork.MainActivity.mesh;
import static com.example.android.meshnetwork.MainActivity.node_id;
import static java.lang.Thread.sleep;

/**
 * Created by Vamsi Karnika on 3/29/2018.
 */

public class ServerClass {

    private final String TAG = "MeshCom/ServerClass";
    public static String LinkState ;
    private static ArrayList<String> other_nodes = new ArrayList<String>();
    private static ArrayList<Integer> neighbours = new ArrayList<Integer>();
    private static String surroundings = "";
    public static HashMap< Integer,BluetoothDevice > DeviceIdMap ;
    private BluetoothAdapter mBTAdapter;
    private Context context;
    private UUID mUUID;
    private AcceptThread mAcceptThread;
    private boolean isRunning;

    public ServerClass(BluetoothAdapter BTAdapter,Context mcontext,UUID serverUUID){
        context = mcontext;
        mBTAdapter = BTAdapter;
        mUUID = serverUUID;
        DeviceIdMap= new HashMap<Integer,BluetoothDevice>();
        isRunning = true;
    }

    public void start(){
        thread.start();
    }

    public void stop(){
        isRunning = false;
    }

    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {

            while (isRunning){

                try{sleep(100);}catch (Exception e){};

                mAcceptThread = new AcceptThread();
                mAcceptThread.start();

                try {
                    mAcceptThread.join();
                }catch (Exception e){Log.d(TAG,"Error in Joining threads");}

            }
        }
    });

    public class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;
        private volatile boolean isRunning ;
        private BluetoothDevice remoteDevice ;


        public AcceptThread() {

            remoteDevice = null;
            BluetoothServerSocket tmp = null;

            isRunning = true;
            try {
                //Log.d("Android:", "Constructor");
                tmp = mBTAdapter.listenUsingRfcommWithServiceRecord(mesh, mUUID);
            } catch (Exception e) {
                Log.d(TAG, "Socket's accept() method failed.");
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "A connection is  made by a client.");
                } catch (Exception e) {
                    Log.d(TAG, "Error in Thread's run method");
                    break;
                }

                if (socket != null) {
                    manageConnectedSocket(socket);

                    try{
                        mmServerSocket.close();
                    }catch (IOException e){
                        Log.d(TAG,"Error in closing client socket.");
                    }

                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
                isRunning = false;
            } catch (IOException e) {
                Log.d(TAG,"Error in closing the socket.");
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket){

        ConnectedThread mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();


    }


    public static String getLsa(){
        String lsa = String.valueOf(node_id) + " " + device_name;
        lsa += " " + String.valueOf(neighbours.size()) + surroundings;
        for(int i = 0; i < other_nodes.size(); i++){
            lsa += '-';
            lsa += other_nodes.get(i);
        }
        return lsa;

    }

    public void cancel(){
        mAcceptThread.cancel();
        isRunning = false;
    }


    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInputStream;
        private BluetoothDevice remoteDevice;
        private byte[] mmBuffer;//to store the stream

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            remoteDevice = socket.getRemoteDevice();
            try{
                tmpIn = socket.getInputStream();
            }catch (IOException e){
                Log.e(TAG,"Error occurred when creating InputStream",e);
            }
            mmInputStream = tmpIn;
        }

        public void run(){
            mmBuffer =new byte[1024];
            int numBytes;//bytes returned from read

            while(true){

                try{
                    numBytes = mmInputStream.read(mmBuffer);
                }catch (Exception e){
                    Log.d(TAG,"Input Stream was disconnected");
                    break;
                }

                byte[] dataRecv = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    dataRecv[i] = mmBuffer[i];
                }

                String in = new String(dataRecv);
                String[] msg = in.split(" ");
                int msg_type = Integer.valueOf(msg[0]);


                Log.d(TAG, "Data Received " + in);

                if(msg_type == 0){
                    //Log.d(TAG,"Type 0 message received");
                    String receivedLsa = in.substring(2,in.length());
                    //Log.d(TAG,receivedLsa);
                    lsatask(receivedLsa,remoteDevice);
                    union(receivedLsa);
                }

                else if(msg_type == 4){
                    int destination_node = Integer.parseInt(msg[1]);;
                    int msg_starts_from = 0;
                    int second = 0;
                    for(int i = 0; i < in.length();i++){
                        if(in.charAt(i) == ' '){
                            second++;
                        }
                        if(second == 2){
                            msg_starts_from = i+1;
                            break;
                        }
                    }

                    String actualMsg = in.substring(msg_starts_from,in.length());

                    if(destination_node == node_id) {
                        Intent intent = new Intent("com.example.android.MESSAGE_RECEIVED");
                        intent.putExtra("text", actualMsg);
                        context.sendBroadcast(intent);
                    }
                    else{
                        int routeToDeviceId = SPF.getRoutenode(destination_node);
                        String formattedMsg = "4 "+ String.valueOf(routeToDeviceId) + " " + actualMsg;

                            /*MyBluetoothService.ConnectThread connectThread = myBluetoothService.new ConnectThread(DeviceIdMap.get(routeToDeviceId),formattedMsg,HELLO_UUID);
                            connectThread.start();*/
                        Pair<String,BluetoothDevice> pair = new Pair<>(formattedMsg,DeviceIdMap.get(routeToDeviceId));
                        HelperClass.insert(pair);
                    }
                }

            }
        }

        private void lsatask(String receivedlsa,BluetoothDevice device){
            String[] receivedlsanode = receivedlsa.split("-");

            String[] data = receivedlsanode[0].split(" ");

            int sender_node_id = Integer.parseInt(data[0]);
            boolean found = false;
            for(int  i = 0; i < neighbours.size();i++){
                if(sender_node_id == neighbours.get(i)) {
                    found = true;
                    break;
                }
            }
            if(!found){
                neighbours.add(sender_node_id);
                surroundings += " " + data[0] + " " + data[1] + " " + "5";
                DeviceIdMap.put(sender_node_id,device);//Mapping newly found device to id in Map
                Log.d(TAG,"Mapping " + String.valueOf(sender_node_id) + " " +device.getName());
            }
        }

        private void union(String newLsa){

            String[] nodes = newLsa.split("-");
            for(int i = 0; i < nodes.length;i++){
                String[] newnodeinfo = nodes[i].split(" ");
                boolean found = false;
                for(int j = 0; j < other_nodes.size(); j++){
                    String[] nodeinfo = other_nodes.get(j).split(" ");
                    if(newnodeinfo[0].compareTo(nodeinfo[0]) == 0){
                        found = true;
                        if(nodeinfo.length < newnodeinfo.length){
                            other_nodes.remove(j);
                            other_nodes.add(nodes[i]);
                        }
                    }
                }
                if(!found){
                    if(newnodeinfo[0].compareTo(String.valueOf(node_id)) != 0){
                        other_nodes.add(nodes[i]);
                        Intent intent = new Intent("com.example.android.LIST_UPDATED");
                        intent.putExtra("text",newnodeinfo[1]);
                        intent.putExtra("IdValue",newnodeinfo[0]);
                        context.sendBroadcast(intent);
                    }
                }
            }
        }

    }
}
