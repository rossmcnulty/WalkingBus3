package ut.walkingbus;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ut.walkingbus.Models.RoutePublic;
import ut.walkingbus.Models.School;

import static ut.walkingbus.R.id.map;

public class RouteMapActivity extends FragmentActivity implements OnInfoWindowClickListener, OnMapReadyCallback {
    private static final String TAG = "RouteMapActivity";

    private GoogleMap mMap;
    private ArrayList<RoutePublic> mRoutes;
    private ArrayList<String> mRouteKeys;
    private School mSchool;
    private String mStudentKey;
    private String mStudentName;
    private String mTimeslot;
    private String mSchoolKey;
    private String mCurrentRouteKey;
    private Marker mSchoolMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

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
        mMap.setInfoWindowAdapter(new MarkerInfoAdapter(getLayoutInflater()));
        mMap.setOnInfoWindowClickListener(this);

        // get the student's school's routes
        mStudentKey = getIntent().getStringExtra("STUDENT_KEY");
        mTimeslot = getIntent().getStringExtra("TIMESLOT");
        if(getIntent().hasExtra("ROUTE_KEY")) {
            mCurrentRouteKey = getIntent().getStringExtra("ROUTE_KEY");
        } else {
            mCurrentRouteKey = "";
        }

        DatabaseReference studentRef = FirebaseUtil.getStudentsRef().child(mStudentKey);

        mRoutes = new ArrayList<RoutePublic>();
        mRouteKeys = new ArrayList<String>();

        studentRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mStudentName = dataSnapshot.getValue().toString();
                Log.d(TAG, "Student Name " + mStudentName);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        studentRef.child("school").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mSchoolKey = dataSnapshot.getValue().toString();
                DatabaseReference schoolRef = FirebaseUtil.getSchoolRef().child(mSchoolKey);
                Log.d(TAG, "School ref: " + schoolRef);
                schoolRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mSchool = dataSnapshot.getValue(School.class);
                        mSchool.setKey(dataSnapshot.getKey());

                        LatLng schoolLocation = new LatLng(mSchool.getLat(), mSchool.getLng());
                        mSchoolMarker = mMap.addMarker(new MarkerOptions()
                                .position(schoolLocation)
                                .title(mSchool.getName())
                                .snippet("")
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_school)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(schoolLocation));

                        DatabaseReference schoolRoutesRef = FirebaseUtil.getSchoolsRoutesRef(mSchool.getKey());

                        schoolRoutesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for(DataSnapshot routeSnapshot : dataSnapshot.getChildren()) {
                                    String routeKey = routeSnapshot.getKey();

                                    DatabaseReference routePublicRef = FirebaseUtil.getRoutePublicRef(routeKey);

                                    routePublicRef.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            RoutePublic route = dataSnapshot.getValue(RoutePublic.class);
                                            route.setKey(dataSnapshot.getKey());
                                            mRoutes.add(route);
                                            // TODO: get correct chap key based on DB structure decision
                                            String chapKey = route.getChaperone().keySet().toArray()[0].toString();
                                            Map<String, String> chaperone = route.getChaperone().get(chapKey);

                                            final Double lat = route.getLocation().get("lat");
                                            final Double lng = route.getLocation().get("lng");
                                            final LatLng routeStart = new LatLng(lat, lng);
                                            final String routeTime = route.getTime();
                                            final String routeName = route.getName();
                                            final String routeKey = route.getKey();
                                            final String chapName = chaperone.get("displayName");
                                            final String chapPhone = chaperone.get("phone");
                                            final String chapPhotoUrl = chaperone.get("photoUrl");

                                            Marker routeMarker;
                                            if(mCurrentRouteKey.equals(routeKey)) {
                                                routeMarker = mMap.addMarker(new MarkerOptions()
                                                        .position(routeStart)
                                                        .title(routeName)
                                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                                        .snippet("Chaperone: " + chapName +
                                                                "\nTime: " + routeTime +
                                                                "\nClick to select this route"));
                                                routeMarker.setTag(routeKey);
                                            } else {
                                                routeMarker = mMap.addMarker(new MarkerOptions()
                                                        .position(routeStart)
                                                        .title(routeName)
                                                        .snippet("Chaperone: " + chapName +
                                                                "\nTime: " + routeTime +
                                                                "\nClick to select this route"));
                                                routeMarker.setTag(routeKey);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });


                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError firebaseError) { }
                });
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) { }
        });
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        if(marker.getSnippet().equals("")) {
            // skip school markers
            return;
        }

        String routeKey = marker.getTag().toString();

        Map routeStudentsValues = new HashMap();

        routeStudentsValues.put(mStudentKey, mStudentName);

        // TODO: replace existing routes

        Map propagatedRouteData = new HashMap();
        propagatedRouteData.put("students/" + mStudentKey + "/routes/" + mTimeslot.toLowerCase(), marker.getTag().toString());
        propagatedRouteData.put("routes/" + routeKey + "/private/students/" + mTimeslot.toLowerCase() + "/" + mStudentKey, mStudentName);

        FirebaseUtil.getBaseRef().updateChildren(propagatedRouteData,
                new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                        if (firebaseError != null) {
                            Toast.makeText(RouteMapActivity.this,
                                    "Couldn't save route data: " + firebaseError.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

}
