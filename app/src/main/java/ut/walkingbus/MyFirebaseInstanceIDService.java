package ut.walkingbus;

/**
 * Created by Ross on 3/29/2017.
 * This service is used to refresh the phone's FCM token whenever it changes.
 * The FCM token is used to send notifications specifically to the user through Firebase.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";
    private String userId;

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        SharedPreferences sharedPref = this.getSharedPreferences("uid_file", Context.MODE_PRIVATE);
        String uid = sharedPref.getString("USER_ID", null);
        if(FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, token);
            DatabaseReference userFcmRef = FirebaseUtil.getUserRef().child(FirebaseUtil.getCurrentUserId()).child("fcm");
            userFcmRef.setValue(token);
        } else if(uid != null) {
            Log.d(TAG, token);
            DatabaseReference userFcmRef = FirebaseUtil.getUserRef().child(uid).child("fcm");
            userFcmRef.setValue(token);
        }
    }
}
