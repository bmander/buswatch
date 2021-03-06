package com.bmander.buswatch;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log; 
import java.util.Set;
import android.view.*; 
import android.widget.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class DisplayPickerActivity extends Activity {
    
    // constants
    int REQUEST_ENABLE_BT=1;
    String TAG = "DisplayPickerActivity";
    
    // class state variables
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice currentDevice = null;
    BroadcastReceiver foundReceiver = null;
    BroadcastReceiver finishedReceiver = null;
    boolean discoveryUnderway = false;
    
    // views
    RadioGroup devicesRadioGroup;
    RelativeLayout scanButton;
    ProgressBar progressBar;
    TextView progressText;
    
    void setScanButtonReady() {
        Log.i( TAG, "done scanning" );
        discoveryUnderway=false;
        progressBar.setVisibility(View.INVISIBLE);
        progressText.setText("Scan for devices");
    }
    
    void setScanButtonWorking() {
        Log.i( TAG, "start scanning for other devices!" );
        // flip a bit so we only can do this once at a time
        discoveryUnderway=true;
        
        // flip the scan button over to an indeterminate progressbar
        progressBar.setVisibility(View.VISIBLE);
        progressText.setText("Scanning");
    }
    
    class ScanButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            if(!discoveryUnderway) {
                setScanButtonWorking();
                
                // search for unpaired devices
                mBluetoothAdapter.startDiscovery();
            } else {
                mBluetoothAdapter.cancelDiscovery();
                setScanButtonReady();
            }
        }
    }
    
    public void onCreate(Bundle savedInstanceState)
    {
        // call super, use XML layout  
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaypicker);
        
        // get views
        devicesRadioGroup = (RadioGroup) findViewById(R.id.devices);
        scanButton = (RelativeLayout) findViewById(R.id.scanbutton);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressText = (TextView) findViewById(R.id.progresstext);
        
        // configure the views
        progressBar.setIndeterminate(true);
        
        // set listeners
        scanButton.setOnClickListener( new ScanButtonClickListener() );
        
        // initialize bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // no bluetooth at all?
        if (mBluetoothAdapter == null) {
            // inform the user they're totally out of luck
            set_failwhale();
        // some bluetooth?
        } else {
            // if it's turned off
            if (!mBluetoothAdapter.isEnabled()) {
                // try to turn it on
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            // if it's turned on
            } else {
                // we're good to go!
                get_devices();
            }
        }
        
        // register receiver for bluetooth discovery found
        // Create a BroadcastReceiver for ACTION_FOUND
        foundReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                // when discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    Log.i( TAG, "found "+device.getName() + " - " + device.getAddress() );
                    addToRadioGroup( device );
                }
            }
        };
        // Register the BroadcastReceiver
        registerReceiver(foundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
        
        // register receiver for bluetooth discovery finished
        finishedReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                // when discovery is complete
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    setScanButtonReady();
                }
            }
        };
        // Register the BroadcastReceiver
        registerReceiver(finishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        
    }
    
    public void onDestroy() {
        super.onDestroy();
        
        unregisterReceiver(foundReceiver);
        unregisterReceiver(finishedReceiver);
    }
    
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        // if bluetooth just got turned on
        if(requestCode==REQUEST_ENABLE_BT && resultCode==RESULT_OK) {
            // get a list of bluetooth devices
            get_devices();
        }
    }
    
    private void set_failwhale() {
        Log.i( TAG, "Bluetooth completely absent" );
    }
    
    
    class SelectRadioButtonListener implements CompoundButton.OnCheckedChangeListener {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                currentDevice = (BluetoothDevice)buttonView.getTag();
            }
        }
    }
    
    private void addToRadioGroup(BluetoothDevice device) {
        // scan through the radiogroup for the device
        for(int i=0; i<devicesRadioGroup.getChildCount(); i++) {
            // if it's in there, return
            RadioButton rb = (RadioButton)devicesRadioGroup.getChildAt(i);
            if( ((BluetoothDevice)rb.getTag()).getAddress().equals( device.getAddress() ) ) {
                return;
            }
        }
        // if it's not there, go ahead
        
        // Add the name and address to an array adapter to show in a ListView
        Log.i(TAG, device.getName() + " - " + device.getAddress());
        
        // put a checkbox on the right, pre-filled out
        RadioButton radioBox = new RadioButton(this);
        
        // set the tag to the routeId string that this radiobox represents
        radioBox.setTag( device );
        radioBox.setText( device.getName() + " - " + device.getAddress() );
        
        //causes the currentRouteId to get set with the radioBox tag, which is the radiobox's route id
        radioBox.setOnCheckedChangeListener( new SelectRadioButtonListener() );
        
        // add the radiobutton to the radiogroup
        devicesRadioGroup.addView( radioBox );
    }
    
    private void get_devices() {
        Log.i( TAG, "bluetooth checked, getting devices" );
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                addToRadioGroup( device );
            }
        }
    }
        
}