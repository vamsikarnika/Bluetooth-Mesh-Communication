package com.example.android.meshnetwork;

/**
 * Created by Vamsi Karnika on 12/17/2017.
 */

public class Message {

    String message;

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    long createdAt;

    int messagetype;

    Message(String text,int type){
        message = text;
        messagetype = type;
    }

    Message(String text,int type,long time){
        message = text;
        messagetype = type;
        createdAt = time;
    }

    public void setMessage(String text){
        message = text;
    }
    public void setType(int type){
        messagetype = type;
    }

    public int getMessagetype(){
        if(messagetype == 1)    return VIEW_TYPE_MESSAGE_SENT;
        return VIEW_TYPE_MESSAGE_RECEIVED;
    }
    public String getMessage(){
        return message;
    }
}
