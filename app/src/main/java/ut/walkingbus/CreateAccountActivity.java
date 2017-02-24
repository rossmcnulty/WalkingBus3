package ut.walkingbus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static ut.walkingbus.WelcomeActivity.currentUser;

public class CreateAccountActivity extends BaseActivity {
    private static final String TAG = "CreateAccountActivity";

    private boolean mIsParent;
    private String mEmail;
    private String mName;
    private String mPhone;
    private String mPhotoUrl;

    // TODO: Validate phone/name/email
    // TODO: Include current user photo in preview

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mIsParent = getIntent().getBooleanExtra("IS_PARENT", true);
        mEmail = getIntent().getStringExtra("EMAIL");
        mName = getIntent().getStringExtra("NAME");
        mPhotoUrl = getIntent().getStringExtra("PHOTO_URL");

        EditText phoneText = (EditText) findViewById(R.id.add_phone);
        EditText emailText = (EditText) findViewById(R.id.add_email);
        emailText.setText(mEmail);
        EditText nameText = (EditText) findViewById(R.id.add_name);
        nameText.setText(mName);
        CircleImageView profilePhotoView = (CircleImageView) findViewById(R.id.add_photo_preview);
        TextView photoName = (TextView) findViewById(R.id.add_photo_filename);

        if(mPhotoUrl != null) {
            GlideUtil.loadProfileIcon(mPhotoUrl, profilePhotoView);
            photoName.setText("Google profile photo");
        }

        PhoneNumberUtils.formatNumber(phoneText.getText().toString());
        Button submit = (Button) findViewById(R.id.add_submit);
        submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mName = ((TextView) findViewById(R.id.add_name)).getText().toString();
                mEmail = ((TextView) findViewById(R.id.add_email)).getText().toString();
                mPhone = PhoneNumberUtils.formatNumber(((TextView) findViewById(R.id.add_phone)).getText().toString(), "US");
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(CreateAccountActivity.this, R.string.user_logged_out_error,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Map<String, Object> updateValues = new HashMap<>();
                    updateValues.put("displayName", mName);
                    updateValues.put("photoUrl", mPhotoUrl);
                    updateValues.put("email", mEmail);
                    updateValues.put("phone", mPhone);

                    Log.d(TAG, "Name: " + mName);
                    Log.d(TAG, "Phone: " + mPhone);
                    Log.d(TAG, "Photo URL: " + mPhotoUrl);
                    Log.d(TAG, "Email: " + mEmail);

                    FirebaseUtil.getUserRef().child(currentUser.getUid()).updateChildren(
                            updateValues,
                            new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                                    if (firebaseError != null) {
                                        Toast.makeText(CreateAccountActivity.this,
                                                "Couldn't save user data: " + firebaseError.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
                if(mIsParent) {
                    // go to isParent activity
                    Intent parentLoginIntent = new Intent(getBaseContext(), ParentActivity.class);
                    startActivity(parentLoginIntent);
                } else {
                    // go to chaperone activity
                    Intent chaperoneLoginIntent = new Intent(getBaseContext(), ChaperoneActivity.class);
                    startActivity(chaperoneLoginIntent);
                }
            }
        });
    }

}
