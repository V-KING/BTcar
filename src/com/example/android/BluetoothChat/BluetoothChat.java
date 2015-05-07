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
package com.example.android.BluetoothChat;

import java.lang.reflect.Field;

import com.vk.colorPanel.ColorPickerView;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity implements OnTouchListener,
		OnSeekBarChangeListener {
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

	private ColorPickerView colorPicker;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
	private TextView text1;
	private int blueStart = 100;

	// send to colorPanel
	public static String address = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D) Log.e(TAG, "+++ ON CREATE +++");
		// Set up the window layout
		setContentView(R.layout.color_panel);
		// 初始化ColorPickerView
		initColorPickerView();
		// 设置要显示rgb的TextView
		initTextView();
		// 初始化seekBar
		initSeekBar();
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
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

	private void initSeekBar() {
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
		int blueStart = 100;
		seek.setProgress(blueStart);
		seek.setMax(255);
		seek.setOnSeekBarChangeListener(this);
	}

	private void initTextView() {
		text1 = (TextView) findViewById(R.id.result1_textview);
		text1.setText("点取颜色");
	}

	private void initColorPickerView() {
		LinearLayout layout = (LinearLayout) findViewById(R.id.color_picker_layout);
		final int width = layout.getWidth();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		colorPicker = new ColorPickerView(this, blueStart, metrics.densityDpi);
		layout.setMinimumHeight(width);
		layout.addView(colorPicker);
		layout.setOnTouchListener(this);
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

		new StringBuffer("");
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

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		int amt = seekBar.getProgress();
		int col = colorPicker.updateShade(amt);
		Log.i(TAG, "" + col);
		sendToArduino(col);
		colorPicker.invalidate();
	}

	private void sendToArduino(int col) {
		int r = Color.red(col);
		int g = Color.green(col);
		int b = Color.blue(col);
		String data = String.format("%03d", r) + String.format("%03d", g)
				+ String.format("%03d", b);
		mChatService.write(data.getBytes());
		Log.i(TAG, "send: " + data);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int color = colorPicker.getColor(event.getX(), event.getY(), true);
		colorPicker.invalidate();
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);
		Log.i(TAG, r + "," + g + "," + b);

		updateTextAreas(color);
		sendToArduino(color);
		return true;
	}

	private void updateTextAreas(int col) {
		int[] colBits = { Color.red(col), Color.green(col), Color.blue(col) };
		// set the text & color backgrounds
		int r = Color.red(col);
		int g = Color.green(col);
		int b = Color.blue(col);
		text1.setText(r + "," + g + "," + b);
		text1.setBackgroundColor(col);

		if (isDarkColor(colBits)) {
			text1.setTextColor(Color.WHITE);
		} else {
			text1.setTextColor(Color.BLACK);
		}
	}

	private boolean isDarkColor(int[] color) {
		if (color[0] * .3 + color[1] * .59 + color[2] * .11 > 150)
			return false;
		return true;
	}
	public void randomColor(View v) {
		Log.i(TAG, "randomColor");
		int r = (int) (Math.random() * 255);
		int g = (int) (Math.random() * 255);
		int b = (int) (Math.random() * 255);
		colorPicker.setColor(r, g, b);
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
		seek.setProgress(b);
		String data = String.format("%03d", r) + String.format("%03d", g)
				+ String.format("%03d", b);
		mChatService.write(data.getBytes());
		Log.i(TAG, "send: " + data);
	}

}
