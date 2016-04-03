/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 *
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided.
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package com.diy.blelib.ble;

import java.util.List;
import java.util.UUID;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.diy.blelib.profile.BleManager;

/**
 * HTSManager class performs BluetoothGatt operations for connection, service discovery, enabling indication and reading characteristics. All operations required to connect to device with BLE HT
 * Service and reading health thermometer values are performed here. HTSActivity implements HTSManagerCallbacks in order to receive callbacks of BluetoothGatt operations
 */
public class HealthManager implements BleManager<HealthManagerCallbacks> {
	private final String TAG = "HTSManager";
	private HealthManagerCallbacks mCallbacks;
	private BluetoothGatt mBluetoothGatt;
	private Context mContext;
	//心率服务
	public final static UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
	private static final UUID HR_SENSOR_LOCATION_CHARACTERISTIC_UUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
	private static final UUID HR_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
	//温度服务
	public final static UUID TP_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
	private static final UUID TP_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");
	private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	//电池服务
	private final static UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	private final static UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
	//版本服务
	private final static UUID BLE_UUID_DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
  	private final static UUID BLE_UUID_MODEL_NUMBER_STRING_CHAR = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
	//串口服务
	public static final UUID RX_SERVICE_UUID = UUID.fromString("00000001-0000-1000-8000-00805f9b34fb");
	//串口写特征值
	public static final UUID W_RX_CHAR_UUID = UUID.fromString("00000002-0000-1000-8000-00805f9b34fb");
	//串口读特征值
	public static final UUID R_TX_CHAR_UUID = UUID.fromString("00000003-0000-1000-8000-00805f9b34fb");
	private final static String ERROR_CONNECTION_STATE_CHANGE = "Error on connection state change";
	private final static String ERROR_DISCOVERY_SERVICE = "Error on discovering services";
	private final static String ERROR_READ_CHARACTERISTIC = "Error on reading characteristic";
	private final static String ERROR_WRITE_DESCRIPTOR = "Error on writing descriptor";
	private final static String ERROR_AUTH_ERROR_WHILE_BONDED = "Phone has lost bonding information";

	private BluetoothGattCharacteristic mTPCharacteristic, mBatteryCharacteritsic,mVersionCharacteristic
	,mHrsCharacteristic,mR_TXCharacteristic,mW_TXCharacteristic;

	private final int HIDE_MSB_8BITS_OUT_OF_32BITS = 0x00FFFFFF;
	private final int HIDE_MSB_8BITS_OUT_OF_16BITS = 0x00FF;
	private final int SHIFT_LEFT_8BITS = 8;
	private final int SHIFT_LEFT_16BITS = 16;
	private final int GET_BIT24 = 0x00400000;
	private static final int FIRST_BIT_MASK = 0x01;
	public static boolean  heart_open = false;
	public static boolean oxygen_open = false;
	private static HealthManager managerInstance = null;

	/**
	 * singleton implementation of HTSManager class
	 */
	public static synchronized HealthManager getHTSManager() {
		if (managerInstance == null) {
			managerInstance = new HealthManager();
		}
		return managerInstance;
	}

	/**
	 * callbacks for activity {HTSActivity} that implements HTSManagerCallbacks interface activity use this method to register itself for receiving callbacks
	 */
	@Override
	public void setGattCallbacks(HealthManagerCallbacks callbacks) {
		mCallbacks = callbacks;
	}

	@Override
	public void connect(Context context, BluetoothDevice device) {
		mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
		mContext = context;
	}

	@Override
	public void disconnect() {
		Log.d(TAG, "Disconnecting device");
		if (mBluetoothGatt != null) {
			mBluetoothGatt.disconnect();
		}
	}

	private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
			final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

			Log.d(TAG, "Bond state changed for: " + device.getAddress() + " new state: " + bondState + " previous: " + previousBondState);

			// skip other devices
			if (!device.getAddress().equals(mBluetoothGatt.getDevice().getAddress()))
				return;

