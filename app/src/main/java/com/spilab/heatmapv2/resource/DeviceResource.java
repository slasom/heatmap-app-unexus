/**
 * Heatmap API
 * This api provides us with the set of positions and frequencies of the different connected devices to generate a heat map.
 *
 * OpenAPI spec version: 3.0
 * Contact: spilab.uex@gmail.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package com.spilab.heatmapv2.resource;




import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.spilab.heatmapv2.model.LocationFrequency;
import com.spilab.heatmapv2.response.DeviceResponse;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;


public class DeviceResource {

    private Context context;
    private RequestQueue request;

    private DeviceResponse deviceResponse;

    public DeviceResource(Context context) {
        this.context = context;
        request = Volley.newRequestQueue(context);

    }

  public void executeMethod(DeviceResponse response){

        deviceResponse=response;

      switch (response.getMethod()){
          case "restartApp":
            restartApp(response.getParams().gettimeout());
            break;
      }


  }

  /**
  * Restart app in to mobile devices.
  * 
   * @param timeout wait time to start app after close (ms)
   * @return List<LocationFrequency>
  */
  public void restartApp (Double timeout){


      //SEND TO MAINACTIVITY



  }


    //TODO: Change for your configuration
    private void sendReply(String url, String idRequest, JSONObject result) {
            JsonObjectRequest jsonObjectRequest = null;


            JSONObject content= null;
            try {
                content=new JSONObject();
                content.put("idRequest",idRequest);
                content.put("body",result);


            } catch (JSONException e) {
                e.printStackTrace();
            }


            jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, content, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("OK: ", String.valueOf(response));

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                    System.out.println();
                    Log.d("ERROR: ", error.toString());
                }
            }
            );


            request.add(jsonObjectRequest);


    }
}