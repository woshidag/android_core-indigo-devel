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
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;
import com.google.common.collect.Lists;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MasterChooser;
import org.ros.android.android_tutorial_map_viewer.annotations_list.AnnotationsList;
import org.ros.android.android_tutorial_map_viewer.annotations_list.AnnotationsPublisher;
import org.ros.android.android_tutorial_map_viewer.annotations_list.MockDataProvider;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Annotation;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Column;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Location;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Marker;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Table;
import org.ros.android.android_tutorial_map_viewer.annotations_list.annotations.Wall;
import org.ros.android.view.RosImageView;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.time.NtpTimeProvider;

import java.util.concurrent.TimeUnit;

import world_canvas_msgs.SaveMapResponse;

/**
 * 新添  2016.07.14
 */

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

/**  2016.7.15 添加 */
  private Annotation annotation;
  private MockDataProvider mockDataProvider;



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
//去掉私有private修饰 改为public
  public OccupancyGridLayer occupancyGridLayer = null;
  public LaserScanLayer laserScanLayer = null;
  public RobotLayer robotLayer = null;



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


    /** 新添  2016.07.14 */
    // Configure the ExpandableListView and its adapter containing current annotations
    ExpandableListView listView = (ExpandableListView) findViewById(R.id.annotations_view);

    //        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
//        {
//            @Override
//            public boolean onChildClick(ExpandableListView arg0, View arg1, int arg2, int arg3, long arg4)
//            {
//                Toast.makeText(getBaseContext(), "Child clicked", Toast.LENGTH_LONG).show();
//                return false;
//            }
//        });
//
//        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener()
//        {
//            @Override
//            public boolean onGroupClick(ExpandableListView arg0, View arg1, int arg2, long arg3)
//            {
//                Toast.makeText(getBaseContext(), "Group clicked", Toast.LENGTH_LONG).show();
//                return false;
//            }
//        });

    /** 新添  2016.07.14 */
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


//  Annotation(String name, float[] vertices, Color color)

  //  标注node点击事件处理
  public void node(View v) {


//    annotationLayer.setMode(MapAnnotationLayer.Mode.ADD_MARKER);

annotation = mockDataProvider.getRandomAnnotation("");

//    annotationsList.addItem(annotation);
annotationsList.addItem(new Location(""));
    nodeMap();

    Toast.makeText(MainActivity.this, "地图已标注",Toast.LENGTH_SHORT).show();
  }

  private  void nodeMap() {

//    ExpandableListView listView = (ExpandableListView) findViewById(R.id.annotations_view);
//
//    annotationsList = new AnnotationsList(this, listView);

    // TODO use reflection to take all classes on annotations package except Annotation
//    annotationsList.addGroup(new Marker(""));
//    annotationsList.addGroup(new Location(""));
//    annotationsList.addGroup(new Table(""));
//    annotationsList.addGroup(new Column(""));
//    annotationsList.addGroup(new Wall(""));

//    annotationsPub = new AnnotationsPublisher(this, annotationsList, params, remaps);
//    listView.setAdapter(annotationsList);

    Toast.makeText(MainActivity.this, "nodeMap run", Toast.LENGTH_SHORT).show();

//    Marker marker = new Marker("myMarker");
//    annotationLayer = new MapAnnotationLayer(this, annotationsList, params);
//    annotationLayer.setMode(MapAnnotationLayer.Mode.ADD_MARKER);
//    annotationLayer.onTouchEvent(this, );

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
      case NAME_MAP_DIALOG_ID:
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
                    safeShowNotiDialog("错误","没有找到服务，回调出现失败");
                  }
                });

                nodeMainExecutor.execute(mapManager,
                        nodeConfiguration.setNodeName("android/save_map"));

              } catch (Exception e) {
                e.printStackTrace();
                safeShowNotiDialog("Error", "保存出现错误!");
//                safeShowNotiDialog("Error", "保存出现错误: " + e.toString());
              }

              removeDialog(NAME_MAP_DIALOG_ID);
              return true;
            } else {
              return false;
            }
          }
        });


//确定按钮点击事件
        button = (Button) dialog.findViewById(R.id.certain_button);
        button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
//            保存地图操作...
            MapManager mapManager = new MapManager(MainActivity.this, remaps);
            String name           = nameField.getText().toString();

            mapManager.saveMap();

//            editText.length()是否为零来判断;
            if (nameField.length() != 0) {
              mapManager.setMapName(name);
//              mapManager.onStart();
              mapManager.saveMap();
              System.out.println("------------------------------name != null-------------");
              Toast.makeText(MainActivity.this, "地图保存成功!", Toast.LENGTH_SHORT).show();
              removeDialog(NAME_MAP_DIALOG_ID);
//              dialog.dismiss();//取消对话框
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

    occupancyGridLayer = new OccupancyGridLayer(appNameSpace.resolve(mapTopic).toString());
    laserScanLayer = new LaserScanLayer(appNameSpace.resolve(scanTopic).toString());
    robotLayer = new RobotLayer(robotFrame);

    mapView.addLayer(viewControlLayer);
//    mapView.addLayer(new OccupancyGridLayer(mapTopic));
    mapView.addLayer(occupancyGridLayer);
    mapView.addLayer(laserScanLayer);
    mapView.addLayer(robotLayer);

    annotationLayer = new MapAnnotationLayer(this, annotationsList, params);
    mapView.addLayer(annotationLayer);

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





//    mapView.addLayer(viewControlLayer);
//    mapView.addLayer(annotationLayer);

//    String mapTopic = remaps.get(getString(R.string.map_topic));
//    mapView.addLayer(new OccupancyGridLayer(mapTopic));
//    annotationLayer = new MapAnnotationLayer(this, annotationsList, params);

//    NtpTimeProvider ntpTimeProvider = new NtpTimeProvider(
//            InetAddressFactory.newFromHostString("192.168.10.1"), // TODO what is this?
//            nodeMainExecutor.getScheduledExecutorService());
//    ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
//    nodeConfiguration.setTimeProvider(ntpTimeProvider);
//    nodeMainExecutor.execute(mapView, nodeConfiguration.setNodeName("android/map_view"));
//
//    annotationsPub.setMasterNameSpace(getMasterNameSpace());
//
//    nodeMainExecutor.execute(annotationsPub, nodeConfiguration.setNodeName("android/annotations_pub"));
  }





/**  =================================================================================================== */





}
