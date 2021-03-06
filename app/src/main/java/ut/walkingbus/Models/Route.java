package ut.walkingbus.Models;

import java.util.Map;

/**
 * Created by Ross on 3/2/2017.
 */

public class Route {

    private String key;
    private String chaperone;
    private String name;
    private String school;
    private String time;
    private String timeslot;
    private String status;
    private Map<String, Double> location;
    private Map<String, Map<String, String>> students;

    public Route() {}

    public String getKey() {
        return key;
    }

    public String getChaperone() {
        return chaperone;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getSchool() {
        return school;
    }

    public String getTime() {
        return time;
    }

    public Map<String, Map<String, String>> getStudents() {
        return students;
    }

    public Map<String, Double> getLocation() {
        return location;
    }

    public void setTimeslot(String timeslot) {
        this.timeslot = timeslot;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setChaperone(String chaperone) {
        this.chaperone = chaperone;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStudents(Map<String, Map<String, String>> students) {
        this.students = students;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setLocation(Map<String, Double> location) {
        this.location = location;
    }
}
