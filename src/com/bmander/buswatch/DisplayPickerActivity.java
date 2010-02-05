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

public class DisplayPickerActivity extends Activity {
    
    // constants
    int REQUEST_ENABLE_BT=1;
    String TAG = "DisplayPickerActivity";
    
    // class state variables
    boolean bluetooth_supported;
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice currentDevice = null;
    
    // views
    RadioGroup devicesRadioGroup;
    RelativeLayout scanButton;
    ProgressBar progressBar;
    TextView progressText;
    
    class ScanButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            // flip the scan button over to an indeterminate progressbar
            progressBar.setVisibility(View.VISIBLE);
            progressText.setText("Scanning");
            
            // search for unpaired devices
            Log.i( TAG, "start scanning for other devices!" );
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
    
    private void get_devices() {
        Log.i( TAG, "bluetooth checked, getting devices" );
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
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
        }
    }
        
}