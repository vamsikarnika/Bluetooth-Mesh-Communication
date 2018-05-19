package com.example.android.meshnetwork;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.HashMap;

import static com.example.android.meshnetwork.MainActivity.HELLO_UUID;
import static com.example.android.meshnetwork.MainActivity.mesh;

/**
 * Created by Vamsi Karnika on 1/2/2018.
 */


public class AcceptThread extends Thread {

    private final BluetoothServerSocket mmServerSocket;
    private Context context;
    //public static ArrayList<Integer> neighbours;
    private BluetoothAdapter mBTAdapter;
    private final String TAG = "MeshCom/AcceptThread";
    private volatile boolean isRunning ;
    public static String LinkState ;
    private static ArrayList<String> other_nodes = new ArrayList<String>();
    private static ArrayList<Integer> neighbours = new ArrayList<Integer>();
    private static String surroundings = "";
    public static int current_node_id = 1;
    private static String device_name ;
    private BluetoothDevice remoteDevice ;
    public static HashMap< Integer,BluetoothDevice > DeviceIdMap ;

    public AcceptThread(BluetoothAdapter BTAdapter, Context mContext, UUID serverUUID) {

        remoteDevice = null;
        BluetoothServerSocket tmp = null;
        DeviceIdMap= new HashMap<Integer,BluetoothDevice>();
        context = mContext;
        mBTAdapter = BTAdapter;

        setNodeName();
        setNodeID();

        isRunning = true;
        try {
            //Log.d("Android:", "Constructor");
            tmp = mBTAdapter.listenUsingRfcommWithServiceRecord(mesh, serverUUID);

        } catch (Exception e) {
            Log.d("Android", "Socket's accept() method failed.");
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;

        while (isRunning) {
            try {
                sleep(100);
                synchronized (this) {
                    socket = mmServerSocket.accept();
                }
                Log.d("Android:", "A connection is  made by a client.");
            } catch (Exception e) {

                Log.d("Android:", "Error in Thread's run method");
                continue;

                //break;
            }

            if (socket != null) {
                InputStream tmpIn = null;
                byte[] mmBuffer;

                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {

                    Log.e(TAG, "Error occurred when creating input stream", e);
                    continue;
                }

                InputStream mmInstream = tmpIn;
                mmBuffer = new byte[1024];
                int numBytes;

                remoteDevice = socket.getRemoteDevice();
                while(true){
                    try{
                        numBytes = mmInstream.read(mmBuffer);
                    }catch (Exception e){Log.d("Android","Input Stream was disconnected");break;}

                    byte[] dataRecv = new byte[numBytes];
                    for (int i = 0; i < numBytes; i++) {
                        dataRecv[i] = mmBuffer[i];
                    }

                    String in = new String(dataRecv);
                    String[] msg = in.split(" ");
                    int msg_type = Integer.valueOf(msg[0]);


                    Log.d("Android:", "Data Received " + in);

                    if(msg_type == 0){
                        Log.d(TAG,"Type 0 message received");
                        String receivedLsa = in.substring(2,in.length());
                        Log.d(TAG,receivedLsa);
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

                        if(destination_node == current_node_id) {
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
                try{
                    socket.close();
                }catch (IOException e){Log.d("Android::","Error in closing client socket.");}
            }
        }
    }

    public static String getLsa(){
        String lsa = String.valueOf(current_node_id) + " " + device_name;
        lsa += " " + String.valueOf(neighbours.size()) + surroundings;
        for(int i = 0; i < other_nodes.size(); i++){
            lsa += '-';
            lsa += other_nodes.get(i);
        }
        return lsa;

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
            DeviceIdMap.put(sender_node_id,device);
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
                if(newnodeinfo[0].compareTo(String.valueOf(current_node_id)) != 0){
                    other_nodes.add(nodes[i]);
                    Intent intent = new Intent("com.example.android.LIST_UPDATED");
                    intent.putExtra("text",newnodeinfo[1]);
                    intent.putExtra("IdValue",newnodeinfo[0]);
                    context.sendBroadcast(intent);
                }
            }
        }
    }



    private void setNodeID(){
        String hardwareAddress = mBTAdapter.getAddress();
        int generatedid = hardwareAddress.charAt(hardwareAddress.length()-1)*hardwareAddress.charAt(hardwareAddress.length()-2);
        generatedid = generatedid%100;
        current_node_id = generatedid;
        Log.d("NodeID:",String.valueOf(current_node_id));
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

    public void cancel() {
        try {
            mmServerSocket.close();
            isRunning = false;
        } catch (IOException e) {
            Log.d("Android:","Error in closing the socket.");
        }
    }


}
