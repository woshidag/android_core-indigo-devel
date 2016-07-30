/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_tutorial_map_viewer;
/*
 * Copyright (C) 2013 OSRF.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MasterChooser;
import org.ros.android.android_tutorial_map_viewer.annotations_list.AnnotationsList;
import org.ros.android.android_tutorial_map_viewer.annotations_list.AnnotationsPublisher;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Annotation;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Column;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Location;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Marker;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Table;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Wall;
import org.ros.android.view.RosImageView;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.XYOrthographicCamera;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.PoseSubscriberLayer;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;
import org.ros.time.NtpTimeProvider;

import java.util.concurrent.TimeUnit;

import ar_track_alvar_msgs.AlvarMarkers;
import geometry_msgs.Pose;
import world_canvas_msgs.SaveMapResponse;





/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends RosAppActivity {

  private static final int NAME_MAP_DIALOG_ID = 0;

  private RosImageView<sensor_msgs.CompressedImage> cameraView;
  private VirtualJoystickView virtualJoystickView;
  private VisualizationView mapView;
  private ViewGroup mainLayout;
  private ViewGroup sideLayout;

  private AnnotationsList annotationsList;
  private MapAnnotationLayer annotationLayer;

  /**  2016.7.15  */
   Annotation annotation;
//  private MockDataProvider mockDataProvider;

  XYOrthographicCamera camera;


  String mapFrame;
  Subscriber<AlvarMarkers> markersSub;
  int marker_num = 0;

  boolean initialized = false;
  boolean markers_initialized = false;
  boolean tables_initialized = false;
  boolean columns_initialized = false;
  boolean walls_initialized = false;


  ConnectedNode connectedNode;
//  VisualizationView view;



  //  private ImageButton refreshButton;
//  private ImageButton saveButton;
//  private Button backButton;
//  去掉私有修饰 改为public
  public ImageButton refreshButton;
  public ImageButton saveButton;
  public Button backButton;
  public Button nodeButton;

  private NodeMainExecutor nodeMainExecutor;
  private NodeConfiguration nodeConfiguration;
  private ProgressDialog waitingDialog;
  private AlertDialog notiDialog;

  private AnnotationsPublisher annotationsPub;

  public ViewControlLayer viewControlLayer;



  //  private OccupancyGridLayer occupancyGridLayer = null;
//  private LaserScanLayer laserScanLayer = null;
//  private RobotLayer robotLayer = null;
/** 去掉私有private修饰 改为public */
  public OccupancyGridLayer occupancyGridLayer = null;
  public LaserScanLayer laserScanLayer = null;
  /** 2016.7.19*/
  public PoseSubscriberLayer poseSubscriberLayer = null;
//  public RobotLayer robotLayer = null;

  Shape shape;
  GraphName targetFrame;


  public MainActivity() {
    // The RosActivity constructor configures the notification title and
    // ticker
    // messages.
    super("Make a map", "Make a map");

  }

  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(Bundle savedInstanceState) {

    String defaultRobotName = getString(R.string.default_robot);
    String defaultAppName = getString(R.string.default_app);
    setDefaultMasterName(defaultRobotName);
    setDefaultAppName(defaultAppName);
    setDashboardResource(R.id.top_bar);
    /** 显示main界面 */
    setMainWindowResource(R.layout.main);

    super.onCreate(savedInstanceState);

    cameraView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
    cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
    cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

    virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
    refreshButton       = (ImageButton) findViewById(R.id.refresh_button);
    saveButton          = (ImageButton) findViewById(R.id.save_map);
    backButton          = (Button) findViewById(R.id.back_button);
    nodeButton          = (Button) findViewById(R.id.node_button);

    mapView = (VisualizationView) findViewById(R.id.map_view);
    mapView.onCreate(Lists.<Layer>newArrayList());


//    Log.e("oncreate---", "mapView === " + mapView);
//    Log.e("oncreate---", "connectedNode === " + connectedNode); //connectedNode 为 null


    refreshButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO
        Toast.makeText(MainActivity.this, "正在刷新地图...",
                Toast.LENGTH_SHORT).show();
        mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));
      }
    });



    saveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

