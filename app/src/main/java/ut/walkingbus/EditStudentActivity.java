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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ut.walkingbus.Models.School;
import ut.walkingbus.Models.Student;

/**
    This handles the popup window for editing an existing student's information.
 */

public class EditStudentActivity extends BaseActivity {
    private static final String TAG = "EditStudentActivity";

    private static int RESULT_LOAD_IMAGE = 1;
    private String mName;
    private School mSchool;
    private String mInfo;
    private String mBluetooth;
    private ArrayList<School> mSchoolArray;
    private ArrayAdapter<School> mSchoolAdapter;
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private StorageReference mStorageRef;
    private String localPhotoPath;
    private Context mContext;

    private Student mStudent;

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
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mContext = this;

        setTitle("Add Student");

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


        mSchoolArray = new ArrayList<School>();

        mSchoolAdapter = new SchoolSpinAdapter(this, R.layout.spinner_item, mSchoolArray);

        mSchoolAdapter.setDropDownViewResource(R.layout.spinner_item);
        Spinner sItems = (Spinner) findViewById(R.id.add_school);
        sItems.setAdapter(mSchoolAdapter);

        // get schools parent is a member of to populate spinner
        DatabaseReference parentSchoolsRef = FirebaseUtil.getUserSchoolsParentRef(FirebaseUtil.getCurrentUserId());
        parentSchoolsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot schoolSnapshot: dataSnapshot.getChildren()) {
                    String key = schoolSnapshot.getKey();
                    String name = schoolSnapshot.getValue().toString();
                    School s = new School();
                    s.setName(name);
                    s.setKey(key);
                    mSchoolAdapter.add(s);
                    Log.d(TAG, "School name: " + name);
                }
                mSchoolAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) { }
        });

        final String studentKey = getIntent().getStringExtra("STUDENT_KEY");
        DatabaseReference studentRef = FirebaseUtil.getStudentsRef();

        studentRef.child(studentKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mStudent = dataSnapshot.getValue(Student.class);
                mStudent.setKey(dataSnapshot.getKey());
                ((TextView) findViewById(R.id.add_name)).setText(mStudent.getName());
                ((TextView) findViewById(R.id.add_bluetooth)).setText(mStudent.getBluetooth());
                ((TextView) findViewById(R.id.add_info)).setText(mStudent.getInfo());
                // TODO: make spinner position correct to right school
                // ((Spinner) findViewById(R.id.add_school)).setSelection();
                if(mStudent.getPhotoUrl() != null) {
                    Log.d(TAG, "Student has Photo URL");

                    StorageReference gsReference = FirebaseStorage.getInstance().getReferenceFromUrl(mStudent.getPhotoUrl());

                    // ImageView in your Activity
                    ImageView imageView = (ImageView)findViewById(R.id.add_photo_preview);
                    imageView.setBackgroundColor(0);

                    // Load the image using Glide
                    if(!isFinishing()) {
                        Log.d(TAG, "Activity is finishing");
                        Glide.with(mContext)
                                .using(new FirebaseImageLoader())
                                .load(gsReference)
                                .signature(new StringSignature(UUID.randomUUID().toString()))
                                .into(imageView);
                        TextView photoStatus = (TextView) findViewById(R.id.add_picture_filename);
                        photoStatus.setText("");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) { }
        });


        Button buttonLoadImage = (Button) findViewById(R.id.add_picture_button);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (ContextCompat.checkSelfPermission(EditStudentActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(EditStudentActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {

                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                        Log.d(TAG, "Show explanation");

                    } else {

                        // No explanation needed, we can request the permission.

                        Log.d(TAG, "Requesting permission");
                        ActivityCompat.requestPermissions(EditStudentActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                } else {
                    // We had permission

                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, RESULT_LOAD_IMAGE);
                }

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

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(EditStudentActivity.this, R.string.user_logged_out_error,
                            Toast.LENGTH_SHORT).show();
                } else {

                    final DatabaseReference studentRouteRefs = FirebaseUtil.getStudentRoutesRef(mStudent.getKey());
                    studentRouteRefs.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            ArrayList<DatabaseReference> routeRefs = new ArrayList<DatabaseReference>();

                            if(dataSnapshot.hasChild("mon_am")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("mon_am").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("mon_pm")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("mon_pm").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("tue_am")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("tue_am").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("tue_pm")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("tue_pm").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("wed_am")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("wed_am").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("wed_pm")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("wed_pm").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("thu_am")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("thu_am").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("thu_pm")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("thu_pm").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("fri_am")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("fri_am").getValue().toString()));
                            }
                            if(dataSnapshot.hasChild("fri_pm")) {
                                routeRefs.add(FirebaseUtil.getRoutesRef().child(dataSnapshot.child("fri_pm").getValue().toString()));
                            }

                            String name = ((TextView) findViewById(R.id.add_name)).getText().toString();
                            String info = ((TextView) findViewById(R.id.add_info)).getText().toString();
                            String bluetooth = ((TextView) findViewById(R.id.add_bluetooth)).getText().toString();
                            mSchool = (School)((Spinner) findViewById(R.id.add_school)).getSelectedItem();

                            String schoolKey = mSchool.getKey();
                            String userKey = FirebaseUtil.getCurrentUserId();

                            Map studentValues = new HashMap();
                            studentValues.put("bluetooth", bluetooth);
                            studentValues.put("name", name);
                            studentValues.put("info", info);
                            studentValues.put("school", schoolKey);

                            Map propagatedStudentData = new HashMap();
                            if(!mStudent.getSchool().equals(schoolKey)) {
                                // Student changed schools
                                Log.d(TAG, "School changed, deleting earlier school and route data");
                                studentValues.put("status", "waiting");
                                propagatedStudentData.put("schools/" + mStudent.getSchool() + "/students/" + studentKey, null);
                                for(DatabaseReference routeRef: routeRefs) {
                                    propagatedStudentData.put(routeRef.toString() + "/" + mStudent.getKey(), null);
                                }
                                propagatedStudentData.put("schools/" + schoolKey + "/students/" + studentKey, name);

                            }
                            // TODO: propagate child name changes where relevant
                            propagatedStudentData.put("students/" + mStudent.getKey() + "/bluetooth", bluetooth);
                            propagatedStudentData.put("students/" + mStudent.getKey() + "/name", name);
                            propagatedStudentData.put("students/" + mStudent.getKey() + "/info", info);
                            propagatedStudentData.put("students/" + mStudent.getKey() + "/school", schoolKey);

                            if(localPhotoPath != null) {
                                Uri photoFile = Uri.fromFile(new File(localPhotoPath));
                                UploadTask uploadTask = mStorageRef.child(studentKey).putFile(photoFile);
                                // Register observers to listen for when the download is done or if it fails
                                uploadTask.addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle unsuccessful uploads
                                        Log.d(TAG, "Upload unsuccessful");
                                    }
                                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                        @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getMetadata().getDownloadUrl();
                                        Log.d(TAG, "Upload successful " + downloadUrl.toString());
                                        Map studentPhotoValue = new HashMap();
                                        studentPhotoValue.put("students/" + studentKey + "/photoUrl", downloadUrl.toString());
                                        FirebaseUtil.getBaseRef().updateChildren(studentPhotoValue, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                if (databaseError != null) {
                                                    Toast.makeText(EditStudentActivity.this,
                                                            "Couldn't add student photo URL: " + databaseError.getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    }
                                });
                            }

                            Log.d(TAG, "UID: " + userKey);
                            Log.d(TAG, "School: " + schoolKey);

                            FirebaseUtil.getBaseRef().updateChildren(propagatedStudentData, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError != null) {
                                        Toast.makeText(EditStudentActivity.this,
                                                "Couldn't edit student data: " + databaseError.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
                EditStudentActivity.super.onBackPressed();
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "ActivityResult callback initiated");

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Log.d(TAG, "Setting imageview data");
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            localPhotoPath = picturePath;

            ImageView imageView = (ImageView) findViewById(R.id.add_photo_preview);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 16;// 1/4 of origin image size from width and height
            Bitmap imagePreview = BitmapFactory.decodeFile(picturePath, options);
            imageView.setImageBitmap(imagePreview);
            TextView photoStatus = (TextView) findViewById(R.id.add_picture_filename);
            photoStatus.setText("");

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // task you need to do.

                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, RESULT_LOAD_IMAGE);
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

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        mContext = this;
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
            final TextView sensortag = (TextView)findViewById(R.id.add_bluetooth);

            mBLEAdapter.stopLeScan(mLeScanCallback);
            if(!sensorTagDevices.isEmpty()) {
                AlertDialog.Builder sensorTagAlertBuilder = new AlertDialog.Builder(mContext);
                ArrayList deviceAddresses = new ArrayList();
                for(BluetoothDevice device: sensorTagDevices) {
                    deviceAddresses.add(device.getAddress());
                }
                final CharSequence[] addresses = (CharSequence[]) deviceAddresses.toArray(new CharSequence[deviceAddresses.size()]);
                sensorTagAlertBuilder.setSingleChoiceItems(addresses, -1, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface d, int which) {
                        Log.d(TAG, "Button clicked " + addresses[which]);
                        sensortag.setText(addresses[which]);
                        sensorTagDevices.clear();
                        d.dismiss();
                    }

                });
                sensorTagAlertBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        sensorTagDevices.clear();
                    }
                });
                sensorTagAlertBuilder.setTitle("Choose Device Address");
                dismissProgressDialog();
                sensorTagAlertBuilder.create().show();
            } else {
                Toast.makeText(EditStudentActivity.this,
                        "Could not find a SensorTag device ",
                        Toast.LENGTH_LONG).show();
                sensorTagDevices.clear();
                dismissProgressDialog();
            }

        }
    };

}