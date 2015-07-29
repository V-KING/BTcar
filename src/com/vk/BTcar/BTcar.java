/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vk.BTcar;

import java.lang.reflect.Field;

import com.example.android.BTcar.R;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BTcar extends Activity implements OnTouchListener,
		SensorEventListener, OnClickListener {
	// 定义系统的Sensor管理器
	SensorManager sensorManager;
	EditText etTxt1;

	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_COLOR_PANEL = 2;
	static final int REQUEST_ENABLE_BT = 3;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	private Button btn_change_mode;
	private Button btn_up;
	private Button btn_back;
	private Button btn_left;
	private Button btn_right;
	private Button btn_stop;
	
	//seekbar
	private SeekBar seekBar;
	protected int speed = 5;
	// send to colorPanel
	public static String address = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");
		// Set up the window layout
		setContentView(R.layout.color_panel);
		
		// liten buttons
		btn_change_mode = (Button)findViewById(R.id.changemode);
		btn_change_mode.setOnClickListener(this);
		btn_stop= (Button)findViewById(R.id.stop);
		btn_stop.setOnClickListener(this);
		
		// touch liten
		btn_up= (Button)findViewById(R.id.up);
		btn_back= (Button)findViewById(R.id.back);
		btn_left= (Button)findViewById(R.id.left);
		btn_right= (Button)findViewById(R.id.right);
		
		btn_up.setOnTouchListener(this);
		btn_back.setOnTouchListener(this);
		btn_left.setOnTouchListener(this);
		btn_right.setOnTouchListener(this);
		
		//seekbar
		seekBar = (SeekBar)findViewById(R.id.seekBar1);
		seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
		seekBar.setMax(10);
		seekBar.setProgress(5);
				
		

		// 获取程序界面上的文本框组件
		etTxt1 = (EditText) findViewById(R.id.txt1);
		// 获取系统的传感器管理服务
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		getBTadapter();
		ActionBar actionBar = getActionBar();
		// 是否显示应用程序图标，默认为true
		actionBar.setDisplayShowHomeEnabled(true);
		// 是否显示应用程序标题，默认为true
		actionBar.setDisplayShowTitleEnabled(true);
		/*
		 * 是否将应用程序图标转变成可点击的按钮，默认为false。
		 * 
		 * 如果设置了DisplayHomeAsUpEnabled为true，
		 * 
		 * 则该设置自动为 true。
		 */
		actionBar.setHomeButtonEnabled(true);
		/*
		 * 在应用程序图标的左边显示一个向左的箭头，
		 * 
		 * 并且将HomeButtonEnabled设为true。
		 * 
		 * 默认为false。
		 */
		actionBar.setDisplayHomeAsUpEnabled(false);

		forceShowOverflowMenu();
	}
	private OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			speed = progress;
			Log.i(TAG, "speed:" + speed);
		}
	}; 

	private void getBTadapter() {
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		// 为系统的加速度传感器注册监听器
//		sensorManager.registerListener(this,
//				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//				SensorManager.SENSOR_DELAY_GAME);
		if (D)
			Log.e(TAG, "+ ON RESUME +");
		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	/**
	 * 如果设备有物理菜单按键，需要将其屏蔽才能显示OverflowMenu
	 */
	private void forceShowOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		// 取消注册
		sensorManager.unregisterListener(this);
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	public void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					break;
				case BluetoothChatService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			address = data.getExtras().getString(
					DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			break;
		case REQUEST_COLOR_PANEL:
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.secure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

	private void sendToArduino(int x, int y) {
		Log.i(TAG, x + "," +y);
		String data = "<" + x + "|" + y + ">";
		mChatService.write(data.getBytes());
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.up:
			sendToArduino(-speed, -speed);
			break;
		case R.id.back:
			sendToArduino(speed, speed);
			break;
		case R.id.left:
			sendToArduino(-speed, speed);
			break;
		case R.id.right:
			sendToArduino(speed, -speed);
			break;

		default:
			break;
		}
		return false;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		StringBuilder sb = new StringBuilder();

		float[] gravity = event.values;
		final float alpha = (float) 0.8;
		// Isolate the force of gravity with the low-pass filter.
		gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
		sb.append("\nx:");
		sb.append(gravity[0]);
		sb.append("\ny:");
		sb.append(gravity[1]);
		etTxt1.setText(sb.toString());
		motorMove((int) gravity[0], (int) gravity[1]);
	}

	private void motorMove(int x, int y) {
		if ((x!= 0) || (y!= 0)) {
			if (y < -3) {
				sendToArduino(y, y);
				return;
			}
			if (y > 4) {
				sendToArduino(y, y);
				return;
			}
			if (x > 4) {
				sendToArduino(-x, x);
				return;
			}
			if (x < -4) {
				sendToArduino(-x, x);
				return;
			}
			
			Log.i(TAG, x+ "," + y);
		}
		else{
			sendToArduino(0, 0);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.changemode:
			changMode();
			break;
		case R.id.stop:
			sendToArduino(0, 0);
			break;
		default:
			break;
		}
	}

	final int BUTTON_MODE = 1;
	final int ACCELER_MODE = 2;
	int flag_mode = BUTTON_MODE;
	private void changMode() {
		// make acceler not work
		if (flag_mode == ACCELER_MODE) {
			sensorManager.unregisterListener(this);
			btn_up.setClickable(true);
			btn_back.setClickable(true);
			btn_left.setClickable(true);
			btn_right.setClickable(true);
			flag_mode = BUTTON_MODE; 
			Log.i(TAG , "buton");
			btn_change_mode.setText(R.string.change_to_button_mode);
			Toast.makeText(this, R.string.change_to_button_mode, Toast.LENGTH_SHORT).show();;
			btn_change_mode.setTextAppearance(BTcar.this, R.style.changemode_button);
			etTxt1.setVisibility(EditText.GONE);
			
			return;
		}
		//make acceler work
		if (flag_mode == BUTTON_MODE) {
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_GAME);
			btn_up.setClickable(false);
			btn_back.setClickable(false);
			btn_left.setClickable(false);
			btn_right.setClickable(false);
			flag_mode = ACCELER_MODE;
			Log.i(TAG , "acceler");
			btn_change_mode.setText(R.string.change_to_acceler_mode);
			Toast.makeText(this, R.string.change_to_acceler_mode, Toast.LENGTH_SHORT).show();;
			btn_change_mode.setTextAppearance(BTcar.this, R.style.changemode_acceler);
			etTxt1.setVisibility(EditText.VISIBLE);
			return;
		}
	}

}
