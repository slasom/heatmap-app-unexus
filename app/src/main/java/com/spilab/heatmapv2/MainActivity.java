package com.spilab.heatmapv2;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.spilab.heatmapv2.database.LocationBeanRealm;
import com.spilab.heatmapv2.database.LocationBeanRealmModule;
import com.spilab.heatmapv2.locationmanager.LocationManager;
import com.spilab.heatmapv2.model.LocationFrequency;
import com.spilab.heatmapv2.service.LocationService;
import com.spilab.heatmapv2.serviceMQTT.Constants;
import com.spilab.heatmapv2.serviceMQTT.MqttMessageService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import io.realm.Realm;
import io.realm.RealmConfiguration;


public class MainActivity extends AppCompatActivity /*implements OnMapReadyCallback*/ {


    private LatLng miPosicion = new LatLng(0, 0);
    private Date currentTime;
    private GoogleMap mMap;
    private boolean flag=false;

    private TileOverlay tileOverlay;

    private static final String TAG = "Location";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 4321;

    private Boolean mLocationPermissionsGranted = false;
    private Boolean storagePermissionsGranted = false;

    private TextView textPosicion;
    private TextView textDateTime;
    private Button buttonGetLocations;


    private static final String CARPETA_RAIZ = "HeatmapV2/";

    private Intent locationIntent;

    Intent mServiceIntent;
    private LocationService service;

    private Gson gson;

    private static String serverIp;

    private boolean startRealm=false;

    private Button restart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        textPosicion = (TextView) findViewById(R.id.textPosicion);
//        textDateTime = (TextView) findViewById(R.id.textDateTime);
//        buttonGetLocations = (Button) findViewById(R.id.buttonGet);

//        buttonGetLocations.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getAllLocations();
//            }
//        });

        tileOverlay = null;
        //Location Service
//        service = new LocationService();
//        mServiceIntent = new Intent(this, service.getClass());


        getLocationPermission();
        getStoragePermission();

        getTokenFirebase();
        subscribeTopicFirebase("HeatmapAPI");

        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
        restart= findViewById(R.id.button);

        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartAPP((double) 100);
            }
        });


        /////// PARSE LOCALIZACIONES ESTATICAS //////////////
        ArrayList<LocationBeanRealm> localizaciones = new ArrayList<>();
        String result=loadJSONFromAsset();
        localizaciones= gson.fromJson(result,new TypeToken<List<LocationBeanRealm>>(){}.getType());

        Log.e("LISTA LOCALIZACIONES: ", String.valueOf(localizaciones.size()));

        //TODO: Guardar localizaciones en la base de datos.
        /////////////////////////////////////////////////////////

        guardarLocsStaticas(localizaciones);

        // check location permission
        if (storagePermissionsGranted && mLocationPermissionsGranted) {
//            if (!isMyServiceRunning(service.getClass())) {
//
//                startService(mServiceIntent);
//
//                startServiceMQTT();
//
//
//                //initMap();
//
//
//            }
            Log.e("Start", " START SERVICE");
        } else {
            Log.e("Permisos:", "No tiene todos los permisos activos");
        }

    }

    public void restartAPP(Double timeout){
        Intent mStartActivity = new Intent(getApplicationContext(), MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId,mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, (long) (System.currentTimeMillis() + timeout), mPendingIntent);
        System.exit(0);
    }


    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("locs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void guardarLocsStaticas(ArrayList<LocationBeanRealm> localizaciones) {

        // To start Realm once
        if (!this.startRealm) {
            Log.i("HEATMAP-INIT", "Starting Realm...");
            try {
                Realm.init(this);

                RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                        .modules(new LocationBeanRealmModule())
                        .name("Database.realm")
                        .deleteRealmIfMigrationNeeded()
                        .build();
                Realm.deleteRealm(realmConfiguration);

                Realm.setDefaultConfiguration(realmConfiguration);
                this.startRealm = true;
                Log.i("HEATMAP-INIT", "Realm started successfully");
            } catch (Exception e) {
                Log.e("HEATMAP-INIT", "Error during start Realm: " + e.getMessage());
            }

        }

        // Store the location in the Realm database
        Realm realm = Realm.getDefaultInstance();

        for (int i = 0; i < localizaciones.size(); i++) {

            realm.beginTransaction();
            LocationBeanRealm lbr = realm.createObject(LocationBeanRealm.class);
            lbr.setLat(localizaciones.get(i).getLat());
            lbr.setLng(localizaciones.get(i).getLng());
            lbr.setTimestamp(localizaciones.get(i).getTimestamp());
            realm.commitTransaction();

        }








        //Log.i("HEATMAP-LBR", lbr.toString());


    }

//    private void startServiceMQTT() {
//        serverIp = Constants.MQTT_BROKER_URL.split("//")[1].split(":")[0];
//
//        // Stopping service if running
//        MqttMessageService service = new MqttMessageService();
//        mServiceIntent = new Intent(this, service.getClass());
//
//       // mServiceIntent.putExtra("profile", profile);
//
//        boolean run = isMyServiceRunning(service.getClass());
//          Log.d(TAG, " - Run1: " + run);
//          if (!isMyServiceRunning(service.getClass())) {
//              //mServiceIntent.putExtra("profile", profile);
//            startService(mServiceIntent);
//
//          }
//          Log.d(TAG, " - Run1: " + run);
//
//    }

    /**
     * Método para inicializar el mapa.
     */
