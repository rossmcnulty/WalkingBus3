package ut.walkingbus.Models;

/**
 * Created by Ross on 2/13/2017.
 */

public class Student {
    private String mName;
    private String mStatus;
    private String mKey;
    private String mBluetooth;
    private String mInfo;

    public Student() {
    }

    public Student(String name, String status, String key, String bluetooth, String info) {
        mName = name;
        mStatus = status;
        mKey = key;
        mBluetooth = bluetooth;
        mInfo = info;
    }


    public void setName(String name) {
        mName = name;
    }

    public void setKey(String key) {
        mKey = key;
    }

    public void setInfo(String info) {
        mInfo = info;
    }

    public void setBluetooth(String bluetooth) {
        mBluetooth = bluetooth;
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

    public String getKey() { return mKey; }

    public String getInfo() { return mInfo; }

    public String getBluetooth() { return mBluetooth; }

}
