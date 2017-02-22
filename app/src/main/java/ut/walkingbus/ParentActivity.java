package ut.walkingbus;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import ut.walkingbus.Models.Student;

public class ParentActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener  {
    private static final String TAG = "ParentActivity";
    private FirebaseAuth mAuth;
    private CircleImageView mProfilePhoto;
    private TextView mProfileEmail;
    private TextView mProfileUsername;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Student> mStudents;
    private ParentStudentAdapter mChildAdapter;

    private static final int RC_SIGN_IN = 103;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize authentication and set up callbacks
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentActivity.this, AddStudentActivity.class));
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        RecyclerView recycler = (RecyclerView) findViewById(R.id.student_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // get all of a parents students
        DatabaseReference parentChildrenRef = FirebaseUtil.getParentChildrenRef(FirebaseUtil.getCurrentUserId());
        parentChildrenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                    String key = childSnapshot.getKey();
                    DatabaseReference childRef = FirebaseUtil.getChildrenRef().child(key);
                    childRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Student c = dataSnapshot.getValue(Student.class);
                            boolean found = false;
                            for(int i = 0; i < mStudents.size(); i++) {
                                Student student = mStudents.get(i);
                                if(student.getName().equals(c.getName())) {
                                    mStudents.set(i, c);
                                    found = true;
                                    break;
                                }
                            }
                            if(!found) {
                                mStudents.add(c);
                            }
                            mChildAdapter.notifyDataSetChanged();
                            Log.d(TAG, "child name: " + c.getName());
                        }

                        @Override
                        public void onCancelled(DatabaseError firebaseError) { }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) { }
        });

        mChildAdapter = new ParentStudentAdapter(mStudents, this);

        recycler.setAdapter(mChildAdapter);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);

        mProfileUsername = (TextView)header.findViewById(R.id.nav_username);
        mProfileEmail = (TextView)header.findViewById(R.id.nav_email);
        mProfilePhoto = (CircleImageView) header.findViewById(R.id.profile_user_photo);

        if (WelcomeActivity.currentUser.getDisplayName() != null) {
            mProfileUsername.setText(WelcomeActivity.currentUser.getDisplayName());
        }

        if (WelcomeActivity.currentUser.getPhotoUrl() != null) {
            GlideUtil.loadProfileIcon(WelcomeActivity.currentUser.getPhotoUrl().toString(), mProfilePhoto);
        }

        if (WelcomeActivity.currentUser.getEmail() != null) {
            mProfileEmail.setText(WelcomeActivity.currentUser.getEmail());
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_parent) {
            this.recreate();
        } else if(id == R.id.nav_sign_out) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
            startActivity(welcomeIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
    }
}