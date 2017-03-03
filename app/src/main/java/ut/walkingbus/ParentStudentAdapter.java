package ut.walkingbus;

/**
 * Created by Ross on 2/17/2017.
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
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

import java.util.List;

import ut.walkingbus.Models.Student;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ParentStudentAdapter extends RecyclerView.Adapter<ParentStudentAdapter.MyViewHolder> {
    private static final String TAG = "ParentStudentAdapter";

    private List<Student> studentList;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, status, chaperone_name;
        public ImageView picture;
        public ImageButton options;
        public View call, message;


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

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final Student student = studentList.get(position);
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
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
            }
        });

        if(status.equals(mContext.getString(R.string.status_picked_up)) ||
                status.equals(mContext.getString(R.string.status_lost))) {

            holder.message.setVisibility(VISIBLE);
            holder.call.setVisibility(VISIBLE);
            holder.chaperone_name.setVisibility(VISIBLE);

            holder.message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                    smsIntent.setType("vnd.android-dir/mms-sms");
                    //smsIntent.putExtra("sms_body","Body of Message");
                    mContext.startActivity(smsIntent);
                }
            });

            holder.call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mContext.startActivity(callIntent);
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