package ut.walkingbus.Models;

import java.util.Map;

/**
 * Created by Ross on 2/13/2017.
 */

public class Student {
    private String mName;
    private String mStatus;
    private String mKey;
    private String mBluetooth;
    private String mInfo;
    private Map<String, Map<String, String>> mParents;
    private String mSchool;
    private Map<String, String> mRoutes;

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

    public void setSchool(String school) {
        mSchool = school;
    }

    public void setBluetooth(String bluetooth) {
        mBluetooth = bluetooth;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public void setRoutes(Map<String, String> routes) {
        mRoutes = routes;
    }

    public void setParents(Map<String, Map<String, String>> parents) {
        mParents = parents;
    }

    public Map<String, Map<String, String>> getParents() {
        return mParents;
    }

    public String getName() {
        return mName;
    }

    public String getSchool() {
        return mSchool;
    }

    public Map<String, String> getRoutes() {
        return mRoutes;
    }

    public String getStatus() {
        return mStatus;
    }

    public String getKey() { return mKey; }

    public String getInfo() { return mInfo; }

    public String getBluetooth() { return mBluetooth; }

}
