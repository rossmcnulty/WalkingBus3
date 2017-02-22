package ut.walkingbus;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Ross on 2/16/2017.
 */

public class StudentHolder extends RecyclerView.ViewHolder {
    private final TextView mNameField;
    private final TextView mStatusField;

    public StudentHolder(View itemView) {
        super(itemView);
        mNameField = (TextView) itemView.findViewById(R.id.name);
        mStatusField = (TextView) itemView.findViewById(R.id.status);
    }

    public void setName(String name) {
        mNameField.setText(name);
    }

    public void setStatus(String text) {
        mStatusField.setText(text);
    }
}