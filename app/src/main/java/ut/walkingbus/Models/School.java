package ut.walkingbus.Models;

/**
 * Created by Ross on 2/23/2017.
 */

public class School {
    String name;
    String key;
    boolean selected;

    public School(String name, String key, boolean selected) {
        this.name = name;
        this.key = key;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
