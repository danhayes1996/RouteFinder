package com.routefinder.dan.routefinder;

import android.app.Activity;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener{

    private GoogleMap mMap;
    private double latitude = 54.5669661, longitude = -2.9378484; //not needed?
    private float zoomLevel = 5.7f;
    private Geocoder geocoder;

    private Button btnAdd, btnMenu;
    private ImageButton btnRemove, btnSave;
    private EditText txtPostcode;

    private RelativeLayout markerPanel;
    private LinearLayout menuPanel, menuScrollPanel;
    private TextView tvMarkerInfo;

    private LinkedList<Location> locationList;
    private Marker currentMarker;
    private String filepath;

    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_maps );
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );

        filepath = getIntent().getStringExtra("filePath");
        geocoder = new Geocoder(this);
        locationList = new LinkedList<>();

        assignViewAttribs();
        assignHandlers();
    }

    private void assignViewAttribs(){
        imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);

        btnAdd    = (Button) findViewById(R.id.btnAdd);
        btnSave   = (ImageButton) findViewById(R.id.btnSave);
        btnMenu   = (Button) findViewById(R.id.btnMenu);
        btnRemove = (ImageButton) findViewById(R.id.btnRemove);

        txtPostcode = (EditText) findViewById(R.id.txtPostcode);

        markerPanel = (RelativeLayout) findViewById(R.id.layoutMarkerPanel);
        menuPanel   = (LinearLayout) findViewById(R.id.layoutMenuPanel);
        menuScrollPanel = (LinearLayout) findViewById(R.id.layoutMenuScrollPanel);

        tvMarkerInfo = (TextView) findViewById(R.id.tvMarkerInfo);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Add a marker 'here' and move the camera
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoomLevel));

        if(filepath == null){ //if new route selected
            filepath = getApplicationContext().getFilesDir() + "/test.obj";
            TextView tv = new TextView(getApplicationContext());
            tv.setText("No Markers Found");
            menuScrollPanel.addView(tv);
        }else{
            loadRouteFromFile();
        }
    }

    private void assignHandlers(){
        btnSave.setOnClickListener(new SaveButtonClickListener());
        btnRemove.setOnClickListener(new RemoveButtonClickListener());
        btnAdd.setOnClickListener(new AddButtonClickListener());
        btnMenu.setOnClickListener(new MenuButtonClickListener());
        txtPostcode.setOnKeyListener(new PostcodeKeyListener());
        txtPostcode.setOnClickListener(new PostcodeClickListener());

        //this is needed to absorb the click event (so the event doesn't trigger on the map)
        menuPanel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
            }
        });

    }

    private void removeCurrentMarker(){
        for(int i = 0; i < locationList.size(); i++){
            if(locationList.get(i).equals(currentMarker)) {
                locationList.remove(i);
                return;
            }
        }
    }

    private void saveRouteToFile(){
        ObjectOutputStream oos;
        try{
            oos = new ObjectOutputStream(new FileOutputStream(filepath));
            oos.writeObject(locationList);
            oos.close();
            Toast.makeText(getApplicationContext(),"Saved!", Toast.LENGTH_SHORT).show();
        }catch(Exception e){
            Toast.makeText(getApplicationContext(),"Error saving route to file \"" + filepath + "\"", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadRouteFromFile(){
        File file = new File(filepath);
        if(file == null || !file.exists()){
            Toast.makeText(getApplicationContext(),"No previous route to load", Toast.LENGTH_SHORT).show();
            return;
        }

        ObjectInputStream ois;
        try{
            ois = new ObjectInputStream(new FileInputStream(file));
            loadMarkers((LinkedList<Location>) ois.readObject());
            ois.close();
        }catch(IOException e){
            Toast.makeText(getApplicationContext(),"Error loading route from file", Toast.LENGTH_SHORT).show();
        }catch(ClassNotFoundException e){
            Toast.makeText(getApplicationContext(),"Error corrupt data in file", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMarkers(LinkedList<Location> locations){
        if(locations.isEmpty()){
            TextView tv = new TextView(getApplicationContext());
            tv.setText("No Markers Found");
            menuScrollPanel.addView(tv);
        }

        for(Location location : locations){
            Marker m = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLat(), location.getLng())).title(location.getName()));
            locationList.add(new Location(m));

            menuScrollPanel.addView(createMarkerTextView(location.getName()));
        }
    }

    private TextView createMarkerTextView(final String text){
        TextView txtView = new TextView(getApplicationContext());
        txtView.setText(text);
        txtView.setTextSize(18);
        txtView.setPadding(0, 20, 0, 20);

        txtView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Location location = locationList.get(getLocationIndex(text));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLat(), location.getLng()), 12));

                //set colour of old current marker to red
                if(currentMarker != null) currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                currentMarker = location.getMarker();

                //set colour of new current marker to blue
                currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                markerPanel.setVisibility(View.VISIBLE);
                menuPanel.setVisibility(View.INVISIBLE);

                tvMarkerInfo.setText(location.getName());
            }
        });
        return txtView;
    }

    private int getLocationIndex(String text){
        for(int i = 0; i < locationList.size(); i++){
           if(text.equals(locationList.get(i).getName())) return i;
        }
        return -1;
    }

    @Override
    public void onStop(){
        saveRouteToFile();
        super.onStop();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        tvMarkerInfo.setText(marker.getTitle());
        //set colour of old currentMarker back to red
        if(currentMarker != null) currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        currentMarker = marker;
        //set colour of new current marker to blue
        currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        markerPanel.setVisibility(View.VISIBLE);
        menuPanel.setVisibility(View.INVISIBLE);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude), 12));
        hideKeyboard();
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //set colour of current marker to red
        if(currentMarker != null) currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        markerPanel.setVisibility(View.INVISIBLE);
        menuPanel.setVisibility(View.INVISIBLE);
        currentMarker = null;
        hideKeyboard();
    }

    private void hideKeyboard(){
        imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
    }

    private class AddButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                String postcode = txtPostcode.getText().toString().toUpperCase();
                List<Address> addressList = geocoder.getFromLocationName(postcode, 1);
                if (addressList == null || addressList.size() == 0) {
                    Toast.makeText(getApplicationContext(), "Error: Couldn't find address.", Toast.LENGTH_SHORT).show();
                } else {
                    Address address = addressList.get(0);
                    //TODO: check if marker already exists? (add info to indicate multiple stops at this location)
                    Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(address.getLatitude(), address.getLongitude())).title(postcode));
                    locationList.add(new Location(marker));

                    //remove the "No Markers Found" TextView from menu
                    if(locationList.size() == 1) menuScrollPanel.removeViewAt(0);

                    menuScrollPanel.addView(createMarkerTextView(postcode));
                    txtPostcode.getText().clear();

                    //set colour of old current marker to red
                    if(currentMarker != null) currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                    currentMarker = marker;

                    //set colour of new current marker to blue
                    currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude), 12));

                }
            } catch (IOException e){
                Toast.makeText(getApplicationContext(), "Error: Geocoder Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class RemoveButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            //markerList.remove(currentMarker);
            markerPanel.setVisibility(View.INVISIBLE);

            for(int i = 0; i < menuScrollPanel.getChildCount(); i++){
                View v = menuScrollPanel.getChildAt(i);
                if(!(v instanceof TextView)) continue;
                if(((TextView) v).getText().equals(currentMarker.getTitle())) {
                    menuScrollPanel.removeViewAt(i);
                    break;
                }
            }

            removeCurrentMarker(); //remove marker from

            if(locationList.isEmpty()){
                TextView tv = new TextView(getApplicationContext());
                tv.setText("No Markers Found");
                menuScrollPanel.addView(tv);
            }

            currentMarker.remove(); //remove marker from map
            currentMarker = null;

        }
    }

    private class SaveButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            saveRouteToFile();
        }
    }

    private class MenuButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            markerPanel.setVisibility(View.INVISIBLE);
            menuPanel.setVisibility(View.VISIBLE);
            hideKeyboard();
        }
    }

    private class PostcodeKeyListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
            //If the keyevent is a key-down event on the "enter" button
            if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                new AddButtonClickListener().onClick(null);
                return true;
            }
            return false;
        }
    }

    private class PostcodeClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            markerPanel.setVisibility(View.INVISIBLE);
            menuPanel.setVisibility(View.INVISIBLE);
        }
    }
}
