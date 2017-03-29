package ut.walkingbus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
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

public class ChaperoneActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener  {
    private static final String TAG = "ChaperoneActivity";
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    //private ArrayList<Student> mExpectedStudents;
    //private ArrayList<Student> mFoundStudents;
    //private ArrayList<Student> mPickedUpStudents;
    //private ArrayList<Student> mLostStudents;
    private ArrayList<Student> mStudents;
    private ChaperoneStudentAdapter mStudentAdapter;
    private RoutePublic mRoutePublic;
    private RoutePrivate mRoutePrivate;
    private String mRouteKey;
    private String mTimeslot;
    private Context mContext;
    private int mCurrentDay;

    private ArrayList<String> mLostStudentPopups;

    //private ArrayList<String> mExpectedBluetooth;
    private ArrayList<String> mFoundBluetooth;
    private ArrayList<String> mPickedUpBluetooth;

    private CircleImageView mProfilePhoto;
    private TextView mProfileEmail;
    private TextView mProfileUsername;

    private static final int MY_PERMISSION_RESPONSE = 2;
    private ArrayList<BluetoothDevice> sensorTagDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBLEAdapter;
    private BluetoothManager manager;
    private Handler scanHandler = new Handler();
    private LocationManager mLocationManager;

    private static final int RC_SIGN_IN = 103;

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

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mContext = this;

        mRouteKey = "";

        // TODO: don't hardcode these pls
        DatabaseReference chapRef = FirebaseUtil.getUserRef().child(FirebaseUtil.getCurrentUserId());
        chapRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User chap = dataSnapshot.getValue(User.class);
                // TODO: Select route based on time
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


        // mRouteKey = "-KeWJO4qs-CcAyFf1pcC";
        mTimeslot = "mon_am";

        setTitle("Today's Bus");

        //mExpectedStudents = new ArrayList<Student>();
        //mFoundStudents = new ArrayList<Student>();
        //mPickedUpStudents = new ArrayList<Student>();
        //mLostStudents = new ArrayList<Student>();
        mStudents = new ArrayList<Student>();
        mLostStudentPopups = new ArrayList<String>();

        //mExpectedBluetooth = new ArrayList<>();
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
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Location access not granted!");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_RESPONSE);
            }
        }
        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Location access not granted!");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_RESPONSE);
            }
        }

        turnonBLE();
        discoverBLEDevices();

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
                            for(Student s : foundStudents) {
                                // Set all found students as picked up
                                DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                studentStatusRef.setValue("picked up");
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
            scanHandler.postDelayed(stopScan, 5000); // invoke stop scan after 5000 ms
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
            if(mRoutePrivate.getStatus().toLowerCase().equals("waiting")) {
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
                    if(s.getStatus().toLowerCase().equals("waiting")) {
                        // don't worry about non-picked up students
                        Log.d(TAG, "Student " + s.getName() + " was never picked up");
                        break;
                    }

                    for (BluetoothDevice d : sensorTagDevices) {
                        if (d.getAddress().equals(s.getBluetooth())) {
                            // found a matching student BT
                            found = true;
                            if (s.getStatus().toLowerCase().equals("lost")) {
                                Log.d(TAG, "Recovered lost student " + s.getName());
                                DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                studentStatusRef.setValue("picked up");
                            }
                        }
                    }

                    if (!found && !mRoutePrivate.getStatus().toLowerCase().equals("dropped off")
                            && !s.getStatus().toLowerCase().equals("lost")
                            && !mLostStudentPopups.contains(s.getKey())) {
                        Log.d(TAG, "Could not find student " + s.getName());
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
                                    ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_RESPONSE);
                                }
                                Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                Map locationValue = new HashMap();
                                locationValue.put("lat", lastKnownLocation.getLatitude());
                                locationValue.put("lng", lastKnownLocation.getLongitude());
                                studentLocationRef.setValue(locationValue);
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
                            lostAlert.show();
                        }
                    }
                }
            }

            sensorTagDevices.clear();

            mBLEAdapter.stopLeScan(mLeScanCallback);
            scanHandler.postDelayed(startScan, 5000); // start scan after 100 ms
        }
    };
}