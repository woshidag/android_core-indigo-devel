package org.ros.android.android_tutorial_map_viewer;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.github.rosjava.android_remocons.common_tools.apps.AppRemappings;

import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import world_canvas_msgs.SaveMap;
import world_canvas_msgs.SaveMapRequest;
import world_canvas_msgs.SaveMapResponse;

/** 地图管理器类 */
public class MapManager extends AbstractNodeMain {

    private ConnectedNode connectedNode;
    private ServiceResponseListener<SaveMapResponse> saveServiceResponseListener;

    private String mapName;
    private String saveSrvName;
    private NameResolver nameResolver;
    private boolean nameResolverSet = false;
    private boolean waitingFlag = false;

    private StatusCallback statusCallback;
//
/**  状态返回接口 */
    public interface StatusCallback {
         void timeoutCallback();
         void onSuccessCallback(SaveMapResponse arg0);
         void onFailureCallback(Exception e);
    }
    public void registerCallback(StatusCallback statusCallback) {
        this.statusCallback = statusCallback;
    }

/** 地图管理器 */
    public MapManager(final Context context, final AppRemappings remaps) {
        // Apply remappings
        saveSrvName = remaps.get(context.getString(R.string.save_map_srv));
        mapName = "";
    }


/** 设置地图名 */
    public void setMapName(String name) {
        mapName = name;
    }

    public void setNameResolver(NameResolver newNameResolver) {
        nameResolver = newNameResolver;
        nameResolverSet = true;
    }


/** 清除等待状态标识 */
    private void clearWaitFor(){
        waitingFlag = false;
    }


    private boolean waitFor(final int timeout) {
        waitingFlag = true;
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                int count = 0;
                int timeout_count = timeout * 1000 / 200;
                while(waitingFlag){
                    try { Thread.sleep(200); }
                    catch (InterruptedException e) { return false; }
                    if(count < timeout_count){
                        count += 1;
                    }
                    else{
                        return false;
                    }
                }
                return true;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
            return asyncTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException e) {
            return false;
        } catch (TimeoutException e) {
            return false;
        }
    }


/**  保存地图    */
    public void saveMap(){
        Log.e("myLog---", "saveMap调用。。。");
        System.out.println("-----------myLog--------------");

        ServiceClient<SaveMapRequest, SaveMapResponse> saveMapClient = null;
        if (connectedNode != null) {
            Log.e("myLog---", "connectedNode != null。。。");
            System.out.println("-----------myLog---connectedNode != null--------------");
            try{
                if (nameResolverSet){
                    System.out.println("-----------myLog---nameResolverSet--------------");
                    saveSrvName = nameResolver.resolve(saveSrvName).toString();
                }
                saveMapClient = connectedNode.newServiceClient(saveSrvName,	SaveMap._TYPE);
            } catch (ServiceNotFoundException e) {
                try {
                    Thread.sleep(1000L);
                } catch (Exception ex) {
                }
                statusCallback.onFailureCallback(e);
            }
            if (saveMapClient != null){
                final SaveMapRequest request = saveMapClient.newMessage();
                request.setMapName(mapName);
                saveMapClient.call(request, new ServiceResponseListener<SaveMapResponse>(){
                    @Override
                    public void onSuccess(SaveMapResponse saveMapResponse) {
                        Log.e("myLog---", "成功调用。。。");
                        System.out.println("-----------myLog---成功调用--------------");
                        if (waitingFlag){
                            clearWaitFor();
                            statusCallback.onSuccessCallback(saveMapResponse);
                        }
                    }
                    @Override
                    public void onFailure(RemoteException e) {
                        Log.e("myLog---", "失败调用。。。");
                        System.out.println("-----------myLog---失败调用--------------");
                        if (waitingFlag) {
                            clearWaitFor();
                            statusCallback.onFailureCallback(e);
                        }
                    }
                });
                if(!waitFor(10)){
                    Log.e("myLog---", "调用超时。。。");
                    System.out.println("-----------myLog----调用超时--------------");
                    statusCallback.timeoutCallback();
                }
            }
        }
        System.out.println("-----------myLog----connectedNode == null--------------");
    }


/**  设置默认标注的名称 */
    @Override
    public GraphName getDefaultNodeName() {
        return null;
    }

    @Override
    public void onStart(final ConnectedNode connectedNode){
        super.onStart(connectedNode);
        this.connectedNode = connectedNode;
        saveMap();
    }
}
