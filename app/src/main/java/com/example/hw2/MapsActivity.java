package com.example.hw2;

import androidx.core.app.ActivityCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private SensorManager sensorManager;
    private Sensor sensor;
    private boolean stop_recording;
    private boolean show_buttons;
    private TextView textView;
    List<Marker> markerList;
    List<Double> markers_positions;
    private final String MARKERS_JSON_FILE = "markers.json";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stop_recording = false;
        show_buttons = true;
        FloatingActionButton hide = (FloatingActionButton) findViewById(R.id.hide_buttons);
        FloatingActionButton record_sensor = (FloatingActionButton) findViewById(R.id.record_sensor);
        textView = (TextView) findViewById(R.id.acceleration_text);
        Button clear_memory_button = (Button) findViewById(R.id.clear_button);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


        markerList = new ArrayList<>(); // Initialize markerList
        markers_positions = new ArrayList<>();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // SUCCESS
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Toast.makeText(getApplicationContext(), "No Accelerometer found", Toast.LENGTH_SHORT).show();
            return;
        } // Failure

        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_buttons = true;
                FlingAnimation flingAnimation = new FlingAnimation(findViewById(R.id.button_animation), DynamicAnimation.SCROLL_Y);
                flingAnimation.setStartVelocity(-2000)
                        .setMinValue(0)
                        .setFriction(1.1f)
                        .start();
                flingAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
            }
        });

        record_sensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!stop_recording) {
                    stop_recording = true;
                    textView.setVisibility(View.VISIBLE);
                }
                else {
                    stop_recording = false;
                    textView.setVisibility(View.INVISIBLE);
                }
            }
        });

        clear_memory_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markerList.clear();
                mMap.clear();
            }
        });
        restore_Markers_From_JSON();
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    private void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
    }

    public void zoomInClick(View v) {
        // Zoom in the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v){
        // Zoom out the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        // Add a custom marker at the position of the long click
        @SuppressLint("DefaultLocale") Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                .alpha(0.8f)
                .title(String.format("Position:(%.2f ; %.2f)", latLng.latitude, latLng.longitude)));
        // Add the marker to the array
       markerList.add(marker);
       markers_positions.add(latLng.latitude);
       markers_positions.add(latLng.longitude);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        mMap.getCameraPosition();
        mMap.getUiSettings().setMapToolbarEnabled(false);
        marker.showInfoWindow();
        if(show_buttons) {
            show_buttons = false;
            FlingAnimation flingAnimation = new FlingAnimation(findViewById(R.id.button_animation), DynamicAnimation.SCROLL_Y);
            flingAnimation.setStartVelocity(2000)
                    .setMinValue(0)
                    .setFriction(1.1f)
                    .start();
            flingAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
        }
        return false;
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onSensorChanged(SensorEvent event) {
        float axisX = event.values[0];
        float axisY = event.values[1];
        textView.setText(String.format("Acceleration: \n X: %.4f ; Y: %.4f", axisX, axisY));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume(){
        super.onResume();
        if(sensor != null)
            sensorManager.registerListener(this, sensor, sensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(sensor != null)
            sensorManager.unregisterListener(this, sensor);
    }

    private void save_Markers_To_JSON() {
        Gson gson = new Gson();
        String listJson = gson.toJson(markers_positions);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(MARKERS_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restore_Markers_From_JSON(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try{
            inputStream = openFileInput(MARKERS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0,n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<Double>>(){}.getType();
            List<Double> o = gson.fromJson(readJson, collectionType);
            markers_positions.clear();
            if(o != null){
                markers_positions.addAll(o);
            }
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
            return;
        }
        markerList.clear();
        try {
            for (int i = 0; i < markers_positions.size(); i += 2) {
                    @SuppressLint("DefaultLocale") Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(markers_positions.get(i), markers_positions.get(i + 1)))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                            .alpha(0.8f)
                            .title(String.format("Position:(%.2f ; %.2f)", markers_positions.get(i), markers_positions.get(i + 1))));
                    markerList.add(marker);
            }
        }
        catch (NullPointerException e){
            return;
        }
    }

    @Override
    protected void onDestroy() {
        save_Markers_To_JSON();
        super.onDestroy();

    }



}
