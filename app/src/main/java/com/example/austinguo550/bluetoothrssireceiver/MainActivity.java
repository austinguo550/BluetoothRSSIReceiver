package com.example.austinguo550.bluetoothrssireceiver;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import android.os.Vibrator;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;    //must be greater than 0
    private static final int REQUEST_COARSE_LOCATION = 2;
    private static final int REQUEST_VIBRATE = 3;

    TextView mTextView;
    Button mDiscoverButton;
    Button mStartPartyButton;
    Button mCancelButton;
    ProgressBar mProgressBar;

    private Context mContext;
    AppCompatActivity thisActivity = this;

    String deviceList;
    int devicesInDeviceList = 0;
    ArrayList<String> mDeviceMap = new ArrayList<String>();

    String partyList;                                                                               //PARTY RESET
    int devicesInPartyList = 0;                                                                     //PARTY RESET
    HashMap<String, Integer> mPartyMap = new HashMap<String, Integer>();                            //PARTY RESET

    private boolean canStartDiscovering = false;
    private boolean selectedParty = false;                                                          //PARTY RESET

    //for debug purposes
    private final static String TAG = MainActivity.class.getSimpleName();

    //private Runnable runnable;
    //private Handler handler;
    //private final int interval = 12000; // 12 Seconds

    // To sample RSSI trend
    HashMap<String, ArrayList<Integer>> rssiTrends = new HashMap<String, ArrayList<Integer>>();     //PARTY RESET
    //HashMap<String, Integer> mPartyMapSignalAvgs = new HashMap<String, Integer>();
    private boolean contScan = false;                                                               //PARTY RESET

    Vibrator vb;
    private boolean vibrateIsOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mTextView = (TextView) findViewById(R.id.textView);
        mDiscoverButton = (Button) findViewById(R.id.button);
        mStartPartyButton = (Button) findViewById(R.id.button3);
        mCancelButton = (Button) findViewById(R.id.button2);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setIndeterminate(true);    //infinite spinning wheel
        mProgressBar.setVisibility(View.INVISIBLE);

        vb = (Vibrator) getSystemService(mContext.VIBRATOR_SERVICE);

        mContext = getApplicationContext();

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
        startActivityForResult(discoverableIntent, 1);

        canStartDiscovering = (ContextCompat.checkSelfPermission(thisActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);


        // Starting discovery with button press
        mDiscoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canStartDiscovering && ContextCompat.checkSelfPermission(thisActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    startDiscovering();
                else {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {

                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(thisActivity,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_COARSE_LOCATION);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }
            }
        });

        // Cancel party with button press
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetParty();
            }
        });

        // Start party with button press
        mStartPartyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canStartDiscovering && ContextCompat.checkSelfPermission(thisActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(thisActivity,
                        Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                    if (selectedParty) {
                        resetParty();
                        Toast.makeText(mContext, "Refreshing party", Toast.LENGTH_SHORT).show();
                    }
                    startParty();
                }
                else {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {

                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                            Manifest.permission.VIBRATE)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(thisActivity,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_COARSE_LOCATION);
                        ActivityCompat.requestPermissions(thisActivity,
                                new String[]{Manifest.permission.VIBRATE},
                                REQUEST_VIBRATE);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }
            }
        });
    }

    private void resetParty() {
        selectedParty = false;
        mProgressBar.setVisibility(View.INVISIBLE);
        partyList = "Party ended.";
        mTextView.setText(partyList);
        mPartyMap.clear();
        devicesInPartyList = 0;
        rssiTrends.clear();
        turnOffContinuousScan();
        cancelVibration();
    }

    private void startParty() {
        selectedParty = true;
        Toast.makeText(mContext, "Scanning for party signals", Toast.LENGTH_SHORT).show();
        if (selectedParty) {
            Log.d(TAG, "////////////Sampling RSSI");
            HashMap<String, Integer> mPartyAvgSignals = sampleRSSI();
            Log.d(TAG, "////////////Done sampling");
            turnOnContinuousScan();
            mProgressBar.setVisibility(View.VISIBLE);
            scanForRSSI();
            /*handler = new Handler();
            runnable = new Runnable(){
                public void run() {
                    scanForRSSI();
                    Log.d(TAG, "running scanForRSSI every 12 seconds");
                }
            };
            handler.postAtTime(runnable, System.currentTimeMillis()+interval);
            handler.postDelayed(runnable, interval);*/
        }
    }

    private HashMap<String, Integer> sampleRSSI() {
        for (int i = 0; i < 5; i++) {
            scanForRSSI();
        }
        HashMap<String, Integer> rssi = new HashMap<String, Integer>();
        for (String device: rssiTrends.keySet()) {
            int avgSignal = 0;
            for (int i = 0; i < 5; i++) {
                avgSignal += rssiTrends.get(device).get(i);
            }
            avgSignal /= 5;
            rssi.put(device, avgSignal);
        }
        return rssi;
    }

    private boolean turnOnContinuousScan() {
        vibrateIsOn = true;
        contScan = true;
        return contScan;
    }

    private boolean turnOffContinuousScan() {
        vibrateIsOn = false;
        contScan = false;
        return contScan;
    }

    private void scanForRSSI() {
        // Starting scan for RSSI of devices in party
        BluetoothDevice remoteDevice = null;
        if (!mBluetoothAdapter.startDiscovery()) {
            Toast.makeText(mContext, "ERROR: Can't scan for nearby devices. A requested permission may be disabled.", Toast.LENGTH_SHORT).show();
            selectedParty = false;
        }
        else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
        }
    }


    public void startDiscovering() {
        // Starting discovery
        deviceList = "";
        mTextView.setText(deviceList);
        Toast.makeText(MainActivity.this, "Starting discovery for remote devices...", Toast.LENGTH_SHORT).show();
        if (!mBluetoothAdapter.startDiscovery())
            Toast.makeText(mContext, "Could not find any devices to connect to. A requested permission may be disabled.", Toast.LENGTH_SHORT).show();
        else {
            if (!selectedParty) {
                deviceList = "Devices found: \n";
                mTextView.setText(deviceList);
            }
            else {
                partyList = "Your party: \n";
                mTextView.setText(partyList);
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(MainActivity.class.getSimpleName(), "---- onReceive method started ----");
            String action = intent.getAction();

            // Just discovering: haven't selected party
            if (!selectedParty) {
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    //discovery starts, we can show progress dialog or perform other tasks
                    Toast.makeText(mContext, "Discovery thread started... Scanning for devices", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, " ---- Reached ACTION_DISCOVERY_STARTED ----");
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //discovery finishes, dismis progress dialog

                    Toast.makeText(mContext, "Done scanning", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, " ---- Reached ACTION_DISCOVERY_FINISHED ----");
                    if(devicesInDeviceList == 0) {
                        deviceList += "None";
                        mTextView.setText(deviceList);
                    }

                    //mBluetoothAdapter.cancelDiscovery();
                    //printDiscoveryResults();
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    Log.d(TAG, " ---- Reached ACTION_FOUND ----");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    //if (device != null) {
                        Toast.makeText(mContext, "Found device " + deviceName, Toast.LENGTH_SHORT).show();

                        //TODO: Make this a checklist where you can select party members
                        devicesInDeviceList++;
                        deviceList += deviceName + "\n";
                        mTextView.setText(deviceList);
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        mDeviceMap.add(deviceName);
                    //}
                }
            }

            // You've selected party, scanning for RSSI values
            else {
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    //discovery starts, we can show progress dialog or perform other tasks
                    Log.d(TAG, " ---- Reached ACTION_DISCOVERY_STARTED ----");
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //discovery finishes, dismis progress dialog

                    // You've already determined which Bluetooth devices are in your party
                    if (contScan) {
                        printPartySignals();
                        scanForRSSI();  // Won't be called if selectedParty = false, so if you click cancel recursive loop will stop
                    }
                    // If not in continuous scan mode, the only reason to be called while selectedParty is on is to sample RSSI and find average

                    //mBluetoothAdapter.cancelDiscovery();
                    //printDiscoveryResults();
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    Log.d(TAG, " ---- Reached ACTION_FOUND ----");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    Toast.makeText(mContext, "Found device " + deviceName, Toast.LENGTH_SHORT).show();

                    devicesInPartyList++;
                    int rssi = (int) intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    mPartyMap.put(deviceName, rssi);

                    // If not in continuous scan mode, the only reason to be called while selectedParty is on is to sample RSSI and find average
                    if (!contScan) {
                        if (!rssiTrends.containsKey(deviceName)) {
                            ArrayList<Integer> deviceSignalTrend = new ArrayList<Integer>();
                            deviceSignalTrend.add(rssi);
                            rssiTrends.put(deviceName, deviceSignalTrend);
                        } else {    // The device is already there, just append the new RSSI to the deviceSignalTrend
                            rssiTrends.get(deviceName).add(rssi);
                        }

                    }
                }
            }
        }
    };

    /*  TODO: get rid of all the null bs
    // Get device name from ble advertised data
    private BluetoothAdapter.LeScanCallback mScanCb = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            final BleAdvertisedData badata = BleUtil.parseAdertisedData(scanRecord);
            String deviceName = device.getName();
            if (deviceName == null) {
                deviceName = badata.getName();
            }
        }
    };
    */

    private void printPartySignals() {
        partyList = "Your party: \n";
        int intensity = 0;
        int phoneRSSIValue = 0;
        String signalStrength = "";
        int numDevices = 0;
        for (String device : mPartyMap.keySet()) {
            numDevices++;
            signalStrength = "";
            phoneRSSIValue = mPartyMap.get(device);
            if (phoneRSSIValue >= -50) {
                signalStrength = "Strong vibes";
            } else if (phoneRSSIValue < -50 && phoneRSSIValue >= -67){
                signalStrength = "Good vibes";
            } else if (phoneRSSIValue < -67 && phoneRSSIValue >= -90) {
                signalStrength = "Ok vibes";
            } else if (phoneRSSIValue < -90 && phoneRSSIValue >= -100) {
                signalStrength = "Low vibes";
            } else if (phoneRSSIValue < -100) {
                signalStrength = "Lost signal";
            }
            partyList += device + " :   " + signalStrength + "\n";
            Log.d(TAG, "" + phoneRSSIValue);

            intensity += phoneRSSIValue;
        }
        intensity /= numDevices;
        mTextView.setText(partyList);

        if (contScan && vb.hasVibrator()) {
            //vibratePhones(intensity);  //TODO: Make the phones vibrate depending on intensity
            //vb.vibrate(10000);
            //vb.cancel();
        }
    }

    // Vibrates host phone based on average of party signals
    private void vibratePhones(int intensity) {
        int gap = 0;
        if (intensity >= -50) {
            // Do nothing, vibrate very strongly
        } else if (intensity < -50 && intensity >= -67){
            gap = 10;
        } else if (intensity < -67 && intensity >= -90) {
            gap = 30;
        } else if (intensity < -90 && intensity >= -100) {
            gap = 80;
        } else if (intensity < -100) {
            gap = 150;
        }

        long pattern[] = {500, gap};

        do {
            vb.vibrate(pattern, -1);
        } while (vibrateIsOn);
    }

    private void cancelVibration() {
        vibrateIsOn = false;
        vb.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    canStartDiscovering = true;

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /*  TODO: Implement onPause and onResume to turn off discovery/etc when app is paused
    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                Length, long, locationListener);
    }
    */

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }
}
