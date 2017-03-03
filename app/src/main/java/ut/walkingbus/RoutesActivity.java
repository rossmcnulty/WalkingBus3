package ut.walkingbus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import ut.walkingbus.Models.DatabaseSchool;
import ut.walkingbus.Models.Route;

/**
 * Created by Ross on 3/1/2017.
 */

public class RoutesActivity extends BaseActivity {
    private static final String TAG = "RoutesActivity";

    private static int RESULT_LOAD_IMAGE = 1;
    private String name;
    private DatabaseSchool mSchool;
    private Map<String, Route> mRoutesByKey;
    private Map<String, String> mRouteKeysByTimeslot;
    TextView monAmText;
    TextView monPmText;
    TextView tuesAmText;
    TextView tuesPmText;
    TextView wedAmText;
    TextView wedPmText;
    TextView thursAmText;
    TextView thursPmText;
    TextView friAmText;
    TextView friPmText;

    Button monAmButton;
    Button monPmButton;
    Button tuesAmButton;
    Button tuesPmButton;
    Button wedAmButton;
    Button wedPmButton;
    Button thursAmButton;
    Button thursPmButton;
    Button friAmButton;
    Button friPmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final String studentKey = getIntent().getStringExtra("STUDENT_KEY");

        monAmText = (TextView) findViewById(R.id.mon_am_text);
        monPmText = (TextView) findViewById(R.id.mon_pm_text);
        tuesAmText = (TextView) findViewById(R.id.tues_am_text);
        tuesPmText = (TextView) findViewById(R.id.tues_pm_text);
        wedAmText = (TextView) findViewById(R.id.wed_am_text);
        wedPmText = (TextView) findViewById(R.id.wed_pm_text);
        thursAmText = (TextView) findViewById(R.id.thurs_am_text);
        thursPmText = (TextView) findViewById(R.id.thurs_pm_text);
        friAmText = (TextView) findViewById(R.id.fri_am_text);
        friPmText = (TextView) findViewById(R.id.fri_pm_text);

        monAmButton = (Button) findViewById(R.id.mon_am_button);
        monAmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "MON_AM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        monPmButton = (Button) findViewById(R.id.mon_pm_button);
        monPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "MON_PM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        tuesAmButton = (Button) findViewById(R.id.tues_am_button);
        tuesAmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "TUES_AM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        tuesPmButton = (Button) findViewById(R.id.tues_pm_button);
        tuesPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "TUES_PM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        wedAmButton = (Button) findViewById(R.id.wed_am_button);
        wedAmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "WED_AM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        wedPmButton = (Button) findViewById(R.id.wed_pm_button);
        wedPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "WED_PM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        thursAmButton = (Button) findViewById(R.id.thurs_am_button);
        thursAmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "THURS_AM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        thursPmButton = (Button) findViewById(R.id.thurs_pm_button);
        thursPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "THURS_PM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        friAmButton = (Button) findViewById(R.id.fri_am_button);
        friAmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "FRI_AM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });

        friPmButton = (Button) findViewById(R.id.fri_pm_button);
        friPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "FRI_PM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                startActivity(routeMapIntent);
            }
        });


        mRoutesByKey = new HashMap<String, Route>();
        mRouteKeysByTimeslot = new HashMap<String, String>();

        setTitle("View Routes");

        // get routes student is a member of
        DatabaseReference studentRoutesRef = FirebaseUtil.getStudentRoutesRef(studentKey);
        studentRoutesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot routeSnapshot: dataSnapshot.getChildren()) {
                    final String timeslot = routeSnapshot.getKey();
                    final String routeKey = routeSnapshot.getValue().toString();
                    Log.d(TAG, "Timeslot pre " + timeslot);
                    mRouteKeysByTimeslot.put(timeslot, routeKey);

                    DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(routeKey);
                    routeRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Route route = dataSnapshot.getValue(Route.class);
                            route.setKey(dataSnapshot.getKey());
                            mRoutesByKey.put(route.getKey(), route);

                            Log.d(TAG, "Timeslot post " + timeslot);
                            switch(timeslot.toUpperCase()) {
                                case "MON_AM":
                                    monAmText.setText(route.getName());
                                    break;
                                case "MON_PM":
                                    monPmText.setText(route.getName());
                                    break;
                                case "TUES_AM":
                                    tuesAmText.setText(route.getName());
                                    break;
                                case "TUES_PM":
                                    tuesPmText.setText(route.getName());
                                    break;
                                case "WED_AM":
                                    wedAmText.setText(route.getName());
                                    break;
                                case "WED_PM":
                                    wedPmText.setText(route.getName());
                                    break;
                                case "THURS_AM":
                                    thursAmText.setText(route.getName());
                                    break;
                                case "THURS_PM":
                                    thursPmText.setText(route.getName());
                                    break;
                                case "FRI_AM":
                                    friAmText.setText(route.getName());
                                    break;
                                case "FRI_PM":
                                    friPmText.setText(route.getName());
                                    break;
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) { }
        });



    }

}
