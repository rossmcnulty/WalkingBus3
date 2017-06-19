package ut.walkingbus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import ut.walkingbus.Models.RoutePrivate;
import ut.walkingbus.Models.RoutePublic;
import ut.walkingbus.Models.Student;
import ut.walkingbus.Models.User;

/**
    This contains the menu, student adapter and controls, and route information. This handles
    all Bluetooth interaction between student and chaperone.
 */

public class ChaperoneActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener  {
    private static final String TAG = "ChaperoneActivity";
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Student> mStudents;
    private ChaperoneStudentAdapter mStudentAdapter;
    private RoutePublic mRoutePublic;
    private RoutePrivate mRoutePrivate;
    private String mRouteKey;
    private String mTimeslot;
    private Context mContext;
    private int mCurrentDay;
    private boolean mGpsDropoffPrompted;
    private static final int INTERVAL = 1000*60;

    private ArrayList<String> mLostStudentPopups;

    //private ArrayList<String> mExpectedBluetooth;
    private ArrayList<String> mFoundBluetooth;
    private ArrayList<String> mPickedUpBluetooth;
    private Map<String, Integer> mBluetoothMisses;

    private CircleImageView mProfilePhoto;
    private TextView mProfileEmail;
    private TextView mProfileUsername;

    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 2;
    private ArrayList<BluetoothDevice> sensorTagDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBLEAdapter;
    private BluetoothManager manager;
    private Handler scanHandler = new Handler();
    private Handler locationHandler = new Handler();
    private LocationManager mLocationManager;
    private Location chapLocation;
    private Double mSchoolLat;
    private Double mSchoolLng;

