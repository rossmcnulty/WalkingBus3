package ut.walkingbus;

/**
 * Created by Ross on 2/17/2017.
 */

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ut.walkingbus.Models.RoutePublic;
import ut.walkingbus.Models.Student;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ParentStudentAdapter extends RecyclerView.Adapter<ParentStudentAdapter.MyViewHolder> {
    private static final String TAG = "ParentStudentAdapter";

    private List<Student> studentList;
    private Context mContext;
    private FirebaseStorage mStorage;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, status, chaperone_name;
        public ImageView picture;
        public ImageButton options;
        public View call, message;
        public String phone;


        public MyViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            options = (ImageButton) view.findViewById(R.id.options);
            status = (TextView) view.findViewById(R.id.status);
            chaperone_name = (TextView) view.findViewById(R.id.chaperone_name);
            call = view.findViewById(R.id.call);
            message = view.findViewById(R.id.text);
            picture = (ImageView) view.findViewById(R.id.child_image);
        }
    }


    public ParentStudentAdapter(List<Student> studentList, Context context) {
        mContext = context;
        this.studentList = studentList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.parent_student, parent, false);
        mStorage = FirebaseStorage.getInstance();
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final Student student = studentList.get(position);
        mContext = holder.itemView.getContext();
        holder.name.setText(student.getName());
        holder.status.setText(student.getStatus());
        String status = student.getStatus();
        Log.d(TAG, "Status: " + status);
        holder.options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(mContext, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_parent_student, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.edit:
                                Intent editStudentIntent = new Intent(mContext, EditStudentActivity.class);
                                editStudentIntent.putExtra("STUDENT_KEY", student.getKey());
                                mContext.startActivity(editStudentIntent);
                                return true;
                            case R.id.routes:
                                Intent viewRoutesIntent = new Intent(mContext, RoutesActivity.class);
                                viewRoutesIntent.putExtra("STUDENT_KEY", student.getKey());
                                mContext.startActivity(viewRoutesIntent);
                                return true;
                            case R.id.delete:
                                final DatabaseReference studentRouteRefs = FirebaseUtil.getStudentRoutesRef(student.getKey());
                                studentRouteRefs.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        ArrayList<String> routeRefs = new ArrayList<String>();

                                        if(dataSnapshot.hasChild("mon_am")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("mon_am").getValue().toString() + "/private/students/mon_am");
                                        }
                                        if(dataSnapshot.hasChild("mon_pm")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("mon_pm").getValue().toString() + "/private/students/mon_pm");
                                        }
                                        if(dataSnapshot.hasChild("tue_am")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("tue_am").getValue().toString() + "/private/students/tue_am");
                                        }
                                        if(dataSnapshot.hasChild("tue_pm")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("tue_pm").getValue().toString() + "/private/students/tue_pm");
                                        }
                                        if(dataSnapshot.hasChild("wed_am")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("wed_am").getValue().toString() + "/private/students/wed_am");
                                        }
                                        if(dataSnapshot.hasChild("wed_pm")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("wed_pm").getValue().toString() + "/private/students/wed_pm");
                                        }
                                        if(dataSnapshot.hasChild("thu_am")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("thu_am").getValue().toString() + "/private/students/thu_am");
                                        }
                                        if(dataSnapshot.hasChild("thu_pm")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("thu_pm").getValue().toString() + "/private/students/thu_pm");
                                        }
                                        if(dataSnapshot.hasChild("fri_am")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("fri_am").getValue().toString() + "/private/students/fri_am");
                                        }
                                        if(dataSnapshot.hasChild("fri_pm")) {
                                            routeRefs.add("routes/" + dataSnapshot.child("fri_pm").getValue().toString() + "/private/students/fri_pm");
                                        }

                                        Map propagatedStudentValues = new HashMap<>();
                                        propagatedStudentValues.put("students/" + student.getKey(), null);
                                        Log.d(TAG, "Student ref: " + "students/" + student.getKey());
                                        propagatedStudentValues.put("users/" + student.getParents().keySet().toArray()[0] + "/students/" + student.getKey(), null);
                                        Log.d(TAG, "Users ref: " + "users/" + student.getParents().keySet().toArray()[0] + "/students/" + student.getKey());
                                        propagatedStudentValues.put("schools/" + student.getSchool() + "/students/" + student.getKey(), null);
                                        Log.d(TAG, "School ref: " + "schools/" + student.getSchool() + "/students/" + student.getKey());
                                        for(String routeRef: routeRefs) {
                                            Log.d(TAG, "Route ref: " + routeRef + "/" + student.getKey());
                                            propagatedStudentValues.put(routeRef + "/" + student.getKey(), null);
                                        }
                                        FirebaseUtil.getBaseRef().updateChildren(propagatedStudentValues, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                if (databaseError != null) {
                                                    Toast.makeText(mContext,
                                                            "Couldn't delete student data: " + databaseError.getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                } else {
                                                    studentList.remove(student);
                                                    notifyDataSetChanged();
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
            }
        });

        if(student.getPhotoUrl() != null) {
            Log.d(TAG, "Student has Photo URL");

            StorageReference gsReference = mStorage.getReferenceFromUrl(student.getPhotoUrl());

            // ImageView in your Activity
            ImageView imageView = holder.picture;
            imageView.setBackgroundColor(0);

            // Load the image using Glide
            Glide.with(mContext)
                    .using(new FirebaseImageLoader())
                    .load(gsReference)
                    .signature(new StringSignature(UUID.randomUUID().toString()))
                    .into(imageView);
        }

        if(status.toLowerCase().equals("picked up") ||
                status.toLowerCase().equals("lost")) {

            holder.message.setVisibility(VISIBLE);
            holder.call.setVisibility(VISIBLE);
            holder.chaperone_name.setVisibility(VISIBLE);

            FirebaseUtil.getBaseRef().child("current_timeslot").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    DatabaseReference studentRouteRef = FirebaseUtil.getStudentsRef().child(student.getKey()).child("routes").child(dataSnapshot.getValue().toString());
                    studentRouteRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            DatabaseReference routeChaperoneRef = FirebaseUtil.getRoutePublicRef(dataSnapshot.getValue().toString());
                            routeChaperoneRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    RoutePublic route = dataSnapshot.getValue(RoutePublic.class);
                                    String chapKey = route.getChaperones().keySet().iterator().next().toString();

                                    holder.chaperone_name.setText(route.getChaperones().get(chapKey).get("displayName"));
                                    holder.phone = route.getChaperones().get(chapKey).get("phone");
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            String chaperoneKey = student.getParents().keySet().iterator().next().toString();

            /*
            Log.d(TAG, "Chaperone key " + chaperoneKey);
            DatabaseReference parentRef = FirebaseUtil.getUserRef().child(chaperoneKey);
            parentRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User p = dataSnapshot.getValue(User.class);
                    holder.chaperone_name.setText(p.getDisplayName());
                    holder.phone = p.getPhone();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            */

            holder.message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                    smsIntent.setData(Uri.parse("sms:" + holder.phone));
                    mContext.startActivity(smsIntent);
                }
            });

            holder.call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + holder.phone));
                    mContext.startActivity(dialIntent);
                }
            });
        } else {
            holder.message.setVisibility(GONE);
            holder.call.setVisibility(GONE);
            holder.chaperone_name.setVisibility(GONE);
        }
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }
}