//       显示保存地图的对话框
        showDialog(NAME_MAP_DIALOG_ID);

/**  2016.7.15 添加 */
        new MapManager(getApplication() ,remaps).saveMap();

//        Toast.makeText(MainActivity.this, "地图已保存",
//                Toast.LENGTH_SHORT).show();

      }

    });

//    返回按钮点击事件
    backButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
//        onBackPressed();
        myBack();
      }
    });

//    标注按钮点击事件
//    nodeButton.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View view) {
//
//      }
//    });


    /**   2016.07.14 */
    // Configure the ExpandableListView and its adapter containing current annotations
    ExpandableListView listView = (ExpandableListView) findViewById(R.id.annotations_view);



    /**   2016.07.14 */
    annotationsList = new AnnotationsList(this, listView);

    // TODO use reflection to take all classes on annotations package except Annotation
    annotationsList.addGroup(new Marker(""));
    annotationsList.addGroup(new Location(""));
    annotationsList.addGroup(new Table(""));
    annotationsList.addGroup(new Column(""));
    annotationsList.addGroup(new Wall(""));

    annotationsPub = new AnnotationsPublisher(this, annotationsList, params, remaps);
    listView.setAdapter(annotationsList);


    /**  Error, 死循环冒error*/
//    mapView.addLayer(viewControlLayer);

    mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));

    mainLayout = (ViewGroup) findViewById(R.id.main_layout);
    sideLayout = (ViewGroup) findViewById(R.id.side_layout);


  }


  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    super.init(nodeMainExecutor);
    this.nodeMainExecutor = nodeMainExecutor;

    nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
            .newNonLoopback().getHostAddress(), getMasterUri());

    String joyTopic = remaps.get(getString(R.string.joystick_topic));
    String camTopic = remaps.get(getString(R.string.camera_topic));

    NameResolver appNameSpace = getMasterNameSpace();
    joyTopic = appNameSpace.resolve(joyTopic).toString();
    camTopic = appNameSpace.resolve(camTopic).toString();
    cameraView.setTopicName(camTopic);
    virtualJoystickView.setTopicName(joyTopic);

    nodeMainExecutor.execute(cameraView,
            nodeConfiguration.setNodeName("android/camera_view"));
    nodeMainExecutor.execute(virtualJoystickView,
            nodeConfiguration.setNodeName("android/virtual_joystick"));

    viewControlLayer = new ViewControlLayer(this,
            nodeMainExecutor.getScheduledExecutorService(), cameraView,
            mapView, mainLayout, sideLayout, params);

    String mapTopic   = remaps.get(getString(R.string.map_topic));
    String scanTopic  = remaps.get(getString(R.string.scan_topic));
    String robotFrame = (String) params.get("robot_frame", getString(R.string.robot_frame));
/** 2016.7.19*/
    String nodeTopic = remaps.get(getString(R.string.node_topic));
//    String nodeTopic = (String) params.get("node_frame", getString(R.string.node_topic));

//        Log.e("---mylog","nodeTopic === " + nodeTopic);
    occupancyGridLayer = new OccupancyGridLayer(appNameSpace.resolve(mapTopic).toString());
    laserScanLayer = new LaserScanLayer(appNameSpace.resolve(scanTopic).toString());
/** 2016.7.19*/
    poseSubscriberLayer = new PoseSubscriberLayer(appNameSpace.resolve(nodeTopic).toString());
//    poseSubscriberLayer = new PoseSubscriberLayer(nodeTopic);
//        Log.e("---mylog","poseSubscriberLayer === " + poseSubscriberLayer);

//    robotLayer = new RobotLayer(robotFrame);

    mapView.addLayer(viewControlLayer);