    private static final int GPS_TIME_INTERVAL = 1000;
    private static final int GPS_DISTANCE = 0;
    private static final int RC_SIGN_IN = 103;
    private int mLostCalls;
    private int mNotLostCalls;

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        mContext = this;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chaperone);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mGpsDropoffPrompted = false;
        mBluetoothMisses = new HashMap<>();

        mLostCalls = 0;
        mNotLostCalls = 1;

        Log.d(TAG, "New activity created");

        if(getIntent().hasExtra("NOTIFY_CHILD") && getIntent().hasExtra("NOTIFY_IS_LOST")) {
            String childKey = getIntent().getStringExtra("NOTIFY_CHILD");
            boolean isLost = getIntent().getBooleanExtra("NOTIFY_IS_LOST", true);
            Log.d(TAG, "Has extra info: " + childKey + ", " + isLost);

            if(isLost) {
                DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(childKey).child("status");
                DatabaseReference studentLocationRef = FirebaseUtil.getStudentsRef().child(childKey).child("location");
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("BleActivity", "Location access not granted!");
                    ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                } else {
                    Log.d(TAG, "Getting last known location");
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Map locationValue = new HashMap();
                    locationValue.put("lat", lastKnownLocation.getLatitude());
                    locationValue.put("lng", lastKnownLocation.getLongitude());
                    studentLocationRef.setValue(locationValue);
                }
                studentStatusRef.setValue("lost");
                mLostStudentPopups.remove(childKey);
            } else {
                // User cancelled the dialog
                mLostStudentPopups.remove(childKey);
            }
        }

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mContext = this;

        // TODO: stop app if no routes for user

        mRouteKey = "";
        Calendar c = Calendar.getInstance();
        int am_pm = c.get(Calendar.AM_PM);
        int day = c.get(Calendar.DAY_OF_WEEK);
        mTimeslot = "";
        FirebaseUtil.getBaseRef().child("current_timeslot").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mTimeslot = dataSnapshot.getValue().toString();
                Log.d(TAG, "Timeslot: " + mTimeslot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // mTimeslot = "mon_am";
        // TODO: remove this line
        // mTimeslot = "mon_am";

        // TODO: don't hardcode these pls
        FirebaseUtil.getBaseRef().child("current_timeslot").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mTimeslot = dataSnapshot.getValue().toString();
                Log.d(TAG, "Timeslot: " + mTimeslot);

                DatabaseReference chapRef = FirebaseUtil.getUserRef().child(FirebaseUtil.getCurrentUserId());
                chapRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User chap = dataSnapshot.getValue(User.class);
                        // TODO: Select route based on time
                        if(chap.getRoutes() == null || chap.getRoutes().isEmpty()) {
                            Toast.makeText(ChaperoneActivity.this, "User is not chaperone of any routes", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "No routes");
                            return;
                        }

                        mRouteKey = chap.getRoutes().keySet().toArray()[0].toString();
                        DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(mRouteKey);
                        routeRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists() || !dataSnapshot.hasChild("public") || !dataSnapshot.hasChild("private")) {
                                    Log.d(TAG, "Route does not exist");
                                    return;
                                }
                                mRoutePublic = dataSnapshot.child("public").getValue(RoutePublic.class);
                                mRoutePrivate = dataSnapshot.child("private").getValue(RoutePrivate.class);
                                Log.d(TAG, "Route name " + mRoutePublic.getName());
                                if(mRoutePrivate.getStudents() == null) {
                                    Log.d(TAG, "No students in route " + mRoutePublic.getName());
                                    return;
                                }
                                if(mRoutePrivate.getStudents().get(mTimeslot) == null) {
                                    Log.d(TAG, "No students in timeslot " + mTimeslot);
                                    return;
                                }
                                String schoolKey = mRoutePublic.getSchool();
                                Log.d(TAG, "School key: " + schoolKey);
                                DatabaseReference schoolRef = FirebaseUtil.getSchoolRef().child(schoolKey);
                                DatabaseReference latRef = schoolRef.child("lat");
                                DatabaseReference lngRef = schoolRef.child("lng");
                                latRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        mSchoolLat = dataSnapshot.getValue(Double.class);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                lngRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        mSchoolLng = dataSnapshot.getValue(Double.class);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                for(String studentKey: mRoutePrivate.getStudents().get(mTimeslot).keySet()) {
                                    Log.d(TAG, "Student key " + studentKey);
                                    DatabaseReference studentRef = FirebaseUtil.getStudentsRef().child(studentKey);
                                    studentRef.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Log.d(TAG, "Student reference " + dataSnapshot.getRef().toString());
                                            Student s = dataSnapshot.getValue(Student.class);
                                            s.setKey(dataSnapshot.getKey().toString());
                                            boolean found = false;
                                            for(int i = 0; i < mStudents.size(); i++) {
                                                Student student = mStudents.get(i);
                                                if(student.getKey().equals(s.getKey())) {
                                                    mStudents.set(i, s);
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            if(!found) {
                                                Log.d(TAG, "Adding student " + s.getName());
                                                Log.d(TAG, "Adding BT " + s.getBluetooth());
                                                mStudents.add(s);
                                                //mExpectedBluetooth.add(s.getBluetooth());
                                            }
                                            mStudentAdapter.notifyDataSetChanged();
                                            Log.d(TAG, "student name: " + s.getName());
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError firebaseError) { }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        setTitle("Today's Bus");

        mStudents = new ArrayList<Student>();
        mLostStudentPopups = new ArrayList<String>();

        mFoundBluetooth = new ArrayList<>();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            Toast.makeText(this, "GPS is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Prompt for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BleActivity", "Location access not granted!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
        } else {
            Log.d(TAG, "Turning on BLE");
            turnonBLE();
            discoverBLEDevices();
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BleActivity", "Location access not granted!");
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {
            Log.d(TAG, "Location permission granted");
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    GPS_TIME_INTERVAL, GPS_DISTANCE, GPSListener);
        }

        mCurrentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mRoutePublic == null || mRoutePrivate == null) {
                    Log.d(TAG, "Route null");
                    return;
                }
                Log.d(TAG, "FAB Clicked, status " + mRoutePrivate.getStatus());
                // pick up children
                if(mRoutePrivate.getStatus().toLowerCase().equals("waiting")) {
                    AlertDialog.Builder pickUpBuilder = new AlertDialog.Builder(mContext);
                    final ArrayList<Student> foundStudents = new ArrayList<Student>();
                    String studentNames = "";
                    for(Student s : mStudents) {
                        if(mFoundBluetooth.contains(s.getBluetooth())) {
                            foundStudents.add(s);
                            studentNames += "\n " + s.getName();
                            Log.d(TAG, "Student: " + s.getName());
                        }
                    }
                    pickUpBuilder.setMessage("Confirm pickup of" + studentNames)
                            .setTitle("Pickup Confirmation");
                    pickUpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            for(Student s : mStudents) {
                                // Set all found students as picked up
                                if(foundStudents.contains(s)) {
                                    DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                    studentStatusRef.setValue("picked up");
                                    Log.d(TAG, "Setting picked up for " + s.getName() + " to true");
                                } else {
                                    // student left behind
                                    DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                    studentStatusRef.setValue("left behind");
                                    Log.d(TAG, "Setting left behind for " + s.getName() + " to true");
                                }
                            }
                            DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(mRouteKey).child("private").child("status");
                            routeRef.setValue("picked up");
                            dialog.dismiss();
                            Log.d(TAG, "Setting picked up to true");
                        }
                    });
                    pickUpBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            dialog.dismiss();
                        }
                    });
                    AlertDialog pickUpAlert = pickUpBuilder.create();
                    pickUpAlert.setCanceledOnTouchOutside(false);
                    if(!((Activity)view.getContext()).isFinishing()) {
                        // TODO: figure out why the activity would finish before showing this
                        pickUpAlert.show();
                    }
                } else if(mRoutePrivate.getStatus().toLowerCase().equals("picked up")) {
                    AlertDialog.Builder dropOffBuilder = new AlertDialog.Builder(mContext);
                    final ArrayList<Student> pickedUpStudents = new ArrayList<Student>();
                    String studentNames = "";
                    for(Student s : mStudents) {
                        if(s.getStatus().toLowerCase().equals("picked up")) {
                            studentNames += "\n" + s.getName();
                            pickedUpStudents.add(s);
                        }
                    }
                    dropOffBuilder.setMessage("Confirm dropoff of" + studentNames)
                            .setTitle("Dropoff Confirmation");
                    dropOffBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            for(Student s : pickedUpStudents) {
                                // Set all found students as picked up
                                DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                studentStatusRef.setValue("dropped off");
                            }
                            Log.d(TAG, "Setting dropped off to true");
                            DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(mRouteKey).child("private").child("status");
                            routeRef.setValue("dropped off");
                            dialog.dismiss();
                        }
                    });
                    dropOffBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dropOffAlert = dropOffBuilder.create();
                    dropOffAlert.setCanceledOnTouchOutside(false);
                    if(!((Activity)view.getContext()).isFinishing()) {
                        dropOffAlert.show();
                    }
                } else {
                    Log.d(TAG, "Route status does not require chaperone interaction");
                }
            }
        });


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        RecyclerView recycler = (RecyclerView) findViewById(R.id.student_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // get route info

        mStudentAdapter = new ChaperoneStudentAdapter(mStudents, this);

        recycler.setAdapter(mStudentAdapter);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);

        mProfileUsername = (TextView)header.findViewById(R.id.nav_username);
        mProfileEmail = (TextView)header.findViewById(R.id.nav_email);
        mProfilePhoto = (CircleImageView) header.findViewById(R.id.profile_user_photo);

        if (WelcomeActivity.currentUser.getDisplayName() != null) {
            mProfileUsername.setText(WelcomeActivity.currentUser.getDisplayName());
        }

        if (WelcomeActivity.currentUser.getPhotoUrl() != null) {
            GlideUtil.loadProfileIcon(WelcomeActivity.currentUser.getPhotoUrl().toString(), mProfilePhoto);
        }

        if (WelcomeActivity.currentUser.getEmail() != null) {
            mProfileEmail.setText(WelcomeActivity.currentUser.getEmail());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "New intent");
        if(intent.hasExtra("NOTIFY_ACTION")) {
            Log.d(TAG, "Intent extras " + intent.getStringExtra("NOTIFY_ACTION"));
        }
        if(intent.hasExtra("NOTIFY_CHILD") && intent.hasExtra("NOTIFY_IS_LOST")) {
            String childKey = intent.getStringExtra("NOTIFY_CHILD");
            boolean isLost = intent.getBooleanExtra("NOTIFY_IS_LOST", true);
            Log.d(TAG, "Has extra info: " + childKey + ", " + isLost);

            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(childKey.hashCode());

            if(isLost) {
                DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(childKey).child("status");
                DatabaseReference studentLocationRef = FirebaseUtil.getStudentsRef().child(childKey).child("location");
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("BleActivity", "Location access not granted!");
                    ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                } else {
                    Log.d(TAG, "Getting last known location");
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Map locationValue = new HashMap();
                    locationValue.put("lat", lastKnownLocation.getLatitude());
                    locationValue.put("lng", lastKnownLocation.getLongitude());
                    studentLocationRef.setValue(locationValue);
                }
                studentStatusRef.setValue("lost");
                mLostStudentPopups.remove(childKey);
            } else {
                // User cancelled the dialog
                Log.d(TAG, "Child marked as not lost");
                mLostStudentPopups.remove(childKey);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chaperone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.reset:
                Log.d(TAG, "Reset pressed");
                Map reset = new HashMap();
                reset.put("/routes/" + mRouteKey + "/private/status", "waiting");
                for(Student s : mStudents) {
                    Log.d(TAG, "Student: " + s.getName());
                    reset.put("/students/" + s.getKey() + "/status", "waiting");
                }
                FirebaseUtil.getBaseRef().updateChildren(reset);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            this.recreate();
        } else if(id == R.id.nav_sign_out) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
            startActivity(welcomeIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
    }

    @SuppressLint("NewApi")
    private void turnonBLE() {
        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBLEAdapter = manager.getAdapter();
        mBLEAdapter.enable();
        Toast.makeText(getApplicationContext(), "BTLE ON Service",
                Toast.LENGTH_LONG).show();
        Log.d("BLE_Scanner", "TurnOnBLE");}

    @SuppressLint("NewApi")
    private void discoverBLEDevices() {
        startScan.run();
        Log.d("BLE_Scanner", "DiscoverBLE");
    }


    private Runnable startScan = new Runnable() {
        @Override
        public void run() {
            scanHandler.postDelayed(stopScan, 10000); // invoke stop scan after 10 s
            mBLEAdapter.startLeScan(mLeScanCallback);
        }
    };
    public static String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");

        return hex.toString();
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @SuppressLint("NewApi")
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            String address = device.getAddress();
            String name = device.getName();
            byte[] data = scanRecord;
            String dataString = "";
            Integer battery = ((int)data[10]);
            //Integer batteryVoltage = (((int)data[7])<<8) | (data[8] & 0xFF);
            // Simply print all raw bytes
            try {
                String decodedRecord = new String(scanRecord,"UTF-8");
                dataString += (ByteArrayToString(scanRecord));
                //Log.d("DEBUG","decoded data : " + ByteArrayToString(scanRecord));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(!sensorTagDevices.contains(device) && name != null && name.toUpperCase().contains("WALKINGBUS")){
                sensorTagDevices.add(device);
            }

            Log.d("mLeScanCallback", "" + address + " : " + name);
        }
    };


    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            if(mRoutePrivate != null && mRoutePrivate.getStatus().toLowerCase().equals("waiting")) {
                for (Student s : mStudents) {
                    Log.d(TAG, "BT Expected Student " + s.getName());
                    for (BluetoothDevice d : sensorTagDevices) {
                        Log.d(TAG, "BT Expected BT " + d.getAddress());
                        if (d.getAddress().equals(s.getBluetooth())) {
                            Log.d(TAG, "BT Expected BT Found");
                            if(!mFoundBluetooth.contains(s.getBluetooth())) {
                                mFoundBluetooth.add(d.getAddress());
                            }
                            break;
                        }
                    }
                }
            } else {
                for (final Student s : mStudents) {
                    // we've picked up everyone
                    // look for lost students
                    // look for students that have become lost

                    boolean found = false;
                    if(s.getStatus().toLowerCase().equals("left behind")) {
                        // don't worry about non-picked up students
                        Log.d(TAG, "Student " + s.getName() + " was never picked up");
                        continue;
                    }

                    for (BluetoothDevice d : sensorTagDevices) {
                        if (d.getAddress().equals(s.getBluetooth())) {
                            // found a matching student BT
                            found = true;
                            mBluetoothMisses.put(s.getKey(), 0);
                            if (s.getStatus().toLowerCase().equals("lost")) {
                                Log.d(TAG, "Recovered lost student " + s.getName());
                                // Toast.makeText(ChaperoneActivity.this, "Lost student " + s.getName() + " has come back in range", Toast.LENGTH_SHORT).show();
                                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0 /* Request code */, new Intent(mContext, ChaperoneActivity.class),
                                        PendingIntent.FLAG_ONE_SHOT);
                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext)
                                        .setSmallIcon(R.drawable.bus)
                                        .setContentTitle("Child Found")
                                        .setPriority(Notification.PRIORITY_MAX)
                                        .setContentText(s.getName() + " has come back into range")
                                        .setContentIntent(pendingIntent);

                                NotificationManager notificationManager =
                                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                notificationManager.notify(s.getKey().hashCode() /* ID of notification */, notificationBuilder.build());
                                DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                if(mRoutePrivate.getStatus().toLowerCase().equals("dropped off")) {
                                    studentStatusRef.setValue("dropped off");
                                }
                                studentStatusRef.setValue("picked up");
                            }
                        }
                    }

                    if(!mBluetoothMisses.containsKey(s.getKey())) {
                        // initialize misses to 0 for this child
                        mBluetoothMisses.put(s.getKey(), 0);
                    }

                    Log.d(TAG, "LostStudentPopups " + mLostStudentPopups.contains(s.getKey()));

                    if (!found && !mRoutePrivate.getStatus().toLowerCase().equals("dropped off")
                            && !s.getStatus().toLowerCase().equals("lost")
                            && !mLostStudentPopups.contains(s.getKey())) {
                        int currentMisses = mBluetoothMisses.get(s.getKey());
                        Log.d(TAG, "Current misses for " + s.getKey() + " = " + currentMisses);
                        if(currentMisses > 1) {
                            currentMisses = 0;
                            mBluetoothMisses.put(s.getKey(), currentMisses);

                            Log.d(TAG, "Could not find student " + s.getName());
                            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0 /* Request code */, new Intent(mContext, ChaperoneActivity.class),
                                    PendingIntent.FLAG_ONE_SHOT);

                            Intent notLostIntent = new Intent(mContext, ChaperoneActivity.class);
                            notLostIntent.putExtra("NOTIFY_CHILD", s.getKey());
                            notLostIntent.putExtra("NOTIFY_IS_LOST", false);
                            notLostIntent.putExtra("NOTIFY_ACTION", "NOT LOST");
                            notLostIntent.putExtra("NOTIFY_ID", s.getKey().hashCode());
                            PendingIntent notLostPendingIntent = PendingIntent.getActivity(mContext, mNotLostCalls /* Request code */, notLostIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);
                            mNotLostCalls += 2;
                            NotificationCompat.Action notLostAction = new NotificationCompat.Action.Builder(R.drawable.ic_clear, "Not Lost", notLostPendingIntent).build();

                            Intent lostIntent = new Intent(mContext, ChaperoneActivity.class);
                            lostIntent.putExtra("NOTIFY_CHILD", s.getKey());
                            lostIntent.putExtra("NOTIFY_IS_LOST", true);
                            lostIntent.putExtra("NOTIFY_ACTION", "LOST");
                            lostIntent.putExtra("NOTIFY_ID", s.getKey().hashCode());
                            PendingIntent lostPendingIntent = PendingIntent.getActivity(mContext, mLostCalls /* Request code */, lostIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);
                            mLostCalls += 2;
                            NotificationCompat.Action lostAction = new NotificationCompat.Action.Builder(R.drawable.ic_check, "Lost", lostPendingIntent).build();

                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext)
                                    .setSmallIcon(R.drawable.bus)
                                    .setContentTitle("Child Lost")
                                    .setContentText(s.getName() + " not found")
                                    .setPriority(Notification.PRIORITY_MAX)
                                    //.setContentIntent(pendingIntent)
                                    .addAction(lostAction)
                                    .addAction(notLostAction);

                            NotificationManager notificationManager =
                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                            notificationManager.notify(s.getKey().hashCode() /* ID of notification */, notificationBuilder.build());
                            mLostStudentPopups.add(s.getKey());
                            AlertDialog.Builder lostAlertBuilder = new AlertDialog.Builder(mContext);
                            lostAlertBuilder.setMessage(s.getName() + " not found")
                                    .setTitle("Student Not Found");
                            lostAlertBuilder.setPositiveButton("Confirm Lost", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button
                                    DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                    DatabaseReference studentLocationRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("location");
                                    if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        Log.w("BleActivity", "Location access not granted!");
                                        ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                                    } else {
                                        Log.d(TAG, "Getting last known location");
                                        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                        Map locationValue = new HashMap();
                                        locationValue.put("lat", lastKnownLocation.getLatitude());
                                        locationValue.put("lng", lastKnownLocation.getLongitude());
                                        studentLocationRef.setValue(locationValue);
                                    }
                                    studentStatusRef.setValue("lost");
                                    mLostStudentPopups.remove(s.getKey());
                                    dialog.dismiss();
                                }
                            });
                            lostAlertBuilder.setNegativeButton("Not Lost", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                    mLostStudentPopups.remove(s.getKey());
                                    dialog.dismiss();
                                }
                            });

                            AlertDialog lostAlert = lostAlertBuilder.create();
                            lostAlert.setCanceledOnTouchOutside(false);
                            if(!((Activity)mContext).isFinishing()) {
                                //lostAlert.show();
                            }
                        } else {
                            currentMisses += 1;
                            Log.d(TAG, "Current misses now " + currentMisses);
                            mBluetoothMisses.put(s.getKey(), currentMisses);
                        }
                    }
                }
            }

            sensorTagDevices.clear();

            mBLEAdapter.stopLeScan(mLeScanCallback);
            scanHandler.postDelayed(startScan, 1000); // start scan after 1000 ms
        }
    };

    private LocationListener GPSListener = new LocationListener(){
        public void onLocationChanged(Location location) {
            // update location
            chapLocation = location;
            if(mSchoolLat == null || mSchoolLng == null) {
                return;
            }
            if(mTimeslot.toLowerCase().contains("am")) {
                Log.d(TAG, "Chap lat, lng: " + chapLocation.getLatitude() + ", " + chapLocation.getLongitude());
                Log.d(TAG, "School lat abs: " + (Math.abs(chapLocation.getLatitude() - mSchoolLat) < 0.001));
                Log.d(TAG, "School lng abs: " + (Math.abs(chapLocation.getLongitude() - mSchoolLng) < 0.001));
                //Toast.makeText(ChaperoneActivity.this, "Lat abs : " + (Math.abs(chapLocation.getLatitude() - mSchoolLat) < 0.001)
                //        + "Lng abs: " + (Math.abs(chapLocation.getLongitude() - mSchoolLng) < 0.001)
                //        + "Lat, Lng: " + chapLocation.getLatitude() + ", " + chapLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                if (Math.abs(chapLocation.getLatitude() - mSchoolLat) < 0.001 && Math.abs(chapLocation.getLongitude() - mSchoolLng) < 0.001) {
                    Log.d(TAG, "Chap in range of school");
                    if (!mGpsDropoffPrompted && mRoutePrivate.getStatus().toLowerCase().equals("picked up")) {
                        AlertDialog.Builder dropOffBuilder = new AlertDialog.Builder(mContext);
                        final ArrayList<Student> pickedUpStudents = new ArrayList<Student>();
                        String studentNames = "";
                        for (Student s : mStudents) {
                            if (s.getStatus().toLowerCase().equals("picked up")) {
                                studentNames += "\n" + s.getName();
                                pickedUpStudents.add(s);
                            }
                        }
                        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0 /* Request code */, new Intent(mContext, ChaperoneActivity.class),
                                PendingIntent.FLAG_ONE_SHOT);
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext)
                                .setSmallIcon(R.drawable.bus)
                                .setContentTitle("Arrived at School Dropoff")
                                .setPriority(Notification.PRIORITY_MAX)
                                .setContentText("Dropping off children")
                                .setContentIntent(pendingIntent);

                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build());
                        for (Student s : pickedUpStudents) {
                            // Set all found students as picked up
                            DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                            studentStatusRef.setValue("dropped off");
                        }
                        Log.d(TAG, "Setting dropped off to true");
                        DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(mRouteKey).child("private").child("status");
                        routeRef.setValue("dropped off");
                        mGpsDropoffPrompted = false;
                    }
                }
            } else {
                // pm
                Double routeLat = mRoutePublic.getLocation().get("lat");
                Double routeLng = mRoutePublic.getLocation().get("lng");
                Log.d(TAG, "Chap lat, lng: " + chapLocation.getLatitude() + ", " + chapLocation.getLongitude());
                Log.d(TAG, "Route lat abs: " + (Math.abs(chapLocation.getLatitude() - routeLat) < 0.001));
                Log.d(TAG, "Route lng abs: " + (Math.abs(chapLocation.getLongitude() - routeLng) < 0.001));
                //Toast.makeText(ChaperoneActivity.this, "Lat abs : " + (Math.abs(chapLocation.getLatitude() - mSchoolLat) < 0.001)
                //        + "Lng abs: " + (Math.abs(chapLocation.getLongitude() - mSchoolLng) < 0.001)
                //        + "Lat, Lng: " + chapLocation.getLatitude() + ", " + chapLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                if (Math.abs(chapLocation.getLatitude() - routeLat) < 0.001 && Math.abs(chapLocation.getLongitude() - routeLng) < 0.001) {
                    Log.d(TAG, "Chap in range of route dropoff");
                    if (!mGpsDropoffPrompted && mRoutePrivate.getStatus().toLowerCase().equals("picked up")) {
                        AlertDialog.Builder dropOffBuilder = new AlertDialog.Builder(mContext);
                        final ArrayList<Student> pickedUpStudents = new ArrayList<Student>();
                        String studentNames = "";
                        for (Student s : mStudents) {
                            if (s.getStatus().toLowerCase().equals("picked up")) {
                                studentNames += "\n" + s.getName();
                                pickedUpStudents.add(s);
                            }
                        }
                        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0 /* Request code */, new Intent(mContext, ChaperoneActivity.class),
                                PendingIntent.FLAG_ONE_SHOT);
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext)
                                .setSmallIcon(R.drawable.bus)
                                .setContentTitle("Arrived at Route Dropoff")
                                .setPriority(Notification.PRIORITY_MAX)
                                .setContentText("Dropping off children")
                                .setContentIntent(pendingIntent);

                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build());
                        for (Student s : pickedUpStudents) {
                            // Set all found students as picked up
                            DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                            studentStatusRef.setValue("dropped off");
                        }
                        Log.d(TAG, "Setting dropped off to true");
                        DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(mRouteKey).child("private").child("status");
                        routeRef.setValue("dropped off");
                        mGpsDropoffPrompted = false;
                    }
                }
            }
            Toast.makeText(ChaperoneActivity.this, "Loc changed", Toast.LENGTH_LONG);
            //mLocationManager.removeUpdates(GPSListener); // remove this listener
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // task you need to do.
                    Log.d(TAG, "Coarse location perms granted");

                    turnonBLE();
                    discoverBLEDevices();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // task you need to do.
                    Log.d(TAG, "Fine location perms granted");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}