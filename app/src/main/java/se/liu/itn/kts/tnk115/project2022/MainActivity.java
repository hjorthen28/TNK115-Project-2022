package se.liu.itn.kts.tnk115.project2022;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Path;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.RangeSlider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String EXTRA_MESSAGE = "se.liu.itn.kts.tnk115.MESSAGE";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int READ_PERMISSION_REQUEST_CODE = 2;
    private boolean first = true;
    private FusedLocationProviderClient fusedLocationClient;
    public static NodeDatabase nodeDB;
    public static NodeDao nodeDao;
    public static LinkDatabase linkDB;
    public static LinkDao linkDao;
    private String address = "130.236.81.13";
    private int port = 8718;
    private GoogleMap map = null;
    private Marker marker;
    private LatLngBounds norrkopingBounds;
    private OptPlan theOP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Loading the map from the GUI
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Get a FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create node database
        nodeDB = Room.databaseBuilder(getApplicationContext(), NodeDatabase.class, "node").allowMainThreadQueries().build();
        nodeDao = nodeDB.userDao();

        // Create node database
        linkDB = Room.databaseBuilder(getApplicationContext(), LinkDatabase.class, "link").allowMainThreadQueries().build();
        linkDao = linkDB.userDao();

        // Create a plan later used for Dijkstra algorithm
        theOP = new OptPlan();

        // Configure weight sliders
        configureSliders();

        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if the app has location permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    if (first) {
                        // Get nodes and links from external database
                        Log.d("MainActivity", "GetData Button clicked");
                        getNodes();
                        Log.d("MainActivity", "Nodes read");
                        getLinks();
                        Log.d("MainActivity", "Links read");
                        if (map != null) {
                            // Visualize the user's position on the map
                            map.setMyLocationEnabled(true);
                        }
                        // Create the optPlan and activates the makeRoute alternative
                        button.setText(getString(R.string.make_route));
                        theOP.addList(nodeDao.getAllNodes(),linkDao.getAllLinks());
                        theOP.createPlan();
                        first = false;
                    } else {
                        // Get the current position of the user and assess if the app can make a route
                        getLastKnownLocation();
                    }
                } else {
                    // Ask the user for location permission
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
                        requestReadPermission();
                    }
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                        requestLocationPermission();
                    }
                }
            }
        });
    }

    // Request of location permission
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("Location permission is needed because of this and that")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Request of read phone permission
    private void requestReadPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("Read phone permission is needed because of this and that")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PERMISSION_REQUEST_CODE);
        }
    }

    // Check if permission request was successfull or not
    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "LOCATION Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "LOCATION Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == READ_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "READ PHONE Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "READ PHONE Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Create the map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("MainActivity", "Map ready");
        map = googleMap;

        // Add a dragable marker in the centre of Norrköping
        LatLng norrkoping = new LatLng(58.588455, 16.188313);
        marker = map.addMarker(new MarkerOptions().position(norrkoping).title("Norrköping").draggable(true));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(norrkoping, 14.5f));

        // Read the position of the marker
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
            }
            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                Log.d("MainActivity","onMarkerDragEnd..."+marker.getPosition().latitude+"..."+marker.getPosition().longitude);
            }
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
                Log.d("MainActivity","onMarkerDragStart..."+marker.getPosition().latitude+"..."+marker.getPosition().longitude);
            }
        });

        // Creating a boundary around the centre of Norrköping
        norrkopingBounds = new LatLngBounds(
                new LatLng(58.58497423248923, 16.169788531387145),  // South west corner
                new LatLng(58.5970857365086, 16.19432793490285));   // North east corner

        Polyline line = map.addPolyline(new PolylineOptions()
                .add(new LatLng(58.58497423248923, 16.169788531387145))
                .add(new LatLng(58.58497423248923, 16.19432793490285))
                .add(new LatLng(58.5970857365086, 16.19432793490285))
                .add(new LatLng(58.5970857365086, 16.169788531387145))
                .add(new LatLng(58.58497423248923, 16.169788531387145)));
        line.setWidth(10.0f);
        line.setColor(Color.argb(255,0,0,0));

        try {
            boolean success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success) Log.e("MainActivity", "Style parsing failed");
        } catch (Resources.NotFoundException e) {
            Log.e("MainActivity", "Can't find style. Error: " + e.toString());
        }
    }

    private void configureSliders() {
        RangeSlider paceSlider = (RangeSlider) findViewById(R.id.pace_bar);
        paceSlider.setValueFrom(0);
        paceSlider.setValueTo(10);
        paceSlider.setStepSize(1f);
        paceSlider.setMinSeparation(1f);
        paceSlider.setMinSeparationValue(1f);

        RangeSlider elevSlider = (RangeSlider) findViewById(R.id.elev_bar);
        elevSlider.setValueFrom(0);
        elevSlider.setValueTo(10);
        elevSlider.setStepSize(1f);
        elevSlider.setMinSeparation(1f);
        elevSlider.setMinSeparationValue(1f);

        RangeSlider airSlider = (RangeSlider) findViewById(R.id.air_bar);
        airSlider.setValueFrom(0);
        airSlider.setValueTo(10);
        airSlider.setStepSize(1f);
        airSlider.setMinSeparation(1f);
        airSlider.setMinSeparationValue(1f);

        RangeSlider ttSlider = (RangeSlider) findViewById(R.id.tt_bar);
        ttSlider.setValueFrom(0);
        ttSlider.setValueTo(10);
        ttSlider.setStepSize(1f);
        ttSlider.setMinSeparation(1f);
        ttSlider.setMinSeparationValue(1f);
    }

    private void getNodes() {
        String nodes = getData("nodes");
        if (nodes == null) {
            while (nodes == null) {
                nodes = getData("nodes");
            }
        }
        String nodesSplit[] = nodes.split(";");
        for (int i = 0; i < nodesSplit.length; i++) {
            String n1[] = nodesSplit[i].split("-");
            String n2[] = n1[1].substring(6, n1[1].length() - 1).split(" ");

            Node node = new Node();
            node.id = Integer.parseInt(n1[0]);
            node.lat = Double.parseDouble(n2[1]);
            node.lng = Double.parseDouble(n2[0]);

            nodeDao.insertNode(node);
        }
        Log.d("MainActivity", "#" + nodesSplit.length + " nodes inserted");
        Toast.makeText(MainActivity.this, "Got nodes", Toast.LENGTH_SHORT).show();
    }

    private void getLinks() {
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

        for (int i = 0; i < links.length; i++) {
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

            //Log.d("MainActivity",link.toString());
            linkDao.insertLink(link);
        }
        Toast.makeText(MainActivity.this, "Got links!", Toast.LENGTH_SHORT).show();
    }

    private String getData(String input) {
        String result = null;

        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            return result;
        }

        /*String IMEI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IMEI = tm.getImei();
            //IMEI = tm.getMeid();
        } else {
            IMEI = tm.getDeviceId();
        }

        //IMEI = tm.getDeviceId();
        Log.d("MainActivity","IMEI: "+IMEI);*/

        JSONObject message = new JSONObject();
        try {
            message.put("message_type", "project2022");
            message.put(input, true);
        } catch (JSONException e) {
            Log.e("MainActivity", "JSONException in getData: " + e.toString());
        }

        StringBuilder builder = new StringBuilder();
        builder.append(message.toString());
        builder.append((char) '|');

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
            Log.d("MainActivity", "Response: " + response);
            if (response.startsWith("{") || response.startsWith("[")) {
                try {
                    if (response.startsWith("[")) {
                        response = response.substring(1, message.length() - 1);
                        responseMessage = new JSONObject(response);
                    } else {
                        responseMessage = new JSONObject(response);
                    }
                } catch (JSONException e) {
                    Log.w("MainActivity", "JSONException 1: " + e.toString());
                }
            }

            exit.close();
            socket.close();

        } catch (IOException e) {
            Log.w("MainActivity", "IOException: " + e.toString());
        }
        Log.d("MainActivity", "Message sent " + input);
        Log.d("MainActivity", message.toString());
        Log.d("MainActivity", "Message received: " + input);
        if (responseMessage == null) Log.w("MainActivity", "Response null");
        else Log.d("MainActivity", responseMessage.toString());

        try {
            if (responseMessage.has(input)) {
                result = responseMessage.getString(input);
            }
        } catch (JSONException e) {
            Log.w("MainActivity", "JSONException: " + e.toString());
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
                    makeRoute(location);
                } else {
                    Log.d("MainActivity", "getLastKnownLocation null...");
                }
            }
        });
    }

    private void makeRoute(Location location) {
        LatLng currP = new LatLng(location.getLatitude(),location.getLongitude());
        Log.d("MainActivity",location.toString());
        LatLng markP = marker.getPosition();
        int currPi = 0;
        int markPi = 0;

        if (norrkopingBounds.contains(currP) && norrkopingBounds.contains(markP)) {
            Toast.makeText(MainActivity.this, "Making route", Toast.LENGTH_SHORT).show();
            currPi = getMin(currP.latitude,currP.longitude);
            Log.d("MainActivity",nodeDao.getNode(currPi).toString());
            markPi = getMin(markP.latitude,markP.longitude);
            Log.d("MainActivity",nodeDao.getNode(markPi).toString());
            String path = theOP.getPath(currPi,markPi);

            Log.d("MainActivity",path);
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra(EXTRA_MESSAGE, path);
            startActivity(intent);
        } else {
            if (!norrkopingBounds.contains(currP)) {
                Toast.makeText(MainActivity.this, getString(R.string.currOut), Toast.LENGTH_SHORT).show();
            }
            if (!norrkopingBounds.contains(markP)) {
                Toast.makeText(MainActivity.this, getString(R.string.markOut), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int getMin(double lat, double lng) {
        double min = Double.MAX_VALUE;
        int id = 0;

        List<Node> nodes = nodeDao.getAllNodes();

        for (int i=0; i<nodes.size(); i++) {
            double latN = nodes.get(i).lat;
            double lngN = nodes.get(i).lng;
            double dist = 0.0;

            if (lat >= latN && lng >= lngN) dist = distance(latN, lat, lngN, lng);
            else if (lat < latN && lng >= lngN) dist = distance(lat, latN, lngN, lng);
            else if (lat >= latN && lng < lngN) dist = distance(latN, lat, lng, lngN);
            else dist = distance(lat, latN, lng, lngN);

            if (dist <= min) {
                min = dist;
                id = nodes.get(i).id;
            }
        }
        return id;
    }

    private double distance(double lat1, double lat2, double lng1, double lng2) {
        final int R = 6371; // Radius of the earth in km

        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = R * c * 1000; // Converts to meters

        return dist;
    }
}