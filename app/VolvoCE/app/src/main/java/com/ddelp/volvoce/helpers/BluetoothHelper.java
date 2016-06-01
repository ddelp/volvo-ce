package com.ddelp.volvoce.helpers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * Helper to communicate with a VolvoCE ME310 hard hat with an Adafruit
 * Feather 32u4 Bluefruit LE. Initialized with the activity application
 * context and the hard hat MAC address (optional).
 *
 * @author  Denny Delp
 * @version 1.0
 * @since   2016-05-10
 */
public class BluetoothHelper {
    // Make a singleton instance
    private static BluetoothHelper sInstance;

    private static final String TAG = "BluetoothHelper";
    private static final String defaultAddr = "F4:A4:9E:FC:CE:05"; // MAC address of new hard hat

    private static String connectAddr;

    BluetoothGattCallback gattCallback;
    BluetoothAdapter.LeScanCallback leScanCallback;
    BluetoothGatt gatt;
    BluetoothGattCharacteristic tx;
    BluetoothGattCharacteristic rx;

    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    Boolean autoConnectBoolean = false;
    Boolean writingFlag = false;
    Boolean connected = false;

    BluetoothAdapter bleAdapter = BluetoothAdapter.getDefaultAdapter();
    ConnectionListener connectionListener;

    // Call getInstance rather than regular constructor.. prevents memory leaks
    public static synchronized BluetoothHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BluetoothHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private BluetoothHelper(final Context context) {
        this(context, defaultAddr);
    }

    private BluetoothHelper(final Context context, final String destAddr) {
        connectAddr = destAddr;
        this.connectionListener = null;
        // Register the lescan
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d(TAG, "Found a device! " + device.getAddress());
                if (device.getAddress().equals(connectAddr)) {
                    // Found the device we are looking for
                    Log.d(TAG, "Found our device!");
                    stopScan();
                    gatt = device.connectGatt(context.getApplicationContext(), autoConnectBoolean, gattCallback);
                }
            }
        };

        // Register the gatt callback
        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                //connect, disconnect, error handling
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == BluetoothGatt.STATE_CONNECTED) {
                    //connected to device, start discovering services
                    if (gatt.discoverServices()) {
                        Log.d(TAG, "OnConnectionStateChanged - GATT discovered");
                    } else {
                        Log.d(TAG, "OnConnectionStateChanged - GATT NOT discovered");
                    }
                }
                else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "OnConnectionStateChanged - BLE Disconnected (Clean up)");
                    connected = false;
                    if(connectionListener != null) {
                        connectionListener.onConnectionChange(false);
                    }
                    disconnect(); //clean up connection
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                // Notify connection failure if service discovery failed
                if (status == BluetoothGatt.GATT_FAILURE) {
                    Log.d(TAG, "onServicesDiscovered - GATT failure");
                    return;
                }
                // Save reference to each UART characteristic, module level
                tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
                rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);
                if (!gatt.setCharacteristicNotification(rx, true)) {
                    Log.d(TAG, "onServicesDiscovered - setCharacteristicNotification failure");
                    return;
                }
                BluetoothGattDescriptor desc = rx.getDescriptor(DESCRIPTOR_UUID);
                if (desc == null) {
                    Log.d(TAG, "onServicesDiscovered - getDescriptor failure (RX -> DESCRIPTOR_UUID)");
                    return;
                }
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    Log.d(TAG, "onServicesDiscovered - COULDN'T WRITE DESCRIPTOR!!!");
                    return;
                }
                Log.i(TAG, "onServicesDiscovered - We made it!");
                connected = true;
                if(connectionListener != null) {
                    connectionListener.onConnectionChange(true);
                }
                //send a "connected" data pack
                //sendData("C");
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                // Receiving data
                super.onCharacteristicChanged(gatt, characteristic);
                Log.d(TAG,"onCharacteristicChanged - Got: " + characteristic.getStringValue(0));
                //do something with the data we got
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic,
                                              int status) {
                //write complete
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d(TAG,"onCharacteristicWrite - Write complete");
                if(status != BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG,"onCharacteristicWrite - ERROR");
                    //error handling
                }
                writingFlag = false;
            }
        };
    }

    /**
     * Start LE scan for our bluetooth module
     */
    public void startScan() {
        bleAdapter.startLeScan(leScanCallback);
    }

    /**
     * Start LE scan for our bluetooth module (given address)
     */
    public void startScan(String address) {
        connectAddr = address;
        bleAdapter.startLeScan(leScanCallback);
    }

    /**
     * Stop LE scan for our bluetooth module
     */
    public void stopScan() {
        bleAdapter.stopLeScan(leScanCallback);
    }

    /**
     * Send data via ble connection
     */
    public void sendData(String data) {
        if(connected) {
            Log.i(TAG, "Sending data: " + data);
            tx.setValue(data);
            writingFlag = true;
            gatt.writeCharacteristic(tx);
        }
    }

    public void delaySendData(String data, int delay) {

    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Disconnect the ble gatt
     */
    public void disconnect() {
        if(connected) {
            //sendData("D");
        }
        if(gatt != null) {
            gatt.disconnect();
            Log.i(TAG, "BLE Disconnected");
        }
        gatt = null;
        tx = null;
        rx = null;
    }

    /**
     * Interface definition for Icon select callback
     */
    public interface ConnectionListener {
        public void onConnectionChange(boolean connectionStatus);
    }

    /**
     * Assign the listener implementing events interface that will receive the events
     *
     * @param listener
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

}