//         mapView.addLayer(new OccupancyGridLayer(mapTopic));
    mapView.addLayer(occupancyGridLayer);
    mapView.addLayer(laserScanLayer);
/** 2016.7.19*/
//       mapView.addLayer(annotationLayer);
//       mapView.addLayer(robotLayer);
    mapView.addLayer(poseSubscriberLayer);

//       mapView.addLayer(viewControlLayer);

    annotationLayer = new MapAnnotationLayer(this, annotationsList, params);
    mapView.addLayer(annotationLayer);
//        mapView.addLayer(poseSubscriberLayer);

//         Log.e("MainActivity----", "nodeMainExecutor === " + nodeMainExecutor);
    mapView.init(nodeMainExecutor);

    viewControlLayer.addListener(new CameraControlListener() {
      @Override
      public void onZoom(float focusX, float focusY, float factor) {}
      @Override
      public void onDoubleTap(float x, float y) {}
      @Override
      public void onTranslate(float distanceX, float distanceY) {}
      @Override
      public void onRotate(float focusX, float focusY, double deltaAngle) {}
    });

    mapView.addLayer(viewControlLayer);

    // dwlee
    //what is a main purpose of this function?
    NtpTimeProvider ntpTimeProvider = new NtpTimeProvider(
            InetAddressFactory.newFromHostString("192.168.0.1"),
            nodeMainExecutor.getScheduledExecutorService());
    ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
    nodeConfiguration.setTimeProvider(ntpTimeProvider);

    nodeMainExecutor.execute(mapView, nodeConfiguration.setNodeName("android/map_view"));

    annotationsPub.setMasterNameSpace(getMasterNameSpace());

    /** 在线程内调用了主线程数据，数据可能还没有初始化，因此会报空指针异常 */
//    nodeMainExecutor.execute(annotationsPub, nodeConfiguration.setNodeName("android/annotations_pub"));

  }






  //  标注node点击事件处理
  public void node(View v) {

    nodeMap();


  }

  private void nodeMap() {




    /** 2016.07.22 */
    geometry_msgs.PoseStamped poseStamped = poseSubscriberLayer.getPoseStamped();
    //Pose pose = poseStamped.getPose();
    annotationLayer.addAnno(poseStamped);
    mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));
