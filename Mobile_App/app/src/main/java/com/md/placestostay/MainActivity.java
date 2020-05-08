package com.md.placestostay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {

    MapView mv;
    ArrayList<Place> unsavedPlaces = new ArrayList<>();
    ArrayList<Place> loadedPlaces = new ArrayList<>();
    ItemizedIconOverlay<OverlayItem> mapMarkers;
    ItemizedIconOverlay.OnItemGestureListener<OverlayItem> markerGestureListener;
    Double gpsLat = null;
    Double gpsLon = null;
    boolean autoLocalSave = false;
    boolean autoLocalLoad = false;
    boolean autoRemoteSave = false;
    boolean autoRemoteLoad = false;

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

        //to make it so when you click on a place to stay it shows you its information.
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

        //creates the overlays.
        mapMarkers = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), markerGestureListener);

        //links overlays to map.
        mv.getOverlays().add(mapMarkers);
    }

    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        autoLocalSave = prefs.getBoolean("auto_local_save", false);
        autoLocalLoad = prefs.getBoolean("auto_local_load", false);
        autoRemoteSave = prefs.getBoolean("auto_remote_save", false);
        autoRemoteLoad = prefs.getBoolean("auto_remote_load", false);
        if (autoLocalLoad) {
            loadLocalPTS();
        }
        if (autoRemoteLoad) {
            InnerRemoteLoad remoteLoad = new InnerRemoteLoad();
            remoteLoad.execute();
        }
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
            return true;
        } else if (item.getItemId() == R.id.save) {
            savePTSLocally();
            return true;
        } else if (item.getItemId() == R.id.load) {
            loadLocalPTS();
            return true;
        } else if (item.getItemId() == R.id.remote_load) {
            InnerRemoteLoad remoteLoad = new InnerRemoteLoad();
            remoteLoad.execute();
            return true;
        } else if (item.getItemId() == R.id.remote_save) {
            InnerRemoteSave remoteSave = new InnerRemoteSave();
            remoteSave.execute();
            return true;
        } else if (item.getItemId() == R.id.preferences) {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivityForResult(intent, 1);
            return true;
        }
        return false;
    }

    //this actions the returned intent from other activity's.
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
                addToOverlay(unsavedPlaces, mapMarkers);
                if (autoLocalSave) {
                    savePTSLocally();
                }
                if (autoRemoteSave) {
                    InnerRemoteSave remoteSave = new InnerRemoteSave();
                    remoteSave.execute();
                }
                Toast.makeText(this, "New PTS added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to add new PTS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClick(View v) {
    }

    public void addToOverlay(ArrayList<Place> places, ItemizedIconOverlay<OverlayItem> overlay) {
        DecimalFormat df = new DecimalFormat("#.00");
        Integer length = places.size();
        for (Integer i = 0; i < length; i++) {
            Place place = places.get(i);
            String title = place.getName();
            String snippet = "Name: " + place.getName() + "\n" + "Type: " + place.getType() + "\n" + "Price: £" + df.format(place.getPrice());
            overlay.addItem(new OverlayItem(title, snippet, new GeoPoint(place.getLatitude(), place.getLongitude())));
        }
    }

    public void savePTSLocally() {
        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PlacesToStay.txt", true));
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
            FileReader fr = new FileReader(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PlacesToStay.txt");
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
                    addToOverlay(loadedPlaces, mapMarkers);
                    Toast.makeText(this, "Local load complete", Toast.LENGTH_SHORT).show();
                }
            }
            reader.close();
        } catch (IOException e) {
            new AlertDialog.Builder(this).setPositiveButton("OK", null).
                    setMessage("ERROR: " + e).show();
        }
    }

    class InnerRemoteLoad extends AsyncTask<Void, Void, String> {

        public String doInBackground(Void... unused) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://www.hikar.org/course/ws/get.php?year=20&username=user008&format=csv");
                conn = (HttpURLConnection) url.openConnection();
                InputStream in = conn.getInputStream();
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        String[] ptsComponents = line.split(",");
                        if (ptsComponents.length == 5) {
                            String name = ptsComponents[0];
                            String type = ptsComponents[1];
                            Double price = Double.parseDouble(ptsComponents[2]);
                            Double latitude = Double.parseDouble(ptsComponents[4]);
                            Double longitude = Double.parseDouble(ptsComponents[3]);
                            Place place = new Place(name, type, price, latitude, longitude);
                            MainActivity.this.loadedPlaces.add(place);
                        }
                    }
                    MainActivity.this.addToOverlay(loadedPlaces, mapMarkers);
                    return "Remote load completed";
                } else {
                    return "HTTP ERROR: " + conn.getResponseCode();
                }
            } catch (IOException e) {
                return e.toString();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        public void onPostExecute(String result) {
            new AlertDialog.Builder(MainActivity.this).setMessage(result).
                    setPositiveButton("OK", null).show();
        }
    }

    class InnerRemoteSave extends AsyncTask<Void, Void, String> {
        public String doInBackground(Void... unused) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://www.hikar.org/course/ws/add.php");
                Integer unsavedPlacesSize = MainActivity.this.unsavedPlaces.size();
                System.out.println(unsavedPlacesSize);
                String all = "";
                for (Integer i = 0; i < unsavedPlacesSize; i++) {
                    Place place = MainActivity.this.unsavedPlaces.get(i);
                    String name = place.getName();
                    String type = place.getType();
                    Double price = place.getPrice();
                    Double latitude = place.getLatitude();
                    Double longitude = place.getLongitude();
                    String postData = "username=user008&name=" + name + "&type=" + type + "&price=" + price + "&lon=" + longitude + "&lat=" + latitude + "&year=20";
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setFixedLengthStreamingMode(postData.length());
                    OutputStream out = null;
                    out = conn.getOutputStream();
                    out.write(postData.getBytes());
                    if (conn.getResponseCode() == 200) {
                        InputStream in = conn.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        String line = "";
                        while ((line = br.readLine()) != null)
                            all += line;
                    } else {
                        return "HTTP ERROR: " + conn.getResponseCode();
                    }
                    conn.disconnect();
                }
                return "Remote save completed";
            } catch (IOException e) {
                return e.toString();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        public void onPostExecute(String result) {
            new AlertDialog.Builder(MainActivity.this).setMessage(result).
                    setPositiveButton("OK", null).show();
        }
    }
}
