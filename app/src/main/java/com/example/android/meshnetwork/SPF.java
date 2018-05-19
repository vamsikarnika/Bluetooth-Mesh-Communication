package com.example.android.meshnetwork;

import android.util.Log;

import static com.example.android.meshnetwork.ServerClass.DeviceIdMap;

/**
 * Created by Vamsi Karnika on 1/5/2018.
 */

public class SPF extends  Thread {
    private int SPA_INTERVAL ;
    private static int MAX_VALUE = 1000;
    private static int num_nodes = 100;
    private int node_id = MainActivity.node_id;
    private boolean running ;
    private static final String TAG = "SPF";
    private static int route[] = new int[num_nodes];

    private int network[][] = new int[num_nodes][num_nodes];
    public SPF(int sPA_INTERVAL){
        SPA_INTERVAL = sPA_INTERVAL;
        running = true;
    }
    public void run(){

        while(running){
            try{
                sleep(SPA_INTERVAL*1000);
            }catch (Exception e){
                Log.d(TAG,"Error in Sleep.");
            }

            for(int i = 0; i < num_nodes;i++){
                for(int j = 0; j < num_nodes;j++){
                    network[i][j] = MAX_VALUE;
                }
            }
            String topology = ServerClass.getLsa();
            Log.d(TAG,topology);

            String[] Availablenodes = topology.split("-");

            for(int i = 0; i < Availablenodes.length;i++){
                String[] data = Availablenodes[i].split(" ");
                int u = Integer.parseInt(data[0]);
                int edges = Integer.parseInt(data[2]);
                for(int j = 0 ;j < edges;j++){
                    int v = Integer.parseInt(data[3*j+3]);
                    int cost = Integer.parseInt(data[3*j+5]);
                    network[u][v] = cost;
                    network[v][u] = cost;
                }
            }

            dijkstra(network);

            //clear the graph
            for(int i = 0; i < 100;i++){
                for(int j = 0; j < 100;j++){
                    network[i][j] = 0;
                }
            }
        }
    }
    public int min_ind(int dist[],Boolean [] fin_nodes){
        int min_index=-1;int min=MAX_VALUE;
        for(int i=0;i<num_nodes;i++){
            if(fin_nodes[i]==false && dist[i]<=min){
                min=dist[i];
                min_index=i;
            }
        }
        return min_index;
    }

    public void dijkstra(int[][] graph){
        int[]dist=new int[num_nodes];
        Boolean[] sptSet=new Boolean[num_nodes];
        int [] parent=new int[num_nodes+10];

        for(int i=0;i<num_nodes;i++){
            dist[i]= MAX_VALUE; sptSet[i]=false;
        }

        parent[node_id]=-1;
        dist[node_id]= 0;
        int u=node_id;

        while(u!=-1)
        {

            sptSet[u]=true;
            for(int v=0;v<num_nodes;v++){
                if(!sptSet[v]){
                    if(graph[u][v]!=MAX_VALUE){
                        if(dist[u]!=MAX_VALUE){
                            if(dist[u]+graph[u][v]<dist[v]){
                                dist[v]=dist[u]+graph[u][v];
                                parent[v]=u;
                            }
                        }
                    }
                }
            }
            u=min_ind(dist,sptSet);

        }


        for(int i = 0; i < num_nodes;i++){
            if(dist[i] != MAX_VALUE  && i!= node_id){
                int v = i;

                while(parent[v] != node_id){
                    v = parent[v];
                }

                route[i] = v;
                //Log.d(TAG,String.valueOf(i) + " " + String.valueOf(DeviceIdMap.get(v).getName()));
            }
        }
    }

    public static int getRoutenode(int v){
        return route[v];
    }



    public void cancel()
    {
        running = false;
    }

}
