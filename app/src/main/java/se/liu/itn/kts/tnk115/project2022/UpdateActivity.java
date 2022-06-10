package se.liu.itn.kts.tnk115.project2022;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.slider.RangeSlider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class UpdateActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap uMap;
    private String path;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private int source = 0, destination = 0;
    private Polyline activeLink;
    private double pave, air, temp, noise;
    private int overall;
    private String address = "130.236.81.13";
    private int port = 8718;
    private boolean send = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        path = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        setContentView(R.layout.activity_update);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.uMap);
        mapFragment.getMapAsync(this);

        Button button = (Button) findViewById(R.id.buttonUpdate);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateValues();
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (path.equals("") || path == null) {
            stopCollecting();
        } else {
            startCollecting();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("UpdateActivity", "Map ready");
        uMap = googleMap;
        uMap.clear();
        LatLng norrkoping = new LatLng(58.59097655119428, 16.183341830042274);

        uMap.moveCamera(CameraUpdateFactory.newLatLngZoom(norrkoping, 14.5f));

        try {
            boolean success = uMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success) Log.e("UpdateActivity", "Style parsing failed");
        } catch (Resources.NotFoundException e) {
            Log.e("UpdateActivity", "Can't find style. Error: " + e.toString());
        }

        // Visualize the boundary on the map
        Polyline bounds = uMap.addPolyline(new PolylineOptions()
                .add(new LatLng(58.58497423248923, 16.169788531387145))
                .add(new LatLng(58.58497423248923, 16.19432793490285))
                .add(new LatLng(58.5970857365086, 16.19432793490285))
                .add(new LatLng(58.5970857365086, 16.169788531387145))
                .add(new LatLng(58.58497423248923, 16.169788531387145)));
        bounds.setWidth(10.0f);
        bounds.setColor(Color.argb(255,0,0,0));

        uMap.setMyLocationEnabled(true);
        if (!path.equals("")) {
            displayPath();
        }
        configureSliders();
    }

    private void displayPath() {
        String nID[] = path.split("->");
        ArrayList<LatLng> nC = new ArrayList<>();

        for (int i=0; i<nID.length; i++) {
            Node node = MainActivity.nodeDao.getNode(Integer.parseInt(nID[i].trim()));
            nC.add(new LatLng(node.lat,node.lng));
        }

        Polyline line = uMap.addPolyline(new PolylineOptions().addAll(nC));
        line.setWidth(10.0f);
        line.setStartCap(new RoundCap());
        line.setEndCap(new SquareCap());
        line.setColor(Color.argb(255,0,0,0));
    }

    private void configureSliders() {

        RangeSlider paveSlider = (RangeSlider) findViewById(R.id.pave_uBar);
        RangeSlider airSlider = (RangeSlider) findViewById(R.id.air_uBar);
        RangeSlider tempSlider = (RangeSlider) findViewById(R.id.temp_uBar);
        RangeSlider noiseSlider = (RangeSlider) findViewById(R.id.noise_uBar);
        RangeSlider overallSlider = (RangeSlider) findViewById(R.id.overall_uBar);

        paveSlider.setValueFrom(0);
        paveSlider.setValueTo(1);

        airSlider.setValueFrom(0);
        airSlider.setValueTo(1);

        tempSlider.setValueFrom(0);
        tempSlider.setValueTo(1);
        tempSlider.setValues(1f);

        noiseSlider.setValueFrom(0);
        noiseSlider.setValueTo(1);
        noiseSlider.setValues(0.5f);

        overallSlider.setValueFrom(0);
        overallSlider.setValueTo(10);
        overallSlider.setStepSize(1f);
        overallSlider.setMinSeparation(1f);
        overallSlider.setMinSeparationValue(1f);
        overallSlider.setValues(5f);
        overall = 5;

        if (path.equals("") || path == null || source == 0 || destination == 0) {
            pave = 0.0;
            air = 0.0;
            temp = 0.0;
            noise = 0.0;
        } else {
            Link activeLink = MainActivity.linkDao.getLink(source, destination);
            pave = activeLink.pave;
            air = activeLink.air;
            temp = activeLink.temp;
            noise = activeLink.noise;

            Log.d("UpdateActivity", "Old values: Link: " + source + "->" + destination + String.format(" P:%.2f", pave) + String.format(" A:%.2f", air) + String.format(" T:%.2f", temp) + String.format(" N:%.2f", noise));

            pave = 1.0 - pave;
            paveSlider.setValues((float) pave);

            air = 1.0 - norm(air, MainActivity.linkDao.getMaxAir(), MainActivity.linkDao.getMinAir());
            airSlider.setValues((float) air);

            temp = 1.0 - norm(temp, MainActivity.linkDao.getMaxTemp(), MainActivity.linkDao.getMinTemp());
            tempSlider.setValues((float) temp);

            noise = 1.0 - noise;
            noiseSlider.setValues((float) noise);

            Log.d("UpdateActivity", "Link: " + source + "->" + destination + " P" + activeLink.pave + " A" + activeLink.air + " T" + activeLink.temp + " N" + activeLink.noise);
            Log.d("UpdateActivity", "Link: " + source + "->" + destination + String.format(" P%.2f", pave) + String.format(" A%.2f", air) + String.format(" T%.2f", temp) + String.format(" N%.2f", noise));
        }

    }

    private void updateValues() {
        if (source == 0 || destination == 0 || path == "" || path.equals("")) {
            Log.d("UpdateActivity","No link to update");
            Toast.makeText(this,"No active link to update",Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("UpdateActivity","Old values: Link: "+source+"->"+destination+String.format(" Pave:%.2f",pave)+String.format(" Air:%.2f",air)+String.format(" Temp:%.2f",temp)+String.format(" Noise:%.2f",noise));
        Log.d("UpdateActivity","Before: Link: "+source+"->"+destination+String.format(" Pave:%.2f",pave)+String.format(" Air:%.2f",air)+String.format(" Temp:%.2f",temp)+String.format(" Noise:%.2f",noise));

        double pN = 100.0;
        double aN = 100.0;
        double tN = 100.0;
        double nN = 100.0;

        RangeSlider paveSlider = (RangeSlider) findViewById(R.id.pave_uBar);
        List<Float> paveValues = paveSlider.getValues();
        double pV = paveValues.get(0);
        if (pV != pave) {
            pN = 1.0-pV;
        }
        //pave = newValue(pV, pave);

        RangeSlider airSlider = (RangeSlider) findViewById(R.id.air_uBar);
        List<Float> airValues = airSlider.getValues();
        double aV = airValues.get(0);
        if (aV != air) {
            aN = 1.0-aV;
        }
        //air = newValue(aV, air);

        RangeSlider tempSlider = (RangeSlider) findViewById(R.id.temp_uBar);
        List<Float> tempValues = tempSlider.getValues();
        double tV = tempValues.get(0);
        if (tV != temp) {
            tN = 1.0-tV;
        }
        //temp = newValue(tV, temp);

        RangeSlider noiseSlider = (RangeSlider) findViewById(R.id.noise_uBar);
        List<Float> noiseValues = noiseSlider.getValues();
        double nV = noiseValues.get(0);
        if (nV != noise) {
            nN = 1.0-nV;
        }
        //noise = newValue(nV, noise);

        RangeSlider overallSlider = (RangeSlider) findViewById(R.id.overall_uBar);
        List<Float> overallValues = overallSlider.getValues();
        overall = Math.round(overallValues.get(0));

        Log.d("UpdateActivity","After : Link: "+source+"->"+destination+String.format(" Pave:%.2f",pV)+String.format(" Air:%.2f",aV)+String.format(" Temp:%.2f",tV)+String.format(" Noise:%.2f",nV));
        Log.d("UpdateActivity","Sending : Link: "+source+"->"+destination+String.format(" Pave:%.2f",pN)+String.format(" Air:%.2f",aN)+String.format(" Temp:%.2f",tN)+String.format(" Noise:%.2f",nN)+" Overall:"+overall);
        //Log.d("UpdateActivity","Old values: Link: "+source+"->"+destination+String.format(" Pave:%.2f",pave)+String.format(" Air:%.2f",air)+String.format(" Temp:%.2f",temp)+String.format(" Noise:%.2f",noise));

        if (send) {
            send = false;
            getLastKnownLocation(pN, aN, tN, nN);
            Toast.makeText(this, "Values sent", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("UpdateActivity","Data not sent due to already sent data for the specific link.");
            Toast.makeText(this, "You've already sent data for this link", Toast.LENGTH_SHORT).show();
        }

    }

    protected void startCollecting() {
        Log.d("UpdateActivity", "Starting to request location updates.");
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d("UpdateActivity", "Updating GUI with current location.");
                    if (path.equals("") || path == null) {
                        stopCollecting();
                    } else {
                        activeLink(location);
                    }
                }
            }
        };

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

        fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.myLooper());
    }

    protected void stopCollecting() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    protected LocationRequest createLocationRequest() {
        Log.d("UpdateActivity", "Creating LocationRequest");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(20 * 1000);
        locationRequest.setFastestInterval(10 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return locationRequest;
    }

    private void activeLink(Location location) {
        double min = Double.MAX_VALUE;
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        double latA = lat;
        double lngA = lng;
        boolean change = false;

        String nID[] = path.split("->");

        //Log.d("UpdateActivity","Finding closest link");
        //Log.d("UpdateActivity",path+ ", Number of nodes in path: "+nID.length);

        for (int i=0; i<nID.length-1; i++) {
            //Log.d("UpdateActivity","Index: "+i+" Nodes: "+nID[i]+":"+nID[i+1]);
            double latN = (MainActivity.nodeDao.getNode(Integer.parseInt(nID[i].trim())).lat+MainActivity.nodeDao.getNode(Integer.parseInt(nID[i+1].trim())).lat)/2;
            double lngN = (MainActivity.nodeDao.getNode(Integer.parseInt(nID[i].trim())).lng+MainActivity.nodeDao.getNode(Integer.parseInt(nID[i+1].trim())).lng)/2;
            double dist = 0.0;

            if (lat >= latN && lng >= lngN) dist = distance(latN, lat, lngN, lng);
            else if (lat < latN && lng >= lngN) dist = distance(lat, latN, lngN, lng);
            else if (lat >= latN && lng < lngN) dist = distance(latN, lat, lng, lngN);
            else dist = distance(lat, latN, lng, lngN);

            if (dist <= min) {
                min = dist;
                if (source != Integer.parseInt(nID[i].trim()) || destination != Integer.parseInt(nID[i+1].trim()))
                    change = true;
                source = Integer.parseInt(nID[i].trim());
                destination = Integer.parseInt(nID[i+1].trim());
                latA = latN;
                lngA = lngN;
            }
        }
        if (activeLink != null) {
            activeLink.remove();
        }
        activeLink = uMap.addPolyline(new PolylineOptions()
                .add(new LatLng(MainActivity.nodeDao.getNode(source).lat,MainActivity.nodeDao.getNode(source).lng))
                .add(new LatLng(MainActivity.nodeDao.getNode(destination).lat,MainActivity.nodeDao.getNode(destination).lng)));
        activeLink.setWidth(10.0f);
        activeLink.setStartCap(new RoundCap());
        activeLink.setEndCap(new SquareCap());
        activeLink.setColor(Color.argb(255,0,255,0));

        Log.d("UpdateActivity","Closest link between node: "+source+" & "+destination);

        if (change) {
            send = true;
            configureSliders();
            uMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latA,lngA), 17.5f));
        }
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

    // Get last location and start the make route process
    protected void getLastKnownLocation(double p, double a, double t, double n) {
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
                    Log.d("UpdateActivity", "Updating GUI with last known location.");
                    sendUpdate(p, a, t, n, location);
                } else {
                    Log.d("UpdateActivity", "getLastKnownLocation null...");
                }
            }
        });
    }

    // Transmit data between database and local unit
    private void sendUpdate(double p, double a, double t, double n, Location location) {

        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        String IMEI;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            IMEI = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (this.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("UpdateActivity","Missing permission READ_PHONE_STATE !!!");
                    Log.w("UpdateActivity","Could not send updated values to the server!!!");
                    return;
                }
            }
            assert tm != null;
            if (tm.getDeviceId() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IMEI = tm.getImei();
                } else {
                    IMEI = tm.getDeviceId();
                }
            } else {
                IMEI = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }

        Log.d("UpdateActivity","IMEI: "+IMEI);

        JSONObject message = new JSONObject();
        try {
            message.put("message_type", "project2022update");
            JSONObject update = new JSONObject();
            message.put("update",update);

            update.put("imei",IMEI);
            update.put("origin",Integer.toString(source));
            update.put("destination",Integer.toString(destination));
            update.put("pavement",String.format("%.2f",p));
            update.put("air",String.format("%.2f",a));
            update.put("temp",String.format("%.2f",t));
            update.put("noise",String.format("%.2f",n));
            update.put("overall",Integer.toString(overall));

            JSONObject locationData = new JSONObject();
            update.put("location",locationData);


            locationData.put("longitude",String.format("%.8f",location.getLongitude()));
            locationData.put("latitude",String.format("%.8f",location.getLatitude()));
        } catch (JSONException e) {
            Log.e("UpdateActivity", "JSONException in getData: " + e.toString());
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
            Log.d("UpdateActivity", "Response: " + response);
            if (response.startsWith("{") || response.startsWith("[")) {
                try {
                    if (response.startsWith("[")) {
                        response = response.substring(1, message.length() - 1);
                        responseMessage = new JSONObject(response);
                    } else {
                        responseMessage = new JSONObject(response);
                    }
                } catch (JSONException e) {
                    Log.w("UpdateActivity", "JSONException 1: " + e.toString());
                }
            }

            exit.close();
            socket.close();

        } catch (IOException e) {
            Log.w("UpdateActivity", "IOException: " + e.toString());
        }
        Log.d("UpdateActivity", "Update message sent!");
        Log.d("UpdateActivity", message.toString());
        Log.d("UpdateActivity", "Update message received");
        if (responseMessage == null) Log.w("UpdateActivity", "Response null");
        else Log.d("UpdateActivity", responseMessage.toString());

        /*try {
            if (responseMessage.has(input)) {
                result = responseMessage.getString(input);
            }
        } catch (JSONException e) {
            Log.w("UpdateActivity", "JSONException: " + e.toString());
        }*/
    }

    private double norm(double value, double max, double min) {
        if (value > max && value == 1000.0) return 1000;
        else if (max != min) return ((value-min)/(max-min));
        else return 1.0;
    }

    private double reverseNorm(double value, double max, double min) {
        return value*(max-min)+min;
    }

    private double newValue(double nV, double cV) {
        return cV+((nV-cV)*(1-Math.pow(Math.abs(nV-cV),0.33)));
    }

}
