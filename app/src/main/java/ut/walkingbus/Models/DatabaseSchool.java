package ut.walkingbus.Models;

/**
 * Created by Ross on 3/1/2017.
 */

public class DatabaseSchool {

    private Double lat;
    private Double lng;
    private String key;
    private String name;

    public DatabaseSchool() {}

    public DatabaseSchool(Double lat, Double lng, String name) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

}
