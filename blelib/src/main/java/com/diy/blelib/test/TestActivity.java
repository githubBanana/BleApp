package com.diy.blelib.test;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.diy.blelib.R;
import com.diy.blelib.scanner.ScannerFragment;

import java.util.UUID;

/**
 * @version V1.0 <描述当前版本功能>
 * @author: Xs
 * @date: 2016-04-03 21:17
 */
public class TestActivity extends AppCompatActivity implements ScannerFragment.OnDeviceSelectedListener {
    private static final String TAG = "TestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity_layout);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scannerShow();
            }
        });
    }

    public final static UUID TP_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    protected UUID getFilterUUID() {
        return TP_SERVICE_UUID;
    }
    protected boolean isCustomFilterUUID() {
        return false;
    }
    private void scannerShow() {
        final ScannerFragment dialog = ScannerFragment.getInstance(this, getFilterUUID(), isCustomFilterUUID());
        dialog.show(getSupportFragmentManager(), "scan_fragment");
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device, String name) {

    }

    @Override
    public void onDialogCanceled() {

    }
}
