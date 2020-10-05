package com.spilab.heatmapv3.service;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogService {
    private Context context;
    private RequestQueue request;
    private String carpetaRaiz= "HeatmapV3/request.txt";

    public LogService(Context context){
        this.context=context;
        request = Volley.newRequestQueue(this.context);
    }

    public void deleteLogs(){
        File myExternalFile = new File(Environment.getExternalStorageDirectory(), carpetaRaiz);
        boolean deleted = myExternalFile.delete();

        Log.i("Delete logs: ", "delete!");
    }


    public void sendLogs() {


        final File myExternalFile = new File(Environment.getExternalStorageDirectory(), carpetaRaiz);


        final StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(myExternalFile));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();

            final Map<String, String> mParams=new HashMap<>();
            mParams.put("Content-Type","application/x-www-form-urlencoded");


            String nombreFichero= UUID.randomUUID().toString()+".txt";

            StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://108.129.48.139:8080/files/dataset/"+nombreFichero, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("Logs:", "Logs send correctly!");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("Logs:", "Logs error upload "+error.getMessage());
                }
            }) {

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return text.toString() == null ? null : text.toString().getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", text.toString(), "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // can get more details such as response.headers
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public Map<String, String> getParams() {
                    Map<String, String> mParams=new HashMap<>();
                    mParams.put("Content-Type","application/x-www-form-urlencoded");
                    mParams.put("filename", "nombrefichero.txt");
                    return mParams;
                }

            };


            request.add(stringRequest);

        }
        catch (IOException e) {
            Log.e("Error file:", "delete or empty?");
        }




    }

}



