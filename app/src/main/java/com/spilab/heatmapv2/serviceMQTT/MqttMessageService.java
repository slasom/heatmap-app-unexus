package com.spilab.heatmapv2.serviceMQTT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spilab.heatmapv2.MainActivity;
import com.spilab.heatmapv2.R;
import com.spilab.heatmapv2.resource.HeatMapResource;
import com.spilab.heatmapv2.response.HeatMapResponse;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class MqttMessageService extends Service {

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    public static Boolean subscribed = false;

    private String profile ;
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();

    public MqttMessageService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        pahoMqttClient = new PahoMqttClient();


        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                if (!subscribed) {
                    subscribeTopic(getApplicationContext(), "HeatmapAPI");
                    Log.d(TAG, "Subscribed to request");
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "Service connection lost");
                subscribeTopic(getApplicationContext(), "HeatmapAPI");
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

/////
    private void executeAPI(JSONObject data) throws JSONException {
        Log.i("switch", data.getString("resource"));
        switch (data.getString("resource")) {

            case "Map":
                try {

                    HeatMapResponse mapresponse = gson.fromJson(String.valueOf(data), HeatMapResponse.class);
                    new HeatMapResource(getApplicationContext()).executeMethod(mapresponse);

                    //TODO Choose what type of notification to show (toast or notification in the bar)
                    showNotification("Resource Execution: " + mapresponse.getResource(), " Method: " + mapresponse.getMethod());

                } catch (Exception e) {
                    Log.e("Error MapResponse", e.getMessage());
                }

                break;


        }
    }


    private void showNotification(String title, String body) {

        //Intent to open APP when click in the notification.
        Intent resultIntent = new Intent(this, MainActivity.class);
        android.support.v4.app.TaskStackBuilder stackBuilder = android.support.v4.app.TaskStackBuilder.create(this);
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

    private void sendProfile() {
        //Log.d(TAG," - Sending..."+profile);
        if (profile.equals("")) {
            Toast.makeText(this, "The profile is empty. Nothing to send.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, " - The profile is empty. Nothing to send.");
        } else {
            Log.d(TAG, " - REQ: Sending profile to " + Constants.MQTT_BROKER_URL);
            try {
                //Constants.MQTT_BROKER_URL = "tcp://" + txtServerIp.getText().toString() + ":1883";
                //Log.d(TAG, "IP=" + Constants.MQTT_BROKER_URL);
                pahoMqttClient.publishMessage(mqttAndroidClient, profile, 1, Constants.PUBLISH_TOPIC);

//                Intent intent = new Intent(ctx, MqttMessageService.class);
//                ctx.startService(intent);

                //Toast.makeText(ctx, "Profile sent successfully.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, " - REQ: Profile sent successfully.");
            } catch (Exception e) {
                //Toast.makeText(ctx, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, " - REQ: Error sending the profile: " + e.getMessage());
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

    private String getMacAddressUpdated() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }
}
