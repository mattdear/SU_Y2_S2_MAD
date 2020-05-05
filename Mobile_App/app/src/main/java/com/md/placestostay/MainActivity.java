package com.md.placestostay;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {

    MapView mv;
    ArrayList<Place> unsavedPlaces = new ArrayList<>();
    ArrayList<Place> loadedPlaces = new ArrayList<>();
    ItemizedIconOverlay<OverlayItem> unsavedPlacesToStay;
    ItemizedIconOverlay<OverlayItem> loadedPlacesToStay;
    ItemizedIconOverlay.OnItemGestureListener<OverlayItem> markerGestureListener;
    Double gpsLat = null;
    Double gpsLon = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //this allows maps to be displayed on screen.
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        //this calls the phones location from the GPS and returns it to the app.
        LocationManager mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        //defines the map view and sets the initial GPS location.
        mv = findViewById(R.id.map1);
        mv.setMultiTouchControls(true);
        mv.getController().setZoom(16.0);
        mv.getController().setCenter(new GeoPoint(51.05, -0.72));

        //To make it so when you click on a place to stay it shows you its information.
        markerGestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            public boolean onItemLongPress(int i, OverlayItem item) {
                Toast.makeText(MainActivity.this, item.getSnippet(), Toast.LENGTH_LONG).show();
                return true;
            }

            public boolean onItemSingleTapUp(int i, OverlayItem item) {
                Toast.makeText(MainActivity.this, item.getSnippet(), Toast.LENGTH_LONG).show();
                return true;
            }
        };

        //creates the overlays
        unsavedPlacesToStay = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), markerGestureListener);
        loadedPlacesToStay = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), markerGestureListener);

        //links overlays to map.
        mv.getOverlays().add(unsavedPlacesToStay);
        mv.getOverlays().add(loadedPlacesToStay);

    }

    //updates the map view base on GPS location.
    public void onLocationChanged(Location newLocation) {
        gpsLat = newLocation.getLatitude();
        gpsLon = newLocation.getLongitude();
        mv.getController().setCenter(new GeoPoint(gpsLat, gpsLon));
    }

    //alert for GPS provider disabled.
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Provider " + provider + " disabled", Toast.LENGTH_SHORT).show();
    }

    //alert for GPS provider enabled.
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Provider " + provider + " enabled", Toast.LENGTH_SHORT).show();
    }

    //alert for GPS provider on status change.
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(this, "Status changed: " + status, Toast.LENGTH_SHORT).show();
    }

    //standard android menu inflater.
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    //reaction to user pressing menu button.
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addnew) {
            Intent intent = new Intent(this, AddNewActivity.class);
            startActivityForResult(intent, 0);
        } else if (item.getItemId() == R.id.save) {
            savePTSLocally();
            return true;
        } else if (item.getItemId() == R.id.load) {
            loadLocalPTS();
            return true;
        }
        return false;

    }

    //this actions the returned intent from the child activity's
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                Bundle extras = intent.getExtras();
                String name = extras.getString("name");
                String type = extras.getString("type");
                Double price = extras.getDouble("price");
                Double latitude = gpsLat;
                Double longitude = gpsLon;
                Place place = new Place(name, type, price, latitude, longitude);
                unsavedPlaces.add(place);
                Boolean isComplete = addToOverlay(unsavedPlaces, unsavedPlacesToStay);
                if (isComplete) {
                    Toast.makeText(this, "New PTS added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to add new PTS", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to add new PTS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClick(View v) {
    }

    public boolean addToOverlay(ArrayList<Place> places, ItemizedIconOverlay<OverlayItem> overlay) {
        Integer length = places.size();
        for (Integer i = 0; i < length; i++) {
            Place place = places.get(i);
            String title = place.getName();
            String snippet = "Name: " + place.getName() + "\n" + "Type: " + place.getType() + "\n" + "Price: £" + place.getPrice();
            overlay.addItem(new OverlayItem(title, snippet, new GeoPoint(place.getLatitude(), place.getLongitude())));
        }
        return true;
    }

    public void savePTSLocally() {
        try {
            PrintWriter pw =
                    new PrintWriter(new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/localPlacesToStay.txt"));
            Integer unsavedPlacesSize = unsavedPlaces.size();
            for (Integer i = 0; i < unsavedPlacesSize; i++) {
                Place place = unsavedPlaces.get(i);
                String name = place.getName();
                String type = place.getType();
                Double price = place.getPrice();
                Double latitude = place.getLatitude();
                Double longitude = place.getLongitude();
                pw.println(name + "," + type + "," + price + "," + latitude + "," + longitude);
            }
            Toast.makeText(this, "Complete local save", Toast.LENGTH_SHORT).show();
            pw.close();
        } catch (IOException e) {
            new AlertDialog.Builder(this).setPositiveButton("OK", null).
                    setMessage("ERROR: " + e).show();
        }
    }

    public void loadLocalPTS() {
        try {
            FileReader fr = new FileReader(Environment.getExternalStorageDirectory().getAbsolutePath() + "/localPlacesToStay.txt");
            BufferedReader reader = new BufferedReader(fr);
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] ptsComponents = line.split(",");
                if (ptsComponents.length == 5) {
                    String name = ptsComponents[0];
                    String type = ptsComponents[1];
                    Double price = Double.parseDouble(ptsComponents[2]);
                    Double latitude = Double.parseDouble(ptsComponents[3]);
                    Double longitude = Double.parseDouble(ptsComponents[4]);
                    Place place = new Place(name, type, price, latitude, longitude);
                    loadedPlaces.add(place);
                    Boolean isComplete = addToOverlay(loadedPlaces, loadedPlacesToStay);
                    if (isComplete) {
                        Toast.makeText(this, "Local load complete", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed local load", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            new AlertDialog.Builder(this).setPositiveButton("OK", null).
                    setMessage("ERROR: " + e).show();
        }
    }
}
