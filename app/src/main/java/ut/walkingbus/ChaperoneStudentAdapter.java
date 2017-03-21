package ut.walkingbus;

/**
 * Created by Ross on 2/17/2017.
 */

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import ut.walkingbus.Models.Route;
import ut.walkingbus.Models.Student;
import ut.walkingbus.Models.User;

public class ChaperoneStudentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "ChaperoneStudentAdapter";
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private List<Student> studentList;
    private Context mContext;

    public class VHStudent extends RecyclerView.ViewHolder {
        public TextView name, status, parent_name;
        public ImageView picture;
        public View call, message;
        public String phone;


        public VHStudent(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            status = (TextView) view.findViewById(R.id.status);
            parent_name = (TextView) view.findViewById(R.id.parent_name);
            call = view.findViewById(R.id.call);
            message = view.findViewById(R.id.text);
            picture = (ImageView) view.findViewById(R.id.child_image);
        }
    }

    public class VHHeader extends RecyclerView.ViewHolder {
        public TextView name, time, location;

        public VHHeader(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            time = (TextView) view.findViewById(R.id.time);
            location = (TextView) view.findViewById(R.id.location);
        }
    }

    public ChaperoneStudentAdapter(List<Student> studentList, Context context) {
        mContext = context;
        this.studentList = studentList;
    }

    public void setStudentList(List<Student> studentList) {
        this.studentList = studentList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            //inflate your layout and pass it to view holder
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chaperone_student, parent, false);
            return new VHStudent(itemView);
        } else if (viewType == TYPE_HEADER) {
            //inflate your layout and pass it to view holder
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chaperone_header, parent, false);
            return new VHHeader(itemView);
        }

        throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VHStudent) {
            //cast holder to VHItem and set data
            final VHStudent studHolder = (VHStudent) holder;
            final Student student = studentList.get(position - 1);
            studHolder.name.setText(student.getName());
            studHolder.status.setText(student.getStatus());
            String status = student.getStatus();
            Log.d(TAG, "Status: " + status);

            String parentKey = student.getParents().keySet().iterator().next().toString();
            Log.d(TAG, "User key " + parentKey);
            DatabaseReference parentRef = FirebaseUtil.getUserRef().child(parentKey);
            parentRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User p = dataSnapshot.getValue(User.class);
                    studHolder.parent_name.setText(p.getDisplayName());
                    studHolder.phone = p.getPhone();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            studHolder.message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                    smsIntent.setData(Uri.parse("sms:" + studHolder.phone));
                    mContext.startActivity(smsIntent);
                }
            });

            studHolder.call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + studHolder.phone));
                    mContext.startActivity(dialIntent);
                }
            });
        } else if (holder instanceof VHHeader) {
            //cast holder to VHHeader and set data for header.
            final VHHeader header = (VHHeader) holder;

            DatabaseReference chapRef = FirebaseUtil.getUserRef().child(FirebaseUtil.getCurrentUserId());
            chapRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User chap = dataSnapshot.getValue(User.class);
                    // TODO: select route based on time
                    DatabaseReference routeRef = FirebaseUtil.getRoutesRef().child(chap.getRoutes().keySet().toArray()[0].toString());
                    routeRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Route r = dataSnapshot.getValue(Route.class);
                            header.name.setText(r.getName());
                            header.time.setText(r.getTime());
                            header.location.setText("Lat, Lng: " + r.getLocation().get("lat") + " " + r.getLocation().get("lng"));
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
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    @Override
    public int getItemCount() {
        return studentList.size() + 1;
    }
}