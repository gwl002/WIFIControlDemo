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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    private RegistryListener registryListener = new MyRegistryListener();
    private AndroidUpnpService upnpService;
    private ArrayAdapter<DeviceDisplay> listAdapter;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            listAdapter.clear();

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
            Log.d("sssss","upnp disconnected");
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView device_list = (ListView) findViewById(R.id.list_device);
        listAdapter = new ArrayAdapter<>(this,android.R.layout.simple_expandable_list_item_1);
        device_list.setAdapter(listAdapter);

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
    };

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
                for (Service service : getDevice().getServices()) {
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
