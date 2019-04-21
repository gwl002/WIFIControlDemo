package com.example.gongwenlan.wifi_remotecontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.aware.PublishConfig;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    private RegistryListener registryListener = new MyRegistryListener();
    private AndroidUpnpService upnpService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            upnpService = (AndroidUpnpService) service;
            upnpService.getRegistry().addListener(registryListener);
//            for (Device device : upnpService.getRegistry().getDevices()) {
//                registryListener.deviceAdded(device);
//            }
            Log.d("sssss","upnp connected");
            upnpService.getControlPoint().search();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            upnpService = null;
        }
    };

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

//        public void deviceAdded(final Device device) {
//            Log.d("add device",device.getDisplayString());
//        }

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            Log.d(registryTAG,"Remote device available: " + device.getDisplayString());
        }

        @Override
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
            Log.d(registryTAG,"Remote device updated:" + device.getDisplayString());
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
           Log.d(registryTAG,"Remote device removed:" + device.getDisplayString());
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            Log.d(registryTAG,"Local device removed:" + device.getDisplayString());
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            Log.d(registryTAG,"Local device removed:" + device.getDisplayString());
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
        Log.d(TAG,"Activity created");
    }

    public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this,DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE,message);
        startActivity(intent);
        Log.d("sendMessage:",message);
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



}
