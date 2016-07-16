
package org.ros.android.android_tutorial_map_viewer.annotations_list;

import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Annotation;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Column;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Location;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Marker;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Table;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Wall;

import java.util.Random;

public class MockDataProvider {
    // A utility method that generates random annotations
    public static Annotation getRandomAnnotation(String name) {
        Annotation annotation = null;
        Random random = new Random();
        int type = random.nextInt(5);
        switch (type) {
            case 0:
                annotation = new Column(name);
                break;
            case 1:
                annotation = new Wall(name);
                break;
            case 2:
                annotation = new Marker(name);
                break;
            case 3:
                annotation = new Table(name);
                break;
            case 4:
                annotation = new Location(name);
                break;
        }
        return annotation;
    }
}
