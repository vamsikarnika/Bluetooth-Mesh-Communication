package com.example.android.meshnetwork;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.UUID;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;

public class MyBluetoothService {
    private static final String TAG = "MyBluetoothService";
    private final Handler mHandler; // handler that gets info from Bluetooth service

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private static final UUID MY_UUID = new UUID(11111111, 00000000);

    BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();

    String msgservice = "MeshNetwork";

    public MyBluetoothService(Handler handler){

        mHandler = handler;

    }


    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
        public static final int FAILED_CONNECTION = 3;

        // ... (Add other message types here as needed.)
    }

    public class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {

            BluetoothServerSocket tmp = null;

            try {

                tmp = mBTAdapter.listenUsingRfcommWithServiceRecord(msgservice, MY_UUID);

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

                    Log.d(TAG, "A connection is made by a client.");
                } catch (Exception e) {
                    Log.d(TAG, "Error in Thread's run method");
                    break;
                }

                if (socket != null) {
                    try {

                        ConnectedThread connectedThread = new ConnectedThread(socket);

                        connectedThread.start();

                        mmServerSocket.close();

                        break;
                    } catch (IOException e) {

                        Log.d(TAG, "Error in closing the socket.");

                    }
                }
            }
        }
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG,"Error in closing the socket.");
            }
        }
    }

    public class ConnectThread extends Thread{

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String textmessage;

        public ConnectThread(BluetoothDevice device,String message,UUID clientUUID){
            BluetoothSocket tmp = null;
            mmDevice = device;
            textmessage = message;
            try{
                tmp = device.createRfcommSocketToServiceRecord(clientUUID);
            }catch(IOException e){
                Log.d(TAG,"Error in ConnectThread Constructor.");
            }
            mmSocket = tmp;
        }

        public void run(){
            mBTAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.

                mmSocket.connect();
                Log.d(TAG,"Phone is Connected to " + mmDevice.getName());

                ConnectedThread connectedThread = new ConnectedThread(mmSocket);
                connectedThread.write(textmessage.getBytes());

                try {
                    sleep(100);
                }catch (Exception e){
                    Log.d(TAG,"Error in Sleep");
                }

                mmSocket.close();


            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Message errorMsg = mHandler.obtainMessage(MessageConstants.FAILED_CONNECTION);
                errorMsg.sendToTarget();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.d(TAG,"Error in Connect Thread's run method.");
                }
                Log.d(TAG,"Phone cannot be Connected to " + mmDevice.getName());
                return;
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d(TAG,"Error in closing the socket in cancel method.");
            }
        }
    }

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {

                Log.e(TAG, "Error occurred when creating input stream", e);

            }

            try {

                tmpOut = socket.getOutputStream();

            } catch (IOException e) {

                Log.e(TAG, "Error occurred when creating output stream", e);

            }

            mmInStream = tmpIn;

            mmOutStream = tmpOut;

        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    Log.d(TAG, "Server is Waiting for messages.");

                    numBytes = mmInStream.read(mmBuffer);

                    byte[] dataRecv = new byte[numBytes];

                    for (int i = 0; i < numBytes; i++) {

                        dataRecv[i] = mmBuffer[i];

                    }

                    Log.d(TAG, "Server received the message.");

                    String in = new String(dataRecv);

                    Log.d(TAG, "Data Received " + in);

                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, -1, -1, in);

                    readMsg.sendToTarget();

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Log.d(TAG, "Message Sent Successfully.");
                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, new String(bytes));
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message errorMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                errorMsg.sendToTarget();
            }
        }

        // Call this method from the main activity to shut down the connection.
        //
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}