//    private void initMap() {
//        Log.d(TAG, "initMap: initializing map");
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(MainActivity.this);
//
//
//    }


    private void getTokenFirebase() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        String token = task.getResult().getToken();
                        Log.e(TAG, token);
                    }
                });
    }

    //    //TODO If you need subscribe in topics, use this method
    private void subscribeTopicFirebase(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Subscribe in topic correctly!";
                        if (!task.isSuccessful()) {
                            msg = "Failed to subscribed";
                        }

                        Log.d("Topic Information", msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};


        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;

            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    private void getStoragePermission() {
        Log.d(TAG, "getStoragePermission: getting storage permissions");
        String[] permissionsStorage = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            storagePermissionsGranted = true;

            if (mLocationPermissionsGranted) {
//                if (!isMyServiceRunning(service.getClass())) {
//                    startService(mServiceIntent);
//
//                    startServiceMQTT();
//                    //initMap();
//                    Log.e("Service: ", "START Service");
//                }
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissionsStorage,
                    STORAGE_PERMISSION_REQUEST_CODE);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                mLocationPermissionsGranted = false;
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            getStoragePermission();
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    getStoragePermission();
                }
            }

            case STORAGE_PERMISSION_REQUEST_CODE: {
                storagePermissionsGranted = false;
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            storagePermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    storagePermissionsGranted = true;
                    if (storagePermissionsGranted) {
                        //startService(locationIntent);
//                        if (!isMyServiceRunning(service.getClass())) {
//                            startService(mServiceIntent);
//
//                            startServiceMQTT();
//                            initMap();
//                        }

                    } else
                        Toast.makeText(MainActivity.this, "Tienes que activar todos los permisos", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        //LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, new IntentFilter("LOCATION"));

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, new IntentFilter("RESTART"));

    }

    @Override
    protected void onPause() {
        super.onPause();

        //LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, new IntentFilter("LOCATION"));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, new IntentFilter("RESTART"));

    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Double timeout = (Double) intent.getExtras().get("timeout");
            restartAPP(timeout);
        }
    };
    //TODO: cAMBIAR PARA QUE VUELVA A PINTAR
//    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            miPosicion = (LatLng) intent.getExtras().get("location");
//            currentTime = Calendar.getInstance().getTime();
//
//            if(!flag) {
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 14));
//                flag=true;
//            }
//
//            List<LocationFrequency> locations = LocationManager.getAllLocationsHistory();
//            //Log.e("HEAT", "locs=" + locations.toString());
//            List<WeightedLatLng> points = new ArrayList<WeightedLatLng>();
//            for (LocationFrequency location : locations) {
//                points.add(new WeightedLatLng(new LatLng(location.getLatitude(), location.getLongitude()), location.getFrequency()));
//            }
//            if (points.size() > 0) {
//                HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
//                        .weightedData(points)
//                        .build();
//                tileOverlay = MainActivity.this.mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
//
//
//            }
//
//
//            textPosicion.setText(miPosicion.toString());
//            textDateTime.setText(currentTime.toString());
//        }
//    };



//    private void writeFileExternalStorage() {
//        if (storagePermissionsGranted) {
//            try {
//                File myExternalFile = new File(Environment.getExternalStorageDirectory(), CARPETA_RAIZ);
//
//                if (!myExternalFile.exists())
//                    myExternalFile.mkdir();
//
//                FileOutputStream fos = new FileOutputStream(myExternalFile.getPath() + File.separator + "locations.json");
//
//
//                //fos.write(gson.toJson(locationDB).getBytes());
//                fos.close();
//                Log.d(TAG, " - File WRITED successfully");
//            } catch (IOException e) {
//                Log.d(TAG, " - Error LOADED file");
//                e.printStackTrace();
//            }
//        } else {
//            Toast toast = Toast.makeText(MainActivity.this, "Not have storage permissions", Toast.LENGTH_LONG);
//            toast.show();
//            getStoragePermission();
//        }
//    }


//    private boolean isMyServiceRunning(Class<?> serviceClass) {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
//                Log.i("Service status", "Running");
//                return true;
//            }
//        }
//        Log.i("Service status", "Not running");
//        return false;
//    }


//    @Override
//    protected void onDestroy() {
//        stopService(mServiceIntent);
//        super.onDestroy();
//    }

    //GMAP
//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//        mMap = googleMap;
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
//        mMap.clear();
//
//        //Log.e("MAP READY:", miPosicion.toString());
//
//        if (mLocationPermissionsGranted) {
//
//
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
//                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                return;
//            }
//            mMap.setMyLocationEnabled(true);
//            mMap.getUiSettings().setMyLocationButtonEnabled(true);
//
//        }
//
//    }


}
