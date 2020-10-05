package com.spilab.heatmapv3.serviceMQTT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spilab.heatmapv3.MainActivity;
import com.spilab.heatmapv3.R;
import com.spilab.heatmapv3.resource.DeviceResource;
import com.spilab.heatmapv3.resource.HeatMapResource;
import com.spilab.heatmapv3.response.DeviceResponse;
import com.spilab.heatmapv3.response.HeatMapResponse;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;


public class MqttMessageService extends Service {

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    public static Boolean subscribed = false;
    private static Boolean connectionLost = false;

    //Client ID
    private AdvertisingIdClient.Info mInfo;

    private String profile ;
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
    private static final String CARPETA_RAIZ = "HeatmapV3/";
    public MqttMessageService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mInfo = null;
        //GetAdvertisingID
       // new GetAdvertisingID().execute();

        configureMQTT();

    }

    private void configureMQTT(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        pahoMqttClient = new PahoMqttClient();


        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), MQTTConfiguration.MQTT_BROKER_URL, UUID.randomUUID().toString());
        //Log.e("IDDDDD: ", UUID.randomUUID().toString());

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                if (!subscribed || connectionLost) {
                    subscribeTopic(getApplicationContext(), "HeatmapAPI");
                    Log.d(TAG, "Subscribed to request");
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "Service connection lost");
                connectionLost = true;
                //subscribeTopic(getApplicationContext(), "HeatmapAPI");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                Log.d(TAG, " - Message!!");
                //Log.d(TAG, "Prof=" + profile);

                //setMessageNotification(s, new String(mqttMessage.getPayload()));

                // Parse message
                String msg = new String(mqttMessage.getPayload());
                JSONObject json = new JSONObject(msg);

                //Log.d(TAG, json.toString());

                // Check if I have to send the profile
//                JSONObject jsonProfile = new JSONObject(profile);
//                String myName = jsonProfile.getString("hasName");
//                String reqName = json.getString("hasName");
                Log.i("Message received: ", json.toString());
                executeAPI(json);



            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }


    private class GetAdvertisingID extends AsyncTask<Void, Void, AdvertisingIdClient.Info> {

        @Override
        protected AdvertisingIdClient.Info doInBackground(Void... voids) {
            AdvertisingIdClient.Info info = null;
            try {
                info = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            }
            return info;
        }

        @Override
        protected void onPostExecute(AdvertisingIdClient.Info info) {
            mInfo = info;

            configureMQTT();
        }
    }

/////
    private void executeAPI(JSONObject data) throws JSONException {
        Log.i("switch", data.getString("resource"));
        long startTime = System.currentTimeMillis();

        switch (data.getString("resource")) {

            case "Map":
                try {



                    HeatMapResponse mapresponse = gson.fromJson(String.valueOf(data), HeatMapResponse.class);
                    new HeatMapResource(getApplicationContext()).executeMethod(mapresponse);

                    long difference = System.currentTimeMillis() - startTime;
                    writeFileExternalStorageComplete(mapresponse.getIdRequest(),mapresponse.getMethod(),difference);

                    //TODO Choose what type of notification to show (toast or notification in the bar)
                    //showNotification("Resource Execution: " + mapresponse.getResource(), " Method: " + mapresponse.getMethod());

                } catch (Exception e) {
                    Log.e("Error MapResponse", e.getMessage());
                }

                break;
            case "Device":
                try {


                    DeviceResponse deviceresponse = gson.fromJson(String.valueOf(data), DeviceResponse.class);

                    new DeviceResource(getApplicationContext()).executeMethod(deviceresponse);

                    long difference = System.currentTimeMillis() - startTime;



                    Log.d("TIME Execution: ", String.valueOf(difference) +" ms");
                    //TODO Choose what type of notification to show (toast or notification in the bar)
                    //showNotification("Resource Execution: " + deviceresponse.getResource(), " Method: " + deviceresponse.getMethod());

                } catch (Exception e) {
                    Log.e("Error MapResponse", e.getMessage());
                }

                break;


        }
    }


    private void writeFileExternalStorageComplete(String id, String method, long time) {

//        try {
            //File myExternalFile = new File(Environment.getExternalStorageDirectory(), CARPETA_RAIZ);

//            if (!myExternalFile.exists())
//                myExternalFile.mkdir();

            String request= id+","+ Calendar.getInstance().getTime()+","+method+","+time+"\n";

            Log.d("HeatmapLog: ", request);
//            Writer output;
//            output = new BufferedWriter(new FileWriter(myExternalFile+ File.separator + "request.txt",true));  //clears file every time
//            output.append(request);
//            output.close();
//
//            Log.d(TAG, " - File WRITED successfully");

//        } catch (IOException e) {
//            Log.d(TAG, " - Error LOADED file");
//            e.printStackTrace();
//        }

    }

    private void  writeFileExternalStorageIncomplete(String id, String method){
        try {
            File myExternalFile = new File(Environment.getExternalStorageDirectory(), CARPETA_RAIZ);

            if (!myExternalFile.exists())
                myExternalFile.mkdir();

            String request= id+","+ Calendar.getInstance().getTime()+","+method+"\n";


            Writer output;
            output = new BufferedWriter(new FileWriter(myExternalFile+ File.separator + "request-incomplete.csv",true));  //clears file every time
            output.append(request);
            output.close();

            Log.d(TAG, " - File WRITED successfully");

        } catch (IOException e) {
            Log.d(TAG, " - Error LOADED file");
            e.printStackTrace();
        }
    }

    private void showNotification(String title, String body) {

        //Intent to open APP when click in the notification.
        Intent resultIntent = new Intent(this, MainActivity.class);
        androidx.core.app.TaskStackBuilder stackBuilder = androidx.core.app.TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "1";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification", NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.setDescription("Android Server");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true).setAutoCancel(true).setDefaults(Notification.DEFAULT_ALL).setContentIntent(resultPendingIntent).setWhen(System.currentTimeMillis()).setSmallIcon(R.drawable.common_full_open_on_phone).setContentTitle(title).setContentText(body);
        notificationManager.notify((new Random().nextInt()), notificationBuilder.build());

    }

    /////

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "es.unex.politecnica.spilab.csprofile";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle(this.getString(R.string.app_name))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        Bundle extras = intent.getExtras();
        if (extras != null)
            profile = extras.getString("profile");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    private void setMessageNotification(@NonNull String topic, @NonNull String msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(topic)
                        .setContentText(msg);
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(100, mBuilder.build());
    }

    private void subscribeTopic(Context ctx, String topic) {
        if (!topic.isEmpty()) {
            try {
                pahoMqttClient.subscribe(mqttAndroidClient, topic, 1);

                Toast.makeText(ctx, "Subscribed to: " + topic, Toast.LENGTH_SHORT).show();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }


    private void readFileExternalStorage(File myExternalFile) {
        try {
            //File myExternalFile = new File(getExternalFilesDir("data"), "profile.json");
            FileInputStream iStream = new FileInputStream(myExternalFile + "/profile.json");

            //InputStream iStream = getApplicationContext().getAssets().open("profile.json");
            byte[] buffer = new byte[iStream.available()];
            iStream.read(buffer);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byteStream.write(buffer);
            byteStream.close();
            iStream.close();
            profile = byteStream.toString();
            Log.d(TAG, " - File LOADED successfully: " + profile);
            //Toast.makeText(MainActivity.this," - Profile LOADED successfully").show();
        } catch (IOException e) {
            Log.d(TAG, " - Error LOADING profile");
            //writeFileExternalStorage(myExternalFile);
            //readFileExternalStorage(myExternalFile);
            e.printStackTrace();
        }
    }


}
