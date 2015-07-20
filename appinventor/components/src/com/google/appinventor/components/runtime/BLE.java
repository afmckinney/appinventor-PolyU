package com.google.appinventor.components.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;


/**
 * BLE provides scanning BLE device and connection
 *
 * By Tony Chan & Bain ZHANG @ PolyU (kwong3513@yahoo.com.hk & 12131354d@connect.polyu.hk)
 */

@DesignerComponent(version = YaVersion.BLE_COMPONENT_VERSION,
description = "This is a trial version of BLE component, blocks need to be specified later",
category = ComponentCategory.CONNECTIVITY,
nonVisible = true,
iconName = "images/ble.png")
@SimpleObject
@UsesPermissions(permissionNames =
"android.permission.BLUETOOTH, " +
"android.permission.BLUETOOTH_ADMIN")

public class BLE extends AndroidNonvisibleComponent implements Component {

    /**
     * Basic Variable
     */
    private BluetoothAdapter mBluetoothAdapter;
    private final Activity activity;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService currentService;
    private BluetoothGattCharacteristic currentChar;
    private Runnable runScan;
    private int device_rssi=0;
    private final Handler uiThread;
    private int selectedIndex=0;
    private int time=1000;
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * BLE Info List
     */
    private String deviceInfoList="";
    private List<BluetoothDevice> mLeDevices;
    private List<BluetoothGattService> mGattService;
    private HashMap<BluetoothDevice, Integer> mLeDevices_rssi;

    /**
     * BLE Device Status
     */
    private static boolean STATE_CONNECTED = false;
    private static boolean STATE_CHARACTERISTICREAD = false;
    private static boolean STATE_CHARACTERISTICWRITE = false;
    private static boolean STATE_SERVICEREAD = false;

    /**
     * For Furture Developement
     public final static String ACTION_GATT_CONNECTED =
     "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
     public final static String ACTION_GATT_DISCONNECTED =
     "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
     public final static String ACTION_GATT_SERVICES_DISCOVERED =
     "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
     public final static String ACTION_DATA_AVAILABLE =
     "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
     public final static String EXTRA_DATA =
     "com.example.bluetooth.le.EXTRA_DATA";
     private BluetoothGatt mBluetoothGatt;
     private String message="";
     */

    /**
     * GATT value
     */
    private int battery = 0;
    private String temperature = "";
    private int findMe = 0;
    private int setFindMe = 0;



    /**
     * Later
     * @param container, component will be placed in
     */
    public BLE(ComponentContainer container) {
        super(container.$form());
        activity = container.$context();
        mLeDevices = new ArrayList<BluetoothDevice>();
        mGattService = new ArrayList<BluetoothGattService>();
        //mChar=new ArrayList<BluetoothGattCharacteristic>();
        final BluetoothManager bluetoothManager = (BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mLeDevices_rssi = new HashMap<BluetoothDevice, Integer>();
        uiThread = new Handler();
    }

    @SimpleFunction
    public void ScanDeviceStart() {
        runScan = new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                uiThread.postDelayed(runScan, time);
            }
        };
        //mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    @SimpleFunction
    public void ScanDeviceStop() {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        uiThread.removeCallbacks(runScan);
    }

    @SimpleFunction
    public void ConnectToDevice(int index) {
        mBluetoothGatt=mLeDevices.get(index-1).connectGatt(activity, false, mGattCallback);
    }

    @SimpleFunction
    public void ReadFindMeValue() {
        readChar(BLEList.FINDME_CHAR, BLEList.FINDME_SER);
    }

