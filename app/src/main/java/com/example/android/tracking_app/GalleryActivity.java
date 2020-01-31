package com.example.android.tracking_app;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

public class GalleryActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener, GoogleMap.OnPolylineClickListener {

    private static final String TAG = GalleryActivity.class.getSimpleName();
    private static final int pick = 100;
    private static final int COLOR_BLACK_ARGB = 0xFF00ACC1;
    private static final int POLYLINE_STROKE_WIDTH_PX = 16;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem GAP = new Gap(3);
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(DOT);
    final Handler handler = new Handler();
    public HashMap<Double, Map<String, Object>> al = new HashMap<>();
    public ArrayList<LatLng> pl = new ArrayList<>();
    String lastSegment;
    Uri videoUri;
    VideoView videoview;
    Marker mCurrLocationMarker = null;
    int key = 0;
    double lat;
    double lng;
    double timer;
    Runnable runnable;
    LatLng slatlng;
    LatLng location;
    Marker mstartmarker = null;
    TextView speed;
    TextView time;


//    public GalleryActivity(Queue<Map<String, Object>> al) {
//        this.al = al;
//    }

    //    public GalleryActivity(Queue<Map<String, Object>> al) {
//        this.al = al;
//    }
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity);
        videoview = (VideoView) findViewById(R.id.playVideo);
        time = (TextView) findViewById(R.id.text);
        speed = (TextView) findViewById(R.id.texts);
        SupportMapFragment mapsFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maps);
        mapsFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));
        mMap.setMaxZoomPreference(16);


    }

    public void openGallery(View view) {
        handler.removeCallbacks(runnable);
        videoview.stopPlayback();
        mMap.clear();
        al.clear();
        videoUri = null;

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, pick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == pick) {
            videoUri = data.getData();
            lastSegment = videoUri.getLastPathSegment();
//            videoview.setVideoURI(videoUri);
//            MediaController mediaController = new MediaController(this);
//            mediaController.setMediaPlayer(videoview);
//            videoview.setMediaController(mediaController);
            videoview.setVideoURI(videoUri);
            MediaController mediaController = new MediaController(this);
            mediaController.setMediaPlayer(videoview);
            videoview.setMediaController(mediaController);
            videoview.start();

            DatabaseReference refs = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path) + "/" + lastSegment.substring(0, lastSegment.length() - 4));
            // My top posts by number of stars
            refs.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        Map<String, Object> value = (Map<String, Object>) postSnapshot.getValue();
                        double videoPosition = Double.parseDouble(value.get("time").toString());
                        lat = Double.parseDouble(value.get("latitude").toString());
                        lng = Double.parseDouble(value.get("longitude").toString());

                        LatLng locat = new LatLng(lat, lng);
                        pl.add(locat);
                        al.put(videoPosition, value);
                    }

                    addpolyline(pl);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

                }
            });


        }
    }


    //

    public void addpolyline(ArrayList<LatLng> pl) {
        Polyline polyline1 = mMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .addAll(pl));
        polyline1.setTag("A");
        mMap.setOnPolylineClickListener( this);
        stylePolyline(polyline1);
        getLocationData(al);
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

        if ((polyline.getPattern() == null) || (!polyline.getPattern().contains(DOT))) {
            polyline.setPattern(PATTERN_POLYLINE_DOTTED);
        } else {

            polyline.setPattern(null);
        }

    }


    private void stylePolyline(Polyline polyline) {
        String type = "";

        if (polyline.getTag() != null) {
            type = polyline.getTag().toString();
        }

        switch (type) {

            case "A":
                polyline.setStartCap(new RoundCap());
                break;
            case "B":

                break;
        }
        polyline.setEndCap(new RoundCap());
        polyline.setWidth(POLYLINE_STROKE_WIDTH_PX);
        polyline.setColor(COLOR_BLACK_ARGB);
        polyline.setJointType(JointType.ROUND);
    }


    public void getLocationData(final HashMap<Double, Map<String, Object>> al) {
        location = null;
        mCurrLocationMarker = null;
        mstartmarker = null;

        timer = 0;

        runnable = new Runnable() {
            @Override
            public void run() {

                Map<String, Object> locationData = al.get((double) getTime(videoview.getCurrentPosition()));
                if (locationData != null) {
                    // if (getTime(videoview.getCurrentPosition()) ==  (locationData.get("time")))

                    timer = Double.parseDouble(locationData.get("time").toString());
                    if (timer == getTime(videoview.getCurrentPosition())) {
                        lat = Double.parseDouble(locationData.get("latitude").toString());

                     time.setText(new SimpleDateFormat("yy:MM:dd  HH:mm ").format(new Date((Long) locationData.get("Time"))));
                      speed.setText(((locationData.get("Speed").toString()) + "Km/h"));
                        lng = Double.parseDouble(locationData.get("longitude").toString());
                        location = new LatLng(lat, lng);
//                        mMap.addMarker(new MarkerOptions()
//                                .position(location)
//                                .title(String.valueOf(key)));


//                           mMap.addMarker(new MarkerOptions()
//                                    .position(location)
//                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
//                                    .title(String.valueOf(key)));

//                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 150));
                        if (mCurrLocationMarker != null) {
                            mCurrLocationMarker.setTitle(String.valueOf(timer));
                            mCurrLocationMarker.setPosition(location);

                        } else {


                            mCurrLocationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                    .title(String.valueOf(timer)));
//                            mMap.addMarker(new MarkerOptions()
//                                    .position(location)
//                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
//                                    .title("Start"));

                        }
                        if (mstartmarker == null) {
                            mstartmarker = mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                    .title("Start"));

                        }

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 150));

                    }

                } else {
                    LatLng location = new LatLng(lat, lng);
                    if (mCurrLocationMarker != null) {
                        mCurrLocationMarker.setTitle(String.valueOf(timer));
                        mCurrLocationMarker.setPosition(location);

                    }
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 150));
                }


                handler.postDelayed(this, 1000);
            }
        };


        handler.post(runnable);
    }

    int getTime(long ms) {
        ms /= 1000;
        return (int) ms;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {

    }
}

//
//    public void Json() {
//        String url = "https://trackingapp-282d6.firebaseio.com/" + "/" + lastSegment + ".json";
//        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
//            @Override
//            public void onResponse(String s) {
//                doOnSuccess(s);
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError volleyError) {
//                System.out.println("" + volleyError);
//            }
//        });
//        RequestQueue rQueue = Volley.newRequestQueue(GalleryActivity.this);
//        rQueue.add(request);
//
//    }
//
//    public void doOnSuccess(String s) {
//        try {
//            JSONObject obj = new JSONObject(s);
//            JSONArray video = obj.getJSONArray(lastSegment);
//            for (int i = 0; i < video.length(); i++) {
//                JSONObject videoLocation = video.getJSONObject(i);
//                Double lat = videoLocation.getDouble("latitude");
//                Double lang = videoLocation.getDouble("longitude");
//                Map<String, Double> map = new HashMap<>();
//                map.put("latitude",lat);
//                map.put("longitude",lang);
//                al.add(map) ;
//            }
//
//            } catch(JSONException e){
//                e.printStackTrace();
//            }
//
//    }
//}
