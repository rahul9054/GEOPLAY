package com.example.android.tracking_app;

import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.tasks.OnSuccessListener;

public class UserLocation extends AppCompatActivity {
    private Location lastLocation;
    private FusedLocationProviderClient fusedLocationClient;
    String addressOutput;
    private AddressResultReceiver mresultReceiver;
    MapFragment mMapFragment;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_location);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchAddressButtonHander();
    }

    protected void startIntentService() {
        mresultReceiver = new AddressResultReceiver(new Handler());
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constant.LOCATION_DATA_EXTRA, lastLocation);
        intent.putExtra(Constant.RECEIVER, mresultReceiver);
        startService(intent);
    }

    private void fetchAddressButtonHander() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        lastLocation = location;

                        // In some rare cases the location returned can be null
                        if (lastLocation == null) {
                            return;
                        }

                        if (!Geocoder.isPresent()) {
                            Toast.makeText(UserLocation.this,
                                    "No_geocode_available",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Start service
                        startIntentService();
                    }
                });


    }


    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultData == null) {
                return;
            }

            // Display the address string
            // or an error message sent from the intent service.
            addressOutput = resultData.getString(Constant.RESULT_DATA_KEY);
            if (addressOutput == null) {
                addressOutput = "";
            }
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constant.SUCCESS_RESULT) {
                Toast.makeText(UserLocation.this,
                        "Address_Found",
                        Toast.LENGTH_LONG).show();
            }

        }
    }


    public void displayAddressOutput() {
        TextView userName = (TextView) findViewById(R.id.username);
        userName.setText("MY Location");
        TextView myLocation = (TextView) findViewById(R.id.my_location);
        myLocation.setText("Address : " + addressOutput);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if (id == R.id.action_My_Account) {

            Intent shareIntent = createShareIntent();
            startActivity(shareIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Intent createShareIntent() {
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText("# MY_Location : " + addressOutput)
                .getIntent();
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return shareIntent;
    }

//    public void getMyLocation(View view) {
//        startActivity(new Intent(UserLocation.this, MyLocation.class));
//    }
}
