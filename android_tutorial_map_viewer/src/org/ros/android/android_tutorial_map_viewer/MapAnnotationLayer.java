package org.ros.android.android_tutorial_map_viewer;



import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.apps.AppParameters;
import com.google.common.base.Preconditions;

import org.ros.android.android_tutorial_map_viewer.annotations_list.AnnotationsList;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Annotation;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Column;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Location;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Marker;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Table;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Wall;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.XYOrthographicCamera;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.shape.GoalShape;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransform;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import geometry_msgs.Pose;

//import org.ros.android.view.visualization.shape.PoseShape;

/**
 * Shows a map retrieved from map_store and allows add annotations (i.e. semantic information)
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */

/** 地图标注层类 */
public class MapAnnotationLayer extends DefaultLayer {

    private final Context context;
    private Annotation annotation;
    private GestureDetector gestureDetector;
    private Transform pose;
    private Transform fixedPose;
    private XYOrthographicCamera camera;
    private ConnectedNode connectedNode;
    private AnnotationsList annotationsList;
    private Mode mode;
//    private PoseShape origin_shape;
    private GoalShape camera_shape;
    private double distance = 1.0;
    private AppParameters params;

    private boolean choosesss = true;
    private final GraphName targetFrame;

    public enum Mode {
        ADD_MARKER, ADD_TABLE, ADD_COLUMN, ADD_WALL, ADD_LOCATION, ADD_NODEMAP
    }

    public MapAnnotationLayer(final Context context, final AnnotationsList annotationsList,
                              final AppParameters params) {
        this.context = context;
        this.annotationsList = annotationsList;
        this.params = params;
        targetFrame = GraphName.of("map");
    }


    public void setMode(Mode mode) {
        this.mode = mode;
    }

    private static final ThreadLocal<FloatBuffer> buffer = new ThreadLocal<FloatBuffer>() {
        @Override
        protected FloatBuffer initialValue() {
            return FloatBuffer.allocate(16);
        };

        @Override
        public FloatBuffer get() {
            FloatBuffer buffer = super.get();
            buffer.clear();
            return buffer;
        };
    };

    @Override
    public void draw(VisualizationView view, GL10 gl) {
        // Draw currently creating annotation
        if (annotation != null) {
            annotation.draw(view, gl);
        }
        //Draw already created annotations
        for (Annotation annotation: annotationsList.listFullContent()) {
            annotation.draw(view, gl);
        }
    }




    private double angle(double x1, double y1, double x2, double y2) {
        double deltaX = x1 - x2;
        double deltaY = y1 - y2;
        return Math.atan2(deltaY, deltaX);
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /** 2016.7.25 */
    public double getDist(double xx1, double yy1, double xx2, double yy2) {
        return dist(xx1, yy1, xx2, yy2);
    }
    /** 2016.7.25 */
    public double getAngle(double x1, double y1, double x2, double y2) {
        return angle(x1, y1, x2, y2);
    }

    @Override
    public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
        if (annotation != null) {
            Preconditions.checkNotNull(pose);

            Vector3 poseVector;
            Vector3 pointerVector;

            if (event.getAction() == MotionEvent.ACTION_MOVE) {

                poseVector = pose.apply(Vector3.zero());
                pointerVector = camera.toCameraFrame((int) event.getX(), (int) event.getY());

                double dist  = dist(pointerVector.getX(), pointerVector.getY(),
                        poseVector.getX(), poseVector.getY());
                double angle = angle(pointerVector.getX(), pointerVector.getY(),
                        poseVector.getX(), poseVector.getY());
                pose = Transform.translation(poseVector).multiply(Transform.zRotation(angle));

                annotation.setTransform(pose);
                annotation.setSizeXY((float) dist);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                confirmAnnotation();
                return true;
            }
        }
        gestureDetector.onTouchEvent(event);
        return false;
    }

