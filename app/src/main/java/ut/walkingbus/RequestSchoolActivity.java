package ut.walkingbus;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import ut.walkingbus.Models.School;

/**
    This is a page shown to a parent that wants to add themselves to a school through the side
    menu. The parent needs the school's assigned database passcode to add themselves.
 */

public class RequestSchoolActivity extends BaseActivity {
    private static final String TAG = "RequestSchoolActivity";

    private SchoolAdapter mSchoolAdapter;
    private ArrayList<School> mSchoolList;
    private Map parentSchoolValues;
    private FirebaseUser user;

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
        // TODO: Stop using schoolnames
        FirebaseUtil.getSchoolNamesRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot schoolNameSnapshot: dataSnapshot.getChildren()) {
                    String schoolName = schoolNameSnapshot.getKey();
                    String schoolKey = (String) schoolNameSnapshot.getValue();
                    School s = new School();
                    s.setKey(schoolKey);
                    s.setName(schoolName);
                    mSchoolList.add(s);
                }
                mSchoolAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) { }
        });
    }

}
