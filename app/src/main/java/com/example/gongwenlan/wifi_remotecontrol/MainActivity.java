package com.example.gongwenlan.wifi_remotecontrol;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    RegistryListener listener;

    UpnpService upnpService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listener = new RegistryListener() {
            @Override
            public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
                System.out.println("Discovery started: " + device.getDisplayString());
            }

            @Override
            public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
                System.out.println("Discovery failed: " + device.getDisplayString() + " =>" + ex);
            }

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                System.out.println("Remote device available: " + device.getDisplayString());
            }

            @Override
            public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                System.out.println("Remote device updated:" + device.getDisplayString());
            }

            @Override
            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                System.out.println("Remote device removed:" + device.getDisplayString());
            }

            @Override
            public void localDeviceAdded(Registry registry, LocalDevice device) {
                System.out.println("Local device removed:" + device.getDisplayString());
            }

            @Override
            public void localDeviceRemoved(Registry registry, LocalDevice device) {
                System.out.println("Local device removed:" + device.getDisplayString());
            }

            @Override
            public void beforeShutdown(Registry registry) {
                System.out.println(
                        "Before shutdown, the registry has devices: "
                                + registry.getDevices().size()
                );
            }

            @Override
            public void afterShutdown() {
                System.out.println("service shutdowned");
            }
        };
        upnpService = new UpnpServiceImpl(listener);
        upnpService.getControlPoint().search(new STAllHeader());
        System.out.println("Waiting for 10 second before shutting down");
        try{
            Thread.sleep(10000);
        }catch(Exception e){
            System.out.println("error: "+e.getLocalizedMessage());
        }
        upnpService.shutdown();
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



}
