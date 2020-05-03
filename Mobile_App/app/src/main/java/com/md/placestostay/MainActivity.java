package com.md.placestostay;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {

    MapView mv;
    ItemizedIconOverlay<OverlayItem> placesToStay;
    ItemizedIconOverlay.OnItemGestureListener<OverlayItem> markerGestureListener;
    Double lat = null;
    Double lon = null;

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

    }

    //updates the map view base on GPS location.
    public void onLocationChanged(Location newLocation) {
        lat = newLocation.getLatitude();
        lon = newLocation.getLongitude();
        mv.getController().setCenter(new GeoPoint(lat, lon));
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
        }
        return false;
    }

    //this actions the returned intent from the child activity's
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                placesToStay = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), markerGestureListener);
                Bundle extras = intent.getExtras();
                String title = extras.getString("name");

                String description = "Name: " + extras.getString("name") + "\n";
                description += "Type: " + extras.getString("type") + "\n";
                description += "Price: £" + extras.getDouble("price");
                placesToStay.addItem(new OverlayItem(title, description, new GeoPoint(lat, lon)));
                mv.getOverlays().add(placesToStay);
            } else {
                Toast.makeText(this, "Failed to add new PTS", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void onClick(View v) { }
}