//    return;
//    Toast.makeText(MainActivity.this, pose.toString(), Toast.LENGTH_SHORT).show();
//
//    Log.e("myLog -- ", "pose ===" + pose); //pose ===MessageImpl<geometry_msgs/Pose>
//
//
//    double x = pose.getPosition().getX();
//    double y = pose.getPosition().getY();
////    double z = pose.getPosition().getZ();
//
////    double xx = poseStamped.getPose().getPosition().getX();
////    double yy = poseStamped.getPose().getPosition().getY();
//
//    Log.e("myLog -- ", "x ===" + x); //x ===0.0
//    Log.e("myLog -- ", "y ===" + y); // y ===0.0
//
//
//    camera = mapView.getCamera();
//    camera.setFrame((String) params.get("node_topic", MainActivity.this.getString(R.string.node_topic)));
//
////   Transform p = Transform.translation(camera.toCameraFrame((int) x, (int) y));
//
//    Transform p;
//    p = Transform.translation(camera.toCameraFrame((int)x, (int) y));
////    annotation = new Marker("marker 1");
//    annotation = new Location("Location 1");
////    annotation = new Location("");
//
//    if (annotation != null) {
//Toast.makeText(MainActivity.this, "annotation != null",Toast.LENGTH_SHORT).show();
//      Log.e("myLog --- ","annotation =-- = = = " + annotation);
//      Log.e("myLog --- ","p =-- = = = " + p);
//
//      Preconditions.checkNotNull(p);
//
//      Vector3 poseVector;
//      Vector3 pointerVector;
//
//      poseVector = p.apply(Vector3.zero());
//      pointerVector = camera.toCameraFrame((int) x, (int) y);
//
//
//
//      /**  */
//      double dist  = annotationLayer.getDist(pointerVector.getX(), pointerVector.getY(),
//              poseVector.getX(), poseVector.getY());
//
//      double angle = annotationLayer.getAngle(pointerVector.getX(), pointerVector.getY(),
//              poseVector.getX(), poseVector.getY());
//
////      p = Transform.translation(poseVector).multiply(Transform.zRotation(angle));
//
//      p = Transform.translation(poseVector).multiply(Transform.zRotation(angle));
//
//
//      Log.e("myLog --- ","annotation = =11 = = " + annotation);
//      Log.e("myLog --- ","p = =11 = = " + p);
//
//      annotation.setTransform(p);
//      annotation.setSizeXY((float) dist);
//
//
//    }
//
//
//    annotationLayer.addAnno(annotation);
//
//





    /** = =  = =  = =  = =  = =  = =  = =  = =



     MapAnnotationLayer mapAnnotationLayer = new MapAnnotationLayer(MainActivity.this, annotationsList, params);

     mapAnnotationLayer.getConfirmAnnotation();


     MessageDefinitionReflectionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
     DefaultMessageFactory messageFactory = new DefaultMessageFactory(messageDefinitionProvider);

     AlvarMarkers  markersMsg = messageFactory.newFromType(AlvarMarkers._TYPE); //记号列表

     Transform makeVertical   = new Transform(new Vector3(0.0, 0.0, 0.0), new Quaternion(0.5, 0.5, 0.5, 0.5));

     //    ann = annotationsList.listFullContent().get(1);

     for (Annotation ann : annotationsList.listFullContent()) {

     AlvarMarker annMsg = messageFactory.newFromType(AlvarMarker._TYPE);
     annMsg.setId(((Marker) ann).getId());
     annMsg.getPose().getHeader().setFrameId(mapFrame);
     Transform tf = ann.getTransform().multiply(makeVertical);
     //    tf.toPoseMessage(annMsg.getPose().getPose());
     tf.toPoseMessage(pose);
     markersMsg.getMarkers().add(annMsg);

     }

     */


/** = = = = = = = = = = = =  */





  }






  //  返回操作，返回到上一页org.ros.android.MasterChooser
  public void myBack() {
    Intent i = new Intent();
    i.setClass(MainActivity.this, MasterChooser.class);
    startActivity(i);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    final Dialog dialog;
    Button button;

    switch (id) {
      case NAME_MAP_DIALOG_ID: /** 確定保存地圖的ID */
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.name_map_dialog);
        dialog.setTitle("保存地图");

        final EditText nameField = (EditText) dialog
                .findViewById(R.id.name_editor);

        nameField.setOnKeyListener(new View.OnKeyListener() {
          @Override
          public boolean onKey(final View view, int keyCode,
                               KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_ENTER) {

              safeShowWaitingDialog("正在保存地图...");
              try {
                final MapManager mapManager = new MapManager(MainActivity.this, remaps);

                String name                 = nameField.getText().toString();
//                if (nameField != null)

//                if (name != null) {
//                  mapManager.setMapName(name);
//                }
                if ( nameField.length() != 0) {
                  mapManager.setMapName(name);
                }

                mapManager.setNameResolver(getMasterNameSpace());
                mapManager.registerCallback(new MapManager.StatusCallback() {
                  @Override
                  public void timeoutCallback() {
                    safeDismissWaitingDialog();
                    safeShowNotiDialog("Error", "超时");
                  }
                  @Override
                  public void onSuccessCallback(SaveMapResponse arg0) {
                    safeDismissWaitingDialog();
                    safeShowNotiDialog("Success", "地图保存成功!!!!");
                  }
                  @Override
                  public void onFailureCallback(Exception e) {
                    safeDismissWaitingDialog();
//                    safeShowNotiDialog("错误","onFailureCallback(Exception e)- -" + e.getMessage());
                    safeShowNotiDialog("错误","没有找到服务，回调失败" + e.getMessage());
                  }
                });

                nodeMainExecutor.execute(mapManager,
                        nodeConfiguration.setNodeName("android/save_map"));

              } catch (Exception e) {
                e.printStackTrace();
                safeShowNotiDialog("Error", "保存出现错误!" + e.getMessage());
//                safeShowNotiDialog("Error", "保存出现错误: " + e.toString());
              }

              removeDialog(NAME_MAP_DIALOG_ID);
              return true;
            } else {
              return false;
            }
          }
        });


