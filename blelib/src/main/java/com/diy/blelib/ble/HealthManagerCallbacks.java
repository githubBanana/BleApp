/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 *
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided.
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package com.diy.blelib.ble;


import com.diy.blelib.profile.BleManagerCallbacks;

/**
 * Interface { HTSManagerCallbacks} must be implemented by { HTSActivity} in order to receive callbacks from { HTSManager}
 */
public interface HealthManagerCallbacks extends BleManagerCallbacks {

	/**
	 * Called when Health Thermometer value has been received
	 *
	 * @param value
	 *            the new value
	 */
	public void onHTValueReceived(double value);

	public void onHRSValueReceived(int value);

	public void onOXYValueReceived(int value);

}
