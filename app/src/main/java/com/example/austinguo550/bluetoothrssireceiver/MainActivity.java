package com.example.austinguo550.bluetoothrssireceiver;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    TextView mTextView;
    Button mButton;
    private Context mContext;
    HashMap<String, Integer> mDeviceMap = new HashMap<String, Integer>();
    private final static String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView);
        mButton = (Button) findViewById(R.id.button);

        mContext = getApplicationContext();

        final int REQUEST_ENABLE_BT = 1;    //must be greater than 0

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }

        // Turning on Bluetooth if it wasn't on
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Making the device discoverable
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);


        // Starting discovery with button press
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovering();
            }
        });

    }

    public void startDiscovering() {
        // Starting discovery
        BluetoothDevice remoteDevice = null;
        Toast.makeText(MainActivity.this, "Discovery in progress", Toast.LENGTH_SHORT).show();
        if (remoteDevice == null) {
            Toast.makeText(MainActivity.this, "Starting discovery for remote devices...", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.startDiscovery())
            Toast.makeText(mContext, "Could not find any devices to connect to", Toast.LENGTH_SHORT).show();
        else {
            //Log.d(TAG, "Running else statement ");
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);

            String deviceList = "";
            for (String key: mDeviceMap.keySet()) {
                Log.d(TAG, "got in");
                deviceList += key + "\n";
            }
            Log.d(TAG, deviceList);
            mTextView.setText("The devices are " + deviceList);  //TODO: Finish setting the text = the devices
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }*/

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(MainActivity.class.getSimpleName(), "---- onReceive method started ----");
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
                Toast.makeText(mContext, "Discovery thread started... Scanning for devices", Toast.LENGTH_SHORT).show();
                Log.d(TAG, " ---- Reached ACTION_DISCOVERY_STARTED ----");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
                Toast.makeText(mContext, "Done scanning", Toast.LENGTH_SHORT).show();
                Log.d(TAG, " ---- Reached ACTION_DISCOVERY_FINISHED ----");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                Log.d(TAG, " ---- Reached ACTION_FOUND ----");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                Toast.makeText(mContext, "Found device " + deviceName, Toast.LENGTH_SHORT).show();
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String deviceHardwareAddress = device.getAddress(); // MAC address
                mDeviceMap.put(deviceName, rssi);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }
}
