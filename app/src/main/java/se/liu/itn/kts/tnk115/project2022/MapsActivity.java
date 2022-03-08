package se.liu.itn.kts.tnk115.project2022;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;

import se.liu.itn.kts.tnk115.project2022.databinding.ActivityMapsBinding;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        path = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mMap);
        mapFragment.getMapAsync(this);
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
        mMap = googleMap;
        mMap.clear();
        LatLng norrkoping = new LatLng(58.588455, 16.188313);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(norrkoping, 14.5f));

        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success) Log.e("MapsActivity", "Style parsing failed");
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivity", "Can't find style. Error: " + e.toString());
        }
        mMap.setMyLocationEnabled(true);
        displayPath();
    }

    private void displayPath() {
        String nID[] = path.split("->");
        ArrayList<LatLng> nC = new ArrayList<>();

        for (int i=0; i<nID.length; i++) {
            Node node = MainActivity.nodeDao.getNode(Integer.parseInt(nID[i].trim()));
            nC.add(new LatLng(node.lat,node.lng));
            if (i == nID.length-1) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nC.get(i), 14.5f));
            }
        }

        Polyline line = mMap.addPolyline(new PolylineOptions().addAll(nC));
        line.setWidth(10.0f);
        line.setStartCap(new RoundCap());
        line.setEndCap(new SquareCap());
        line.setColor(Color.argb(255,0,0,0));
    }
}