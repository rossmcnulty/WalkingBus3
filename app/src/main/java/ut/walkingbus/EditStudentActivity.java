package ut.walkingbus;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class EditStudentActivity extends BaseActivity {
    private static final String TAG = "EditStudentActivity";

    private static int RESULT_LOAD_IMAGE = 1;
    private String name;
    private String school;
    private String info;
    private TextView mNameView;
    private TextView mInfoView;
    private Spinner mSchoolView;
    private String mChildKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mChildKey = getIntent().getStringExtra("STUDENT_KEY");

        // Load existing values into display
        TextView mNameView = (TextView)findViewById(R.id.add_name);
        TextView mInfoView = (TextView)findViewById(R.id.add_info);
        Spinner mSchoolView = (Spinner)findViewById(R.id.add_school);

        final DatabaseReference studentsRef = FirebaseUtil.getStudentsRef();

        setTitle("Add Student");

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

        Button submit = (Button) findViewById(R.id.add_submit);
        submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                String name = ((TextView) findViewById(R.id.add_name)).getText().toString();
                String info = ((TextView) findViewById(R.id.add_info)).getText().toString();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(EditStudentActivity.this, R.string.user_logged_out_error,
                            Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseReference studentRef = FirebaseUtil.getStudentsRef().push();
                    String studentKey = studentRef.getKey();


                    Map studentParentsValues = new HashMap();
                    studentParentsValues.put(FirebaseUtil.getCurrentUserId(), FirebaseUtil.getCurrentUserId());

                    Map studentValues = new HashMap();
                    studentValues.put("bluetooth", "11:22:33:44:55:66");
                    studentValues.put("name", name);
                    studentValues.put("info", info);
                    studentValues.put("status", "waiting");
                    studentValues.put("parents", studentParentsValues);

                    Map updatedData = new HashMap();
                    updatedData.put("students/" + studentKey, studentValues);
                    updatedData.put("users/" + FirebaseUtil.getCurrentUserId() + "/children/" + studentKey, studentKey);

                    Log.d(TAG, "UID: " + user.getUid());

                    FirebaseUtil.getBaseRef().updateChildren(
                            updatedData,
                            new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                                    if (firebaseError != null) {
                                        Toast.makeText(EditStudentActivity.this,
                                                "Couldn't save student data: " + firebaseError.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
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

}