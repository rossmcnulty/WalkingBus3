package ut.walkingbus.Models;

import java.util.Map;

/**
 * Created by Ross on 3/21/2017.
 */

public class RoutePrivate {

    private String status;
    private Map<String, Map<String, String>> students;
    private String key;

    public RoutePrivate() {

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Map<String, String>> getStudents() {
        return students;
    }

    public void setStudents(Map<String, Map<String, String>> students) {
        this.students = students;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
