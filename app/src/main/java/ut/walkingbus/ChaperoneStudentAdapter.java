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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import ut.walkingbus.Models.Student;
import ut.walkingbus.Models.User;

public class ChaperoneStudentAdapter extends RecyclerView.Adapter<ChaperoneStudentAdapter.MyViewHolder> {
    private static final String TAG = "ChaperoneStudentAdapter";

    private List<Student> studentList;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, status, parent_name;
        public ImageView picture;
        public ImageButton options;
        public View call, message;
        public String phone;


        public MyViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            status = (TextView) view.findViewById(R.id.status);
            parent_name = (TextView) view.findViewById(R.id.parent_name);
            call = view.findViewById(R.id.call);
            message = view.findViewById(R.id.text);
            picture = (ImageView) view.findViewById(R.id.child_image);
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
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chaperone_student, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final Student student = studentList.get(position);
        holder.name.setText(student.getName());
        holder.status.setText(student.getStatus());
        String status = student.getStatus();
        Log.d(TAG, "Status: " + status);

        String parentKey = student.getParents().keySet().iterator().next().toString();
        Log.d(TAG, "User key " + parentKey);
        DatabaseReference parentRef = FirebaseUtil.getUserRef().child(parentKey);
        parentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User p = dataSnapshot.getValue(User.class);
                holder.parent_name.setText(p.getDisplayName());
                holder.phone = p.getPhone();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

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
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }
}