/** 确定保存地圖按钮点击事件 */
        button = (Button) dialog.findViewById(R.id.certain_button);
        button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
//            保存地图操作...
            MapManager mapManager = new MapManager(MainActivity.this, remaps);
            String name           = nameField.getText().toString();

//            mapManager.saveMap();

//            editText.length()是否为零来判断;
            if (nameField.length() != 0) {
              mapManager.setMapName(name);

              saveMyMap();


              mapManager.saveMap();
              System.out.println("------------------------------name != null-------------");
              Toast.makeText(MainActivity.this, "地图保存成功!", Toast.LENGTH_SHORT).show();
              removeDialog(NAME_MAP_DIALOG_ID);//移除對話框和其內容
//              dialog.dismiss();//取消对话框，但內容下次彈出對話框時還在
            }
            else {
              Toast.makeText(MainActivity.this, "请输入地图名称", Toast.LENGTH_LONG).show();
              System.out.println("------------------------------name == null-------------");

            }


          }
        });


//        取消按钮点击事件
        button = (Button) dialog.findViewById(R.id.cancel_button);
        button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            removeDialog(NAME_MAP_DIALOG_ID);
          }
        });
        break;
      default:
        dialog = null;
    }
    return dialog;
  }


  /** 2016.07.16 添加 */
  public void saveMyMap() {
    try {
      final MapManager mapManager = new MapManager(MainActivity.this, remaps);


      mapManager.setNameResolver(getMasterNameSpace());
      mapManager.registerCallback(new MapManager.StatusCallback() {
        @Override
        public void timeoutCallback() {
          safeDismissWaitingDialog();
          safeShowNotiDialog("Error", "超时");
        }
        @Override
        public void onSuccessCallback(SaveMapResponse arg0) {
          safeDismissWaitingDialog();
          safeShowNotiDialog("Success", "地图保存成功!!!!");
        }
        @Override
        public void onFailureCallback(Exception e) {
          safeDismissWaitingDialog();
          safeShowNotiDialog("Error","onFailureCallback(Exception e)- -" + e.getMessage());
//          safeShowNotiDialog("错误","没有找到服务，回调失败");
        }
      });

      nodeMainExecutor.execute(mapManager,
              nodeConfiguration.setNodeName("android/save_map"));

    } catch (Exception e) {
      e.printStackTrace();
//      safeShowNotiDialog("Error", "保存出现错误!");
      safeShowNotiDialog("Error", "保存出现错误: " + e.toString());
    }

    removeDialog(NAME_MAP_DIALOG_ID);
  }



  private void safeDismissWaitingDialog() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (waitingDialog != null) {
          waitingDialog.dismiss();
          waitingDialog = null;
        }
      }
    });
  }

  private void safeShowWaitingDialog(final CharSequence message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (waitingDialog != null) {
          waitingDialog.dismiss();
          waitingDialog = null;
        }
        waitingDialog = ProgressDialog.show(MainActivity.this, "",
                message, true);
      }
    });
  }

  private void safeShowNotiDialog(final String title, final CharSequence message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (notiDialog != null) {
          notiDialog.dismiss();
          notiDialog = null;
        }
        if (waitingDialog != null) {
          waitingDialog.dismiss();
          waitingDialog = null;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(
                MainActivity.this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setNeutralButton("Ok",
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dlog, int i) {
                    dlog.dismiss();
                  }
                });
        notiDialog = dialog.show();
      }
    });
  }

}
