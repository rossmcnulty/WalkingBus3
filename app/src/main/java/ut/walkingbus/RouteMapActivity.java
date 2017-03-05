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

import ut.walkingbus.Models.DatabaseSchool;
import ut.walkingbus.Models.Route;

public class RouteMapActivity extends FragmentActivity implements OnInfoWindowClickListener, OnMapReadyCallback {
    private static final String TAG = "RouteMapActivity";

    private GoogleMap mMap;
    private ArrayList<Route> mRoutes;
    private ArrayList<String> mRouteKeys;
    private DatabaseSchool mSchool;
    private String mStudentKey;
    private String mStudentName;
    private String mTimeslot;
    private String mSchoolKey;
    private Marker mSchoolMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
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
        mMap.setOnInfoWindowClickListener(this);

        // get the student's school's routes
        mStudentKey = getIntent().getStringExtra("STUDENT_KEY");
        mTimeslot = getIntent().getStringExtra("TIMESLOT");

        DatabaseReference studentRef = FirebaseUtil.getStudentsRef().child(mStudentKey);

        mRoutes = new ArrayList<Route>();
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
                        mSchool = dataSnapshot.getValue(DatabaseSchool.class);
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

                                    DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(routeKey);

                                    routeRef.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Route route = dataSnapshot.getValue(Route.class);
                                            route.setKey(dataSnapshot.getKey());
                                            mRoutes.add(route);
                                            String chapKey = route.getChaperone();
                                            String chapName = "";
                                            // TODO: get chap name value programmatically
                                            Double lat = route.getLocation().get("lat");
                                            Double lng = route.getLocation().get("lng");
                                            LatLng routeStart = new LatLng(lat, lng);
                                            Marker routeMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(routeStart)
                                                    .title(route.getName())
                                                    .snippet("Chaperone: " + chapName +
                                                            "\nClick to select this route"));
                                            routeMarker.setTag(route.getKey());
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

        DatabaseReference routeStudentsRef = FirebaseUtil.getRouteStudentsRef(routeKey).child(mTimeslot.toLowerCase());
        routeStudentsRef.updateChildren(routeStudentsValues,
                new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                        if (firebaseError != null) {
                            Toast.makeText(RouteMapActivity.this,
                                    "Couldn't save route student data: " + firebaseError.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Map studentRoutesValues = new HashMap();
                            studentRoutesValues.put(mTimeslot, marker.getTag().toString());
                            DatabaseReference studentRoutesRef = FirebaseUtil.getStudentRoutesRef(mStudentKey);
                            studentRoutesRef.updateChildren(studentRoutesValues, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError != null) {
                                        Toast.makeText(RouteMapActivity.this,
                                                "Couldn't save student route data: " + databaseError.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }
                });
    }

}
