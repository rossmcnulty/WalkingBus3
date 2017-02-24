package ut.walkingbus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddStudentActivity extends BaseActivity {
    private static final String TAG = "AddStudentActivity";

    private static int RESULT_LOAD_IMAGE = 1;
    private String name;
    private String school;
    private String info;

    private static final int MY_PERMISSION_RESPONSE = 2;
    private ArrayList<BluetoothDevice> sensorTagDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBLEAdapter;
    private BluetoothManager manager;
    private Handler scanHandler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        scanHandler = new Handler();

        setTitle("Add Student");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Location access not granted!");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_RESPONSE);
            }
        }

        Button buttonLoadImage = (Button) findViewById(R.id.add_picture_button);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        Button scanButton = (Button) findViewById(R.id.scan_bluetooth);
        scanButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                turnonBLE();
                discoverBLEDevices();
                dismissProgressDialog();
            }
        });

        Button submit = (Button) findViewById(R.id.add_submit);
        submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                String name = ((TextView) findViewById(R.id.add_name)).getText().toString();
                String info = ((TextView) findViewById(R.id.add_info)).getText().toString();
                String bluetooth = ((TextView) findViewById(R.id.add_bluetooth)).getText().toString();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(AddStudentActivity.this, R.string.user_logged_out_error,
                            Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseReference studentRef = FirebaseUtil.getStudentsRef().push();
                    //String studentKey = studentRef.getKey();

                    Map studentParentsValues = new HashMap();
                    studentParentsValues.put(FirebaseUtil.getCurrentUserId(), FirebaseUtil.getCurrentUserId());

                    Map studentValues = new HashMap();
                    studentValues.put("bluetooth", bluetooth);
                    studentValues.put("name", name);
                    studentValues.put("info", info);
                    studentValues.put("status", "waiting");
                    studentValues.put("parents", studentParentsValues);

                    Log.d(TAG, "UID: " + user.getUid());

                    studentRef.updateChildren(
                            studentValues,
                            new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                                    Log.d(TAG, "Student reference: " + databaseReference.toString());
                                    if (firebaseError != null) {
                                        Toast.makeText(AddStudentActivity.this,
                                                "Couldn't save student data: " + firebaseError.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        DatabaseReference parentStudentRef = FirebaseUtil.getUserStudentsRef(FirebaseUtil.getCurrentUserId());
                                        Map parentStudentUpdate = new HashMap();
                                        parentStudentUpdate.put(databaseReference.getKey(), databaseReference.getKey());
                                        Log.d(TAG, "Parent student key: " + databaseReference.getKey().toString());
                                        parentStudentRef.updateChildren(parentStudentUpdate,
                                                new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                                                        Log.d(TAG, "Parent student reference: " + databaseReference.toString());
                                                        if (firebaseError != null) {
                                                            Toast.makeText(AddStudentActivity.this,
                                                                    "Couldn't save parent student data: " + firebaseError.getMessage(),
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });
                                    }
                                }
                            });
                }
                AddStudentActivity.super.onBackPressed();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.add_photo_preview);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }

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
            showProgressDialog("Scanning");
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
                Log.d("DEBUG","decoded data : " + ByteArrayToString(scanRecord));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(!sensorTagDevices.contains(device) && name != null && name.contains("SensorTag")){
                sensorTagDevices.add(device);
            }

            Log.d("mLeScanCallback", "" + address + " : " + name);
        }
    };


    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            TextView sensortag = (TextView)findViewById(R.id.add_bluetooth);

            if(!sensorTagDevices.isEmpty()) {
                BluetoothDevice device = sensorTagDevices.get(0);
                sensortag.setText(device.getAddress());
            } else {
                Toast.makeText(AddStudentActivity.this,
                        "Could not find a SensorTag device ",
                        Toast.LENGTH_LONG).show();
            }

            if(!allDevices.contains(sensorTagDevices)) {
                sensorTagDevices.clear();
            }

            mBLEAdapter.stopLeScan(mLeScanCallback);
            dismissProgressDialog();
        }
    };

}