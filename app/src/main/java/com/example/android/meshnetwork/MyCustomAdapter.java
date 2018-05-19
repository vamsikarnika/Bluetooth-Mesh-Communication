package com.example.android.meshnetwork;

import android.content.Context;
import android.os.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Vamsi Karnika on 12/17/2017.
 */

public class MyCustomAdapter extends BaseAdapter {

    private ArrayList<Message> mListItems;
    private LayoutInflater mLayoutInflater;

    public MyCustomAdapter(Context context,ArrayList<Message> arrayList){

        mListItems = arrayList;

        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public int getCount(){

        return mListItems.size();
    }



    @Override

    public Object getItem(int i) {
        return null;
    }

    @Override

    public long getItemId(int i) {
        return 0;
    }

    public View getView(int position, View view, ViewGroup viewGroup){


        Message messageItem = mListItems.get(position);

        if(messageItem.getMessagetype() == 1) {
            view = mLayoutInflater.inflate(R.layout.message_area_sender, null);
        }
        else{
            view = mLayoutInflater.inflate(R.layout.message_area_receive, null);
        }


        if (messageItem != null) {

            TextView itemName = (TextView) view.findViewById(R.id.textView);

            if (itemName != null) {

                itemName.setText(messageItem.getMessage());
            }
        }

        return view;
    }
}
