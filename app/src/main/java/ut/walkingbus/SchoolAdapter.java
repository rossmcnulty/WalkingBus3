package ut.walkingbus;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.util.List;

import ut.walkingbus.Models.School;

/**
 * Created by Ross on 2/24/2017.
 */


public class SchoolAdapter extends RecyclerView.Adapter<SchoolAdapter.MyViewHolder> {

    private List<School> schoolList;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public CheckBox name;

        public MyViewHolder(View view) {
            super(view);
            name = (CheckBox) view.findViewById(R.id.checkBox1);
        }
    }


    public SchoolAdapter(List<School> schoolList, Context context) {
        mContext = context;
        this.schoolList = schoolList;
    }

    @Override
    public SchoolAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.school, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final School school = schoolList.get(position);
        holder.name.setTag(school);
        holder.name.setText(school.getName());

        holder.name.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                CheckBox cb = (CheckBox) v ;
                School school = (School) cb.getTag();
                school.setSelected(cb.isChecked());
            }
        });
    }

    @Override
    public int getItemCount() {
        return schoolList.size();
    }

}
