package org.ros.android.android_tutorial_map_viewer.annotations_list.annotations;

import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.VisualizationView;

import javax.microedition.khronos.opengles.GL10;

public class Table extends Annotation {
    public static final String GROUP_NAME = "Tables";

    private static final float MINIMUM_RADIUS = 0.1f;
    private static final float DEFAULT_HEIGHT = 1.0f;
    private static final Color COLOR = Color.fromHexAndAlpha("C8B560", 0.8f);
    private static final float VERTICES[] = circleVertices(30, MINIMUM_RADIUS, 0.0f, 0.0f);

    private static float[] circleVertices(int vertexCount, float radius,
                                          float center_x, float center_y) {
        //create a buffer for vertex data
        float buffer[] = new float[vertexCount*3]; // (x,y,z) for each vertex
        int idx = 0;

        // center vertex for triangle fan
        buffer[idx++] = center_x;
        buffer[idx++] = center_y;
        buffer[idx++] = 0.0f;  // z-axis

        // outer vertices of the circle
        int outerVertexCount = vertexCount-1;

        for (int i = 0; i < outerVertexCount; ++i) {
            float percent = (i / (float) (outerVertexCount-1));
            float radians = percent * 2f * (float)Math.PI;

            // vertex position
            float outer_x = center_x + radius * (float)Math.cos(radians);
            float outer_y = center_y + radius * (float)Math.sin(radians);

            buffer[idx++] = outer_x;
            buffer[idx++] = outer_y;
            buffer[idx++] = 0.0f;  // z-axis
        }

        return buffer;
    }

    public Table(String name) {
        super(name, VERTICES, COLOR);
        setGroup(GROUP_NAME);

        sizeXY = MINIMUM_RADIUS;
        height = DEFAULT_HEIGHT;
    }

    public float getRadius() { return sizeXY; }

    @Override
    protected void scale(VisualizationView view, GL10 gl) {
        // The scale is in metric space, so we can directly use shape's size.
        gl.glScalef(sizeXY / MINIMUM_RADIUS, sizeXY / MINIMUM_RADIUS, 1.0f);
    }
    @Override
    protected  void invertScale(GL10 gl){
        gl.glScalef( MINIMUM_RADIUS/sizeXY, MINIMUM_RADIUS/sizeXY, 1.0f);
    }
}
