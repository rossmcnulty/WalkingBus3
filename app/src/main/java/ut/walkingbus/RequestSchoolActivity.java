package ut.walkingbus;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ut.walkingbus.Models.School;

public class RequestSchoolActivity extends BaseActivity {
    private static final String TAG = "RequestSchoolActivity";

    private SchoolAdapter mSchoolAdapter;
    private ArrayList<School> mSchoolList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_school);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSchoolList = new ArrayList<School>();
        mSchoolAdapter = new SchoolAdapter(mSchoolList, this);

        RecyclerView recycler = (RecyclerView) findViewById(R.id.school_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(mSchoolAdapter);

        // get all schools
        FirebaseUtil.getSchoolNamesRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot schoolNameSnapshot: dataSnapshot.getChildren()) {
                    String schoolName = schoolNameSnapshot.getKey();
                    String schoolKey = (String) schoolNameSnapshot.getValue();
                    mSchoolList.add(new School(schoolName, schoolKey, false));
                }
                mSchoolAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) { }
        });

        Button requestButton = (Button) findViewById(R.id.request_submit);
        requestButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Map parentSchoolValues = new HashMap<>();

                ArrayList<School> schoolList = mSchoolList;
                for(int i=0; i < schoolList.size(); i++){
                    School school = schoolList.get(i);
                    if(school.isSelected()){
                        parentSchoolValues.put(school.getKey(), school.getName());
                    }
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(RequestSchoolActivity.this, R.string.user_logged_out_error,
                            Toast.LENGTH_SHORT).show();
                } else {

                    Log.d(TAG, "UID: " + user.getUid());

                    FirebaseUtil.getUserSchoolsParentRef(user.getUid()).updateChildren(
                            parentSchoolValues,
                            new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                                    Log.d(TAG, "Student reference: " + databaseReference.toString());
                                    if (firebaseError != null) {
                                        Toast.makeText(RequestSchoolActivity.this,
                                                "Couldn't save parent school data: " + firebaseError.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
                RequestSchoolActivity.super.onBackPressed();
            }
        });
    }

}
