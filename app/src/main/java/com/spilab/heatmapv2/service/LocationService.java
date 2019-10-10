package com.spilab.heatmapv2.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spilab.heatmapv2.database.LocationBeanRealm;
import com.spilab.heatmapv2.database.LocationBeanRealmModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class LocationService /*extends Service*/ {

//    private FusedLocationProviderClient mFusedLocationProviderClient;
//
//    private LatLng miPosicion = new LatLng(0, 0);
//
//    private static final String TAG = "Location";
//
//
//    private static final String CARPETA_RAIZ = "HeatmapV3/";
//
//    private Timer timerGetLocation;
//
//    private long MILISECONDS_REFRESH = 300000; //5 min
//
//    private boolean startRealm;
//
//    private Gson gson;


    public LocationService() {

    }

//    @Override
//    public void onCreate() {
//        super.onCreate();
//        timerGetLocation = new Timer();
//        this.startRealm = false;
//
//        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
//
//        getDeviceLocation();
//
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
//            startMyOwnForeground();
//        else
//            startForeground(1, new Notification());
//    }
//
//
//    private void getDeviceLocation() {
//        Log.d(TAG, "getDeviceLocation: getting the devices current location");
//        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//
//        try {
//            final Task location = mFusedLocationProviderClient.getLastLocation();
//            location.addOnCompleteListener(new OnCompleteListener() {
//                @Override
//                public void onComplete(@NonNull Task task) {
//                    if (task.isSuccessful()) {
//                        Log.d(TAG, "onComplete: found location!");
//                        Location currentLocation = (Location) task.getResult();
//
//                        miPosicion = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//                        //TODO: Cambiar para que vuelva a guardar las localizaciones
//                        //postGPSPosition();
//
//                        //TODO: Descomentar para que vuelva a dibujarse el mapa de calor
//                        //SEND TO MAINACTIVITY
//                        Intent intentProfile = new Intent();
//                        intentProfile.putExtra("location", miPosicion);
//                        intentProfile.setAction("LOCATION");
//                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentProfile);
//
//
//                        Log.e("Location", miPosicion.toString());
//                    } else {
//                        Log.e(TAG, "onComplete: current location is null");
//                        Toast.makeText(LocationService.this, "unable to get current location", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
//
//        } catch (SecurityException e) {
//            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
//        }
//    }
//
//
//    public void postGPSPosition() {
//
//        // To start Realm once
//        if (!this.startRealm) {
//            Log.i("HEATMAP-INIT", "Starting Realm...");
//            try {
//                Realm.init(this);
//                RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
//                        .modules(new LocationBeanRealmModule())
//                        .name("Database.realm")
//                        .deleteRealmIfMigrationNeeded()
//                        .build();
//                Realm.setDefaultConfiguration(realmConfiguration);
//                this.startRealm = true;
//                Log.i("HEATMAP-INIT", "Realm started successfully");
//            } catch (Exception e) {
//                Log.e("HEATMAP-INIT", "Error during start Realm: " + e.getMessage());
//            }
//
//        }
//
//
//        // Store the location in the Realm database
//        Realm realm = Realm.getDefaultInstance();
//        realm.beginTransaction();
//
//        LocationBeanRealm lbr = realm.createObject(LocationBeanRealm.class);
//        lbr.setLat(miPosicion.latitude);
//        lbr.setLng(miPosicion.longitude);
//        Date curDateTime = Calendar.getInstance().getTime();
//        lbr.setTimestamp(curDateTime);
//
//        realm.commitTransaction();
//
//        Log.i("HEATMAP-LBR", lbr.toString());
//
//
//    }
//
//
//    private void writeFileExternalStorage() {
//
//        try {
//            File myExternalFile = new File(Environment.getExternalStorageDirectory(), CARPETA_RAIZ);
//
//            if (!myExternalFile.exists())
//                myExternalFile.mkdir();
//
//            FileOutputStream fos = new FileOutputStream(myExternalFile.getPath() + File.separator + "locations.json");
//
//            LocationBeanRealm lbr = new LocationBeanRealm(miPosicion.latitude,miPosicion.longitude, Calendar.getInstance().getTime());
//
//            fos.write(gson.toJson(lbr).getBytes());
//            fos.close();
//            Log.d(TAG, " - File WRITED successfully");
//        } catch (IOException e) {
//            Log.d(TAG, " - Error LOADED file");
//            e.printStackTrace();
//        }
//
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private void startMyOwnForeground() {
//        String NOTIFICATION_CHANNEL_ID = "example.permanence";
//        String channelName = "Background Service";
//        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
//        chan.setLightColor(Color.BLUE);
//        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//
//        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        assert manager != null;
//        manager.createNotificationChannel(chan);
//
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
//        Notification notification = notificationBuilder.setOngoing(true)
//                .setContentTitle("App is running in background")
//                .setPriority(NotificationManager.IMPORTANCE_MIN)
//                .setCategory(Notification.CATEGORY_SERVICE)
//                .build();
//        startForeground(2, notification);
//    }
//
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        super.onStartCommand(intent, flags, startId);
//
//
//        timerGetLocation.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                getDeviceLocation();
//                //TODO: Rewrite method to obtain from realm
//                //writeFileExternalStorage();
//            }
//        }, 0, MILISECONDS_REFRESH);
//
//
//        //startTimer();
//        return START_STICKY;
//    }
//
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction("restartservice");
//        broadcastIntent.setClass(this, Restarted.class);
//        this.sendBroadcast(broadcastIntent);
//    }
//
//
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
}
