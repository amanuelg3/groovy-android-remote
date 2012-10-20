/**
 * @file MouseKeysRemote.java
 * @brief 
 *
 * Copyright (C)2010-2012 Magnus Uppman <magnus.uppman@gmail.com>
 * License: GPLv3+
 */

/**
 * Changelog:
 * 20101219	v.1.1 
 * Bugfix. New keys don't have a valid state when changing rows and cols.
 * Bugfix. Settings screen isn't scrollable.
 * Added key N/C for the possibility of dummy keys.
 * 20101229 v.1.2
 * Bugfix: Mouse wheel buttons didn't work.  
 * 2011-01-07 v.1.3
 * Added mouseclick on tap
 * "Send text" now always send a CR/LF at the end.
 *  Reduced mousewheel sensitivity
 * Fixed back button
 * Added mouse wheel on touchpad
 * Changed postInvalidate to invalidate in keypad+
 * 2011-01-17 v1.4
 * Added password
 * Changed icon
 * 2011-03-??
 * Changed name to MouseKeys..
 * 2011-03-29 v1.04
 * Removed transparency for buttons. Reverted to black background.
 * 
 * Changed name to MouseKeysRemote
 * 2011-05-20 v1.01
 * DEFAULT_TOUCH_TIME = 200
 * Added sticky keys
 * 
 * 2011-07-24 v1.03
 * Keyboard is now displayed automatically
 * Layouts can be named.
 * Increased number of layouts to 16.
 * 
 * Notes:
 * Special characters and zoom:
 * Must enable Compose and Meta key in KDE.
 * System settings -> Keyboard layout -> Advanced
 * Free version exactly the same except only one layout. 
 * DEFAULT_TOUCH_TIME = 100
 * 
 *  2011-08-17 v1.03.1
 *  Better sensitivity control to avoid jumping mouse.
 *  2011-09-02 v1.03.2
 *  Further improvements to mouse and mousewheel sensitivity control.
 *  Each layout can now be set to portrait, landscape or auto-rotate.
 *  Display can be set to always on per layout.
 *  Wifi is now never disabled when the app is running.
 *  Bugfix where pinch to zoom was wrongly activated when pressing a button after moving the mouse.
 *  Haptic feedback is now configurable per layout. 
 *  2011-09-03 v1.03.3
 *  Bugfix where tap on touch didn't work on some devices.
 *  2011-09-11 v1.03.4
 *  Added support for install to SD-card.
 *  Language setting is now sent to the server on resume.
 *  Added sensor-assisted cursor movement.
 *  2011-09-13 v1.03.5
 *  Reduced mouse sensitivity when a finger is lifted to avoid jumping mouse.
 *  2011-09-24 v1.03.6
 *  Improved mouse accuracy.
 *  More sensor features.
 *  2011-09-27 v1.03.7
 *  Added North, South etc.. keys which maps to the cursor keys. The key North-W maps for example to both cursor up and cursor left. 
 *  Reduced the size of the hit-rectangle on keys to better match the actual graphics.
 *  2011-10-17 v1.03.8
 *  Added checkbox for enable/disable click on tap.
 *  2012-01-10 v1.03.9
 *  Button colors can be changed as a separate edit mode.
 *  Background and text colors can be changed (from settings).
 *  2012-01-27 v1.03.10
 *  Fix for password and keyboard bug. Thanks to "Carl" for the bug report!
 *  Increased text size.
 *  2012-02-20 v.104.1
 *  Added checkbox for enable/disable pinch zoom.
 *  Increased mouse sensitivity by removing the 25ms deadzone before moving the mouse.
 *  Minor color changes to the color picker. 
 *  2012-10-10 2.0 (Banbury)
 *  Major rewrite.
 */

