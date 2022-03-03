package se.liu.itn.kts.tnk115.project2022;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import android.content.Context;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    public boolean collecting = false;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private NodeDatabase nodeDB;
    private NodeDao nodeDao;
    private LinkDatabase linkDB;
    private LinkDao linkDao;
    private String address = "130.236.81.13";
    private int port = 8718;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create node database
        nodeDB = Room.databaseBuilder(getApplicationContext(), NodeDatabase.class, "node").allowMainThreadQueries().build();
        nodeDao = nodeDB.userDao();

        // Create node database
        linkDB = Room.databaseBuilder(getApplicationContext(), LinkDatabase.class, "link").allowMainThreadQueries().build();
        linkDao = linkDB.userDao();

        // Update the GUI with the last known location.
        getLastKnownLocation();
    }

    public void getData(View view) {
        Log.d("MainActivity","GetData Button clicked");
        getNodes();
        Log.d("MainActivity","Nodes read");
        getLinks();
        Log.d("MainActivity","Links read");
    }

    public void getNodes() {
        String nodes = getData("nodes");
        if (nodes == null) {
            while (nodes == null) {
                nodes = getData("nodes");
            }
        }
        String nodesSplit[] = nodes.split(";");
        for (int i=0; i<nodesSplit.length; i++) {
            String n1[] = nodesSplit[i].split("-");
            String n2[] = n1[1].substring(6,n1[1].length()-1).split(" ");

            Node node = new Node();
            node.id = Integer.parseInt(n1[0]);
            node.lat = Double.parseDouble(n2[0]);
            node.lng = Double.parseDouble(n2[1]);
            nodeDao.insertNode(node);
        }
        TextView text = (TextView) findViewById(R.id.textView);

        Log.d("MainActivity","#"+nodesSplit.length+" nodes inserted");
        text.setText("#"+nodesSplit.length+" nodes inserted");
    }

    public void getLinks() {
        String links[] = getData("links").split(";");
        String air[] = getData("air").split(";");
        String elev[] = getData("elev").split(";");
        String pave[] = getData("pave").split(";");
        String pedq[] = getData("ped").split(";");
        String wcpave[] = getData("wcpave").split(";");
        String ttcog[] = getData("ttcog").split(";");
        String ttcycle[] = getData("ttcycle").split(";");
        String ttelev[] = getData("ttelev").split(";");
        String ttwcpq[] = getData("ttwcpq").split(";");

        for (int i=0; i<links.length; i++) {
            String li1[] = links[i].split("-");
            String ai1[] = air[i].split("-");
            String el1[] = elev[i].split("-");
            String pa1[] = pave[i].split("-");
            String pe1[] = pedq[i].split("-");
            String wc1[] = wcpave[i].split("-");
            String tc1[] = ttcog[i].split("-");
            String tc2[] = ttcycle[i].split("-");
            String te1[] = ttelev[i].split("-");
            String tw1[] = ttwcpq[i].split("-");

            Link link = new Link();
            link.source = Integer.parseInt(li1[0]);
            link.destination = Integer.parseInt(li1[1]);
            link.dist = Integer.parseInt(li1[2]);
            link.air = Integer.parseInt(ai1[2]);
            link.elev = Integer.parseInt(el1[2]);
            link.pave = Integer.parseInt(pa1[2]);
            link.pedp = Integer.parseInt(pe1[2]);
            link.wcpave = Integer.parseInt(wc1[2]);
            link.ttcog = Double.parseDouble(tc1[2]);
            link.ttcycle = Double.parseDouble(tc2[2]);
            link.ttelev = Double.parseDouble(te1[2]);
            link.ttwc = Double.parseDouble(tw1[2]);

            linkDao.insertLink(link);
        }
    }

    public String getData(String input) {
        String result = null;

        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            return result;
        }

        String IMEI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IMEI = tm.getImei();
            //IMEI = tm.getMeid();
        } else {
            IMEI = tm.getDeviceId();
        }

        //IMEI = tm.getDeviceId();
        Log.d("MainActivity","IMEI: "+IMEI);

        JSONObject message = new JSONObject();
        try {
            message.put("message_type","project2022");
            message.put(input,true);
        } catch (JSONException e) {
            Log.e("MainActivity","JSONException in getData: "+e.toString());
        }

        StringBuilder builder = new StringBuilder();
        builder.append(message.toString());
        builder.append((char)'|');

        byte[] packetToSend = builder.toString().getBytes();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        JSONObject responseMessage = null;
        try {
            InetAddress serverAddrIP = InetAddress.getByName(address);
            Socket socket = new Socket(serverAddrIP, port);
            PrintStream exit = new PrintStream(socket.getOutputStream(), false);
            InputStream inputStream = socket.getInputStream();
            exit.write(packetToSend);
            Log.i("NewSend", "Packet Sent ");
            exit.flush();

            ByteArrayOutputStream holder = new ByteArrayOutputStream();
            int outer_loop_limit = 0;

            while (socket.isConnected() && outer_loop_limit < 1) {
                char curr;
                curr = (char) inputStream.read();

                if (curr == '|') {
                    break;
                }

                holder.write(curr);
            }
            String response = holder.toString();

            response = response.trim();
            Log.d("MainActivity","Response: "+response);
            if (response.startsWith("{") || response.startsWith("[")) {
                try {
                    if (response.startsWith("[")) {
                        response = response.substring(1, message.length()-1);
                        responseMessage = new JSONObject(response);
                    } else {
                        responseMessage = new JSONObject(response);
                    }
                } catch (JSONException e) {
                    Log.w("MainActivity","JSONException 1: "+e.toString());
                }
            }

            exit.close();
            socket.close();

        } catch (IOException e) {
            Log.w("MainActivity","IOException: "+e.toString());
        }
        Log.d("MainActivity","Message sent "+input);
        Log.d("MainActivity",message.toString());
        Log.d("MainActivity","Message received: "+input);
        if (responseMessage == null) Log.w("MainActivity","Response null");
        else Log.d("MainActivity", responseMessage.toString());

        try {
            if (responseMessage.has(input)) {
                result = responseMessage.getString(input);
            }
        } catch (JSONException e) {
            Log.w("MainActivity","JSONException: "+e.toString());
        }

        return result;
    }

    protected void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    Log.d("MainActivity", "Updating GUI with last known location.");
                } else {
                    Log.d("MainActivity", "getLastKnownLocation null...");
                }
            }
        });
    }

    protected LocationRequest createLocationRequest() {
        Log.d("MainActivity","Creating LocationRequest");

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(30*1000);
        locationRequest.setFastestInterval(5*1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return locationRequest;
    }

    protected void startCollecting() {
        Log.d("MainActivity","Starting to request location updates");

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d("MainActivity","Updating GUI with current location.");
                }
            }
        };


    }
}