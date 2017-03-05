package ut.walkingbus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import ut.walkingbus.Models.Route;
import ut.walkingbus.Models.Student;

public class ChaperoneActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener  {
    private static final String TAG = "ChaperoneActivity";
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Student> mExpectedStudents;
    private ArrayList<Student> mFoundStudents;
    private ArrayList<Student> mPickedUpStudents;
    private ArrayList<Student> mLostStudents;
    private ChaperoneStudentAdapter mStudentAdapter;
    private Route mRoute;
    private String mRouteKey;
    private String mTimeslot;
    private boolean mPickedUp;
    private Context mContext;
    private int mCurrentDay;

    private ArrayList<String> mExpectedBluetooth;
    private ArrayList<String> mFoundBluetooth;
    private ArrayList<String> mPickedUpBluetooth;

    private static final int MY_PERMISSION_RESPONSE = 2;
    private ArrayList<BluetoothDevice> sensorTagDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBLEAdapter;
    private BluetoothManager manager;
    private Handler scanHandler = new Handler();

    private static final int RC_SIGN_IN = 103;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chaperone);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = this;

        // TODO: don't hardcode these pls
        // TODO: picked up field?
        mRouteKey = "route1";
        mTimeslot = "mon_am";

        setTitle("Today's Bus");
        mPickedUp = false;

        mExpectedStudents = new ArrayList<Student>();
        mFoundStudents = new ArrayList<Student>();
        mPickedUpStudents = new ArrayList<Student>();

        mExpectedBluetooth = new ArrayList<>();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported on this device", Toast.LENGTH_SHORT).show();
            //finish();
        }
        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                // pick up children
                if(!mPickedUp) {
                    AlertDialog.Builder pickUpBuilder = new AlertDialog.Builder(mContext);
                    String studentNames = "";
                    for(Student s : mFoundStudents) {
                        studentNames += "\n" + s.getName();
                    }
                    pickUpBuilder.setMessage("Confirm pickup of" + studentNames)
                            .setTitle("Pickup Confirmation");
                    pickUpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            mPickedUpStudents.addAll(mFoundStudents);
                            mPickedUp = true;
                        }
                    });
                    pickUpBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
                    pickUpBuilder.create();
                    Log.d(TAG, "Setting picked up to true");
                    mPickedUp = true;
                } else {
                    AlertDialog.Builder dropOffBuilder = new AlertDialog.Builder(mContext);
                    String studentNames = "";
                    for(Student s : mPickedUpStudents) {
                        studentNames += "\n" + s.getName();
                    }
                    dropOffBuilder.setMessage("Confirm dropoff of" + studentNames)
                            .setTitle("Pickup Confirmation");
                    dropOffBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                        }
                    });
                    dropOffBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
                    dropOffBuilder.create();
                    Log.d(TAG, "Setting picked up to true");
                    mPickedUp = true;
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
        DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(mRouteKey);
        routeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mRoute = dataSnapshot.getValue(Route.class);
                Log.d(TAG, "Route name " + mRoute.getName());
                if(mRoute.getStudents() == null) {
                    Log.d(TAG, "No students in route " + mRoute.getName());
                    return;
                }
                if(mRoute.getStudents().get(mTimeslot) == null) {
                    Log.d(TAG, "No students in timeslot " + mTimeslot);
                    return;
                }
                for(String studentKey: mRoute.getStudents().get(mTimeslot).keySet()) {
                    Log.d(TAG, "Student key " + studentKey);
                    DatabaseReference studentRef = FirebaseUtil.getStudentsRef().child(studentKey);
                    studentRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Student s = dataSnapshot.getValue(Student.class);
                            s.setKey(dataSnapshot.getKey().toString());
                            boolean found = false;
                            for(int i = 0; i < mExpectedStudents.size(); i++) {
                                Student student = mExpectedStudents.get(i);
                                if(student.getName().equals(s.getName())) {
                                    mExpectedStudents.set(i, s);
                                    found = true;
                                    break;
                                }
                            }
                            if(!found) {
                                Log.d(TAG, "Adding student " + s.getName());
                                Log.d(TAG, "Adding BT " + s.getBluetooth());
                                mExpectedStudents.add(s);
                                mExpectedBluetooth.add(s.getBluetooth());
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

        mStudentAdapter = new ChaperoneStudentAdapter(mExpectedStudents, this);

        recycler.setAdapter(mStudentAdapter);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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
            allDevices.clear();
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
            if(!allDevices.contains(device)){
                allDevices.add(device);
            }
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
            if(!mPickedUp) {
                for (Student s : mExpectedStudents) {
                    boolean found = false;
                    for (BluetoothDevice d : sensorTagDevices) {
                        if (d.getAddress().equals(s.getBluetooth())) {
                            found = true;
                        }
                    }
                    if (!found) {
                        // TODO: ensure BT address is unique
                        mFoundStudents.add(s);
                    }
                }
            } else {
                for (final Student s : mPickedUpStudents) {
                    boolean found = false;
                    for (BluetoothDevice d : sensorTagDevices) {
                        if (d.getAddress().equals(s.getBluetooth())) {
                            found = true;
                            for(Student lostStudent: mLostStudents) {
                                if(lostStudent.getBluetooth().equals(s.getBluetooth())) {
                                    // lost student found
                                    mLostStudents.remove(lostStudent);
                                    DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(lostStudent.getKey()).child("status");
                                    studentStatusRef.setValue("picked up");
                                }
                            }
                        }
                    }
                    if (!found) {
                        Log.d(TAG, "Could not find student " + s.getName());
                        AlertDialog.Builder lostAlertBuilder = new AlertDialog.Builder(mContext);
                        lostAlertBuilder.setMessage(s.getName() + " not found")
                                .setTitle("Student Not Found");
                        lostAlertBuilder.setPositiveButton("Confirm Lost", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                                DatabaseReference studentStatusRef = FirebaseUtil.getStudentsRef().child(s.getKey()).child("status");
                                mLostStudents.add(s);
                                studentStatusRef.setValue("lost");
                            }
                        });
                        lostAlertBuilder.setNegativeButton("Not Lost", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                        lostAlertBuilder.create();
                    }
                }
            }

            if(!allDevices.contains(sensorTagDevices)) {
                sensorTagDevices.clear();
            }

            mBLEAdapter.stopLeScan(mLeScanCallback);
            scanHandler.postDelayed(startScan, 1000); // start scan after 10 ms
        }
    };
}