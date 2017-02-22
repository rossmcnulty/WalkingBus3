package ut.walkingbus.Models;

/**
 * Created by Ross on 2/13/2017.
 */

public class Student {
    private String mName;
    private String mStatus;

    public Student() {
    }

    public Student(String name, String status) {
        mName = name;
        mStatus = status;
    }


    public void setName(String name) {
        mName = name;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public String getName() {
        return mName;
    }

    public String getStatus() {
        return mStatus;
    }

}
