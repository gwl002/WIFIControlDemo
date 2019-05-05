package com.example.gongwenlan.wifi_remotecontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import java.nio.channels.Channel;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    private RegistryListener registryListener = new MyRegistryListener();
    private AndroidUpnpService upnpService;
    private ControlPoint cp;
    private ArrayAdapter<DeviceDisplay> listAdapter;
    private Context mContext;


    private Handler handler;
    private Timer timer;
    private int progress = 0;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            upnpService = (AndroidUpnpService) service;
            cp = upnpService.getControlPoint();

            listAdapter.clear();

            upnpService.getRegistry().addListener(registryListener);
//            for (Device device : upnpService.getRegistry().getDevices()) {
//                registryListener.deviceAdded(device);
//            }
            Log.d("sssss","upnp connected");
            cp.search();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            upnpService = null;
            Log.d("sssss","upnp disconnected");
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreenActivity.show(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView device_list = (ListView) findViewById(R.id.list_device);
        listAdapter = new ArrayAdapter<>(this,android.R.layout.simple_expandable_list_item_1);
        device_list.setAdapter(listAdapter);
        mContext = this;

        device_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DeviceDisplay deviceDisplay = (DeviceDisplay) parent.getItemAtPosition(position);
                Device device = deviceDisplay.getDevice();
                Log.d("xxxxx",device.toString());
                final Service avtService = device.findService(ServiceType.valueOf("urn:schemas-upnp-org:service:AVTransport:1"));
                final Service rctrlService = device.findService(ServiceType.valueOf("urn:schemas-upnp-org:service:RenderingControl:1"));
                Log.d("DeviceDetails",deviceDisplay.getDetailsMessage());

                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle(R.string.deviceDetails);
                alertDialog.setMessage(deviceDisplay.getDetailsMessage());
                alertDialog.setButton(
                        DialogInterface.BUTTON_POSITIVE,
                        getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d("Alert Dialog", "clicked");
//                                setVolume(50,rctrlService);
                                playMusic("http://192.168.2.175:9080/002.mp3",avtService);
                            }
                        }
                );
                alertDialog.show();
            }
        });

        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(upnpService != null){
                    upnpService.getControlPoint().search();
                }
            }
        });

        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
        Log.d(TAG,"Activity created");

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                int what = msg.what;
                if(what<=100){
                    SplashScreenActivity.setProgressBar(what);
                }else{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                SplashScreenActivity.hideProgressBar();
                                Thread.sleep(1000);
                                SplashScreenActivity.hide(MainActivity.this);
                            }catch(Exception e) {

                            }
                        }
                    }).start();

                }
            }
        };

        timer = new Timer();
        timer.schedule(new MyTimerTask(),0,500);
    }

    class MyTimerTask extends TimerTask{
        @Override
        public void run(){
            Log.d("schedule","+++"+progress);
            if(progress>100){
                timer.cancel();
                return;
            }
            progress+=10;
            Message message = new Message();
            message.what = progress;
            handler.sendMessage(message);
        }
    }

    public void playMusic(final String uri,final Service avtService){
        if(cp != null){
            cp.execute(new SubscriptionCallback(avtService,100) {
                @Override
                protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception, String defaultMsg) {
                    Log.d("sub fail:",defaultMsg);
                }

                @Override
                protected void established(GENASubscription subscription) {
                    Log.d("sub establish:",subscription.toString());
                }

                @Override
                protected void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus) {
                    Log.d("sub establish:",reason.toString());
                }

                @Override
                protected void eventReceived(GENASubscription subscription) {
                    Log.d("sub recev:",subscription.getCurrentSequence().getValue().toString());
                }

                @Override
                protected void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {

                }
            });

            cp.execute(new Stop(avtService){
                @Override
                public void success(ActionInvocation invocation){
                    cp.execute(new SetAVTransportURI(avtService,uri,"no meta data") {
                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            Log.d("play failt:",defaultMsg);
                            Toast.makeText(mContext,defaultMsg,Toast.LENGTH_LONG);
                        }
                        @Override
                        public void success(ActionInvocation invocation){
                            Log.d("set success:","success");

                            cp.execute(new Play(avtService) {
                                @Override
                                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                                    Toast.makeText(mContext,defaultMsg,Toast.LENGTH_LONG);
                                }
                                @Override
                                public void success(ActionInvocation invocation){
                                    cp.execute(new GetPositionInfo(avtService){
                                        @Override
                                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                                            Log.d("GetPositionInfo","yyyyyyyyyyyyyyy");
                                        }

                                        @Override
                                        public void success(ActionInvocation invocation){
                                            Log.d("GetPositionInfo","xxxxxxx");
                                            super.success(invocation);
                                        }

                                        @Override
                                        public void received(ActionInvocation invocation, PositionInfo positionInfo) {
                                            Log.d("GetPositionInfo",positionInfo.toString());
                                        }
                                    });
                                }
                            });
                        }
                    });
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg){

                }
            });

        }
    }

    public void setVolume(final int volumeValue,final Service rctrlService){
            cp.execute(new GetVolume(rctrlService) {
                @Override
                public void received(ActionInvocation actionInvocation, final int currentVolume) {
                    Log.d("recv getVolume:",currentVolume + "");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext,"current volume:"+currentVolume,Toast.LENGTH_LONG).show();
                        }
                    });


                    cp.execute(new SetVolume(rctrlService,volumeValue) {
                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext,"调节音量失败",Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });


                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    Log.d("fail getVolume:",defaultMsg);
                }
            });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        Log.d(TAG,"Activity finished");
        // This will stop the UPnP service if nobody else is bound to it
        getApplicationContext().unbindService(serviceConnection);
    }

    protected class MyRegistryListener extends DefaultRegistryListener {
        private static final String registryTAG = "MyRegistryListener";
        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            Log.d(registryTAG,"Discovery started: " + device.getDisplayString());
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
            Log.d(registryTAG,"Discovery failed: " + device.getDisplayString() + " =>" + ex);
        }

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            Log.d(registryTAG,"Remote device available: " + device.getDisplayString());
        }

        @Override
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
            Log.d(registryTAG,"Remote device updated:" + device.getDisplayString());
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            Log.d(registryTAG,"Remote device removed:" + device.getDisplayString());
            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            Log.d(registryTAG,"Local device removed:" + device.getDisplayString());
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            Log.d(registryTAG,"Local device removed:" + device.getDisplayString());
            deviceRemoved(device);
        }

        @Override
        public void beforeShutdown(Registry registry) {
            Log.d(registryTAG,
                    "Before shutdown, the registry has devices: "
                            + registry.getDevices().size()
            );
        }

        @Override
        public void afterShutdown() {
            Log.d(registryTAG,"Service was shutdowned");
        }

        public void deviceAdded(final Device device) {
            Log.d("add device",device.getDisplayString());
            Log.d("device type",device.getType().getType());
            if(!device.getType().getType().equals("MediaRenderer")){
                return;
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);
                    int position = listAdapter.getPosition(d);
                    if (position >= 0) {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(d);
                        listAdapter.insert(d, position);
                    } else {
                        listAdapter.add(d);
                    }
                }
            });
        }

        public void deviceRemoved(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    listAdapter.remove(new DeviceDisplay(device));
                }
            });
        }
    }

    protected class DeviceDisplay {

        Device device;

        public DeviceDisplay(Device device) {
            this.device = device;
        }

        public Device getDevice() {
            return device;
        }

        // DOC:DETAILS
        public String getDetailsMessage() {
            StringBuilder sb = new StringBuilder();
            if (getDevice().isFullyHydrated()) {
                sb.append(getDevice().getDisplayString());
                sb.append("\n\n");
                sb.append(getDevice().getType().getType());
                sb.append("\n\n");
                for (Service service : getDevice().getServices()) {
                    Log.d("serviceType:",service.getServiceType().getType());
                    sb.append(service.getServiceType()).append("\n");
                }
            } else {
                sb.append(getString(R.string.deviceDetailsNotYetAvailable));
            }
            return sb.toString();
        }
        // DOC:DETAILS

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeviceDisplay that = (DeviceDisplay) o;
            return device.equals(that.device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }

        @Override
        public String toString() {
            String name =
                    getDevice().getDetails() != null && getDevice().getDetails().getFriendlyName() != null
                            ? getDevice().getDetails().getFriendlyName()
                            : getDevice().getDisplayString();
            // Display a little star while the device is being loaded (see performance optimization earlier)
            return device.isFullyHydrated() ? name : name + " *";
        }
    }

}
