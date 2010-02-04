package com.bmander.buswatch;

import android.app.Activity;
import android.os.Bundle;

public class DisplayPickerActivity extends Activity {
    public void onCreate(Bundle savedInstanceState)
    {
        // call super, use XML layout 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaypicker);
    }
}