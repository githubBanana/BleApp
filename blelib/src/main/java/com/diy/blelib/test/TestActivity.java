package com.diy.blelib.test;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.diy.blelib.R;
import com.diy.blelib.ble.HealthService;
import com.diy.blelib.profile.BleProfileService;
import com.diy.blelib.scanner.ScannerFragment;

import java.util.UUID;

/**
 * @version V1.0 <test : 获取ble数据activity>
 * @author: Xs
 * @date: 2016-04-03 21:17
 */
public class TestActivity extends AppCompatActivity implements ScannerFragment.OnDeviceSelectedListener {
    private static final String TAG = "TestActivity";

    //ble
    private BleProfileService.LocalBinder mService;
    public static BluetoothDevice mDevice;
    private String mDeviceName;


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

        LocalBroadcastManager.getInstance(this).registerReceiver(mCommonBroadcastReceiver, makeIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCommonBroadcastReceiver);
        closeBleService();
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
        final Intent service = new Intent(this, getServiceClass());
        service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
        startService(service);
        bindService(service, mServiceConnection, 0);
    }

    @Override
    public void onDialogCanceled() {

    }

    /**
     * Returns the service class for sensor communication. The service class must derive from {@link BleProfileService} in order to operate with this class.
     *
     * @return the service class
     */
    protected Class<? extends BleProfileService> getServiceClass() {
        return HealthService.class;
    }

    protected void closeBleService() {
        if(mService != null) {
            Intent intent = new Intent(this,getServiceClass());
            unbindService(mServiceConnection);
            stopService(intent);
            mService = null;
        }
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            final BleProfileService.LocalBinder bleService =mService = (BleProfileService.LocalBinder) service;
            // update UI
            mDeviceName = bleService.getDeviceName();
            // and notify user if device is connected
            if (bleService.isConnected())
                notifyConnected();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mService = null;
            mDeviceName = null;
        }
    };

    /**
     * Called when service discovery has finished but the main services were not found on the device. This may occur when connecting to bonded device that does not support required services.
     */
    public void onDeviceNotSupported() {
        //showToast(R.string.not_supported);
    }
    private void notifyConnected() {	//notify user it is connected
    }
    private void onLinklossOccur() {}
    /**
     * Called when the device has started bonding process
     */
    public void onBondingRequired() {
        // empty default implementation
    }
    /**
     * Called when the device has finished bonding process sucessfully
     */
    public void onBonded() {
        // empty default implementation
    }


    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        /*intentFilter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_SERVICES_DISCOVERED);
        intentFilter.addAction(BleProfileService.BROADCAST_BOND_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_BATTERY_LEVEL);
        intentFilter.addAction(BleProfileService.BROADCAST_ERROR);
        intentFilter.addAction(HealthService.BROADCAST_TPS_MEASUREMENT);
        intentFilter.addAction(HealthService.BROADCAST_HRS_MEASUREMENT);
        intentFilter.addAction(HealthService.BROADCAST_OXY_MEASUREMENT);*/
        return intentFilter;
    }
    private BroadcastReceiver mCommonBroadcastReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BleProfileService.BROADCAST_CONNECTION_STATE.equals(action)) {
                final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);
                switch (state) {
                    case BleProfileService.STATE_CONNECTED:
                        break;
                    case BleProfileService.STATE_CONNECTING:
                        break;
                    case BleProfileService.STATE_DISCONNECTED:
                        break;
                    case BleProfileService.STATE_DISCONNECTING:
                        break;
                    case BleProfileService.STATE_LINK_LOSS:
                        onLinklossOccur();
                        break;
                    default:
                        break;
                }
            } else if(BleProfileService.BROADCAST_SERVICES_DISCOVERED.equals(action)){
                final boolean primaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_PRIMARY, false);
                @SuppressWarnings("unused")
                final boolean secondaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_SECONDARY, false);
                if (primaryService) {
                    //				onServicesDiscovered(secondaryService);
                } else {
                    onDeviceNotSupported();
                }
            } else if (BleProfileService.BROADCAST_BOND_STATE.equals(action)) {
                final int state = intent.getIntExtra(BleProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                switch (state) {
                    case BluetoothDevice.BOND_BONDING:
                        onBondingRequired();
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        onBonded();
                        break;
                }
            } else if (BleProfileService.BROADCAST_BATTERY_LEVEL.equals(action)) {
                final int value = intent.getIntExtra(BleProfileService.EXTRA_BATTERY_LEVEL, -1);
                if (value > 0);
//                    onBatteryValueReceived(value);
            } else if (BleProfileService.BROADCAST_ERROR.equals(action)) {
                final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
              /*  mainPresenter.reConnThreadMonitor();
                onError(message, errorCode);*/
            } else if (HealthService.BROADCAST_TPS_MEASUREMENT.equals(action)) {
                double tp = intent.getDoubleExtra(HealthService.EXTRA_TEMPERATURE, 0.0);
            } else if (HealthService.BROADCAST_HRS_MEASUREMENT.equals(action)) {
                int hrt = intent.getIntExtra(HealthService.EXTRA_HEART, 0);
            } else if (HealthService.BROADCAST_OXY_MEASUREMENT.equals(action)) {
                int oxy = intent.getIntExtra(HealthService.EXTRA_OXYGEN, 0);
            }  /*else if(BleProfileService.EXTRA_DESTORY_SERVICE.equals(action)) {
				DebugLog.i(TAG, "dddddddddddddddddd");
				mOnListenShareToRealTime.shareBleConnState(BleProfileService.EXTRA_DESTORY_SERVICE_INT);
			}*/
        }
    };

}