package com.linuxfunkar.mousekeysremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import yuku.ambilwarna.AmbilWarnaDialog;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MouseKeysRemote extends Activity implements
		ColorPickerDialog.OnColorChangedListener, OnClickListener,
		OnItemClickListener, OnItemSelectedListener, SensorEventListener {
	private static final String TAG = "MouseKeysRemote";

	private static final boolean debug = false;
	// private static final boolean debug=true;

	private SensorManager mSensorManager;
	private PowerManager mPowerManager;
	private WindowManager mWindowManager;
	private Display mDisplay;
	private WakeLock mWakeLock;
	private Sensor mAccelerometer;
	private WifiManager mWifiManager;

	public static final int KEYPAD_STATE_COMPAT = 0;
	public static final int KEYPAD_STATE_TOUCH = 1;

	public static final int KEY_STATE_DOWN = 0;
	public static final int KEY_STATE_UP = 1;
	public static final int KEY_STATE_DOWN_PENDING_UP = 2;
	public static final int KEY_STATE_DOWN_PENDING_CONFIRMED_DOWN = 3;

	public static final int MOUSE_NO_GESTURE = 0;
	public static final int MOUSE_AWAITING_GESTURE = 1;
	public static final int MOUSE_ZOOM = 2;

	private static final int MENU_QUIT = 1;
	private static final int MENU_MORE = 2;
	private static final int MENU_LAYOUT = 3;
	private static final int MENU_MAIN = 4;
	private static final int MENU_EDIT = 5;
	private static final int MENU_KP_TOUCH = 6;
	private static final int MENU_STICK = 7;
	private static final int MENU_UNSTICK = 8;
	private static final int MENU_ABOUT = 9;
	private static final int MENU_CALIBRATE = 10;
	private static final int MENU_COLOR = 11;
	private static final int MENU_UNHIDE = 12;

	private enum ColorElement {
		Text(0), Back(1), MousePad(2), MouseWheel(3), Button(4);

		private final int value;

		private ColorElement(int value) {
			this.value = value;
		}

		public int toInt() {
			return value;
		}
	}

	private static final int SENSORS_MOUSE = 0;
	private static final int SENSORS_CURSOR = 1;
	private static final int SENSORS_MOUSE_GAME = 2;

	private static final int MAX_KEYS = 200;

	private static final int DEFAULT_NUM_ROWS = 5;
	private static final int DEFAULT_NUM_COLS = 4;

	private static final int DEFAULT_MP_SIZE = 50;

	private static final int DEFAULT_LAYOUTS = 32;

	private static final int DEFAULT_MOUSE_ACC = 2;
	private static final float DEFAULT_TEXT_SIZE = 1.2f;

	private static final int DEFAULT_LANGUAGE = 1; // English (0 = Custom, for
													// more languages update
													// lang_array in
													// strings.xml)

	public static final int STUCK_KEY = 0;
	public static final int UNSTUCK_KEY = 1;
	public static final int UNSTUCK_KEY_PENDING_RELEASE = 2;

	// public static final int NO_EDIT = 0;
	// public static final int KEY_BINDING_EDIT = 1;
	// public static final int KEY_NAME_EDIT = 2;
	// public static final int KEY_COL_EDIT = 3;

	public enum EditMode {
		None, Binding, Name, Color, Sticky
	}

	public static final int DEFAULT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;
	// public static final int DEFAULT_SENSOR_DELAY =
	// SensorManager.SENSOR_DELAY_GAME;
	// public static final int DEFAULT_SENSOR_DELAY =
	// SensorManager.SENSOR_DELAY_NORMAL;

	public static final float DEFAULT_SENSOR_SENSITIVTY = 1f;

	private EditMode edit = EditMode.None;
	private boolean sticky = false;

	public String passwd; // = new String("");

	public String host;

	static public int lang_pos;
	static public int mouse_speed_pos;
	static public int layout_mode_pos;

	static public boolean calibrate = false;

	static public int keys_layout;
	static public int key_to_edit;

	TextView heading;

	EditText ip;
	Button send;

	Spinner spinner;
	Spinner mouse_spinner;
	Spinner layout_mode_spinner;

	LinearLayout layout_action;

	ListView choose_action;

	TextView action_text;

	LinearLayout layout_select;

	ListView choose_layout;

	TextView layout_text;

	EditText key_name;

	float mLastTouchX = -1;
	float mLastTouchY = -1;

	float mPosX = -1;
	float mPosY = -1;

	int accX = -1;
	float accXFloat = -1;
	float xPosLast;

	int accY = -1;
	float accYFloat = -1;
	float yPosLast;

	float mouseMovePressure;
	float currentMousePressure;
	long down;
	long up;
	long last_up = 0;

	int action;
	int xPos = -1;
	int yPos = -1;
	float xPosFloat = -1;
	float yPosFloat = -1;
	int pointerIndex = -1;
	int pointerCnt = 1;

	float calibrate_x = 0;
	float calibrate_y = 0;

	CustomDrawableView keyPadView;

	static public int[] keyState; // key current state (up/down)
	static public int[] x1Pos;
	static public int[] y1Pos;
	static public int[] x2Pos;
	static public int[] y2Pos;

	static public int numberOfKeyRows = DEFAULT_NUM_ROWS;
	static public int numberOfKeyCols = DEFAULT_NUM_COLS;
	static public int mousepadRelativeSize = DEFAULT_MP_SIZE;
	static public float textSize = DEFAULT_TEXT_SIZE;

	static public int xOffsetLeft = 0;
	static public int xOffsetRight = 0;
	static public int yOffsetTop = 0;
	static public int yOffsetBottom = 0; // Will be automatically calculated

	static int[] key_actions; // Available actions
	static int number_of_keycodes; // Number of currently defined key bindings

	public String[] EDIT_Names; // Keys
	public String[] LAYOUT_Names = { // Layouts
	"Layout 1", "Layout 2", "Layout 3", "Layout 4", "Layout 5", "Layout 6",
			"Layout 7", "Layout 8", "Layout 9", "Layout 10", "Layout 11",
			"Layout 12", "Layout 13", "Layout 14", "Layout 15", "Layout 16" };

	public String[] LAYOUT_Names3 = new String[DEFAULT_LAYOUTS];

	static public boolean enableMouseWheel;
	public static int mouseWheelWidth = 0;

	static public boolean enableVibrate;
	static public boolean enableMousePad;
	static public boolean enableAlwaysOn;
	static public boolean enableSensors;
	static public boolean enableSensorsX;
	static public boolean enableSensorsY;
	static public boolean enableClickOnTap;
	static public boolean enablePinchZoom;

	static public WrapMotionEvent ev;

	NotificationManager nm;
	Notification vibrateNotification;

	static public SharedPreferences mySharedPreferences;

	static public String keys_layout_name;

	private int sensors_mode;

	float oldDist = 1f;
	int port;

	int old_zoom;

	float xPosFirstFloat;
	float yPosFirstFloat;
	float accXFloatOld;
	float accYFloatOld;
	float accXFloatDiff;
	float accYFloatDiff;
	int mouseMovePointer;

	float sensorRotateLeft = DEFAULT_SENSOR_SENSITIVTY;
	float sensorRotateRight = -DEFAULT_SENSOR_SENSITIVTY;
	float sensorRotateForward = DEFAULT_SENSOR_SENSITIVTY;
	float sensorRotateBack = -DEFAULT_SENSOR_SENSITIVTY;
	// float sensorOffsetZ = 4f;
	float sensorOffsetY = 0f;
	float sensorHysteris = 0.2f;

	public static final int ROTATE_X_NONE = 0;
	public static final int ROTATE_X_LEFT = 1;
	public static final int ROTATE_X_RIGHT = 2;

	public static final int ROTATE_Z_NONE = 0;
	public static final int ROTATE_Z_FORWARD = 1;
	public static final int ROTATE_Z_BACK = 2;

	public static final int ROTATE_Y_NONE = 0;
	public static final int ROTATE_Y_FORWARD = 1;
	public static final int ROTATE_Y_BACK = 2;

	int sensorStateX = ROTATE_X_NONE;
	int sensorStateZ = ROTATE_Z_NONE;
	int sensorStateY = ROTATE_Y_NONE;

	float mSensorX = 0;
	float mSensorY = 0;
	float mSensorZ = 0;

	PowerManager.WakeLock wl;
	private WifiLock wifiLock = null;

	static public int keyWidth = 0;
	static public int keyHeight = 0;

	private View mousePanel;
	private GestureDetector mouseDetector;
	private ScaleGestureDetector zoomDetector;
	private View mouseWheelPanel;
	private GestureDetector mouseWheelDetector;
	private ButtonView buttonView;

	public static Paint mPaint;
	// private MaskFilter mEmboss;
	// private MaskFilter mBlur;

	public static int textCol;
	public static int backCol;
	public static int mousepadCol;
	public static int mousewheelCol;

	private ServiceConnection pingServiceConnection;
	private Security security;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			security = new Security(Preferences.getInstance(this).getPassword());
		} catch (Exception ex) {
			debug(ex.toString());
		}

		// Should remember all button states?

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(0xFFFF0000);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(12);

		// mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 },
		// 0.4f, 6, 3.5f);

		// mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);

		last_up = 0;
		// try {
		// Get an instance of the SensorManager
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// Get an instance of the PowerManager
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

		// Create a bright wake lock
		wl = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, getClass().getName());
		// wl.setReferenceCounted(false);

		// Get an instance of the WindowManager
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay();

		mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

		// Create a Wifi wakelock
		wifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
				"MyWifiLock");

		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}

		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		vibrateNotification = new Notification();
		vibrateNotification.vibrate = new long[] { 0, 40 };

		mySharedPreferences = getSharedPreferences("MY_PREFS",
				Activity.MODE_PRIVATE);

		keyState = new int[MAX_KEYS];
		x1Pos = new int[MAX_KEYS];
		y1Pos = new int[MAX_KEYS];
		x2Pos = new int[MAX_KEYS];
		y2Pos = new int[MAX_KEYS];

		for (int i = 0; i < MAX_KEYS; i++) {
			keyState[i] = KEY_STATE_UP;
		}

		// Store available actions. Used when editing layouts.
		number_of_keycodes = storeActions();

		EDIT_Names = new String[number_of_keycodes];

		for (int i = 0; i < number_of_keycodes; i++) {
			// debug(getActionName(key_actions[i]));
			EDIT_Names[i] = Constants.getActionName(key_actions[i]);
		}

		reload_settings();

		debug("onCreate - layout_mode_pos: " + layout_mode_pos);

		setDefaultKeys();

		layout_action = new LinearLayout(this);
		layout_action.setOrientation(LinearLayout.VERTICAL);
		action_text = new TextView(this);
		action_text.setText("Choose action:");

		choose_action = new ListView(this);
		choose_action.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, EDIT_Names);
		choose_action.setClickable(true);
		choose_action.setOnItemClickListener(this);

		choose_action.setAdapter(adapter);
		choose_action.setTextFilterEnabled(true);
		layout_action.addView(action_text);
		layout_action.addView(choose_action);

		layout_select = new LinearLayout(this);
		layout_select.setOrientation(LinearLayout.VERTICAL);
		layout_text = new TextView(this);
		layout_text.setText("Choose layout:");
		choose_layout = new ListView(this);
		choose_layout.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		ArrayAdapter<String> adapter_layout = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, LAYOUT_Names);
		choose_layout.setClickable(true);
		choose_layout.setOnItemClickListener(this);

		choose_layout.setAdapter(adapter_layout);
		choose_layout.setTextFilterEnabled(true);
		layout_select.addView(layout_text);
		layout_select.addView(choose_layout);

		sendLanguage();

		setContentView(R.layout.layout_view);
		setupGui();
	}

	@Override
	protected void onStart() {
		super.onStart();

		startPing();
	}

	private void setupGui() {
		mousePanel = (View) findViewById(R.id.mousePanel);
		mouseWheelPanel = (View) findViewById(R.id.mouseWheelPanel);

		mouseDetector = getMouseDetector();
		zoomDetector = getZoomDetector();
		mouseWheelDetector = getMouseWheelDetector();

		mousePanel.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mouseDetector.onTouchEvent(event)) {
					return true;
				} else {
					return zoomDetector.onTouchEvent(event);
				}
			}
		});

		mouseWheelPanel.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mouseWheelDetector.onTouchEvent(event);
			}
		});

		buttonView = (ButtonView) findViewById(R.id.buttonView);
		registerForContextMenu(buttonView);

		buttonView
				.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
					@Override
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						MenuInflater inflater = getMenuInflater();
						inflater.inflate(R.menu.button_edit_menu, menu);

						final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

						if (Preferences
								.getInstance(MouseKeysRemote.this)
								.isButtonSticky(
										Integer.valueOf(
												((Button) info.targetView)
														.getTag(R.id.button_id)
														.toString()).intValue())) {
							menu.findItem(R.id.mnuSticky).setChecked(true);
						}

					}
				});

		buttonView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Button b = (Button) v;
				boolean sticky = Boolean.valueOf(b.getTag(R.id.button_sticky)
						.toString());

				switch (event.getAction()) {
					case KeyEvent.ACTION_DOWN:
						if (!sticky)
							sendUDP(Constants.getActionPress(keyCode));
						vibrate();
						return true;
					case KeyEvent.ACTION_UP:
						if (sticky) {
							if (b.isPressed())
								sendUDP(Constants.getActionPress(keyCode));
							else
								sendUDP(Constants.getActionRelease(keyCode));
						} else
							sendUDP(Constants.getActionRelease(keyCode));
						return true;
				}
				return false;
			}

			private void vibrate() {
				if (enableVibrate) {
					nm.notify(1, vibrateNotification);
				}
			}
		});
	}

	private void startPing() {
		pingServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
			}
		};

		Intent intent = new Intent(this, PingService.class);
		bindService(intent, pingServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private GestureDetector getMouseDetector() {
		return new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDoubleTap(MotionEvent e) {
						return false;
					}

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						if (e1.getPointerCount() > 1
								|| e2.getPointerCount() > 1)
							return false;
						if (distanceX != 0) { // Only move if there has been a
												// movement
							String msg = "XMM" + (int) -distanceX
									* (mouse_speed_pos + 1);
							sendUDP(msg);
						}
						if (distanceY != 0) { // Only move if there has been a
												// movement
							String msg = "YMM" + (int) -distanceY
									* (mouse_speed_pos + 1);
							sendUDP(msg);
						}
						return true;
					}

					@Override
					public boolean onSingleTapConfirmed(MotionEvent e) {
						if (e.getPointerCount() > 1)
							return false;
						if (enableClickOnTap) {
							String msg = "MLC";
							sendUDP(msg);
							msg = "MLR";
							sendUDP(msg);
							if (enableVibrate) {
								nm.notify(1, vibrateNotification);
							}
							return true;
						}
						return false;
					}
				});
	}

	private ScaleGestureDetector getZoomDetector() {
		return new ScaleGestureDetector(this,
				new SimpleOnScaleGestureListener() {
					@Override
					public boolean onScale(ScaleGestureDetector detector) {
						if (enablePinchZoom && detector.getScaleFactor() != 1) {
							int zoom_diff = (int) (detector.getCurrentSpan() - detector
									.getPreviousSpan());
							String msg = "MPZ" + zoom_diff; // Mouse Pinch
															// Zoom.. Positive
															// values means zoom
															// in and
															// negative zoom
															// out.
							sendUDP(msg);
							return true;
						}

						return false;
					}

				});
	}

	private GestureDetector getMouseWheelDetector() {
		return new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						if (Math.abs(distanceY) > Math.abs(distanceX)) {
							int newAccY = (int) (distanceY / 6);
							if (newAccY != 0) {
								sendUDP("MWS" + newAccY);
							} else {// Make sure that something happens even for
									// small movements
								if (distanceY > 0) {
									sendUDP("MWS1");
								} else {
									sendUDP("MWS-1");
								}
							}
						} else {
							return true;
						}
						return false;
					}
				});
	}

	private int storeActions() {
		key_actions = new int[200];

		key_actions[0] = Constants.CUR_RIGHT;
		key_actions[1] = Constants.CUR_LEFT;
		key_actions[2] = Constants.CUR_UP;
		key_actions[3] = Constants.CUR_DOWN;
		key_actions[4] = Constants.MOUSE_RIGHT;
		key_actions[5] = Constants.MOUSE_LEFT;
		key_actions[6] = Constants.MOUSE_UP;
		key_actions[7] = Constants.MOUSE_DOWN;
		key_actions[8] = Constants.MOUSE_LCLICK;
		key_actions[9] = Constants.MOUSE_RCLICK;
		key_actions[10] = Constants.ESC;
		key_actions[11] = Constants.CR;
		key_actions[12] = Constants.NUM_KP_0;
		key_actions[13] = Constants.NUM_KP_1;
		key_actions[14] = Constants.NUM_KP_2;
		key_actions[15] = Constants.NUM_KP_3;
		key_actions[16] = Constants.NUM_KP_4;
		key_actions[17] = Constants.NUM_KP_5;
		key_actions[18] = Constants.NUM_KP_6;
		key_actions[19] = Constants.NUM_KP_7;
		key_actions[20] = Constants.NUM_KP_8;
		key_actions[21] = Constants.NUM_KP_9;
		key_actions[22] = Constants.SPACE;
		key_actions[23] = Constants.MOUSE_WUP;
		key_actions[24] = Constants.MOUSE_WDOWN;
		key_actions[25] = Constants.F1;
		key_actions[26] = Constants.F2;
		key_actions[27] = Constants.F3;
		key_actions[28] = Constants.F4;
		key_actions[29] = Constants.F5;
		key_actions[30] = Constants.F6;
		key_actions[31] = Constants.F7;
		key_actions[32] = Constants.F8;
		key_actions[33] = Constants.F9;
		key_actions[34] = Constants.F10;
		key_actions[35] = Constants.F11;
		key_actions[36] = Constants.F12;
		key_actions[37] = Constants.LCTRL;
		key_actions[38] = Constants.DEL;
		key_actions[39] = Constants.LSHIFT;
		key_actions[40] = Constants.LALT;
		key_actions[41] = Constants.INS;
		key_actions[42] = Constants.CHRA;
		key_actions[43] = Constants.CHRB;
		key_actions[44] = Constants.CHRC;
		key_actions[45] = Constants.CHRD;
		key_actions[46] = Constants.CHRE;
		key_actions[47] = Constants.CHRF;
		key_actions[48] = Constants.CHRG;
		key_actions[49] = Constants.CHRH;
		key_actions[50] = Constants.CHRI;
		key_actions[51] = Constants.CHRJ;
		key_actions[52] = Constants.CHRK;
		key_actions[53] = Constants.CHRL;
		key_actions[54] = Constants.CHRM;
		key_actions[55] = Constants.CHRN;
		key_actions[56] = Constants.CHRO;
		key_actions[57] = Constants.CHRP;
		key_actions[58] = Constants.CHRQ;
		key_actions[59] = Constants.CHRR;
		key_actions[60] = Constants.CHRS;
		key_actions[61] = Constants.CHRT;
		key_actions[62] = Constants.CHRU;
		key_actions[63] = Constants.CHRV;
		key_actions[64] = Constants.CHRW;
		key_actions[65] = Constants.CHRX;
		key_actions[66] = Constants.CHRY;
		key_actions[67] = Constants.CHRZ;
		key_actions[68] = Constants.CHR0;
		key_actions[69] = Constants.CHR1;
		key_actions[70] = Constants.CHR2;
		key_actions[71] = Constants.CHR3;
		key_actions[72] = Constants.CHR4;
		key_actions[73] = Constants.CHR5;
		key_actions[74] = Constants.CHR6;
		key_actions[75] = Constants.CHR7;
		key_actions[76] = Constants.CHR8;
		key_actions[77] = Constants.CHR9;
		key_actions[78] = Constants.PLUS;
		key_actions[79] = Constants.MINUS;
		key_actions[80] = Constants.TAB;
		key_actions[81] = Constants.PGUP;
		key_actions[82] = Constants.PGDOWN;
		key_actions[83] = Constants.COMMA;
		key_actions[84] = Constants.PERIOD;
		key_actions[85] = Constants.END;
		key_actions[86] = Constants.HOME;
		key_actions[87] = Constants.BACKSPACE;
		key_actions[88] = Constants.DUMMY;
		key_actions[89] = Constants.SEMI_COLON;
		key_actions[90] = Constants.BACKSLASH;
		key_actions[91] = Constants.AT;
		key_actions[92] = Constants.SLASH;
		key_actions[93] = Constants.COLON;
		key_actions[94] = Constants.EQUALS;
		key_actions[95] = Constants.QUESTION;
		key_actions[96] = Constants.ZOOM_IN;
		key_actions[97] = Constants.ZOOM_OUT;
		key_actions[98] = Constants.ZOOM_RESET;
		key_actions[99] = Constants.NORTH;
		key_actions[100] = Constants.SOUTH;
		key_actions[101] = Constants.WEST;
		key_actions[102] = Constants.EAST;
		key_actions[103] = Constants.NORTHWEST;
		key_actions[104] = Constants.NORTHEAST;
		key_actions[105] = Constants.SOUTHWEST;
		key_actions[106] = Constants.SOUTHEAST;

		return key_actions.length;
	}

	@Override
	protected void onStop() {
		debug("onStop");
		release_locks();
		stopSensors();
		super.onStop();
	}

	private void release_locks() {
		if (wl.isHeld()) {
			wl.release();
		}
		if (wifiLock.isHeld()) {
			wifiLock.release();
		}
	}

	@Override
	protected void onDestroy() {
		debug("onDestroy");
		release_locks();
		stopSensors();

		unbindService(pingServiceConnection);

		super.onDestroy();
	}

	@Override
	protected void onResume() {
		debug("onResume");
		super.onResume();
		/*
		 * when the activity is resumed, we acquire a wake-lock so that the
		 * screen stays on, since the user will likely not be fiddling with the
		 * screen or buttons.
		 */
		// mWakeLock.acquire();
		set_locks();
		sendLanguage();
		accXFloatOld = 0;
		accYFloatOld = 0;
		last_up = 0;
		if (enableSensors) {
			// Activate sensors
			if (sensors_mode == SENSORS_MOUSE_GAME
					&& (enableSensorsX || enableSensorsY)) {
				sendUDP("MMC"); // Center mouse
			}
			mSensorManager.registerListener(this, mAccelerometer,
					DEFAULT_SENSOR_DELAY);
		} else {
			stopSensors();
		}

	}

	private void set_locks() {
		if (enableAlwaysOn) {
			if (!wl.isHeld()) {
				wl.acquire();
			}
		}
		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}
	}

	@Override
	protected void onPause() {
		debug("onPause");

		super.onPause();
		/*
		 * When the activity is paused, we make sure to stop the simulation,
		 * release our sensor resources and wake locks
		 */

		stopSensors();

		// and release our wake-lock
		release_locks();
		// mWakeLock.release();
	}

	void stopSensors() {
		mSensorManager.unregisterListener(this);
		if (sensors_mode == SENSORS_CURSOR) {
			if (sensorStateX == ROTATE_X_LEFT) {
				// stop left
				sendUDP("KBR" + 37);
			} else if (sensorStateX == ROTATE_X_RIGHT) {
				// stop right
				sendUDP("KBR" + 39);
			}
			if (sensorStateY == ROTATE_Y_FORWARD) {
				// stop forward
				sendUDP("KBR" + 38);
			} else if (sensorStateY == ROTATE_Y_BACK) {
				// stop back
				sendUDP("KBR" + 40);
			}
		} else if (sensors_mode == SENSORS_MOUSE
				&& (enableSensorsX || enableSensorsY)) {
			sendUDP("MSR");
			sendUDP("MSL");
			sendUDP("MSU");
			sendUDP("MSD");
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// debug(event.toString());

		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
			return;
		/*
		 * record the accelerometer data, the event's timestamp as well as the
		 * current time. The latter is needed so we can calculate the "present"
		 * time during rendering. In this application, we need to take into
		 * account how the screen is rotated with respect to the sensors (which
		 * always return data in a coordinate space aligned to with the screen
		 * in its native orientation).
		 */

		// debug(event.toString());
		/*
		 * debug("-------"); debug(event.values[0]); debug(event.values[1]);
		 * debug(event.values[2]); debug("-------");
		 */
		// mSensorZ = event.values[2];

		switch (mDisplay.getOrientation()) {
			case Surface.ROTATION_0:
				// debug("0");
				mSensorX = event.values[0];
				mSensorY = -event.values[1];
				break;
			case Surface.ROTATION_90:
				// debug("90");
				mSensorX = -event.values[1];
				mSensorY = -event.values[0];
				break;
			case Surface.ROTATION_180:
				// debug("180");

				mSensorX = -event.values[0];
				mSensorY = event.values[1];
				break;
			case Surface.ROTATION_270:
				// debug("270");
				mSensorX = event.values[1];
				mSensorY = event.values[0];
				break;
		}
		// debug("mSensorX: " + mSensorX + " mSensorY: " + mSensorY +
		// " mSensorZ: " + mSensorZ);
		if (calibrate == true) {
			calibrate_x = mSensorX;
			calibrate_y = mSensorY;
		} else {
			if (sensors_mode == SENSORS_MOUSE_GAME) {
				sensorsMoveMouse();
			} else {
				sensorsMoveCursorOrMouse();
			}
		}
	}

	void sensorsMoveMouse() {
		// debug("Move mouse");
		/*
		 * Kolla om ändring av X eller Y, om större än 0 flytta musen. Skala
		 * upp sensorvärdena med faktor 5? Utgångsvärden alltid lika med
		 * värden för plant läge. Behövs manuell kalibreringsfunktion. Kolla
		 * flera värden på raken för att få ett stabilt?
		 */

		accXFloat = -(mSensorX - calibrate_x) * 5;
		accYFloat = -(mSensorY - calibrate_y) * 5;

		// debug("xPosFirstFloat:" + xPosFirstFloat + " yPosFirstFloat:" +
		// yPosFirstFloat);

		// debug("accXFloat:" + accXFloat + " accYFloat:" + accYFloat);

		// Calculate any change in distance
		accXFloatDiff = -(accXFloatOld - accXFloat);
		accYFloatDiff = -(accYFloatOld - accYFloat);

		// Convert change in distance to int
		accX = (int) accXFloatDiff;
		accY = (int) accYFloatDiff;
		// debug("accX:" + accX + " accY:" + accY);
		if (accX != 0 && enableSensorsX) // Only move if there has been a
											// movement!
		{
			// store new distance
			accXFloatOld = accXFloat;
			// debug("accX: "+ accX);
			String msg = "XMM" + accX * (mouse_speed_pos + 1);
			// debug("UDP msg: " + msg);
			sendUDP(msg);
			// store new distance
		}

		if (accY != 0 && enableSensorsY) // Only move if there has been a
											// movement!
		{
			accYFloatOld = accYFloat;
			// debug("accY: "+ accY);
			String msg = "YMM" + accY * (mouse_speed_pos + 1);
			// debug("UDP msg: " + msg);
			sendUDP(msg);
		}

	}

	void sensorsMoveCursorOrMouse() {
		if (mSensorX - calibrate_x > (sensorRotateLeft + sensorHysteris)
				&& enableSensorsX) {
			// debug("Left");
			if (sensorStateX == ROTATE_X_NONE) {
				// start left
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MML");
				} else {
					sendUDP("KBP" + 37);
				}
			} else if (sensorStateX == ROTATE_X_RIGHT) {
				// stop right and start left
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSR");
					sendUDP("MML");
				} else {
					sendUDP("KBR" + 39);
					sendUDP("KBP" + 37);
				}
			}
			sensorStateX = ROTATE_X_LEFT;
		} else if (mSensorX - calibrate_x > sensorRotateLeft
				&& mSensorX <= (sensorRotateLeft + sensorHysteris)) {
			// debug("ignorning left hysteresis");
		} else if (mSensorX - calibrate_x < (sensorRotateRight - sensorHysteris)
				&& enableSensorsX) {
			// debug("Right");
			if (sensorStateX == ROTATE_X_NONE) {
				// start right
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MMR");
				} else {
					sendUDP("KBP" + 39);
				}
			} else if (sensorStateX == ROTATE_X_LEFT) {
				// stop left and start right
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSL");
					sendUDP("MMR");
				} else {
					sendUDP("KBR" + 37);
					sendUDP("KBP" + 39);
				}

			}
			sensorStateX = ROTATE_X_RIGHT;
		} else if (mSensorX - calibrate_x < sensorRotateRight
				&& mSensorX >= (sensorRotateRight - sensorHysteris)) {
			// debug("ignorning right hysteresis");
		} else if (enableSensorsX) {
			// debug("CenterX");
			if (sensorStateX == ROTATE_X_LEFT) {
				// stop left
				// debug("CenterX");
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSL");
				} else {
					sendUDP("KBR" + 37);
				}
			} else if (sensorStateX == ROTATE_X_RIGHT) {
				// stop right
				// debug("CenterX");
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSR");
				} else {
					sendUDP("KBR" + 39);
				}
			}
			sensorStateX = ROTATE_X_NONE;
		}

		if (mSensorY - calibrate_y > sensorOffsetY
				+ (sensorRotateForward + sensorHysteris)
				&& enableSensorsY) {
			// debug("Forward");
			if (sensorStateY == ROTATE_Y_NONE) {
				// start forward
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MMU");
				} else {
					sendUDP("KBP" + 38);
				}
			} else if (sensorStateY == ROTATE_Y_BACK) {
				// stop back and start forward
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSD");
					sendUDP("MMU");
				} else {
					sendUDP("KBR" + 40);
					sendUDP("KBP" + 38);
				}
			}
			sensorStateY = ROTATE_Y_FORWARD;
		} else if (mSensorY - calibrate_y > sensorOffsetY + sensorRotateForward
				&& mSensorY <= sensorOffsetY
						+ (sensorRotateForward + sensorHysteris)) {
			// debug("ignorning forward hysteris");
		} else if (mSensorY - calibrate_y < sensorOffsetY
				+ (sensorRotateBack - sensorHysteris)
				&& enableSensorsY) {
			// debug("Back");
			if (sensorStateY == ROTATE_Y_NONE) {
				// start back
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MMD");
				} else {
					sendUDP("KBP" + 40);
				}
			} else if (sensorStateY == ROTATE_Y_FORWARD) {
				// stop forward and start back
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSU");
					sendUDP("MMD");
				} else {
					sendUDP("KBR" + 38);
					sendUDP("KBP" + 40);
				}

			}
			sensorStateY = ROTATE_Y_BACK;
		} else if (mSensorY - calibrate_y < sensorOffsetY + sensorRotateBack
				&& mSensorY >= sensorOffsetY
						+ (sensorRotateBack - sensorHysteris)) {
			// debug("ignorning back hysteris");
		} else if (enableSensorsY) {
			// debug("CenterY");
			if (sensorStateY == ROTATE_Y_FORWARD) {
				// stop forward
				// debug("CenterY");
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSU");
				} else {
					sendUDP("KBR" + 38);
				}
			} else if (sensorStateY == ROTATE_Y_BACK) {
				// stop back
				// debug("CenterY");
				if (sensors_mode == SENSORS_MOUSE) {
					sendUDP("MSD");
				} else {
					sendUDP("KBR" + 40);
				}
			}
			sensorStateY = ROTATE_Y_NONE;
		}
	}

	void setDefaultKeys() {
		SharedPreferences.Editor editor = mySharedPreferences.edit();

		if (!mySharedPreferences.contains("layout" + keys_layout + "key1")) {
			// Set default values if layouts are empty
			editor.putInt("layout" + keys_layout + "key1", Constants.ESC);
			editor.putInt("layout" + keys_layout + "key2", Constants.LCTRL);
			editor.putInt("layout" + keys_layout + "key3", Constants.CUR_UP);
			editor.putInt("layout" + keys_layout + "key4", Constants.CR);
			editor.putInt("layout" + keys_layout + "key5", Constants.SPACE);
			editor.putInt("layout" + keys_layout + "key6", Constants.CUR_LEFT);
			editor.putInt("layout" + keys_layout + "key7", Constants.DUMMY);
			editor.putInt("layout" + keys_layout + "key8", Constants.CUR_RIGHT);
			editor.putInt("layout" + keys_layout + "key9", Constants.TAB);
			editor.putInt("layout" + keys_layout + "key10", Constants.MOUSE_UP);
			editor.putInt("layout" + keys_layout + "key11", Constants.CUR_DOWN);
			editor.putInt("layout" + keys_layout + "key12", Constants.PLUS);
			editor.putInt("layout" + keys_layout + "key13",
					Constants.MOUSE_LEFT);
			editor.putInt("layout" + keys_layout + "key14", Constants.DUMMY);
			editor.putInt("layout" + keys_layout + "key15",
					Constants.MOUSE_RIGHT);
			editor.putInt("layout" + keys_layout + "key16", Constants.MINUS);
			editor.putInt("layout" + keys_layout + "key17",
					Constants.MOUSE_LCLICK);
			editor.putInt("layout" + keys_layout + "key18",
					Constants.MOUSE_DOWN);
			editor.putInt("layout" + keys_layout + "key19",
					Constants.MOUSE_RCLICK);
			editor.putInt("layout" + keys_layout + "key20", Constants.DUMMY);
			editor.commit();
		}
	}

	/** Creates the menu items **/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_MAIN, 0, "Virtual keyboard");
		menu.add(0, MENU_KP_TOUCH, 0, "Mouse/Keypad");
		menu.add(0, MENU_UNHIDE, 0,
				getResources().getString(R.string.unhide_all));
		menu.add(0, MENU_LAYOUT, 0, "Select layout");
		menu.add(0, MENU_STICK, 0, "Toggle sticky keys");
		menu.add(0, MENU_MORE, 0, "Settings");
		menu.add(0, MENU_CALIBRATE, 0, "Calibrate sensors");
		menu.add(0, MENU_ABOUT, 0, "About");
		menu.add(0, MENU_QUIT, 0, "Exit");
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_MAIN: // Virtual keyboard...
				setContentView(R.layout.main);
				EditText sendtext = (EditText) findViewById(R.id.keyboard_input);
				sendtext.setText("");
				sendtext.requestFocusFromTouch();
				sendtext.requestFocus();
				sendtext.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						if (count - before > 0) { // Char added
							char ch = s.charAt(start + count - 1);
							sendUDP("KBP" + (int) ch);
							sendUDP("KBR" + (int) ch);
						} else if (before - count == 1) {
							// Backspace
							sendUDP("KBP" + 8);
							sendUDP("KBR" + 8);
						}
					}

				});

				InputMethodManager inputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMgr.toggleSoftInput(0, 0);

				return true;
			case MENU_KP_TOUCH:
				sendLanguage();
				setContentView(R.layout.layout_view);
				return true;
			case MENU_EDIT:
				if (edit == EditMode.None)
					edit = EditMode.Binding;
				else if (edit == EditMode.Binding)
					edit = EditMode.Name;
				else if (edit == EditMode.Name)
					edit = EditMode.Color;
				else if (edit == EditMode.Color)
					edit = EditMode.None;
				if (sticky)
					keyPadView.setMark(EditMode.Sticky);
				else
					keyPadView.setMark(edit);
				setContentView(R.layout.layout_view);
				return true;
			case MENU_ABOUT:
				setContentView(R.layout.info);
				return true;
			case MENU_COLOR:
				// showColorPicker(mPaint.getColor());
				return true;
			case MENU_QUIT:
				quit();
				return true;
			case MENU_CALIBRATE:
				debug("calibrate");
				calibrate = true;
				setContentView(R.layout.layout_view);
				return true;
			case MENU_LAYOUT:
				setContentView(layout_select);

				for (int i = 0; i < DEFAULT_LAYOUTS; i++) {
					LAYOUT_Names3[i] = mySharedPreferences.getString(
							"layout_name" + i, "Layout " + (i + 1));
				}
				ArrayAdapter<String> adapter_layout = new ArrayAdapter<String>(
						this, android.R.layout.simple_list_item_1,
						LAYOUT_Names3);
				choose_layout.setAdapter(adapter_layout);
				return true;
			case MENU_MORE:
				setContentView(R.layout.settings);
				try {
					spinner = (Spinner) findViewById(R.id.spinner);
					ArrayAdapter<CharSequence> adapter_lang = ArrayAdapter
							.createFromResource(this, R.array.lang_array,
									android.R.layout.simple_spinner_item);
					adapter_lang
							.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner.setAdapter(adapter_lang);
					spinner.setSelection(lang_pos, true);
					spinner.setOnItemSelectedListener(this);
				} catch (Exception e) {
					debug(e.toString());
				}

				try {
					mouse_spinner = (Spinner) findViewById(R.id.mouse_spinner);
					ArrayAdapter<CharSequence> adapter_mouse = ArrayAdapter
							.createFromResource(this,
									R.array.mouse_speed_array,
									android.R.layout.simple_spinner_item);
					adapter_mouse
							.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					mouse_spinner.setAdapter(adapter_mouse);
					mouse_spinner.setSelection(mouse_speed_pos);
					mouse_spinner.setOnItemSelectedListener(this);
				} catch (Exception e) {
					debug(e.toString());
				}
				try {
					layout_mode_spinner = (Spinner) findViewById(R.id.layout_mode_spinner);
					ArrayAdapter<CharSequence> adapter_layout_mode = ArrayAdapter
							.createFromResource(this,
									R.array.layout_mode_array,
									android.R.layout.simple_spinner_item);
					adapter_layout_mode
							.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					layout_mode_spinner.setAdapter(adapter_layout_mode);
					layout_mode_spinner.setSelection(layout_mode_pos);
					layout_mode_spinner.setOnItemSelectedListener(this);
				} catch (Exception e) {
					debug(e.toString());
				}

				TextView lang_text = (TextView) findViewById(R.id.keyboard_lang);
				TextView mouse_speed_text = (TextView) findViewById(R.id.mouse_speed);
				EditText ed = (EditText) findViewById(R.id.entry_ip);
				EditText port_ed = (EditText) findViewById(R.id.entry_port);
				EditText mousepadentry = (EditText) findViewById(R.id.mousepadentry);
				EditText colsentry = (EditText) findViewById(R.id.entry_kpcols);
				EditText rowsentry = (EditText) findViewById(R.id.entry_kprows);
				EditText passentry = (EditText) findViewById(R.id.password_entry);
				EditText layoutentry = (EditText) findViewById(R.id.layout_entry);
				EditText textSizeEntry = (EditText) findViewById(R.id.entry_text_size);

				CheckBox mousewheel = (CheckBox) findViewById(R.id.mousewheel);
				CheckBox vibrate = (CheckBox) findViewById(R.id.vibrate);
				CheckBox mousepad = (CheckBox) findViewById(R.id.mousepad);
				CheckBox always_on = (CheckBox) findViewById(R.id.always_on);
				CheckBox sensors = (CheckBox) findViewById(R.id.sensors);
				CheckBox sensors_x = (CheckBox) findViewById(R.id.sensors_x);
				CheckBox sensors_y = (CheckBox) findViewById(R.id.sensors_y);
				CheckBox clickontap = (CheckBox) findViewById(R.id.clickontap);
				CheckBox pinchzoom = (CheckBox) findViewById(R.id.pinch_zoom);

				RadioButton sensors_cursor = (RadioButton) findViewById(R.id.sensors_cursor);
				RadioButton sensors_mouse = (RadioButton) findViewById(R.id.sensors_mouse);
				RadioButton sensors_mouse_game = (RadioButton) findViewById(R.id.sensors_mouse_game);
				// mouse_speed_text.setText("Mouse speed (" +
				// mouse_spinner.getItemAtPosition(mouse_speed_pos).toString() +
				// ")");
				// lang_text.setText("PC keyboard type (" +
				// spinner.getItemAtPosition(lang_pos).toString() + ")");
				Button text_col = (Button) findViewById(R.id.textcol);
				text_col.setBackgroundColor(textCol);
				Button back_col = (Button) findViewById(R.id.background_col);
				back_col.setBackgroundColor(backCol);
				Button mousepad_col = (Button) findViewById(R.id.mousepad_col);
				mousepad_col.setBackgroundColor(mousepadCol);
				Button mousewheel_col = (Button) findViewById(R.id.mousewheel_col);
				mousewheel_col.setBackgroundColor(mousewheelCol);

				text_col.setOnClickListener(this);
				back_col.setOnClickListener(this);
				mousepad_col.setOnClickListener(this);
				mousewheel_col.setOnClickListener(this);

				ed.setText(host);
				ed.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						host = (s.toString()).trim();
						SharedPreferences.Editor editor = mySharedPreferences
								.edit();
						editor.putString("host", host);
						editor.commit();
					}

				});

				port_ed.setText("" + port);
				port_ed.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						// debug("e3");
						// debug(s.toString() + " start: " + start + " before: "
						// +
						// before + " count: " + count);

						try {
							int val = Integer.parseInt((s.toString()).trim());
							if (val >= 0 & val <= 65535) {
								port = val;
								SharedPreferences.Editor editor = mySharedPreferences
										.edit();
								editor.putInt("port", port);
								editor.commit();
							}
						} catch (Exception e) {
							debug(e.toString());
						}
					}

				});

				passentry.setText(passwd);
				passentry.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						passwd = (s.toString()).trim();
						SharedPreferences.Editor editor = mySharedPreferences
								.edit();
						editor.putString("password", passwd);
						try {
							security = new Security(passwd);
						} catch (Exception ex) {
							debug(ex.toString());
						}
						editor.commit();
					}

				});

				mousepadentry.setText("" + mousepadRelativeSize);
				mousepadentry.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						try {
							int val = Integer.parseInt((s.toString()).trim());
							if (val >= 0 & val <= 100) {
								mousepadRelativeSize = val;
								SharedPreferences.Editor editor = mySharedPreferences
										.edit();
								editor.putInt("mousepadRelativeSize"
										+ keys_layout, mousepadRelativeSize);
								editor.commit();
								keyPadView.postInvalidate();
							}
						} catch (Exception e) {
							debug(e.toString());
						}
					}

				});

				colsentry.setText("" + numberOfKeyCols);
				colsentry.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						try {
							int val = Integer.parseInt((s.toString()).trim());
							if (val >= 0 & val <= 100
									& (val * numberOfKeyRows < 200)) {
								numberOfKeyCols = val;
								SharedPreferences.Editor editor = mySharedPreferences
										.edit();
								editor.putInt("numberOfKeyCols" + keys_layout,
										numberOfKeyCols);
								editor.commit();
								keyPadView.postInvalidate();
							}
						} catch (Exception e) {
							debug(e.toString());
						}
					}

				});

				rowsentry.setText("" + numberOfKeyRows);
				rowsentry.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						try {
							int val = Integer.parseInt((s.toString()).trim());
							if (val >= 0 & val <= 100
									& (val * numberOfKeyCols < 200)) {
								numberOfKeyRows = val;
								SharedPreferences.Editor editor = mySharedPreferences
										.edit();
								editor.putInt("numberOfKeyRows" + keys_layout,
										numberOfKeyRows);
								editor.commit();
								keyPadView.postInvalidate();
							}
						} catch (Exception e) {
							debug(e.toString());
						}
					}

				});

				textSizeEntry.setText("" + textSize);
				textSizeEntry.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						try {
							float val = Float.parseFloat((s.toString()).trim());
							if (val > 0) {
								textSize = val;
								SharedPreferences.Editor editor = mySharedPreferences
										.edit();
								editor.putFloat("textSize" + keys_layout,
										textSize);
								editor.commit();
								keyPadView.postInvalidate();
							}
						} catch (Exception e) {
							debug(e.toString());
						}
					}

				});

				layoutentry.setText(keys_layout_name);
				layoutentry.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						keys_layout_name = (s.toString()).trim();
						SharedPreferences.Editor editor = mySharedPreferences
								.edit();
						editor.putString("layout_name" + keys_layout,
								keys_layout_name);
						editor.commit();
						keyPadView.postInvalidate();
					}
				});

				mousepad.setChecked(enableMousePad);
				mousepad.setOnClickListener(this);
				mousewheel.setChecked(enableMouseWheel);
				mousewheel.setOnClickListener(this);
				vibrate.setChecked(enableVibrate);
				vibrate.setOnClickListener(this);
				always_on.setChecked(enableAlwaysOn);
				always_on.setOnClickListener(this);
				sensors.setChecked(enableSensors);
				sensors.setOnClickListener(this);
				sensors_x.setChecked(enableSensorsX);
				sensors_x.setOnClickListener(this);
				sensors_y.setChecked(enableSensorsY);
				sensors_y.setOnClickListener(this);
				clickontap.setChecked(enableClickOnTap);
				clickontap.setOnClickListener(this);
				pinchzoom.setChecked(enablePinchZoom);
				pinchzoom.setOnClickListener(this);
				// send = (Button)findViewById(R.id.send);
				// send.setOnClickListener(this);
				if (sensors_mode == SENSORS_MOUSE) {
					sensors_mouse.setChecked(true);
				} else if (sensors_mode == SENSORS_MOUSE_GAME) {
					sensors_mouse_game.setChecked(true);
				} else {
					sensors_cursor.setChecked(true);
				}
				sensors_cursor.setOnClickListener(this);
				sensors_mouse.setOnClickListener(this);
				sensors_mouse_game.setOnClickListener(this);

				return true;
			case MENU_STICK:
				sticky = !sticky;
				setContentView(R.layout.layout_view);
				return true;
			case MENU_UNHIDE:
				buttonView.unhideAll();
				return true;
		}

		return false;
	}

	private void sendLanguage() {
		String msg = "LNG" + lang_pos;
		// debug("UDP msg: " + msg);
		sendUDP(msg);
	}

	void quit() {
		this.finish();
	}

	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		debug(parent.getItemAtPosition(pos).toString());
		debug(parent.toString());
		// Toast.makeText(parent.getContext(), "The planet is " +
		// parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
		if (parent == spinner) {
			debug("lang");
			lang_pos = pos;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("lang_pos", lang_pos);
			editor.commit();
			sendLanguage();
		} else if (parent == mouse_spinner) {
			debug("mouse");
			mouse_speed_pos = pos;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("mouse_speed_pos" + keys_layout, mouse_speed_pos);
			editor.commit();
		} else if (parent == layout_mode_spinner) {
			debug("layout_mode");
			layout_mode_pos = pos;
			debug(pos);
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("layout_mode_pos" + keys_layout, layout_mode_pos);
			editor.commit();
			setOrientation();
		}
	}

	// Events from keyboard
	// Implement the OnKeyDown callback
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK: {
				debug("back");
				sendLanguage();
				if (findViewById(R.id.layout_view) == null)
					setContentView(R.layout.layout_view);
				else
					quit();
			}
		}
		return false;
	}

	// Events from views

	// Implement the OnItemClickListener callback
	public void onItemClick(AdapterView<?> v, View w, int i, long l) {
		// do something when the button is clicked
		debug("onItemClick: " + v.toString());
		debug("i:" + i + " l:" + l);
		if (v == choose_action) // Edit key action
		{
			debug("choose_action");
			debug(Constants.getActionName(key_actions[i]));

			// SharedPreferences mySharedPreferences =
			// getSharedPreferences("MY_PREFS", Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("layout" + keys_layout + "key" + key_to_edit,
					key_actions[i]);
			editor.commit();

			sendLanguage();
			setContentView(R.layout.layout_view);
		} else if (v == choose_layout) // Select layout
		{
			debug("choose_layout");
			keys_layout = i;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("layout", i);
			editor.commit();

			reload_settings();

			setDefaultKeys();
			sendLanguage();
			setContentView(R.layout.layout_view);
		}

	}

	private void reload_settings() {
		// Global values
		host = mySharedPreferences.getString("host", "192.168.10.184");
		port = mySharedPreferences.getInt("port", Constants.UDP_PORT);

		keys_layout = mySharedPreferences.getInt("layout", 0);
		lang_pos = mySharedPreferences.getInt("lang_pos", DEFAULT_LANGUAGE);
		passwd = mySharedPreferences.getString("password", "");

		// Values unique per layout
		keys_layout_name = mySharedPreferences.getString("layout_name"
				+ keys_layout, "Layout " + (keys_layout + 1));
		mouse_speed_pos = mySharedPreferences.getInt("mouse_speed_pos"
				+ keys_layout, DEFAULT_MOUSE_ACC - 1); // List index starts at
														// zero, add one when
														// using..
		numberOfKeyRows = mySharedPreferences.getInt("numberOfKeyRows"
				+ keys_layout, DEFAULT_NUM_ROWS);
		numberOfKeyCols = mySharedPreferences.getInt("numberOfKeyCols"
				+ keys_layout, DEFAULT_NUM_COLS);
		mousepadRelativeSize = mySharedPreferences.getInt(
				"mousepadRelativeSize" + keys_layout, DEFAULT_MP_SIZE);
		enableMouseWheel = mySharedPreferences.getBoolean("enableMouseWheel"
				+ keys_layout, true);
		enableVibrate = mySharedPreferences.getBoolean("enableVibrate"
				+ keys_layout, true);
		enableMousePad = mySharedPreferences.getBoolean("enableMousePad"
				+ keys_layout, true);
		textSize = mySharedPreferences.getFloat("textSize" + keys_layout,
				DEFAULT_TEXT_SIZE);
		enableAlwaysOn = mySharedPreferences.getBoolean("enableAlwaysOn"
				+ keys_layout, false);
		enableSensors = mySharedPreferences.getBoolean("enableSensors"
				+ keys_layout, false);
		enableSensorsX = mySharedPreferences.getBoolean("enableSensorsX"
				+ keys_layout, true);
		enableSensorsY = mySharedPreferences.getBoolean("enableSensorsY"
				+ keys_layout, true);
		layout_mode_pos = mySharedPreferences.getInt("layout_mode_pos"
				+ keys_layout, 0);
		sensors_mode = mySharedPreferences.getInt("sensors_mode" + keys_layout,
				SENSORS_MOUSE);
		enableClickOnTap = mySharedPreferences.getBoolean("enableClickOnTap"
				+ keys_layout, true);
		enablePinchZoom = mySharedPreferences.getBoolean("enablePinchZoom"
				+ keys_layout, false);

		backCol = mySharedPreferences.getInt("background_col" + keys_layout,
				0xff000000);
		textCol = mySharedPreferences.getInt("text_col" + keys_layout,
				0xffffffff);
		mousepadCol = mySharedPreferences.getInt("mousepad_col" + keys_layout,
				0xff2222ff);
		mousewheelCol = mySharedPreferences.getInt("mousewheel_col"
				+ keys_layout, 0xff8888ff);

		if (enableAlwaysOn) {
			if (!wl.isHeld()) {
				wl.acquire();
			}
		} else {
			if (wl.isHeld()) {
				wl.release();
			}
		}

		if (enableSensors) {
			// Activate sensors
			mSensorManager.registerListener(this, mAccelerometer,
					DEFAULT_SENSOR_DELAY);
			if (sensors_mode == SENSORS_MOUSE_GAME
					&& (enableSensorsX || enableSensorsY)) {
				sendUDP("MMC"); // Center mouse
			}
		} else {
			stopSensors();
		}

		setOrientation();
	}

	private void setOrientation() {
		if (layout_mode_pos == 0) {
			// Auto rotate
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		} else if (layout_mode_pos == 1) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (layout_mode_pos == 2) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	// Implement the OnClickListener callback
	public void onClick(View v) {
		// do something when the button is clicked
		debug("onClick:" + v.toString());

		CheckBox mousewheel = (CheckBox) findViewById(R.id.mousewheel);
		CheckBox vibrate = (CheckBox) findViewById(R.id.vibrate);
		CheckBox mousepad = (CheckBox) findViewById(R.id.mousepad);
		CheckBox always_on = (CheckBox) findViewById(R.id.always_on);
		CheckBox sensors = (CheckBox) findViewById(R.id.sensors);
		CheckBox sensors_x = (CheckBox) findViewById(R.id.sensors_x);
		CheckBox sensors_y = (CheckBox) findViewById(R.id.sensors_y);
		RadioButton sensors_cursor = (RadioButton) findViewById(R.id.sensors_cursor);
		RadioButton sensors_mouse = (RadioButton) findViewById(R.id.sensors_mouse);
		RadioButton sensors_mouse_game = (RadioButton) findViewById(R.id.sensors_mouse_game);
		CheckBox clickontap = (CheckBox) findViewById(R.id.clickontap);
		CheckBox pinchzoom = (CheckBox) findViewById(R.id.pinch_zoom);

		Button text_col = (Button) findViewById(R.id.textcol);
		Button back_col = (Button) findViewById(R.id.background_col);
		Button mousepad_col = (Button) findViewById(R.id.mousepad_col);
		Button mousewheel_col = (Button) findViewById(R.id.mousewheel_col);

		if (v == mousewheel) {
			debug("mousewheel changed");
			try {
				enableMouseWheel = mousewheel.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableMouseWheel" + keys_layout,
						enableMouseWheel);
				editor.commit();
				keyPadView.postInvalidate();
			} catch (Exception e) {
				debug(e.toString());
			}
		} else if (v == vibrate) {
			debug("vibrate changed");
			try {
				enableVibrate = vibrate.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableVibrate" + keys_layout, enableVibrate);
				editor.commit();
				keyPadView.postInvalidate();
			} catch (Exception e) {
				debug(e.toString());
			}
		} else if (v == mousepad) {
			debug("mousepad changed");
			try {
				enableMousePad = mousepad.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableMousePad" + keys_layout,
						enableMousePad);
				editor.commit();
				keyPadView.postInvalidate();
			} catch (Exception e) {
				debug(e.toString());
			}
		} else if (v == always_on) {
			debug("always on changed");
			if (enableAlwaysOn) {
				if (wl.isHeld()) {
					wl.release();
				}
			} else {
				if (!wl.isHeld()) {
					wl.acquire();
				}
			}
			try {
				enableAlwaysOn = always_on.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableAlwaysOn" + keys_layout,
						enableAlwaysOn);
				editor.commit();
			} catch (Exception e) {
				debug(e.toString());
			}

		} else if (v == sensors) {
			debug("sensors changed");
			try {
				enableSensors = sensors.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableSensors" + keys_layout, enableSensors);
				editor.commit();
			} catch (Exception e) {
				debug(e.toString());
			}

			if (enableSensors) {
				// Activate sensors
				accXFloatOld = 0;
				accYFloatOld = 0;
				mSensorManager.registerListener(this, mAccelerometer,
						DEFAULT_SENSOR_DELAY);
			} else {
				stopSensors();
			}
		} else if (v == sensors_x) {
			debug("sensors_x changed");
			try {
				enableSensorsX = sensors_x.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableSensorsX" + keys_layout,
						enableSensorsX);
				editor.commit();
			} catch (Exception e) {
				debug(e.toString());
			}

			if (enableSensorsX) {
			} else {
				sendUDP("MSL");
				sendUDP("MSR");
			}
		} else if (v == sensors_y) {
			debug("sensors_y changed");
			try {
				enableSensorsY = sensors_y.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableSensorsY" + keys_layout,
						enableSensorsY);
				editor.commit();
			} catch (Exception e) {
				debug(e.toString());
			}

			if (enableSensorsY) {
			} else {
				sendUDP("MSU");
				sendUDP("MSD");
			}
		} else if (v == sensors_mouse) {
			debug("sensors mouse changed");
			sensors_mode = SENSORS_MOUSE;
			accXFloatOld = 0;
			accYFloatOld = 0;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("sensors_mode" + keys_layout, sensors_mode);
			editor.commit();
		} else if (v == sensors_mouse_game) {
			debug("sensors mouse game changed");
			sensors_mode = SENSORS_MOUSE_GAME;
			accXFloatOld = 0;
			accYFloatOld = 0;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("sensors_mode" + keys_layout, sensors_mode);
			editor.commit();
			sendUDP("MMC"); // Center mouse
		} else if (v == sensors_cursor) {
			debug("sensors cursor changed");
			sendUDP("MSR");
			sendUDP("MSL");
			sendUDP("MSU");
			sendUDP("MSD");
			sensors_mode = SENSORS_CURSOR;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("sensors_mode" + keys_layout, sensors_mode);
			editor.commit();
		} else if (v == clickontap) {
			debug("clickontap changed");
			try {
				enableClickOnTap = clickontap.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enableClickOnTap" + keys_layout,
						enableClickOnTap);
				editor.commit();
				keyPadView.postInvalidate();
			} catch (Exception e) {
				debug(e.toString());
			}
		} else if (v == pinchzoom) {
			debug("pinchzoom changed");
			try {
				enablePinchZoom = pinchzoom.isChecked();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean("enablePinchZoom" + keys_layout,
						enablePinchZoom);
				editor.commit();
				keyPadView.postInvalidate();
			} catch (Exception e) {
				debug(e.toString());
			}
		} else if (v == text_col) {
			showColorPicker(mPaint.getColor(), ColorElement.Text);
		} else if (v == back_col) {
			showColorPicker(mPaint.getColor(), ColorElement.Back);
		} else if (v == mousepad_col) {
			showColorPicker(mPaint.getColor(), ColorElement.MousePad);
		} else if (v == mousewheel_col) {
			showColorPicker(mPaint.getColor(), ColorElement.MouseWheel);
		} else // catch all, return to menu
		{
			debug("catch all, return to menu");

		}

	}

	private void showColorPicker(int color, ColorElement elem) {
		Bundle b = new Bundle();
		b.putInt(ColorPickerDialog.PROP_INITIAL_COLOR, color);
		b.putInt(ColorPickerDialog.PROP_PARENT_WIDTH,
				keyPadView.getMeasuredWidth());
		b.putInt(ColorPickerDialog.PROP_PARENT_HEIGHT,
				keyPadView.getMeasuredHeight());
		b.putInt(ColorPickerDialog.PROP_COLORELEM, elem.toInt());
		ColorPickerDialog cpd = new ColorPickerDialog(this, b);
		cpd.setColorChangedListener(this);
		cpd.show();
	}

	private void display_edit_name_layout() {
		setContentView(R.layout.edit_key_name);
		EditText name = (EditText) findViewById(R.id.keyboard_input);
		name.setText(mySharedPreferences.getString("nameOfKey" + "layout"
				+ keys_layout + "key" + key_to_edit,
				Constants.getActionName(getKeyValue(key_to_edit))));
		name.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String name = (s.toString()).trim();
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putString("nameOfKey" + "layout" + keys_layout + "key"
						+ key_to_edit, name);
				editor.commit();
			}
		});
	}

	static public int getKeyValue(int key_num) {
		int value = mySharedPreferences.getInt("layout" + keys_layout + "key"
				+ key_num, Constants.DUMMY);
		return value;
	}

	void sendUDP(String msg_plain) {
		try {
			String msg = security.encrypt(msg_plain);
			try {
				DatagramSocket s = new DatagramSocket();
				InetAddress local = InetAddress.getByName(host);
				int msg_length = msg.length();
				byte[] message = msg.getBytes();
				DatagramPacket p = new DatagramPacket(message, msg_length,
						local, port);
				s.send(p);
			} catch (Exception e) {
				debug(e.toString());
			}
		} catch (Exception ex) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.security_context_failed_),
					Toast.LENGTH_LONG).show();

			debug(ex.toString());
		}
	}

	static void debug(String s) {
		if (debug)
			Log.d(TAG, s);
	}

	static void debug(int i) {
		if (debug)
			debug("" + i);
	}

	static void debug(float f) {
		if (debug)
			debug("" + f);
	}

	public void colorChanged_int(int color, ColorElement elem) {
		mPaint.setColor(color);
		// int keys_layout = MouseKeysRemote.keys_layout;
		if (elem == ColorElement.Button) {
			debug("color: " + color + "key: " + key_to_edit);
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("KeyColor" + "layout" + keys_layout + "key"
					+ key_to_edit, color);
			editor.commit();
			keyPadView.invalidate();
		} else if (elem == ColorElement.Text) {
			debug("text color: " + color + " layout: " + keys_layout);
			textCol = color;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("text_col" + keys_layout, color);
			editor.commit();
			keyPadView.postInvalidate();
			setContentView(R.layout.layout_view);
		} else if (elem == ColorElement.Back) {
			backCol = color;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("background_col" + keys_layout, color);
			editor.commit();
			keyPadView.postInvalidate();
			setContentView(R.layout.layout_view);
		} else if (elem == ColorElement.MousePad) {
			mousepadCol = color;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("mousepad_col" + keys_layout, color);
			editor.commit();
			keyPadView.postInvalidate();
			setContentView(R.layout.layout_view);
		} else if (elem == ColorElement.MouseWheel) {
			mousewheelCol = color;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("mousewheel_col" + keys_layout, color);
			editor.commit();
			keyPadView.postInvalidate();
			setContentView(R.layout.layout_view);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		key_to_edit = Integer.valueOf(
				info.targetView.getTag(R.id.button_id).toString()).intValue();

		switch (item.getItemId()) {
			case R.id.mnuCommand:
				setContentView(layout_action);
				return true;
			case R.id.mnuLabel:
				// display_edit_name_layout();
				SimpleTextDialog dlg = new SimpleTextDialog(this);

				dlg.setOnTextChangedListener(new SimpleTextDialog.OnTextChangedListener() {
					@Override
					public void onTextChanged(String text) {
						SharedPreferences.Editor editor = mySharedPreferences
								.edit();
						editor.putString("layout" + keys_layout + "label"
								+ key_to_edit, text);
						editor.commit();
						((Button) info.targetView).setText(text);
					}
				});

				dlg.show(getResources().getString(R.string.edit_label),
						getResources().getString(R.string.name),
						((Button) info.targetView).getText().toString());

				return true;
			case R.id.mnuColor:
				AmbilWarnaDialog cdlg = new AmbilWarnaDialog(this,
						android.R.color.white,
						new AmbilWarnaDialog.OnAmbilWarnaListener() {
							@Override
							public void onOk(AmbilWarnaDialog dialog, int color) {
								SharedPreferences.Editor editor = mySharedPreferences
										.edit();
								editor.putInt("KeyColor" + "layout"
										+ keys_layout + "key" + key_to_edit,
										color);
								editor.commit();
								((Button) info.targetView).getBackground()
										.setColorFilter(
												new LightingColorFilter(color,
														android.R.color.white));
							}

							@Override
							public void onCancel(AmbilWarnaDialog dialog) {
							}
						});
				cdlg.show();
				return true;
			case R.id.mnuSticky:
				Preferences.getInstance(this).setButtonSticky(
						key_to_edit,
						!Preferences.getInstance(this).isButtonSticky(
								key_to_edit));

				return true;
			case R.id.mnuVisible:
				Preferences.getInstance(this).setButtonVisible(key_to_edit,
						false);
				((Button) info.targetView).setVisibility(View.INVISIBLE);
				return true;
		}

		return false;
	}

	@Override
	public void colorChanged(int color, int elem) {
		colorChanged_int(color, ColorElement.valueOf("" + elem));
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}
}
