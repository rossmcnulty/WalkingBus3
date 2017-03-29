package ut.walkingbus;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import ut.walkingbus.Models.School;

/**
 * Created by Ross on 2/24/2017.
 */

public class SchoolSpinAdapter extends ArrayAdapter<School> {

    // Your sent context
    private Context context;
    // Your custom values for the spinner (User)
    private ArrayList<School> values;

    public SchoolSpinAdapter(Context context, int textViewResourceId,
                       ArrayList<School> values) {
        super(context, textViewResourceId, values);
        this.context = context;
        this.values = values;
    }

    public int getCount(){
        return values.size();
    }

    public School getItem(int position){
        return values.get(position);
    }

    public long getItemId(int position){
        return position;
    }
}
