package se.liu.itn.kts.tnk115.project2022;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;

import se.liu.itn.kts.tnk115.project2022.databinding.ActivityMapsBinding;

public class UpdateActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap uMap;
    private String path;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private int source, destination;
    private Polyline activeLink;

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
        //TODO: Add button listener and other logic
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
        Log.d("MainActivity", "Map ready");
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
    }

    private void displayPath() {
        String nID[] = path.split("->");
        ArrayList<LatLng> nC = new ArrayList<>();

        for (int i=0; i<nID.length; i++) {
            Node node = MainActivity.nodeDao.getNode(Integer.parseInt(nID[i].trim()));
            nC.add(new LatLng(node.lat,node.lng));
            //if (i == nID.length-1) {
            //    map.moveCamera(CameraUpdateFactory.newLatLngZoom(nC.get(i), 14.5f));
            //}
        }

        Polyline line = uMap.addPolyline(new PolylineOptions().addAll(nC));
        line.setWidth(10.0f);
        line.setStartCap(new RoundCap());
        line.setEndCap(new SquareCap());
        line.setColor(Color.argb(255,0,0,0));
    }

    //TODO: Update with correct sliders
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

    //TODO: Implement updating of values
    private void updateValues() {
        RangeSlider paceSlider = (RangeSlider) findViewById(R.id.pace_bar);
        List<Float> paceValues = paceSlider.getValues();
        double pace = paceValues.get(0);
    }

    protected void startCollecting() {

        Log.d("MainActivity", "Starting to request location updates.");
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d("MainActivity", "Updating GUI with current location.");
                    //TODO: Add code
                    activeLink(location);
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
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    protected LocationRequest createLocationRequest() {
        Log.d("MainActivity", "Creating LocationRequest");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(20 * 1000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return locationRequest;
    }

    private void activeLink(Location location) {
        double min = Double.MAX_VALUE;
        double lat = location.getLatitude();
        double lng = location.getLongitude();

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
                source = Integer.parseInt(nID[i].trim());
                destination = Integer.parseInt(nID[i+1].trim());
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
