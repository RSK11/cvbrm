package edu.calpoly.cvbrm;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class MapsActivity extends AppCompatActivity implements  OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST = 10;
    private GoogleMap mMap;

    private CompoundButton mAnimateToggle;
    private CompoundButton mCustomDurationToggle;
    private SeekBar mCustomDurationBar;
    private PolylineOptions currPolylineOptions;
    private boolean isCanceled = false;
    private boolean routeLoaded = false;
    private boolean mapReady = false;
    private LatLng[] beforePoints;
    private ArrayList<LatLng> afterPoints;
    private Button startLocButton;
    private EditText startLocTextField;
    private int disabledGrey = Color.parseColor("#888888");
    private int onGrey = Color.parseColor("#CCCCCC");

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation;
    private LatLng location;
    private boolean usingGeoLoc = true;
    private boolean mRequestingLocationUpdates = true;
    private LocationRequest mLocationRequest;
    private Marker navMarker;

    /**
     * The amount by which to scroll the camera. Note that this amount is in raw pixels, not dp
     * (density-independent pixels).
     */
    private static final int SCROLL_BY_PX = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLastLocation = new Location("startPosition");
        mLastLocation.setLatitude(35.3050);
        mLastLocation.setLongitude(120.6625);
        PointConverter.startLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());


        startLocButton = (Button) findViewById(R.id.go_to_location);
        startLocTextField = (EditText) findViewById(R.id.start_location);
        ImageButton locButton = (ImageButton)findViewById(R.id.location_button);


        if (getIntent().getStringExtra("routename") != null) {
            String rname = getIntent().getStringExtra("routename");
            loadRoute(rname);
            startLocButton.setEnabled(false);
            startLocTextField.setEnabled(false);
            locButton.setEnabled(false);

            startLocButton.getBackground().setTint(disabledGrey);
            locButton.getBackground().setTint(disabledGrey);
        }
        else {
            startLocButton.setEnabled(true);
            startLocTextField.setEnabled(true);
            locButton.setEnabled(true);

            startLocButton.getBackground().setTint(onGrey);
            locButton.getBackground().setTint(onGrey);
        }

        //mAnimateToggle = (CompoundButton) findViewById(R.id.animate);
        //mCustomDurationToggle = (CompoundButton) findViewById(R.id.duration_toggle);
        //mCustomDurationBar = (SeekBar) findViewById(R.id.duration_bar);

        startLocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usingGeoLoc = false;
                mMap.clear();
                Log.d("Address", startLocTextField.getText().toString());
                PointConverter.startLocation = getCoordinatesFromAddress(startLocTextField.getText().toString());
                Log.d("LatLong", PointConverter.startLocation.latitude + ":" + PointConverter.startLocation.longitude);
                // Change to be the lastLocation
                CameraPosition newPos = coordinatesToCamPosition(PointConverter.startLocation);
                onGoToStartLocation(newPos);
                generateRoute();
                mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radiobox_marked_black_18dp)).position(location));
            }
        });


        ((ImageButton)findViewById(R.id.location_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (location != null) {
                    usingGeoLoc = true;
                    mMap.clear();
                    PointConverter.startLocation = location;
                    CameraPosition newPos = coordinatesToCamPosition(PointConverter.startLocation);
                    onGoToStartLocation(newPos);
                    generateRoute();
                    mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radiobox_marked_black_18dp)).position(location));
                }
                else {
                    Toast.makeText(v.getContext(), "Current location not available. Turn on Location services and try again.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            Log.d("GEO", "Built the apiclient");
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST);
            return;
        }
        Log.d("GEO", "Connected to location services");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        usingGeoLoc = true;
        location = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radiobox_marked_black_18dp)).position(location));
        PointConverter.startLocation = location;
        if (afterPoints == null || afterPoints.size() < 1) {
            CameraPosition newPos = coordinatesToCamPosition(PointConverter.startLocation);
            onGoToStartLocation(newPos);
        }
        //generateRoute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull
            String permissions[],@NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {

                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        usingGeoLoc = true;
                        location = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location_black_24dp)).position(location));
                        PointConverter.startLocation = location;
                    } catch (SecurityException ex) {
                        ex.printStackTrace();
                    }

                } else {
                    mLastLocation = new Location("startPosition");
                    mLastLocation.setLatitude(35.3050);
                    mLastLocation.setLongitude(120.6625);
                    PointConverter.startLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                }

                if (mLastLocation != null) {
                    updateLoc();
                }
            }
        }
    }

    private void updateLoc() {
        LatLng myLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc));
        mMap.addMarker(new MarkerOptions().position(myLoc).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location_black_24dp)));
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("MAPLOG", "CONNECTION SUS");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("MAPLOG", "CONNECTION FAILED");
    }

    /**
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        stopLocationUpdates();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    } */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mInflater = getMenuInflater();
        mInflater.inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_route:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                final View fView = getLayoutInflater().inflate(R.layout.field_layout, null);

                builder.setView(fView);
                builder.setTitle("Save Route");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fname = ((EditText)fView.findViewById(R.id.dialog_field)).getText().toString().trim();
                        if (fname.length() > 0) {
                            saveRoute(fname);
                        }
                    }
                });
                builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveRoute(String fname) {
        if (afterPoints != null && afterPoints.size() > 0) {
            Gson gson = new Gson();
            String routeJson = gson.toJson(afterPoints);
            File outrte = new File(getDir("routes",MODE_PRIVATE), fname + "_route");
            try {
                FileWriter fw = new FileWriter(outrte);
                fw.write(routeJson);
                fw.flush();
                fw.close();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Could not save route.", Toast.LENGTH_SHORT).show();
            }
            File outimg = new File(getDir("routes",MODE_PRIVATE), fname + "_image");
            try {
                FileOutputStream fos = new FileOutputStream(outimg);
                PointConverter.getImage().compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Could not save image.", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this,"Could not save route.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRoute(String rname) {
        try {
            Gson gson = new Gson();
            File rfi = new File(getDir("routes", MODE_PRIVATE), rname + "_route");
            FileReader fr = new FileReader(rfi);
            afterPoints = gson.fromJson(fr, new TypeToken<List<LatLng>>() {
            }.getType());
            if (mapReady) {
                drawAfterPoints();
                mapReady = false;
            }
            else {
                routeLoaded = true;
            }
        }
        catch (Exception ex) {
            Toast.makeText(this,"Could not load route.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }**/

    public interface GoogleRoadsService {
        @GET("v1/snapToRoads")
        Call<MappedPoints> snapToRoads(@Query("path") String coordinates,@Query("interpolate") boolean interpolate, @Query("key") String apiKey);
    }

    private class MappedPoints {
        public List<SnapPoint> snappedPoints;

        public class SnapPoint {
            public class Location {
                double latitude;
                double longitude;
            }
            public Location location;
            public int originalIndex;
            public String placeId;
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
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // We will provide our own zoom controls.
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        // Show start location
        if (routeLoaded) {
            drawAfterPoints();
            routeLoaded = false;
        }
        else {
            mapReady = true;
        }
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 15.5f));

        //generateRoute();
    }

    private void drawAfterPoints() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(afterPoints.get(0), 15.5f));

        PolylineOptions po = new PolylineOptions();
        for (LatLng sp : afterPoints) {
            po.add(sp);
        }
        Polyline pLine =  mMap.addPolyline(po
                .width(5)
                .color(Color.BLUE));
    }

    private void changeCamera(CameraUpdate update) {
        mMap.moveCamera(update);
    }

    private LatLng getCoordinatesFromAddress(String address) {
        double latitude = 0;
        double longitude = 0;
        try {
            Log.d("Geocoder", Geocoder.isPresent() + "");
            Geocoder geocoder = new Geocoder(this.getApplicationContext());
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses.size() > 0) {
                longitude = addresses.get(0).getLongitude();
                latitude = addresses.get(0).getLatitude();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return new LatLng(latitude, longitude);
    }

    private CameraPosition coordinatesToCamPosition(LatLng coords) {
        if (!usingGeoLoc) {
            mMap.addMarker(new MarkerOptions().position(coords).title("Start Location"));
        }

        return new CameraPosition.Builder().target(coords).zoom(15.5f).bearing(0).tilt(0).build();
    }

    private void onGoToStartLocation(CameraPosition startPoint) {
        changeCamera(CameraUpdateFactory.newCameraPosition(startPoint));
    }

    private void generateRoute() {
        AsyncTask<Void,Void,MappedPoints> getSnappedRoads = new AsyncTask<Void,Void,MappedPoints>() {
            @Override
            protected MappedPoints doInBackground(Void... params) {
                try {
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("https://roads.googleapis.com/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    GoogleRoadsService roadsService = retrofit.create(GoogleRoadsService.class);
                    beforePoints = PointConverter.convert(PointConverter.startLocation);
                    String pointss = PointConverter.makePathString(beforePoints);
                    Call<MappedPoints> response = roadsService.snapToRoads(pointss, true, "AIzaSyDRLuzE7YMdQAR7WmxQ5I6e4eyB5FuT1n4");
                    Response<MappedPoints> res = response.execute();
                    return res.body();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }


                return null;
            }

            @Override
            protected void onPostExecute(MappedPoints mp) {
                super.onPostExecute(mp);
                PolylineOptions po = new PolylineOptions();
                afterPoints = new ArrayList<LatLng>();
                for (MappedPoints.SnapPoint sp : mp.snappedPoints) {
                    LatLng ltln = new LatLng(sp.location.latitude, sp.location.longitude);
                    po.add(ltln);
                    afterPoints.add(ltln);
                }
                Polyline pLine =  mMap.addPolyline(po
                        .width(5)
                        .color(Color.BLUE));
                PolylineOptions po2 = new PolylineOptions();
                for (LatLng llng: beforePoints) {
                    po2.add(llng);
                }
                Polyline p2Line =  mMap.addPolyline(po2
                        .width(5)
                        .color(Color.rgb(255, 165, 0)));
                generateRouteURL(mp.snappedPoints);
            }
        };
        getSnappedRoads.execute();
    }

    private void generateRouteURL(List<MappedPoints.SnapPoint> snapPoints) {
        String url = "https://www.google.com/maps/dir/";
        int i;

        for(i = 0; i < snapPoints.size() - 1; i++) {
            url += snapPoints.get(i).location.latitude + "," + snapPoints.get(i).location.longitude + "/";
        }
        url += "@" + snapPoints.get(i).location.latitude + "," + snapPoints.get(i).location.longitude + ",17z/data=!4m2!4m1!3e1";

        Log.d("MAP URL", url);
    }

}