			if (bondState == BluetoothDevice.BOND_BONDED) {
				// We've read Battery Level, now enabling HT indications
				if (mTPCharacteristic != null) {
					enableTPIndication();
				}
				mContext.unregisterReceiver(this);
				mCallbacks.onBonded();
			}
		}
	};

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving indication, etc
	 */
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					Log.d(TAG, "Device connected");
					mBluetoothGatt.discoverServices();
					//This will send callback to HTSActivity when device get connected
					mCallbacks.onDeviceConnected();
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					Log.d(TAG, "Device disconnected1");
					//This will send callback to HTSActivity when device get disconnected
					if(mCallbacks != null);
						mCallbacks.onDeviceDisconnected(false);
				}
			} else {
				Log.d(TAG, "Device disconnected2");
				if(mCallbacks != null);
					mCallbacks.onError(ERROR_CONNECTION_STATE_CHANGE, status,false);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				List<BluetoothGattService> services = gatt.getServices();
				for (BluetoothGattService service : services) {
					if (service.getUuid().equals(TP_SERVICE_UUID)) {
						mTPCharacteristic = service.getCharacteristic(TP_MEASUREMENT_CHARACTERISTIC_UUID);
					} else if (service.getUuid().equals(BATTERY_SERVICE)) {
						mBatteryCharacteritsic = service.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC);
					} else if(service.getUuid().equals(BLE_UUID_DEVICE_INFORMATION_SERVICE)) {
						mVersionCharacteristic = service.getCharacteristic(BLE_UUID_MODEL_NUMBER_STRING_CHAR);
					} else if(service.getUuid().equals(HR_SERVICE_UUID)) {
						mHrsCharacteristic = service.getCharacteristic(HR_MEASUREMENT_CHARACTERISTIC_UUID);
					} else if(service.getUuid().equals(RX_SERVICE_UUID)){
						mR_TXCharacteristic = service.getCharacteristic(R_TX_CHAR_UUID);
					}
				}

				if (mTPCharacteristic != null) {
					mCallbacks.onServicesDiscovered(false);
				} else {
					mCallbacks.onDeviceNotSupported();
					gatt.disconnect();
					return;
				}
				if(mHrsCharacteristic != null) {
					Log.i(TAG, "mHrsCharacteristic!!");
				} else {
					Log.i(TAG, "mHrsCharacteristic = nullnull");
				}
				if (mBatteryCharacteritsic != null) {
					readBatteryLevel();
				} else {
					Log.i(TAG, "enableTPIndication = enableTPIndication");
					enableTPIndication();
				}
			} else {
				mCallbacks.onError(ERROR_DISCOVERY_SERVICE, status,false);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (characteristic.getUuid().equals(BATTERY_LEVEL_CHARACTERISTIC)) {
					int batteryValue = characteristic.getValue()[0];
					mCallbacks.onBatteryValueReceived(batteryValue);
					if (mVersionCharacteristic != null) {
		            	//	enableFWIndication();
		    				readVersion();
		    			}
				}
				if (characteristic.getUuid().equals(BLE_UUID_MODEL_NUMBER_STRING_CHAR)) {
					int version = characteristic.getValue()[4];
					Log.i("test", "CCCCAAAAVERSION"+version+"  "+new String(characteristic.getValue()));
					enableTPIndication();
				}
				if (characteristic.getUuid().equals(R_TX_CHAR_UUID)) {
					Log.i(TAG, "qqqqqqqqq "+characteristic.getUuid().equals(R_TX_CHAR_UUID));
				}

			} else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
				if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
					Log.w(TAG, ERROR_AUTH_ERROR_WHILE_BONDED);
					mCallbacks.onError(ERROR_AUTH_ERROR_WHILE_BONDED, status,false);
				}
			} else {
				mCallbacks.onError(ERROR_READ_CHARACTERISTIC, status,false);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			double tempValue = 0.0;
			int hrValue = 0;int heart = 0;int oxy = 0;
				if(mR_TXCharacteristic != null) {//default c
				//	writeCMDtoBle(0X06, 0, 0, 0, 0);
				}
			if (characteristic.getUuid().equals(TP_MEASUREMENT_CHARACTERISTIC_UUID)) {
				try {
					tempValue = decodeTemperature(characteristic.getValue());
					mCallbacks.onHTValueReceived(tempValue);
//					Log.e(TAG, "TP_MEASUREMENT_CHARACTERISTIC_UUID temperature value --"+tempValue+" "+TimeStrUtil.getCurrentTime());
				} catch (Exception e) {
					Log.e(TAG, "invalid temperature value");
				}
			}
			if (characteristic.getUuid().equals(R_TX_CHAR_UUID) || mR_TXCharacteristic == characteristic) {
				Log.i(TAG, "mR_TXCharacteristic ~~ ");
				decodeUartData(characteristic.getValue());
				enableTPIndication();
			}
			if(characteristic.getUuid().equals(HR_MEASUREMENT_CHARACTERISTIC_UUID)) {
				enableTPIndication();
				/*if (isHeartRateInUINT16(characteristic.getValue()[0])) {
					hrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
					heart = hrValue / 100;
					oxy = hrValue % 100;
					Log.i(TAG, "eeee1  "+hrValue+" heart="+heart+"oxy="+oxy);

				} else {
					hrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
					Log.i(TAG, "eeee "+hrValue);
					mCallbacks.onOXYValueReceived(hrValue);
				}*/
				if(heart_open) {
					hrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
					heart = hrValue / 100;
					oxy = hrValue % 100;
					Log.i(TAG, "eeee1  "+hrValue+" heart="+heart+"oxy="+oxy);
					if(heart >= 40 && heart <= 200)
						mCallbacks.onHRSValueReceived(heart);
				}
				if(oxygen_open) {
					hrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
					Log.i(TAG, "eeee "+hrValue);
					if(hrValue >= 60 && hrValue <= 100)
						mCallbacks.onOXYValueReceived(hrValue);
				}
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// HT indications has been enabled
			} else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
				if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
					mCallbacks.onBondingRequired();

					final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
					mContext.registerReceiver(mBondingBroadcastReceiver, filter);
				} else {
					Log.w(TAG, ERROR_AUTH_ERROR_WHILE_BONDED);
					mCallbacks.onError(ERROR_AUTH_ERROR_WHILE_BONDED, status,false);
				}
			} else {
				Log.e(TAG, ERROR_WRITE_DESCRIPTOR + " (" + status + ")");
				mCallbacks.onError(ERROR_WRITE_DESCRIPTOR, status,false);
			}
		}
	};


	public void readBatteryLevel() {
		if (mBatteryCharacteritsic != null) {
			mBluetoothGatt.readCharacteristic(mBatteryCharacteritsic);
		} else {
			Log.e(TAG, "Battery Level Characteristic is null");
		}
	}
	public void readVersion() {
		mBluetoothGatt.readCharacteristic(mVersionCharacteristic);
	}

	private void enableUartReci() {
		mBluetoothGatt.setCharacteristicNotification(mR_TXCharacteristic,true);
		BluetoothGattDescriptor descriptor = mR_TXCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
	}
	/**
	 * enable Health Thermometer indication on Health Thermometer Measurement characteristic
	 */
	private void enableTPIndication() {
		mBluetoothGatt.setCharacteristicNotification(mTPCharacteristic, true);
		BluetoothGattDescriptor descriptor = mTPCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
		mBluetoothGatt.writeDescriptor(descriptor);
	}
	/**
	 * Enabling notification on Heart Rate Characteristic
	 */
	private void enableHRNotification() {
		Log.d(TAG, "Enabling heart rate notifications");
		mBluetoothGatt.setCharacteristicNotification(mHrsCharacteristic, true);
		BluetoothGattDescriptor descriptor = mHrsCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		mBluetoothGatt.writeDescriptor(descriptor);
	}

	/**
	 * This method decode temperature value received from Health Thermometer device First byte {0} of data is flag and first bit of flag shows unit information of temperature. if bit 0 has value 1
	 * then unit is Fahrenheit and Celsius otherwise Four bytes {1 to 4} after Flag bytes represent the temperature value in IEEE-11073 32-bit Float format
	 */
	private double decodeTemperature(byte[] data) throws Exception {
		double temperatureValue = 0.0;
		byte flag = data[0];
		byte exponential = data[4];
		short firstOctet = convertNegativeByteToPositiveShort(data[1]);
		short secondOctet = convertNegativeByteToPositiveShort(data[2]);
		short thirdOctet = convertNegativeByteToPositiveShort(data[3]);
		int mantissa = ((thirdOctet << SHIFT_LEFT_16BITS) | (secondOctet << SHIFT_LEFT_8BITS) | (firstOctet)) & HIDE_MSB_8BITS_OUT_OF_32BITS;
		mantissa = getTwosComplimentOfNegativeMantissa(mantissa);
		temperatureValue = (mantissa * Math.pow(10, exponential));
		/*
		 * Conversion of temperature unit from Fahrenheit to Celsius if unit is in Fahrenheit
		 * Celsius = (98.6*Fahrenheit -32) 5/9
		 */
		if ((flag & FIRST_BIT_MASK) != 0) {
			temperatureValue = (float) ((98.6 * temperatureValue - 32) * (5 / 9.0));
		}
		return temperatureValue;
	}

	private short convertNegativeByteToPositiveShort(byte octet) {
		if (octet < 0) {
			return (short) (octet & HIDE_MSB_8BITS_OUT_OF_16BITS);
		} else {
			return octet;
		}
	}

	private int getTwosComplimentOfNegativeMantissa(int mantissa) {
		if ((mantissa & GET_BIT24) != 0) {
			return ((((~mantissa) & HIDE_MSB_8BITS_OUT_OF_32BITS) + 1) * (-1));
		} else {
			return mantissa;
		}
	}

	/**
	 * This method will check if Heart rate value is in 8 bits or 16 bits
	 */
	private boolean isHeartRateInUINT16(byte value) {
		if ((value & FIRST_BIT_MASK) != 0)
			return true;
		return false;
	}
	@Override
	public void closeBluetoothGatt() {
		try {
			mContext.unregisterReceiver(mBondingBroadcastReceiver);
		} catch (Exception e) {
			// the receiver must have been not registered or unregistered before
		}
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			mBluetoothGatt = null;
			mBatteryCharacteritsic = null;
			mTPCharacteristic = null;
			mHrsCharacteristic = null;

		}
	}

	private void decodeUartData(byte[] data) {
		Log.i(TAG, "version--- "+data[0]+" "+data[1]);
		switch (data[0]+256) {
		case 0x06:
			Log.i(TAG, "version0x06--- "+data[1]+" "+data[2]);
			break;
		default:
			break;
		}

	}
	private void writeCMDtoBle(int cmd,int Upper, int UpperDecimal, int Lower, int LowerDecimal) {
		switch (cmd) {
		case 0x00:
			byte[] valuec_f = new byte[2];
			valuec_f[0] = (byte) cmd;
			valuec_f[1] = (byte) Upper;
			writeCharacteristic(valuec_f);
//			enableTPIndication();
			break;
		case 0x01:
			byte[] value = new byte[3];
			value[0] = (byte) cmd;
			value[1] = (byte) Upper;
			value[2] = (byte) UpperDecimal;
			writeCharacteristic(value);

			break;
		case 0x02:
			byte[] heart_start = new byte[2];
			heart_start[0] = (byte) cmd;
			heart_start[1] = (byte) Upper;
			writeCharacteristic(heart_start);
			enableHRNotification();
			if(Upper == 1)
				heart_open = true;
			else
				heart_open = false;
			break;
		case 0x04:
			byte[] oxygen_start = new byte[2];
			oxygen_start[0] = (byte) cmd;
			oxygen_start[1] = (byte) Upper;
			writeCharacteristic(oxygen_start);
			enableHRNotification();
			if(Upper == 1)
				oxygen_open = true;
			else
				oxygen_open = false;
			break;
		case 0X03:
			byte[] value_heart = new byte[3];
			value_heart[0] = (byte) cmd;
			value_heart[1] = (byte) Lower;
			value_heart[2] = (byte) Upper;
			writeCharacteristic(value_heart);
			break;
		case 0X05:
			byte[] value_oxygen = new byte[3];
			value_oxygen[0] = (byte) cmd;
			value_oxygen[1] = (byte) Lower;
			writeCharacteristic(value_oxygen);
			break;
		case 0X06:
			byte[] version = new byte[3];
			version[0] = (byte) cmd;
			writeCharacteristic(version);
			enableUartReci();
			break;

		default:
			break;
		}
	}
	private void writeCmdToFota(int cmd) {
			byte[] fota = new byte[3];
			fota[0] = (byte) cmd;
			writeCharacteristic(fota);
			enableUartReci();
	}
	public void writeCharacteristic(byte[] value){

			if(mBluetoothGatt != null){
				BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
		    	if (RxService == null) {
		            return;
		        }
		    	BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(W_RX_CHAR_UUID);
		        if (RxChar == null) {
		            return;
		        }
		        	RxChar.setValue(value);
		        	boolean status = mBluetoothGatt.writeCharacteristic(RxChar);
			}
		//	enableTPIndication();
		}

	@Override
	public void setLimit(int cmd,int Upper, int UpperDecimal, int Lower, int LowerDecimal) {
		// TODO Auto-generated method stub
		writeCMDtoBle(cmd, Upper,UpperDecimal, Lower,LowerDecimal);
	}
	@Override
	public void setCelsiusOrFahrenheit(int cmd,int cf) {
		writeCMDtoBle(cmd, cf, 0, 0, 0);
	}

	@Override
	public void fota(int cmd) {
		writeCmdToFota(cmd);
	}
}
