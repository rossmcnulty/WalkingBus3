package ut.walkingbus.Models;

import java.util.Map;

/**
 * Created by Ross on 3/21/2017.
 */

public class RoutePublic {
    private String name;
    private String school;
    private String time;
    private String key;
    private Map<String, Map<String, String>> chaperones;
    private Map<String, Double> location;

    public RoutePublic() {

    }

    public Map<String, Double> getLocation() {
        return location;
    }

    public void setLocation(Map<String, Double> location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Map<String, String>> getChaperones() {
        return chaperones;
    }

    public void setChaperones(Map<String, Map<String, String>> chaperones) {
        this.chaperones = chaperones;
    }
}