    @Override
    public void onStart(VisualizationView view, ConnectedNode connectedNode) {
        this.camera = view.getCamera();
        this.camera.setFrame((String) params.get("map_frame", context.getString(R.string.map_frame)));
//        this.origin_shape = new PoseShape(camera);
        this.camera_shape = new GoalShape();
        this.mode = Mode.ADD_MARKER;
        Handler handler = view.getHandler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                gestureDetector = new GestureDetector(context,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public void onLongPress(MotionEvent e) {
                                pose = Transform.translation(camera.toCameraFrame((int) e.getX(), (int) e.getY()));
                                switch (mode) {
                                    case ADD_MARKER:
                                        annotation = new Marker("marker 1");
                                        break;
                                    case ADD_TABLE:
                                        annotation = new Table("Table 1");
                                        break;
                                    case ADD_COLUMN:
                                        annotation = new Column("Column 1");
                                        break;
                                    case ADD_WALL:
                                        annotation = new Wall("Wall 1");
                                        break;
                                    case ADD_LOCATION:
                                        annotation = new Location("Location 1");
                                        break;
                                    default:
                                        Log.e("MapAnn", "Unimplemented annotation mode: " + mode);
                                }

                                annotation.setTransform(pose);
                                //fixedPose = Transform.translation(camera.toMetricCoordinates((int) e.getX(), (int) e.getY()));
                            }
                        });
            }
        });
    }

    @Override
    public void onShutdown(VisualizationView view, Node node) {}

    /** 2016.7.22 */
    public void getConfirmAnnotation() {
        confirmAnnotation();
    }

    private void confirmAnnotation() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.annotation_cfg, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptView);

        final boolean annotationAccepted = false;
        final EditText name_edit    = (EditText) promptView.findViewById(R.id.name_edit);
        final EditText height_edit  = (EditText) promptView.findViewById(R.id.height_edit);
//        final TextView name_label   = (TextView) promptView.findViewById(R.id.name_label);
//        final TextView height_label = (TextView) promptView.findViewById(R.id.height_label);

        // Customize for some slightly exotic annotations
/** 自定义一些外在的标注 */         // Marker name must be its id, a positive integer
        if (mode == Mode.ADD_MARKER)    {
            name_edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        }
        if (mode == Mode.ADD_NODEMAP)    {
            name_edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); //调用数字键盘
        }


        // Setup a dialog windowAnnotation
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // get user input and set it to result
                        try {
                            if (name_edit.getText().length() == 0)
                                throw new Exception("标注名不能为空");
//                                throw new Exception("Annotation name cannot be empty");
                            if (height_edit.getText().length() > 0)
                                annotation.setHeight(Float.parseFloat(height_edit.getText().toString()));
                            annotation.setName(name_edit.getText().toString());
                            annotationsList.addItem(annotation);
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "高度必须是数字类型", Toast.LENGTH_LONG).show();
//                            Toast.makeText(context, "Height must be a number; discarding...", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(context, "ee - - " + e.getMessage()+ " - - ee", Toast.LENGTH_LONG).show();
                        } finally {
                            annotation = null;
                        }
                    }
                }
        ).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                annotation = null;
            }
        });

        // Create and show an alert dialog to get annotation info
        final AlertDialog alertDlg = alertDialogBuilder.create();
        alertDlg.show();

        name_edit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                alertDlg.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setEnabled(name_edit.getText().length() > 0);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }
    public void addAnno(Annotation anno) {
        annotationsList.addItem(anno);
    }
    public void addAnno(geometry_msgs.PoseStamped pose) {
        if (choosesss) {
            annotation = new Marker("marker 1");
            choosesss = false;
        } else {
            annotation = new Table("Table 1");
            choosesss = true;
        }

        Transform poseTransform = Transform.fromPoseMessage(pose.getPose());
        annotation.setTransform(camera.getCameraToRosTransform().multiply(poseTransform));
        confirmAnnotation();

    }
}
