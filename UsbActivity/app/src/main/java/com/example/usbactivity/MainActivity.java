package com.example.usbactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Runnable{
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String TAG = "Accessory";

    private UsbManager manager;
    private UsbAccessory accessory;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private ParcelFileDescriptor fileDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(receiver, filter);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ( getIntent() != null && getIntent().getAction().equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED) ) {
            openAccessory((UsbAccessory) getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY));
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        fileDescriptor = manager.openAccessory(accessory);
        if ( fileDescriptor != null ) {
            this.accessory = accessory;
            FileDescriptor fd = fileDescriptor.getFileDescriptor();
            inputStream = new FileInputStream(fd);
            outputStream = new FileOutputStream(fd);
            Thread thread = new Thread(null, this, "AccessoryController");
            thread.start();
            Log.d(TAG, "accessory opened");
        }
        else {
            Log.d(TAG, "accessory open failed");
        }
    }

    public void onDestroy() {
        try {
            if ( inputStream != null ) {
                inputStream.close();
            }
            if ( outputStream != null ) {
                outputStream.close();
            }
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        finally {
            inputStream = null;
            outputStream = null;
        }
        closeAccessory();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void closeAccessory() {
        try {
            if ( fileDescriptor != null ) {
                fileDescriptor.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            fileDescriptor = null;
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action) ) {
                UsbAccessory acc = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if ( acc != null && acc.equals(accessory) ) {
                    closeAccessory();
                }
            }
        }
    };

    @Override
    public void run() {
        byte[] buffer = new byte[16384];
        int bytesRead;

        try {
            while((bytesRead = inputStream.read(buffer)) > -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
