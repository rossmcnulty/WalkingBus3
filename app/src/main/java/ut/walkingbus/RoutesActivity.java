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

import ut.walkingbus.Models.RoutePublic;
import ut.walkingbus.Models.School;

/**
 * Created by Ross on 3/1/2017.
 */

/**
    This displays all available timeslots (Mon-Fri AM/PM) for a student and allows a parent to
    select each timeslot to view the RouteMapActivity and assign routes.
    Selected route names are shown when assigned.
 */

public class RoutesActivity extends BaseActivity {
    private static final String TAG = "RoutesActivity";

    private static int RESULT_LOAD_IMAGE = 1;
    private String name;
    private School mSchool;
    private Map<String, RoutePublic> mRoutesByKey;
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

    private Map<String, String> mCurrentRouteKeys;

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
                if(mCurrentRouteKeys.containsKey("MON_AM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("MON_AM"));
                }
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
                if(mCurrentRouteKeys.containsKey("MON_PM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("MON_PM"));
                }
                startActivity(routeMapIntent);
            }
        });

        tuesAmButton = (Button) findViewById(R.id.tues_am_button);
        tuesAmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "TUE_AM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                if(mCurrentRouteKeys.containsKey("TUE_AM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("TUE_AM"));
                }
                startActivity(routeMapIntent);
            }
        });

        tuesPmButton = (Button) findViewById(R.id.tues_pm_button);
        tuesPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "TUE_PM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                if(mCurrentRouteKeys.containsKey("TUE_PM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("TUE_PM"));
                }
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
                if(mCurrentRouteKeys.containsKey("WED_AM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("WED_AM"));
                }
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
                if(mCurrentRouteKeys.containsKey("WED_PM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("WED_PM"));
                }
                startActivity(routeMapIntent);
            }
        });

        thursAmButton = (Button) findViewById(R.id.thurs_am_button);
        thursAmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "THU_AM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                if(mCurrentRouteKeys.containsKey("THU_AM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("THU_AM"));
                }
                startActivity(routeMapIntent);
            }
        });

        thursPmButton = (Button) findViewById(R.id.thurs_pm_button);
        thursPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent routeMapIntent = new Intent(v.getContext(), RouteMapActivity.class);
                routeMapIntent.putExtra("TIMESLOT", "THU_PM");
                routeMapIntent.putExtra("STUDENT_KEY", studentKey);
                if(mCurrentRouteKeys.containsKey("THU_PM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("THU_PM"));
                }
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
                if(mCurrentRouteKeys.containsKey("FRI_AM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("FRI_AM"));
                }
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
                if(mCurrentRouteKeys.containsKey("FRI_PM")) {
                    routeMapIntent.putExtra("ROUTE_KEY", mCurrentRouteKeys.get("FRI_PM"));
                }
                startActivity(routeMapIntent);
            }
        });


        mRoutesByKey = new HashMap<String, RoutePublic>();
        mRouteKeysByTimeslot = new HashMap<String, String>();
        mCurrentRouteKeys = new HashMap<String, String>();

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

                    DatabaseReference routeRef = FirebaseUtil.getRoutePublicRef(routeKey);
                    routeRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            RoutePublic route = dataSnapshot.getValue(RoutePublic.class);
                            route.setKey(dataSnapshot.getKey());
                            mRoutesByKey.put(route.getKey(), route);

                            Log.d(TAG, "Timeslot post " + timeslot);
                            mCurrentRouteKeys.put(timeslot.toUpperCase(), route.getKey());
                            switch(timeslot.toUpperCase()) {
                                case "MON_AM":
                                    monAmText.setText(route.getName());
                                    break;
                                case "MON_PM":
                                    monPmText.setText(route.getName());
                                    break;
                                case "TUE_AM":
                                    tuesAmText.setText(route.getName());
                                    break;
                                case "TUE_PM":
                                    tuesPmText.setText(route.getName());
                                    break;
                                case "WED_AM":
                                    wedAmText.setText(route.getName());
                                    break;
                                case "WED_PM":
                                    wedPmText.setText(route.getName());
                                    break;
                                case "THU_AM":
                                    thursAmText.setText(route.getName());
                                    break;
                                case "THU_PM":
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
