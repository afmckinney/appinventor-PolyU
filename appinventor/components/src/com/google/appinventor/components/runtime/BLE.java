package com.google.appinventor.components.runtime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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


/**
 * BLE provides scanning BLE device and connection
 *
 * By Tony Chan @ PolyU (kwong3513@yahoo.com.hk)
 */
@DesignerComponent(version = YaVersion.BLE_COMPONENT_VERSION,
   description = "Add it later",
   category = ComponentCategory.CONNECTIVITY,
   nonVisible = true,
   iconName = "images/ble.png")
@SimpleObject
@UsesPermissions(permissionNames =
				"android.permission.BLUETOOTH, " +
				"android.permission.BLUETOOTH_ADMIN")

public class BLE extends AndroidNonvisibleComponent implements Component{
	
  //-----------basic-------------------------
  private BluetoothAdapter mBluetoothAdapter;
  private final Activity activity;
  private BluetoothGatt mBluetoothGatt;
  private BluetoothGattService currentser;
  private BluetoothGattCharacteristic currentchar;
  private int device_rssi=0;
  private Handler uiThread;
  private int selectedIndex=0;
  private Runnable runScan;
  
  //----------list-------------------------
  private String deviceAddressList="";
  private List<BluetoothDevice> mLeDevices;
  private List<BluetoothGattService> mGattService;
  List<BluetoothGattCharacteristic> mGattChar;
  private HashMap<BluetoothDevice, Integer> mLeDevices_rssi;
  
  //---------status------------------------
  private static boolean STATE_CONNECTED = false;
  private static boolean STATE_CHARREAD = false;
  private static boolean STATE_SERREAD = false;
  
  /*//----------for future use---------------
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
  private String message="";*/
  
  //---------------Value------------------
  private int battery=0;
  private String temperature="";
  private byte []data;
  private byte[] value;
  private int find_me=0;
  private int set_find_me=0;
  private String test="";
  private String tempUnit="";
  int i=0;
  int j=0;
  
  
  
