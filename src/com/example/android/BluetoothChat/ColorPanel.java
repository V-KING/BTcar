package com.example.android.BluetoothChat;


import com.vk.colorPanel.ColorPickerView;

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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ColorPanel extends Activity implements OnTouchListener,
		OnSeekBarChangeListener {
	public final static String TAG = "ColorPanel";
	public ColorPickerView colorPicker;
	private static final int blueStart = 100;
	TextView text1;
	private String address = "00:00:00:00:00:00";
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
	//Bluetooth about
	private BluetoothAdapter mbtAdapter = null;
    private BluetoothChatService mChatService = null;
    // info from BluetoothServerChat
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
	            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                case BluetoothChatService.STATE_CONNECTED:
	                    break;
	                case BluetoothChatService.STATE_CONNECTING:
	                    break;
	                case BluetoothChatService.STATE_LISTEN:
	                case BluetoothChatService.STATE_NONE:
	                    break;
	                }
	                break;
	            case MESSAGE_WRITE:
	                byte[] writeBuf = (byte[]) msg.obj;
	                // construct a string from the buffer
	                String writeMessage = new String(writeBuf);
	                break;
	            case MESSAGE_READ:
	                byte[] readBuf = (byte[]) msg.obj;
	                // construct a string from the valid bytes in the buffer
	                String readMessage = new String(readBuf, 0, msg.arg1);
	                break;
	            case MESSAGE_DEVICE_NAME:
	                // save the connected device's name
	                break;
	            case MESSAGE_TOAST:
	                break;
	            }
	        }
		};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.color_panel);
		// 初始化ColorPickerView
		initColorPickerView();
		// 设置要显示rgb的TextView
		initTextView();
		// 初始化seekBar
		initSeekBar();
		//获取地址
		address = getIntent().getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		Log.d(TAG, "accepted MAC: " + address);
		//获取本地蓝牙
		mbtAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
        if (mbtAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Initialize the BluetoothChatService to perform bluetooth connections
        if (mChatService == null) {
        	mChatService = new BluetoothChatService(this, mHandler);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume--");
		if (mChatService != null) {
			Log.d(TAG, "mchatserver--");
			
//			mChatService.start();
	        BluetoothDevice device = mbtAdapter.getRemoteDevice(address);
	        // Attempt to connect to the device
	        mChatService.connect(device, false);
		}
	}
	private void initSeekBar() {
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
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
	public boolean onTouch(View v, MotionEvent event) {
		int color = 0;
		color = colorPicker.getColor(event.getX(), event.getY(), true);
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
		// TODO Auto-generated method stub
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

	public boolean isDarkColor(int[] color) {
		if (color[0] * .3 + color[1] * .59 + color[2] * .11 > 150)
			return false;
		return true;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
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
		String s_r = String.format("%03d", r); 
		String s_g = String.format("%03d", g); 
		String s_b = String.format("%03d", b); 
		String data = s_r + s_g + s_b;
		
		mChatService.write(data.getBytes());
		Log.i(TAG, "send: "+ data);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}
	// generate a random hex color & display it
	public void randomColor(View v) {
		Log.i(TAG, "randomColor");
		int r = (int) (Math.random() * 255);
		int g = (int) (Math.random() * 255);
		int b = (int) (Math.random() * 255);
		colorPicker.setColor(r, g, b);
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
		seek.setProgress(b);
		
		String s_x = String.format("%03d", r); 
		String s_y = String.format("%03d", g); 
		String s_z = String.format("%03d", b); 
		String data = s_x + s_y + s_z;
		mChatService.write(data.getBytes());
		Log.i(TAG, "send: "+ data);
		
	}
}
