package com.example.android.meshnetwork;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import java.util.Random;
import static com.example.android.meshnetwork.MainActivity.HELLO_UUID;

/**
 * Created by Vamsi Karnika on 1/23/2018.
 */

public class LSA extends Thread{

    private volatile boolean isRunning ;
    private static final String TAG = "LSA";

    public LSA(){
        isRunning = true;
    }

    public void run(){

        while(isRunning){

            try{
                int sleeptime = randomint(8,4);
                sleep(sleeptime*1000);
            }catch(Exception e){}

            int neighbours = MainActivity.AvailableDevices.size();
            String sendData = "0 " + ServerClass.getLsa();
            for(int i = 0; i < neighbours;i++) {

                /*MyBluetoothService.ConnectThread connection = myBluetoothService.new ConnectThread(MainActivity.AvailableDevices.get(i), sendData, HELLO_UUID);
                connection.start();*/

                Pair<String,BluetoothDevice> pair = new Pair<>(sendData,MainActivity.AvailableDevices.get(i));
                HelperClass.insert(pair);
                try{sleep(50);
                } catch (Exception e){}
            }
        }
    }

    public int randomint(int max,int min){
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public void cancel() {
        isRunning = false;
    }


}