    @SimpleFunction
    public void WriteFindMeValue(int value) {
        if (value <= 2 && value >= 0) {
            setFindMe=value;
            writeChar(BLEList.FINDME_CHAR, BLEList.FINDME_CHAR, value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        }
    }

    @SimpleFunction
    public void ReadBatteryValue() {
        readChar(BLEList.BATTERY_LEVEL_CHAR, BLEList.BATTERY_LEVEL_SER);
    }

    @SimpleFunction
    public void ReadTemperatureValue() {
        readChar(BLEList.THERMOMETER_CHAR, BLEList.THERMOMETER_SER);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String FindMeValue() {
        if(STATE_CHARACTERISTICREAD) {
            return Integer.toString(findMe);
        } else {
            return "Cannot read Find Me Value";
        }
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String BatteryValue() {
        if(STATE_CHARACTERISTICREAD) {
            return Integer.toString(battery);
        } else {
            return "Cannot read Battery Level";
        }
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String TemperatureValue() {
        if(STATE_CHARACTERISTICREAD) {
            return temperature;
        } else {
            return "Cannot read Temperature";
        }
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public boolean IsDeviceConnected() {
        if(STATE_CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String DeviceList() {
        deviceInfoList="";
        List<BluetoothDevice> sortList = new ArrayList<BluetoothDevice>();
        sortList = mLeDevices;
        mLeDevices = sortDeviceList(sortList);
        if(!mLeDevices.isEmpty()) {
            for(int i = 0;i < mLeDevices.size();i++){
                if(i != (mLeDevices.size() - 1))
                    deviceInfoList += mLeDevices.get(i).toString() + " " +
                    mLeDevices.get(i).getName() + " " + Integer.toString(mLeDevices_rssi.get(mLeDevices.get(i))) + ",";
                else
                    deviceInfoList += mLeDevices.get(i).toString() + " " +
                    mLeDevices.get(i).getName() + " " + Integer.toString(mLeDevices_rssi.get(mLeDevices.get(i)));;
            }
        }
        return deviceInfoList;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String SelectedDeviceRssi() {
        return Integer.toString(mLeDevices_rssi.get(mLeDevices.get(selectedIndex)));
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String SelectedDeviceName() {
        return mLeDevices.get(selectedIndex).getName();
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String ConnectedDeviceRssi() {
        return Integer.toString(device_rssi);
    }


    @SimpleEvent(description = "")
    public void GetConnected() {
        EventDispatcher.dispatchEvent(this, "Connected");
    }

    @SimpleEvent(description = "")
    public void ValueChanged() {
        EventDispatcher.dispatchEvent(this, "ValueChanged");
    }

    @SimpleEvent(description = "")
    public void RssiChanged() {
        EventDispatcher.dispatchEvent(this, "RssiChanged");
    }

    @SimpleEvent(description = "")
    public void DeviceFound() {
        EventDispatcher.dispatchEvent(this, "DeviceFound");
    }

    @SimpleEvent(description = "") // have not tested yet
    public void ValueRead() {
        EventDispatcher.dispatchEvent(this, "ValueRead");
    }

    /**
     * Functions
     */
    //sort the device list by RSSI
    private List<BluetoothDevice> sortDeviceList(List<BluetoothDevice> deviceList) {
        /*List<BluetoothDevice> deviceList=new ArrayList<BluetoothDevice>();
        BluetoothDevice small;
        for(int i=0;i<input.size();i++) {
            small=input.get(i);
            if(mLeDevices_rssi.containsKey(small)) {
                for(int j=0;j<input.size();j++) {
                    BluetoothDevice bl2=input.get(j);
                    if(!small.equals(bl2)) {
                        if(mLeDevices_rssi.get(small)<=mLeDevices_rssi.get(bl2)) {
                            small=bl2;
                        }
                    }
                }
                deviceList.add(small);
                input.remove(small);
            }
        }
        return deviceList;*/

        Collections.sort(deviceList, new Comparator<BluetoothDevice>() {
            @Override
            public int compare(BluetoothDevice device1, BluetoothDevice device2) {
                int result = mLeDevices_rssi.get(device1) - mLeDevices_rssi.get(device2);
                return result;
            }
        });

        Collections.reverse(deviceList);
        return deviceList;
    }

    //add device when scanning
    private void addDevice(BluetoothDevice device, int rssi) {
        if(!mLeDevices.contains(device)) {
            mLeDevices.add(device);
            mLeDevices_rssi.put(device, rssi);
            DeviceFound();
        }
        mLeDevices_rssi.put(device, rssi);
        RssiChanged();
    }

    //read characteristic based on UUID
    private void readChar(UUID char_uuid, UUID ser_uuid) {

        if(STATE_SERVICEREAD && !mGattService.isEmpty()) {
            for(int i = 0;i < mGattService.size();i++) {
                BluetoothGattService check=mGattService.get(i);
                if(check.getUuid().equals(ser_uuid)) {
                    currentService=check;

                    BluetoothGattDescriptor desc = mGattService.get(i).getCharacteristic(char_uuid).getDescriptor(BLEList.THERMOMETER_DES);
                    boolean set=desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    boolean write=mBluetoothGatt.writeDescriptor(desc);

                    currentChar=currentService.getCharacteristic(char_uuid);
                    mBluetoothGatt.setCharacteristicNotification(mGattService.get(i).getCharacteristic(char_uuid), true);
                    mBluetoothGatt.readCharacteristic(currentChar);
                }
            }
        }
    }

    //Write characteristic based on uuid
    private void writeChar(UUID char_uuid, UUID ser_uuid, int value, int dataFormat, int offset) {
        if(STATE_SERVICEREAD && !mGattService.isEmpty()) {
            for(int i = 0;i < mGattService.size();i++) {
                BluetoothGattService check=mGattService.get(i);
                if(check.getUuid().equals(ser_uuid)) {
                    currentService=check;
                    currentChar=currentService.getCharacteristic(char_uuid);
                    currentChar.setValue(value, dataFormat, offset);
                    mBluetoothGatt.writeCharacteristic(currentChar);
                }
            }
        }
    }

    //-----------------------------------callback-------------------------------------------------
    //scan callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addDevice(device, rssi);
                }
            });
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                STATE_CONNECTED=true;
                mBluetoothGatt.discoverServices();
                mBluetoothGatt.readRemoteRssi();
                GetConnected();
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGattService= (ArrayList<BluetoothGattService>) gatt.getServices();
                STATE_SERVICEREAD=true;
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(characteristic.getUuid().equals(BLEList.BATTERY_LEVEL_CHAR)) {
                    battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    STATE_CHARACTERISTICREAD=true;
                    ValueRead();
                } else if(characteristic.getUuid().equals(BLEList.THERMOMETER_CHAR)) {
                    byte[]value = characteristic.getValue();
                    temperature = Byte.toString(value[1])+Byte.toString(value[2])+Byte.toString(value[3])+Byte.toString(value[4]);
                    STATE_CHARACTERISTICREAD=true;
                    ValueRead();
                } else if(characteristic.getUuid().equals(BLEList.FINDME_CHAR)) {
                    findMe = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    STATE_CHARACTERISTICREAD=true;
                    ValueRead();
                }
            }
        }

        @Override
        // Result of a characteristic read operation is changed
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(characteristic.getUuid().equals(BLEList.BATTERY_LEVEL_CHAR)) {
                battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                STATE_CHARACTERISTICREAD=true;
                ValueChanged();
            } else if(characteristic.getUuid().equals(BLEList.THERMOMETER_CHAR)) {
                byte[]value = characteristic.getValue();
                temperature = Byte.toString(value[1])+Byte.toString(value[2])+Byte.toString(value[3])+Byte.toString(value[4]);
                STATE_CHARACTERISTICREAD=true;
                ValueChanged();
            } else if(characteristic.getUuid().equals(BLEList.FINDME_CHAR)) {
                findMe = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                STATE_CHARACTERISTICREAD=true;
                ValueChanged();
            }
        }

        @Override
        //set value of characteristic
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(characteristic.getUuid().equals(BLEList.FINDME_CHAR)) {
            characteristic.setValue(setFindMe, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            }
        }

        @Override
        //get the RSSI
        public void onReadRemoteRssi (BluetoothGatt gatt, int rssi, int status) {
            device_rssi=rssi;
            RssiChanged();
        }
    };
}
