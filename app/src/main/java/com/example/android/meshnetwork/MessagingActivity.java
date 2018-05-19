package com.example.android.meshnetwork;

import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.example.android.meshnetwork.ServerClass.DeviceIdMap;
import static com.example.android.meshnetwork.MainActivity.HELLO_UUID;


/**
 * Created by Vamsi Karnika on 2/12/2018.
 */


public class MessagingActivity extends Activity {

    private String ReceiverDevice;
    private final static int MESSAGE_SENT = 1;
    private final static int MESSAGE_RECEIVED = 2;
    private String receiverDeviceId;
    private static ArrayList<Message> MessageList;
    private MyCustomAdapter myCustomMessagesAdapter;
    private ListView mListView;
    private static final String TAG = "MessagingActivity";

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messaging_layout);

        ReceiverDevice = getIntent().getStringExtra("DeviceName");
        receiverDeviceId = getIntent().getStringExtra("DeviceId");

        TextView Receiver_Name = (TextView)findViewById(R.id.receiver_device);
        Receiver_Name.setTextSize(20);
        Receiver_Name.setText(ReceiverDevice);


        MessageList = new ArrayList<Message>();
        mListView = (ListView)findViewById(R.id.MessageList);
        myCustomMessagesAdapter = new MyCustomAdapter(this,MessageList);

        mListView.setAdapter(myCustomMessagesAdapter);

        IntentFilter iFilter = new IntentFilter("com.example.android.MESSAGE_RECEIVED");
        registerReceiver(MessageListener,iFilter);

    }

    private final BroadcastReceiver MessageListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("com.example.android.MESSAGE_RECEIVED")){

                String textMessage = intent.getExtras().getString("text");

                Log.d("Android:",textMessage);

                Message receivedMessage = new Message(textMessage,MESSAGE_RECEIVED);

                MessageList.add(receivedMessage);

                myCustomMessagesAdapter.notifyDataSetChanged();
            }
        }
    };
    private final Handler handler = new Handler(){

        public void handleMessage(android.os.Message msg){

            String text = (String)msg.obj;
            String actualText = text.substring(2,text.length());
            /*if(msg.what == 1){
                Message sendMsg = new Message(actualText,1);
                MessageList.add(sendMsg);
                myCustomMessagesAdapter.notifyDataSetChanged();
            }*/
            if(msg.what == 2){

                Toast.makeText(getBaseContext(),text, Toast.LENGTH_LONG).show();
            }
            /*else{
                Log.d("Android:","Message type not found.");
            }*/
        }
    };
    public void sendMessage(View v){

        int routeToDeviceId = SPF.getRoutenode(Integer.parseInt(receiverDeviceId));
        Log.d(TAG,String.valueOf(routeToDeviceId));

        if(ReceiverDevice != null){
            EditText edittext = (EditText) findViewById(R.id.textmesaage);
            String actualMsg = edittext.getText().toString();


            if(actualMsg != ""){
                String formattedMsg = "4 " + String.valueOf(receiverDeviceId) + " " +  actualMsg;

                /*MyBluetoothService.ConnectThread connectThread = myBluetoothService.new ConnectThread(DeviceIdMap.get(routeToDeviceId),formattedMsg,HELLO_UUID);
                connectThread.start();*/

                Pair<String,BluetoothDevice> packet = new Pair<>(formattedMsg,DeviceIdMap.get(routeToDeviceId));
                Log.d(TAG,String.valueOf(routeToDeviceId));
                HelperClass.insert(packet);

                Message sentMsg = new Message(actualMsg,MESSAGE_SENT);
                MessageList.add(sentMsg);
                myCustomMessagesAdapter.notifyDataSetChanged();
                edittext.setText("");
            }
        }

    }

    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(MessageListener);
    }
}
