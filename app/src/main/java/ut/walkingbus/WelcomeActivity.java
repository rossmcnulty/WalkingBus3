package ut.walkingbus;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class WelcomeActivity extends BaseActivity implements View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WelcomeActivity";
    private static final int RC_SIGN_IN = 103;
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    public static FirebaseUser currentUser;
    private boolean isParent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        isParent = false;

        findViewById(R.id.parent_button).setOnClickListener(this);
        findViewById(R.id.chaperone_button).setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        // GoogleApiClient with Sign In
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API,
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .build())
                .build();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.parent_button:
                isParent = true;
                launchSignInIntent();
                break;
            case R.id.chaperone_button:
                isParent = false;
                launchSignInIntent();
                break;
        }
    }

    private void launchSignInIntent() {
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleGoogleSignInResult(result);
        }
    }

    private void handleGoogleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.getStatus());
        if (result.isSuccess()) {
            // Successful Google sign in, authenticate with Firebase.
            GoogleSignInAccount acct = result.getSignInAccount();
            firebaseAuthWithGoogle(acct);
        } else {
            // Unsuccessful Google Sign In, show signed-out UI
            Log.d(TAG, "Google Sign-In failed.");
            if(result.getStatus().hasResolution()) {
                Log.d(TAG, "Resolution: " + result.getStatus().getResolution());
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGooogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        showProgressDialog(getString(R.string.profile_progress_message));
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        handleFirebaseAuthResult(result);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        FirebaseCrash.logcat(Log.ERROR, TAG, "auth:onFailure:" + e.getMessage());
                        handleFirebaseAuthResult(null);
                    }
                });
    }

    private void swapActivity() {
        if(isParent) {
            // go to isParent activity
            Intent parentLoginIntent = new Intent(getBaseContext(), ParentActivity.class);
            startActivity(parentLoginIntent);
        } else {
            // go to chaperone activity
            Intent chaperoneLoginIntent = new Intent(getBaseContext(), ChaperoneActivity.class);
            startActivity(chaperoneLoginIntent);
        }
    }


    private void handleFirebaseAuthResult(AuthResult result) {
        dismissProgressDialog();
        if (result != null) {
            Log.d(TAG, "handleFirebaseAuthResult:SUCCESS");
            currentUser = result.getUser();
            FirebaseUtil.getUserRef().child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        swapActivity();
                    } else {
                        Intent createAccountIntent = new Intent(getBaseContext(), CreateAccountActivity.class);
                        createAccountIntent.putExtra("IS_PARENT", isParent);
                        createAccountIntent.putExtra("NAME", currentUser.getDisplayName());
                        createAccountIntent.putExtra("EMAIL", currentUser.getEmail());
                        createAccountIntent.putExtra("PHOTO_URL", currentUser.getPhotoUrl().toString());
                        startActivity(createAccountIntent);
                    }
                }

                @Override
                public void onCancelled(DatabaseError firebaseError) { }
            });
        } else {
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
    }
}