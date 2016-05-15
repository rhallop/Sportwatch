package com.akaver.gmapdemo02;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private final static String TAG = "MainActivity";

    private GoogleMap mGoogleMap;
    private Menu mOptionsMenu;

    private LocationManager locationManager;

    private String provider;

    private NotificationManager mNotificationManager;
    private BroadcastReceiver mBroadcastReceiver;

    private int markerCount = 0;
    private Location locationPrevious;
    private Location locationStart;
    private Location locationLastWP;
    private Location locationLastCReset;
    private double totalDistance;
    private double totalDistanceLine;
    private double wayPointDistance;
    private double wayPointDistanceLine;
    private double cResetDistance;
    private double cResetDistanceLine;
    private String paceCurrent;
    private long timePrevious;

    private Polyline mPolyline;
    private PolylineOptions mPolylineOptions;


    private TextView textViewWPCount;
    private TextView textViewSpeed;
    private TextView textViewTotalDistance;
    private TextView textViewTotalDistanceLine;
    private TextView textViewWayPointDistance;
    private TextView textViewWayPointDistanceLine;
    private TextView textViewCResetDistance;
    private TextView textViewCresetDistanceLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("addwaypoint")){
                    addWayPoint();
                }
                if (intent.getAction().equals("resetodo")){
                    resetTracker();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("addwaypoint");
        intentFilter.addAction("resetodo");
        registerReceiver(mBroadcastReceiver, intentFilter);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();

        // get the location provider (GPS/CEL-towers, WIFI)
        provider = locationManager.getBestProvider(criteria, false);

        //Log.d(TAG, provider);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");

        }

        locationPrevious = locationManager.getLastKnownLocation(provider);

        if (locationPrevious != null){
            // do something with initial position? yes
            locationPrevious = null;
        }

        textViewWPCount = (TextView) findViewById(R.id.textview_wpcount);
        textViewSpeed = (TextView) findViewById(R.id.textview_speed);
        textViewTotalDistance = (TextView) findViewById(R.id.textview_total_distance);
        textViewTotalDistanceLine = (TextView) findViewById(R.id.textview_total_line);
        textViewWayPointDistance = (TextView) findViewById(R.id.textview_wp_distance);
        textViewWayPointDistanceLine = (TextView) findViewById(R.id.textview_wp_line);
        textViewCResetDistance = (TextView) findViewById(R.id.textview_creset_distance);
        textViewCresetDistanceLine = (TextView) findViewById(R.id.textview_creset_line);

        createNotification();



    }

    private void updateDisplay() {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.HALF_UP);
        textViewTotalDistance.setText(df.format(totalDistance));
        textViewTotalDistanceLine.setText(df.format(totalDistanceLine));

        textViewWayPointDistance.setText(df.format(wayPointDistance));
        textViewWayPointDistanceLine.setText(df.format(wayPointDistanceLine));

        textViewCResetDistance.setText(df.format(cResetDistance));
        textViewCresetDistanceLine.setText(df.format(cResetDistanceLine));


        textViewSpeed.setText(paceCurrent + "min/km");

    }

    private void updateDistances(Location currentLocation){
        double distance = locationPrevious.distanceTo(currentLocation);

        // total distance
        totalDistance += distance;

        totalDistanceLine = locationStart.distanceTo(currentLocation);


        // distance from last waypoint
        if (markerCount != 0){
            wayPointDistance += distance;
        }else {
            wayPointDistance = 0;
        }

        // line distance from last waypoint
        if (locationLastWP != null){
            wayPointDistanceLine = locationLastWP.distanceTo(currentLocation);
        }else{
            wayPointDistanceLine = 0;
        }

        // distance from last counter reset
        cResetDistance += distance;

        // line distance from last counter reset
        if (locationLastCReset != null){
            cResetDistanceLine = locationLastCReset.distanceTo(currentLocation);
        }else{
            cResetDistanceLine = totalDistanceLine;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_mylocation:
                item.setChecked(!item.isChecked());
                updateMyLocation();
                return true;
            //case R.id.menu_trackpositionline:
            case R.id.menu_trackposition:
                item.setChecked(!item.isChecked());
                updateTrackPosition();
                return true;
            case R.id.menu_keepmapcentered:
                item.setChecked(!item.isChecked());
                return true;
            case R.id.menu_map_type_hybrid:
            case R.id.menu_map_type_none:
            case R.id.menu_map_type_normal:
            case R.id.menu_map_type_satellite:
            case R.id.menu_map_type_terrain:
                item.setChecked(true);
                updateMapType();
                return true;

            case R.id.menu_map_zoom_10:
            case R.id.menu_map_zoom_15:
            case R.id.menu_map_zoom_20:
            case R.id.menu_map_zoom_in:
            case R.id.menu_map_zoom_out:
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomLevel(item.getItemId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void updateMapZoomLevel(int itemId){
        if (!checkReady()) {
            return;
        }

        switch (itemId) {
            case R.id.menu_map_zoom_10:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
                break;
            case R.id.menu_map_zoom_15:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                break;
            case R.id.menu_map_zoom_20:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                break;
            case R.id.menu_map_zoom_in:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomIn());
                break;
            case R.id.menu_map_zoom_out:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomOut());
                break;
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomFitTrack();
                break;
        }
    }

    private void updateMapZoomFitTrack(){
        if (mPolyline==null){
            return;
        }

        List<LatLng> points = mPolyline.getPoints();

        if (points.size()<=1){
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

    }

    private void updateTrackPosition(){
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            mPolylineOptions = new PolylineOptions().width(5).color(Color.RED);
            mPolyline = mGoogleMap.addPolyline(mPolylineOptions);
        }

    }

    private void updateMapType() {
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_map_type_normal).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_hybrid).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_none).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_satellite).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_terrain).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }

    }

    private boolean checkReady() {
        if (mGoogleMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateMyLocation() {
        if (mOptionsMenu.findItem(R.id.menu_mylocation).isChecked()) {
            mGoogleMap.setMyLocationEnabled(true);
            return;
        }

        mGoogleMap.setMyLocationEnabled(false);
    }

    public void buttonAddWayPointClicked(View view){
        addWayPoint();
    }

    public void addWayPoint(){
        wayPointDistance = 0; // reset distance from waypoint

        if (locationPrevious==null){
            return;
        }

        markerCount++;

        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())).title(Integer.toString(markerCount)));
        textViewWPCount.setText(Integer.toString(markerCount));

        locationLastWP = locationPrevious;
    }

    public void buttonCResetClicked(View view){
        resetTracker();
    }

    public void resetTracker(){
        locationLastCReset = locationPrevious;
        locationLastWP = null;
        markerCount = 0;
        textViewWPCount.setText(Integer.toString(markerCount));
        mGoogleMap.clear();

        cResetDistance = 0;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(17));

        // if there was initial location received, move map to it
        if (locationPrevious != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())));
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @Override
    public void onLocationChanged(Location location) {
        long timeCurrent = System.currentTimeMillis() / 1000L;
        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());

        if (mGoogleMap==null) return;

        if (mOptionsMenu.findItem(R.id.menu_keepmapcentered).isChecked() || locationPrevious == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            List<LatLng> points = mPolyline.getPoints();
            points.add(newPoint);
            mPolyline.setPoints(points);
        }

        if (locationStart == null){
            locationStart = location;
            return;
        }
        if (locationPrevious != null){
            paceCurrent = calculatePace(timePrevious, timeCurrent, locationPrevious, location);
            updateDistances(location);
        }

        locationPrevious = location;
        updateDisplay();
        updateNotification();
        timePrevious = timeCurrent;
    }

    public String calculatePace(long prevTime, long curTime, Location prevLoc, Location curLoc){
        long time = curTime - prevTime;

        double dist = prevLoc.distanceTo(curLoc);

        int pacesec = (int)(time/(dist/1000));


        int hours = pacesec/3600;
        int minutes = pacesec/60;
        int seconds = pacesec%60;


        return String.format("%02d:%02d", minutes, seconds);
    }





    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    protected void onResume(){
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.requestLocationUpdates(provider, 500, 1, this);
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.removeUpdates(this);
        }
    }

    public void createNotification(){

        // get the view layout
        RemoteViews remoteView = new RemoteViews(
                getPackageName(), R.layout.notification);

        // define intents
        PendingIntent pIntentAddWaypoint = PendingIntent.getBroadcast(
                this,
                0,
                new Intent("addwaypoint"),
                0
        );

        PendingIntent pIntentResetTripmeter = PendingIntent.getBroadcast(
                this,
                0,
                new Intent("resetodo"),
                0
        );

        // bring back already running activity
        // in manifest set android:launchMode="singleTop"
        PendingIntent pIntentOpenActivity = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // attach events
        remoteView.setOnClickPendingIntent(R.id.buttonAddWayPointNotif, pIntentAddWaypoint);
        remoteView.setOnClickPendingIntent(R.id.buttonResetTripmeterNotif, pIntentResetTripmeter);
        remoteView.setOnClickPendingIntent(R.id.buttonOpenActivityNotif, pIntentOpenActivity);

        // build notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContent(remoteView)
                        .setSmallIcon(R.drawable.ic_my_location_white_48dp);

        // notify
        mNotificationManager.notify(4, mBuilder.build());

    }

    public void updateNotification(){
        RemoteViews remoteView = new RemoteViews(
                getPackageName(), R.layout.notification);
        DecimalFormat df = new DecimalFormat("#");
        df.setRoundingMode(RoundingMode.HALF_UP);
        remoteView.setTextViewText(R.id.textViewTripmeterWayPoint, "WP: "+df.format(wayPointDistance));
        remoteView.setTextViewText(R.id.textViewTripmeterMetricsNotif, "ODO: "+df.format(cResetDistance));
        remoteView.setTextViewText(R.id.textViewWayPointMetricsNotif, Integer.toString(markerCount));

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContent(remoteView)
                        .setSmallIcon(R.drawable.ic_my_location_white_48dp);

        mNotificationManager.notify(4, mBuilder.build());
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }
}
