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

    public static DatabaseReference getUserRef() {
        return getBaseRef().child("users");
    }

    public static DatabaseReference getUserStudentsRef(String key) {
        return getUserRef().child(key).child("students");
    }

    public static DatabaseReference getStudentsRef() {
        return getBaseRef().child("students");
    }

    public static DatabaseReference getSchoolNamesRef() {
        return getBaseRef().child("school_names");
    }

    public static DatabaseReference getStudentRoutesRef(String key) {
        return getStudentsRef().child(key).child("routes");
    }

    public static DatabaseReference getSchoolsRoutesRef(String key) {
        return getSchoolRef().child(key).child("routes");
    }

    public static DatabaseReference getRoutesRef() {
        return getBaseRef().child("routes");
    }

    public static DatabaseReference getUserSchoolsParentRef(String key) {
        return getUserRef().child(key).child("schools_parent");
    }

    public static DatabaseReference getRouteStudentsRef(String key) {
        return getRoutesRef().child(key).child("private").child("students");
    }

    public static DatabaseReference getRoutePublicRef(String key) {
        return getRoutesRef().child(key).child("public");
    }

    public static DatabaseReference getSchoolUsersRef(String key) {
        return getSchoolRef().child(key).child("users");
    }

    public static DatabaseReference getSchoolStudentsRef(String key) {
        return getSchoolRef().child(key).child("students");
    }

    public static DatabaseReference getSchoolRef() { return getBaseRef().child("schools"); }

}