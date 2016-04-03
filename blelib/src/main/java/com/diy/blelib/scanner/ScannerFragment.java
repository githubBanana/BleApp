package com.diy.blelib.scanner;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.diy.blelib.R;

import java.util.Set;
import java.util.UUID;

/**
 * @version V1.0 <ble 搜索框>
 * @author: Xs
 * @date: 2016-04-03 20:39
 */
public class ScannerFragment extends DialogFragment{
    private static final String TAG = "ScannerFragment";

    private final static String PARAM_UUID = "param_uuid";
    private final static String DISCOVERABLE_REQUIRED = "discoverable_required";
    private final static long SCAN_DURATION = 5000;

    private BluetoothAdapter mBluetoothAdapter;
    private OnDeviceSelectedListener mListener;
    private DeviceListAdapter mAdapter;
    private Handler mHandler = new Handler();
    private Button mScanButton;

    private boolean mDiscoverableRequired;
    private UUID mUuid;

    private boolean mIsScanning = false;

    private static final boolean DEVICE_IS_BONDED = true;
    private static final boolean DEVICE_NOT_BONDED = false;
    /* package */static final int NO_RSSI = -1000;

    /**
     * Static implementation of fragment so that it keeps data when phone orientation is changed For standard BLE Service UUID, we can filter devices using normal android provided command
     * startScanLe() with required BLE Service UUID For custom BLE Service UUID, we will use class ScannerServiceParser to filter out required device.
     */
    public static ScannerFragment getInstance(final Context context, final UUID uuid, final boolean discoverableRequired) {
        final ScannerFragment fragment = new ScannerFragment();

        final Bundle args = new Bundle();
        args.putParcelable(PARAM_UUID, new ParcelUuid(uuid));
        args.putBoolean(DISCOVERABLE_REQUIRED, discoverableRequired);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Interface required to be implemented by activity.
     */
    public static interface OnDeviceSelectedListener {
        /**
         * Fired when user selected the device.
         *
         * @param device
         *            the device to connect to
         * @param name
         *            the device name. Unfortunately on some devices {@link BluetoothDevice#getName()} always returns <code>null</code>, f.e. Sony Xperia Z1 (C6903) with Android 4.3. The name has to
         *            be parsed manually form the Advertisement packet.
         */
        public void onDeviceSelected(final BluetoothDevice device, final String name);

        /**
         * Fired when scanner dialog has been cancelled without selecting a device.
         */
        public void onDialogCanceled();
    }

    /**
     * This will make sure that {@link OnDeviceSelectedListener} interface is implemented by activity.
     */
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnDeviceSelectedListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnDeviceSelectedListener");
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args.containsKey(PARAM_UUID)) {
            final ParcelUuid pu = args.getParcelable(PARAM_UUID);
            mUuid = pu.getUuid();
        }
        mDiscoverableRequired = args.getBoolean(DISCOVERABLE_REQUIRED);

        final BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
    }

    @Override
    public void onDestroyView() {
        stopScan();
        super.onDestroyView();
    }

    /**
     * When dialog is created then set AlertDialog with list and button views.
     */
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        //setStyle(R.style.ScannDialog, 1);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_device_selection, null);
        final ListView listview = (ListView) dialogView.findViewById(android.R.id.list);
        listview.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listview.setAdapter(mAdapter = new DeviceListAdapter(getActivity()));
        SpannableString styledText = new SpannableString(getString(R.string.scanner_title));
        ForegroundColorSpan span = new ForegroundColorSpan(getResources().getColor(R.color.colorGreen));
        styledText.setSpan(span, 0, styledText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setTitle(styledText);
        final AlertDialog dialog = builder.setView(dialogView).create();
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                stopScan();
                dialog.dismiss();
                final ExtendedBluetoothDevice d = (ExtendedBluetoothDevice) mAdapter.getItem(position);
                mListener.onDeviceSelected(d.device, d.name);
            }
        });

        mScanButton = (Button) dialogView.findViewById(R.id.action_cancel);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.action_cancel) {
                    if (mIsScanning) {
                        dialog.cancel();
                    } else {
                        startScan();
                    }
                }
            }
        });

		addBondedDevices();
        if (savedInstanceState == null)
            startScan();
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mListener.onDialogCanceled();
    }

    /**
     * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback is activated This will perform regular scan for custom BLE Service UUID and then filter out.
     * using class ScannerServiceParser
     */
    private void startScan() {
        mAdapter.clearDevices();
        mScanButton.setText(R.string.scanner_action_cancel);

        // Samsung Note II with Android 4.3 build JSS15J.N7100XXUEMK9 is not filtering by UUID at all. We must parse UUIDs manually
        mBluetoothAdapter.startLeScan(mLEScanCallback);

        mIsScanning = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsScanning) {
                    stopScan();
                }
            }
        }, SCAN_DURATION);
    }

    /**
     * Stop scan if user tap Cancel button.
     */
    private void stopScan() {
        if (mIsScanning) {
            mScanButton.setText(R.string.scanner_action_scan);
            mBluetoothAdapter.stopLeScan(mLEScanCallback);
            mIsScanning = false;
        }
    }

    private void addBondedDevices() {
        final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            mAdapter.addBondedDevice(new ExtendedBluetoothDevice(device, device.getName(), NO_RSSI, DEVICE_IS_BONDED));
        }
    }

    /**
     * if scanned device already in the list then update it otherwise add as a new device
     */
    private void addScannedDevice(final BluetoothDevice device, final String name, final int rssi, final boolean isBonded) {
        if (getActivity() != null)
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addOrUpdateDevice(new ExtendedBluetoothDevice(device, name, rssi, isBonded));
                }
            });
    }

    /**
     * if scanned device already in the list then update it otherwise add as a new device.
     */
    private void updateScannedDevice(final BluetoothDevice device, final int rssi) {
        if (getActivity() != null)
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.updateRssiOfBondedDevice(device.getAddress(), rssi);
                }
            });
    }

    /**
     * Callback for scanned devices class {@link ScannerServiceParser} will be used to filter devices with custom BLE service UUID then the device will be added in a list.
     */
    private BluetoothAdapter.LeScanCallback mLEScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (device != null) {
                Log.i(TAG, "device.getUuids():" + device.getUuids());
                updateScannedDevice(device, rssi);
                try {
                    if (ScannerServiceParser.decodeDeviceAdvData(scanRecord, mUuid, mDiscoverableRequired)) {
                        // On some devices device.getName() is always null. We have to parse the name manually :(
                        // This bug has been found on Sony Xperia Z1 (C6903) with Android 4.3.
                        // https://devzone.nordicsemi.com/index.php/cannot-see-device-name-in-sony-z1
                        addScannedDevice(device, ScannerServiceParser.decodeDeviceName(scanRecord), rssi, DEVICE_NOT_BONDED);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Invalid data in Advertisement packet " + e.toString());
                }
            }
        }
    };
}
