package ut.walkingbus.Models;

import java.util.Map;

/**
 * Created by Ross on 2/13/2017.
 */

public class User {
    private String displayName;
    private String email;
    private String fcm;
    private String phone;
    private String photoUrl;
    private Map<String, String> routes;
    private Map<String, String> schools_parent;
    private Map<String, String> students;
    private String key;

    public User() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFcm() {
        return fcm;
    }

    public void setFcm(String fcm) {
        this.fcm = fcm;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Map<String, String> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, String> routes) {
        this.routes = routes;
    }

    public Map<String, String> getSchools_parent() {
        return schools_parent;
    }

    public void setSchools_parent(Map<String, String> schools_parent) {
        this.schools_parent = schools_parent;
    }

    public Map<String, String> getStudents() {
        return students;
    }

    public void setStudents(Map<String, String> students) {
        this.students = students;
    }
}
