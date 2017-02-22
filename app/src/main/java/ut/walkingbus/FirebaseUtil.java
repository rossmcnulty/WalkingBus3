package ut.walkingbus;

/**
 * Created by Ross on 2/13/2017.
 */

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

class FirebaseUtil {
    public static DatabaseReference getBaseRef() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public static String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return null;
    }

    public static DatabaseReference getParentRef() {
        return getBaseRef().child("parents");
    }

    public static DatabaseReference getParentChildrenRef(String key) {
        return getParentRef().child(key).child("children");
    }

    public static DatabaseReference getChildrenRef() {
        return getBaseRef().child("children");
    }

}