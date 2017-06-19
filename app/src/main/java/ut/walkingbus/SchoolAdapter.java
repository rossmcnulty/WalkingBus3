package ut.walkingbus;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ut.walkingbus.Models.School;
import ut.walkingbus.Models.User;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ut.walkingbus.R.layout.school;

/**
 * Created by Ross on 2/24/2017.
 * Adapter that holds schools when presented to parents. Handles parent adding themself to school
 * via passcode.
 */


public class SchoolAdapter extends RecyclerView.Adapter<SchoolAdapter.MyViewHolder> {

    final String TAG = "SchoolAdapter";
    private List<School> schoolList;
    private List<String> joinedSchools;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, status;
        public Button request;

        public MyViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            status = (TextView) view.findViewById(R.id.status);
            request = (Button) view.findViewById(R.id.request_submit);
        }
    }


    public SchoolAdapter(List<School> schoolList, Context context) {
        mContext = context;
        this.schoolList = schoolList;
    }

    @Override
    public SchoolAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(school, parent, false);

        joinedSchools = new ArrayList<>();
        DatabaseReference parentSchoolsRef = FirebaseUtil.getUserSchoolsParentRef(FirebaseUtil.getCurrentUserId());
        parentSchoolsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Parent School Data Changed");
                if(!dataSnapshot.exists()) {
                    // TODO: deal with removing membership?
                    return;
                }
                Map schoolValues = (Map) dataSnapshot.getValue();
                Iterator it = schoolValues.entrySet().iterator();
                joinedSchools.clear();
                while(it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    Log.d(TAG, "School key is " + pair.getKey().toString());
                    if(!joinedSchools.contains(pair.getKey().toString())) {
                        joinedSchools.add(pair.getKey().toString());
                        notifyDataSetChanged();
                    }
                    it.remove();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final School school = schoolList.get(position);
        holder.name.setTag(school);
        holder.name.setText(school.getName());
        holder.request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder schoolCodeBuilder = new AlertDialog.Builder(mContext);
                schoolCodeBuilder.setMessage("Enter school code")
                        .setTitle("School Code Verification");

                final EditText input = new EditText(v.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                schoolCodeBuilder.setView(input);

                schoolCodeBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        FirebaseUtil.getUserRef().child(FirebaseUtil.getCurrentUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                User user = dataSnapshot.getValue(User.class);
                                user.setKey(dataSnapshot.getKey());

                                Map parentSchoolValues = new HashMap<>();
                                parentSchoolValues.put(school.getKey(), school.getName());
                                String schoolCode = input.getText().toString();

                                Map propagatedSchoolValues = new HashMap();
                                propagatedSchoolValues.put("users/" + user.getKey() + "/schools_parent/" + school.getKey(), school.getName());

                                Map schoolParentValues = new HashMap();
                                schoolParentValues.put("displayName", user.getDisplayName());
                                schoolParentValues.put("code", schoolCode);
                                schoolParentValues.put("photoUrl", user.getPhotoUrl().toString());
                                schoolParentValues.put("phone", user.getPhone().toString());
                                Log.d(TAG, "Path: " + "schools/" + school.getKey() + "/users/" + user.getKey());
                                Log.d(TAG, "Value: " + schoolParentValues.keySet().toString());
                                propagatedSchoolValues.put("schools/" + school.getKey() + "/users/" + user.getKey(), schoolParentValues);

                                FirebaseUtil.getBaseRef().updateChildren(propagatedSchoolValues, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            Log.d(TAG, "Save data failed");
                                            Toast.makeText(input.getContext(),
                                                    "Couldn't save school data: " + databaseError.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                });
                schoolCodeBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.dismiss();
                    }
                });
                AlertDialog schoolCodeAlert = schoolCodeBuilder.create();
                schoolCodeAlert.show();
            }
        });
        Log.d(TAG, "Rebinding view holder");
        if(joinedSchools.contains(school.getKey())) {
            // user is already a member
            holder.status.setText("Joined");
            holder.request.setVisibility(GONE);
        } else {
            holder.status.setText("Not joined");
            holder.request.setVisibility(VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return schoolList.size();
    }

}
