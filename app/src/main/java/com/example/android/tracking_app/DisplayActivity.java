package com.example.android.tracking_app;


import android.content.Intent;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class DisplayActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final String TAG = DisplayActivity.class.getSimpleName();
    private static final int pick = 100;
    static String timeStamp;
    static File mediaFile;
    int key = -1;
    FusedLocationProviderClient client;
    Button videoButton;
    // String lastSegment;
    String sLastSegment;
    Marker mCurrLocationMarker = null;
    int seconds;
    boolean running;
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private GoogleMap mMap;
    private LocationCallback locationCallback;
    private MediaRecorder mediaRecorder;
    private Camera mCamera;
    private CameraPreview mPreview;
    private boolean isRecording = false;
    final Handler handler = new Handler();
    Runnable runnable;

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
            c.setDisplayOrientation(90);
        } catch (Exception e) {

        }
        return c;
    }

/*
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }
*/

    private static File getOutputMediaFile(int type) {


        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "MyCameraApp");


        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }


        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        videoButton = (Button) findViewById(R.id.video);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mCamera = getCameraInstance();


        //  videoview = (VideoView) findViewById(R.id.playVideo);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));
        mMap.setMaxZoomPreference(16);


    }

    private void requestLocationUpdates(final String filepath) {
        seconds = 0;
        running = true;
        LocationRequest request = new LocationRequest();
        request.setInterval(1000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        client = LocationServices.getFusedLocationProviderClient(this);
        runnable = new Runnable() {
            @Override
            public void run() {

                //  String time = String.format("%d:%02d:%02d", hours, minutes, sec);

                if (running) {
                    seconds++;
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "location update " + location);
                    Map<String, Double> map = new HashMap<>();
                    map.put("time", (double) (seconds));
                    map.put("latitude", location.getLatitude());
                    map.put("Speed", (double) ((int) ((location.getSpeed() * 3600) / 1000)));
                    map.put("Time", (double) location.getTime());
                    map.put("longitude", location.getLongitude());
                    key++;

                    final String path = getString(R.string.firebase_path) + "/" + filepath;
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    ref.push().setValue(map);

                    subscribeToUpdates();
                }
            }
        };
        client.requestLocationUpdates(request, locationCallback, null);

    }

    private void subscribeToUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path) + "/" + sLastSegment);
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {

                setMarker(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                //  setMarker(dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void setMarker(DataSnapshot dataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        String key = dataSnapshot.getKey();

        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
        double lat = Double.parseDouble(value.get("latitude").toString());
        double lng = Double.parseDouble(value.get("longitude").toString());
        LatLng location = new LatLng(lat, lng);
//        if (!mMarkers.containsKey(key)) {
//            mMarkers.put(key, mMap.addMarker(new MarkerOptions().title(key).position(location)));
//        } else {
//            mMarkers.get(key).setPosition(location);
//        }
//        LatLngBounds.Builder builder = new LatLngBounds.Builder();
//        for (Marker marker : mMarkers.values()) {
//            builder.include(marker.getPosition());
//        }

        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.setTitle(key);
            mCurrLocationMarker.setPosition(location);

        } else {
            mCurrLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .title(key));
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 150));


    }

    private boolean prepareVideoRecorder() {

        mCamera = getCameraInstance();
        mediaRecorder = new MediaRecorder();


        mCamera.unlock();

        mediaRecorder.setCamera(mCamera);


        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());


        mediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    public void onClick(View v) {
        mCamera = getCameraInstance();
        if (isRecording) {
            handler.removeCallbacks(runnable);
            videoButton.setBackgroundResource(R.drawable.play);
            mediaRecorder.stop();
            client.removeLocationUpdates(locationCallback);
            releaseMediaRecorder();
            mCamera.lock();


            isRecording = false;
        } else {


            if (prepareVideoRecorder()) {

                mCurrLocationMarker = null;
                mediaRecorder.start();


                mMap.clear();

                videoButton.setBackgroundResource(R.drawable.stop);
                Uri uri = Uri.fromFile(mediaFile);
                String uriLastSegment = uri.getLastPathSegment();
                sLastSegment = uriLastSegment.substring(0, uriLastSegment.length() - 4);

                requestLocationUpdates(sLastSegment);


                isRecording = true;
            } else {

                releaseMediaRecorder();

            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        client.removeLocationUpdates(locationCallback);
        releaseCamera();
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

//    public void inGallery(View view) {
//        Intent galleryIntent = new Intent(DisplayActivity.this, GalleryActivity.class);
//        startActivity(galleryIntent);
//    }

//    public void openGallery(View view) {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, pick);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK && requestCode == pick) {
//            videoUri = data.getData();
//            lastSegment = videoUri.getLastPathSegment();
//            videoview.setVideoURI(videoUri);
//            videoview.start();
//            DatabaseReference refs = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path) +"/"+ lastSegment.substring(0,lastSegment.length()-5));
//            refs.addChildEventListener(new ChildEventListener() {
//                @Override
//                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
//                    setMarker(dataSnapshot);
//                }
//
//                @Override
//                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
//                    setMarker(dataSnapshot);
//                }
//
//                @Override
//                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
//                }
//
//                @Override
//                public void onChildRemoved(DataSnapshot dataSnapshot) {
//                }
//
//                @Override
//                public void onCancelled(DatabaseError error) {
//                    Log.d(TAG, "Failed to read value.", error.toException());
//                }
//            });
//
//        }
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}



