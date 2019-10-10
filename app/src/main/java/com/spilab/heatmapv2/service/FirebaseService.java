package com.spilab.heatmapv2.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spilab.heatmapv2.MainActivity;
import com.spilab.heatmapv2.R;
import com.spilab.heatmapv2.resource.DeviceResource;
import com.spilab.heatmapv2.resource.HeatMapResource;
import com.spilab.heatmapv2.response.DeviceResponse;
import com.spilab.heatmapv2.response.HeatMapResponse;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Map;
import java.util.Random;


public class FirebaseService extends FirebaseMessagingService {
    private static final String TAG = "Location";


    private static final String CARPETA_RAIZ = "HeatmapV3/";

    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();

    public FirebaseService() {
    }


    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.d("TOKEN FIREBASE", s);
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...
        String TAG = "FirebaseService: ";
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {

            Map<String, String> data = remoteMessage.getData();
            executeAPI(data);

        }

    }

    private void executeAPI(Map<String, String> data) {

        switch (data.get("resource")) {

            case "Map":
                try {
                    long startTime = System.currentTimeMillis();

                    HeatMapResponse mapresponse = gson.fromJson(String.valueOf(data), HeatMapResponse.class);


                    new HeatMapResource(getApplicationContext()).executeMethod(mapresponse);

                    long difference = System.currentTimeMillis() - startTime;

                    writeFileExternalStorage(mapresponse.getIdRequest(),mapresponse.getMethod(),difference);


                    Log.d("TIME Execution: ", String.valueOf(difference) +" ms");
                    //TODO Choose what type of notification to show (toast or notification in the bar)
                    //showNotification("Resource Execution: " + mapresponse.getResource(), " Method: " + mapresponse.getMethod());

                } catch (Exception e) {
                    Log.e("Error MapResponse", e.getMessage());
                }

                break;
            case "Device":
                try {
                    long startTime = System.currentTimeMillis();

                    DeviceResponse deviceresponse = gson.fromJson(String.valueOf(data), DeviceResponse.class);

                    //SEND TO MAINACTIVITY
                    Intent intentProfile = new Intent();
                    intentProfile.putExtra("timeout", deviceresponse.getParams().gettimeout());
                    intentProfile.setAction("RESTART");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentProfile);

                    //new DeviceResource(getApplicationContext()).executeMethod(deviceresponse);

                    long difference = System.currentTimeMillis() - startTime;

                    writeFileExternalStorage(deviceresponse.getIdRequest(),deviceresponse.getMethod(),difference);


                    Log.d("TIME Execution: ", String.valueOf(difference) +" ms");
                    //TODO Choose what type of notification to show (toast or notification in the bar)
                    //showNotification("Resource Execution: " + mapresponse.getResource(), " Method: " + mapresponse.getMethod());

                } catch (Exception e) {
                    Log.e("Error MapResponse", e.getMessage());
                }

                break;


        }
    }

    private void writeFileExternalStorage(String id, String method, long time) {

        try {
            File myExternalFile = new File(Environment.getExternalStorageDirectory(), CARPETA_RAIZ);

            if (!myExternalFile.exists())
                myExternalFile.mkdir();

            String request= id+","+ Calendar.getInstance().getTime()+","+method+","+time+"\n";


            Writer output;
            output = new BufferedWriter(new FileWriter(myExternalFile+ File.separator + "request.csv",true));  //clears file every time
            output.append(request);
            output.close();

            Log.d(TAG, " - File WRITED successfully");

        } catch (IOException e) {
            Log.d(TAG, " - Error LOADED file");
            e.printStackTrace();
        }

    }


//    private void showNotification(String title, String body) {
//
//        //Intent to open APP when click in the notification.
//        Intent resultIntent = new Intent(this, MainActivity.class);
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addNextIntentWithParentStack(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        String NOTIFICATION_CHANNEL_ID = "1";
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification", NotificationManager.IMPORTANCE_DEFAULT);
//
//            notificationChannel.setDescription("Android Server");
//            notificationChannel.enableLights(true);
//            notificationChannel.setLightColor(Color.BLUE);
//            notificationManager.createNotificationChannel(notificationChannel);
//        }
//
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
//        notificationBuilder.setAutoCancel(true).setAutoCancel(true).setDefaults(Notification.DEFAULT_ALL).setContentIntent(resultPendingIntent).setWhen(System.currentTimeMillis()).setSmallIcon(R.drawable.common_full_open_on_phone).setContentTitle(title).setContentText(body);
//        notificationManager.notify((new Random().nextInt()), notificationBuilder.build());
//
//    }


}