  /**
   * Later
   *
   * Later
   *
   * @param container container, component will be placed in
   */
  public BLE(ComponentContainer container) {
	  super(container.$form());
	  activity = container.$context();
	  mLeDevices=new ArrayList<BluetoothDevice>();
	  mGattService=new ArrayList<BluetoothGattService>();
	  //mChar=new ArrayList<BluetoothGattCharacteristic>();
	  final BluetoothManager bluetoothManager =(BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
      mBluetoothAdapter = bluetoothManager.getAdapter();
      mLeDevices_rssi=new HashMap<BluetoothDevice, Integer>();
      uiThread=new Handler();
  }

//-----------------------------block-------------------------------------------------------------
  /**
   * Later
   */
  @SimpleFunction
  public void ScanDevice() {
			  mBluetoothAdapter.startLeScan(mLeScanCallback);
	  //mBluetoothAdapter.startLeScan(mLeScanCallback);
  }
  
  @SimpleFunction
  public void StopScanning()
  {
	  mBluetoothAdapter.stopLeScan(mLeScanCallback);
  }
  
  /**
   * Later
   */
  @SimpleFunction
  public void ConnectDevice(int index) {
	  mBluetoothGatt=mLeDevices.get(index-1).connectGatt(activity, false, mGattCallback);
  }
  
  @SimpleFunction
  public void ReadFindMeValue() {
	  readChar(BLEList.FINDME_CHAR, BLEList.FINDME_SER);
  }
  
  @SimpleFunction
  public void SetFindMeValue(int set_find_me) {
	  if(set_find_me<=2&&set_find_me>=0)
	  {
		  writeChar(BLEList.FINDME_CHAR, BLEList.FINDME_SER, set_find_me, BluetoothGattCharacteristic.FORMAT_UINT8,0);
	  }
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String GetFindMeValue() {
	  if(STATE_CHARREAD)
	  {
		  return Integer.toString(find_me);
	  }
	  else
		  return "Cannot read Find Me Value";
  }
  
  @SimpleFunction
  public void ReadBatteryValue() {
	  readChar(BLEList.BATTERY_LEVEL_CHAR, BLEList.BATTERY_LEVEL_SER);
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String GetBatteryValue() {
	  if(STATE_CHARREAD)
	  {
		  return Integer.toString(battery);
	  }
	  else
		  return "Cannot read Battery Level";
  }
  
  @SimpleFunction
  public void ReadTemperature() {
	  readChar(BLEList.THERMOMETER_CHAR, BLEList.THERMOMETER_SER);
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String IsBLEConnected() {
	  if(STATE_CONNECTED)
		  return "True";
	  else
		  return "False";
  }
  
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String GetTemperature() {
	  test+=Boolean.toString(STATE_CHARREAD);
	  if(STATE_CHARREAD)
	  {
		  /*String TempUnit="";
		  String TempValue="";
		  String TempType="";
		  if(new Integer(data[0]).equals(new Integer(0))){
			  TempUnit="Celsius";
			  TempValue=Byte.toString(data[1])+Byte.toString(data[2])+"."+Byte.toString(data[3])+Byte.toString(data[4]);
		  }
		  else if(new Integer(data[0]).equals(new Integer(1))){
			  TempUnit="Fahrenheit";
			  TempValue=Byte.toString(data[1])+Byte.toString(data[2])+"."+Byte.toString(data[3])+Byte.toString(data[4]);
		  }
		  temperature = ("Temperature: "+TempValue+" "+TempUnit+"\n"
				  +"Temperature Type: "+TempType+"\n");*/
		  /*byte[] temp = new byte[4];
		  for (int i = 0; i < 4; i++)
			  temp[i] = value[i+1];*/
		  if((int)value[0] == 0)
		  {
			  tempUnit=" Celsius";
		  }
		  else
			  tempUnit=" Fahrenheit";
		  float mTemp = ((value[2] & 0xff) << 8) + (value[1] & 0xff);
		  return mTemp + tempUnit;
	  }
	  else{
		  return "Cannot read temperature";
	  }
	  
  }
  
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String Test() {
	  return test;
  }
  
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String DeviceList()
  {
	  deviceAddressList="";
	  //List<BluetoothDevice> sortList=new ArrayList<BluetoothDevice>();
	  //sortList=mLeDevices;
	  mLeDevices=sorting(mLeDevices);
	  if(!mLeDevices.isEmpty())
	  {
		  test+=("size:"+Integer.toString(mLeDevices.size()));
		  for(int i=0;i<mLeDevices.size();i++){
			  if(i!=(mLeDevices.size()-1))
				  deviceAddressList+=mLeDevices.get(i).toString()+" "+mLeDevices.get(i).getName()+" "+Integer.toString(mLeDevices_rssi.get(mLeDevices.get(i)))+",";
			  else
				  deviceAddressList+=mLeDevices.get(i).toString()+" "+mLeDevices.get(i).getName()+" "+Integer.toString(mLeDevices_rssi.get(mLeDevices.get(i)));
	        }
	  }
	  return deviceAddressList;
  }
  
  /*@SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public YailList DeviceList()
  {
	  List<BluetoothDevice> sortList=new ArrayList<BluetoothDevice>();
	  List<String> deviceList=new ArrayList<String>();
	  sortList=mLeDevices;
	  mLeDevices=sorting(sortList);
	  if(!mLeDevices.isEmpty())
	  {
		  test+=Integer.toString(mLeDevices.size());
		  for(int i=0;i<mLeDevices.size();i++){
			  String deviceInfo=mLeDevices.get(i).toString()+" "+mLeDevices.get(i).getName()+" "+Integer.toString(mLeDevices_rssi.get(mLeDevices.get(i)));
			  deviceList.add(deviceInfo);
	        }
	  }
	  return YailList.makeList(deviceList);
  }*/
  
  @SimpleFunction
  public void SelectedDevice(int i) {
	  if(i<mLeDevices.size())
	  {
		  selectedIndex=(i-1);
	  }
  }
  
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String SelectedDeviceRssi()
  {
	  return Integer.toString(mLeDevices_rssi.get(mLeDevices.get(selectedIndex)));
  }
  
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String SelectedDeviceName()
  {
	  return mLeDevices.get(selectedIndex).getName();
  }
  
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String SelectedDeviceAddress()
  {
	  return mLeDevices.get(selectedIndex).getAddress();
  }
  
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String ConnectedDeviceRssi()
  {
	  return Integer.toString(device_rssi);
  }
  

  @SimpleEvent(description = "")
  public void Connected() {
	  test+=" ConnectedCall ";
	  EventDispatcher.dispatchEvent(BLE.this, "Connected");
	  test+=" DoConnected ";
	  }
  
  @SimpleEvent(description = "")
  public void ValueChanged() {
	  uiThread.post(new Runnable(){
			@Override
			public void run() {
				test+=" runChanged ";
				EventDispatcher.dispatchEvent(BLE.this, "ValueChanged");
			}
		  });
	  }
  
  /*@SimpleEvent(description = "")
  public void RssiChanged() {
	  /*uiThread.postDelayed(new Runnable(){
		@Override
		public void run() {
			EventDispatcher.dispatchEvent(this, "RssiChanged");
		//}
		  
	  //}, 1000);
  }*/
  
  //have not tested
  @SimpleEvent(description = "")
  public void ValueRead() {
	  EventDispatcher.dispatchEvent(BLE.this, "ValueRead");
  }
  
  
  @SimpleEvent(description = "")
  public void ConnectedDeviceRssiChanged() {
	  uiThread.postDelayed(new Runnable(){
		@Override
		public void run() {
			EventDispatcher.dispatchEvent(BLE.this, "ConnectedDeviceRssiChanged");
			mBluetoothGatt.readRemoteRssi();
		}
		  
	  }, 1000);
  }
	  
	/*  Runnable runnable=new Runnable(){
		   @Override
		   public void run() {
			   uiThread.postDelayed(this, 2000);
		   } 
		};*/
  
  
  @SimpleEvent(description = "")
  public void DeviceFound() {
	  /*
	  EventDispatcher.dispatchEvent(BLE.this, "DeviceFound");
	  uiThread.post(new Runnable(){
			@Override
			public void run() {
					EventDispatcher.dispatchEvent(BLE.this, "DeviceFound");
			}
	  }
	  );*/
	  //test+=("count"+Integer.toString(j))+"\n";
	  //j++;
	  EventDispatcher.dispatchEvent(BLE.this, "DeviceFound");
  }
//-------------------------------------------------------------------------------------------------

//-------------------------function-----------------------------------------------------------------
  //sort the device list by RSSI*******
  private List<BluetoothDevice> sorting(List<BluetoothDevice> input)
  {
	  List<BluetoothDevice> deviceList=new ArrayList<BluetoothDevice>();
	  BluetoothDevice small;
	  for(int i=0;i<input.size();i++)
	  {
		  small=input.get(i);
		  if(mLeDevices_rssi.containsKey(small))
		  {
			  for(int j=0;j<input.size();j++)
			  {
				  BluetoothDevice bl2=input.get(j);
				  if(!small.equals(bl2))
				  {
					  if(mLeDevices_rssi.get(small)<=mLeDevices_rssi.get(bl2))
					  {
						  small=bl2;
					  }
				  }
			  }
			  deviceList.add(small);
			  input.remove(small);
		  }
	  }
	  input=deviceList;
	  return input;
  }
  
  
  //add device when scanning
   private void addDevice(BluetoothDevice device, int rssi) {
       if(!mLeDevices.contains(device)) {
    	   //test+="adding:"+device.toString()+"\n";
           mLeDevices.add(device);
           mLeDevices_rssi.put(device, rssi);
           //test+="foundNew"+"\n";
           DeviceFound();
       }else{
    	   //test+="updateNew"+device.toString()+"\n";
    	   mLeDevices_rssi.put(device, rssi);
       }
   }
   
   //write characteristic based on UUID
   private void writeChar(UUID char_uuid, UUID ser_uuid, int value, int format, int offset)
   {
	   if(STATE_SERREAD&&!mGattService.isEmpty())
	   {
		   for(int i=0;i<mGattService.size();i++)
		   {
			   if(mGattService.get(i).getUuid().equals(ser_uuid))
			   {
				   //test+=" serFound ";
				   mGattChar=mGattService.get(i).getCharacteristics();
				   for(int j=0;j<mGattChar.size();j++)
				   {
					   if(mGattChar.get(j).getUuid().equals(char_uuid))
					   {
						  // test+=" charFound ";
						   //test+=" Writting ";
						   mGattChar.get(j).setValue(value, format, offset);
						   mBluetoothGatt.writeCharacteristic(mGattChar.get(j));
					   }
				   }
			   }
		   }
	   }
   }
   
   //read characteristic based on UUID
   private void readChar(UUID char_uuid, UUID ser_uuid)
   {
	   if(STATE_SERREAD&&!mGattService.isEmpty())
	   {
		   for(int i=0;i<mGattService.size();i++)
		   {
			   if(mGattService.get(i).getUuid().equals(ser_uuid))
			   {
				   test+=" serFound ";
				   //mGattChar=mGattService.get(i).getCharacteristics();
				   //BluetoothGattCharacteristic currentChar=mGattChar.get(j);
				   BluetoothGattDescriptor desc=mGattService.get(i).getCharacteristic(char_uuid).getDescriptor(BLEList.THERMOMETER_DES);
				   boolean set=desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
				   boolean write=mBluetoothGatt.writeDescriptor(desc);
				   
				  boolean NotiChar=mBluetoothGatt.setCharacteristicNotification(mGattService.get(i).getCharacteristic(char_uuid), true);
				   boolean resultChar = mBluetoothGatt.readCharacteristic(mGattService.get(i).getCharacteristic(char_uuid));
				   test+=" Premiss:"+mGattService.get(i).getCharacteristic(char_uuid).PERMISSION_READ + " "+mGattService.get(i).getCharacteristic(char_uuid).getPermissions();
				   test+= " write:"+Boolean.toString(write)+" set:"+Boolean.toString(set)+" NotiChar:"+Boolean.toString(NotiChar)+" Char:"+Boolean.toString(resultChar)+" Pro:"+Integer.toString(mGattService.get(i).getCharacteristic(char_uuid).getProperties())+" Noti:"+Integer.toString(mGattService.get(i).getCharacteristic(char_uuid).PROPERTY_READ)
						   +" Pre:"+Integer.toString(mGattService.get(i).getCharacteristic(char_uuid).getPermissions())+" PreRead:"+Integer.toString(mGattService.get(i).getCharacteristic(char_uuid).PERMISSION_READ);
				   /*for(int j=0;j<mGattChar.size();j++)
				   {
					   
					   if(mGattChar.get(j).getUuid().equals(char_uuid))
					   {
						   //BluetoothGattDescriptor descriptor=mGattChar.get(j).getDescriptor(BLEList.THERMOMETER_DES);
						   //boolean resultDes2 = mBluetoothGatt.readDescriptor(descriptor);
						   //byte[]value={Byte.parseByte("3"),Byte.parseByte("0")};
						   //descriptor.setValue(value);
						   //boolean resultDes = mBluetoothGatt.writeDescriptor(descriptor);
						   BluetoothGattCharacteristic currentChar=mGattChar.get(j);
						   boolean NotiChar=mBluetoothGatt.setCharacteristicNotification(currentChar, true);
						   boolean resultChar = mBluetoothGatt.readCharacteristic(currentChar);
						   test+=" Premiss:"+currentChar.PERMISSION_READ + " "+currentChar.getPermissions();
						   test+= " NotiChar:"+Boolean.toString(NotiChar)+" Char:"+Boolean.toString(resultChar)+" Pro:"+Integer.toString(currentChar.getProperties())+" Noti:"+Integer.toString(currentChar.PROPERTY_READ)
								   +" Pre:"+Integer.toString(currentChar.getPermissions())+" PreRead:"+Integer.toString(currentChar.PERMISSION_READ);
					   }
				   }*/
			   }
		   }
	   }
   }
   
   protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
   private static String bytesToHex (byte[] bytes) {
	   char[] hexChars = new char [bytes.length*2];
	   for(int j = 0; j < bytes.length; j++) {
		   int v = bytes[j] & 0xff;
		   hexChars[j*2] = hexArray[v >>> 4];
		   hexChars[j*2 + 1] = hexArray[v & 0x0F];
	   }
	   return new String(hexChars);
   }
//--------------------------------------------------------------------------------------------
   
//-----------------------------------callback-------------------------------------------------
  //scan callback
  private BluetoothAdapter.LeScanCallback mLeScanCallback =
          new BluetoothAdapter.LeScanCallback() {
      @Override
      public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
    	  //test+="Scan:"+device.toString()+"\n";
    	  addDevice(device, rssi);
    	  /*activity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  addDevice(device, rssi);
              }
          });*/
    	  /*runScan = new Runnable()
    	  {
    		  @Override
    		  public void run()
    		  {
    			  addDevice(device, rssi);
    		  }
    	  };
    	  uiThread.postDelayed(runScan, 1000);*/
      }
  };
  
  
  // for connect***********************
 private final BluetoothGattCallback mGattCallback =
          new BluetoothGattCallback() {
      @Override
      public void onConnectionStateChange(BluetoothGatt gatts, int status,
              int newState) {
          if (newState == BluetoothProfile.STATE_CONNECTED) {
        	  STATE_CONNECTED=true;
        	  test+="connected";
        	  mBluetoothGatt.discoverServices();
        	  //mBluetoothGatt.readRemoteRssi();
        	  test+=" nextIsConnected ";
        	  Connected();
          }
      }
      
      @Override
      // New services discovered
      public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    	  if (status == BluetoothGatt.GATT_SUCCESS) {
    		  mGattService= (ArrayList<BluetoothGattService>) gatt.getServices();
    		  test+=" SerGET ";
    		  STATE_SERREAD=true;
          }
      }

      @Override
      // Result of a characteristic read operation
      public void onCharacteristicRead(BluetoothGatt gatt,
              BluetoothGattCharacteristic characteristic,
              int status) {
    	  test+=" afterConnected "+Integer.toString(status)+" ";
    	  if (status == BluetoothGatt.GATT_SUCCESS) {
    		  test+=" tryToRead ";
    		  if(characteristic.getUuid().equals(BLEList.BATTERY_LEVEL_CHAR))
    		  {
	    		  battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
	              STATE_CHARREAD=true;
	              ValueRead();
    		  }
    		  else if(characteristic.getUuid().equals(BLEList.THERMOMETER_CHAR))
    		  {
    			  test+=" Read ";
    			  value = characteristic.getValue();
    			  temperature = " char:"+Byte.toString(value[0])+Byte.toString(value[1])+Byte.toString(value[2])+Byte.toString(value[3])+Byte.toString(value[4]);
	              STATE_CHARREAD=true;
	              ValueRead();
    		  }
    		  else if(characteristic.getUuid().equals(BLEList.FINDME_CHAR))
    		  {
    			  find_me = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                  STATE_CHARREAD=true;
                  ValueRead();
    		  }
    		  //else get the value[].. add it later
    	  }
    	  //else show am message
      }
      
      @Override
      public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
      {
    	  test+=" valueChanged "+characteristic.getUuid().toString();
    	  if(characteristic.getUuid().equals(BLEList.BATTERY_LEVEL_CHAR))
		  {
    		  battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
              STATE_CHARREAD=true;
              ValueChanged();
		  }
		  else if(characteristic.getUuid().equals(BLEList.THERMOMETER_CHAR))
		  {
			  test+=" tempChanged ";
			  value = characteristic.getValue();
			  STATE_CHARREAD=true;
              ValueChanged();
		  }
		  else if(characteristic.getUuid().equals(BLEList.FINDME_CHAR))
		  {
			  find_me = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
              STATE_CHARREAD=true;
              ValueChanged();
		  }
      }
      
      @Override
      //set value of characteristic
      public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
      {
    	  test+=" Successful ";
      }
      
      @Override
      public void onDescriptorRead (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
      {
    	  byte[]value = descriptor.getValue();
    	  temperature+=("desValue:"+Byte.toString(value[0])+Byte.toString(value[1]));  
      }
      
      @Override
      public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    	  test+=" Successful ";
      }
      
      @Override
      //get the RSSI
      public void onReadRemoteRssi (BluetoothGatt gatt, int rssi, int status)
      {
    		  device_rssi=rssi;
    		  ConnectedDeviceRssiChanged();
    	  
      }
  };
}
//-------------------------------------------------------------------------------------

