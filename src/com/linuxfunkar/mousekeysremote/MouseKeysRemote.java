/**
 * @file MouseKeysRemote.java
 * @brief 
 *
 * Copyright (C)2010-2012 Magnus Uppman <magnus.uppman@gmail.com>
 * License: GPLv3+
 */

/*
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

 * 2011-07-24 v1.03
 * Keyboard is now displayed automatically
 * Layouts can be named.
 * Increased number of layouts to 16.
 * // Notes:
 // Special characters and zoom:
 // Must enable Compose and Meta key in KDE.
 // System settings -> Keyboard layout -> Advanced
 Free version exactly the same except only one layout. 
 DEFAULT_TOUCH_TIME = 100
 2011-08-17 v1.03.1
 Better sensitivity control to avoid jumping mouse.

 2011-09-02 v1.03.2
 Further improvements to mouse and mousewheel sensitivity control.
 Each layout can now be set to portrait, landscape or auto-rotate.
 Display can be set to always on per layout.
 Wifi is now never disabled when the app is running.
 Bugfix where pinch to zoom was wrongly activated when pressing a button after moving the mouse.
 Haptic feedback is now configurable per layout. 
 2011-09-03 v1.03.3
 Bugfix where tap on touch didn't work on some devices.

 2011-09-11 v1.03.4
 Added support for install to SD-card.
 Language setting is now sent to the server on resume.
 Added sensor-assisted cursor movement.
 2011-09-13 v1.03.5
 Reduced mouse sensitivity when a finger is lifted to avoid jumping mouse.
 2011-09-24 v1.03.6
 Improved mouse accuracy.
 More sensor features.
 2011-09-27 v1.03.7
 Added North, South etc.. keys which maps to the cursor keys. The key North-W maps for example to both cursor up and cursor left. 
 Reduced the size of the hit-rectangle on keys to better match the actual graphics.
 2011-10-17 v1.03.8
 Added checkbox for enable/disable click on tap.
 2012-01-10 v1.03.9
 Button colors can be changed as a separate edit mode.
 Background and text colors can be changed (from settings).
 2012-01-27 v1.03.10
 Fix for password and keyboard bug. Thanks to "Carl" for the bug report!
 Increased text size.
 2012-02-20 v.104.1
 Added checkbox for enable/disable pinch zoom.
 Increased mouse sensitivity by removing the 25ms deadzone before moving the mouse.
 Minor color changes to the color picker. 

 */

package com.linuxfunkar.mousekeysremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
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

public class MouseKeysRemote extends Activity implements
		ColorPickerDialog.OnColorChangedListener, OnTouchListener,
		OnClickListener, OnItemClickListener, OnItemSelectedListener,
		SensorEventListener {
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

	private static final int UDP_PORT = 5555;

	private static final int INVALID_POINTER_ID = -1;

	public static final int KEYPAD_STATE_COMPAT = 0;
	public static final int KEYPAD_STATE_TOUCH = 1;

	public static final int KEY_STATE_DOWN = 0;
	public static final int KEY_STATE_UP = 1;
	public static final int KEY_STATE_DOWN_PENDING_UP = 2;
	public static final int KEY_STATE_DOWN_PENDING_CONFIRMED_DOWN = 3;

	public static final int MOUSE_NO_GESTURE = 0;
	public static final int MOUSE_AWAITING_GESTURE = 1;
	public static final int MOUSE_ZOOM = 2;

	private static final int MENU_NEW_GAME = 0;
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

	// Defined actions.
	/*
	 * Define constant here to use internally for each key. Translate the
	 * constant into a protocol specific value in getActionPress/Release. Add
	 * each constant to the key_actions array.
	 */

	static final int F1 = -25;
	static final int F2 = -26;
	static final int F3 = -27;
	static final int F4 = -28;
	static final int F5 = -29;
	static final int F6 = -30;
	static final int F7 = -31;
	static final int F8 = -32;
	static final int F9 = -33;
	static final int F10 = -34;
	static final int F11 = -35;
	static final int F12 = -36;
	static final int LCTRL = -37;
	static final int DEL = -38;
	static final int LSHIFT = -39;
	static final int LALT = -40;
	static final int INS = -41;
	static final int CHRA = -50;
	static final int CHRB = -51;
	static final int CHRC = -52;
	static final int CHRD = -53;
	static final int CHRE = -54;
	static final int CHRF = -55;
	static final int CHRG = -56;
	static final int CHRH = -57;
	static final int CHRI = -58;
	static final int CHRJ = -59;
	static final int CHRK = -60;
	static final int CHRL = -61;
	static final int CHRM = -62;
	static final int CHRN = -63;
	static final int CHRO = -64;
	static final int CHRP = -65;
	static final int CHRQ = -66;
	static final int CHRR = -67;
	static final int CHRS = -68;
	static final int CHRT = -69;
	static final int CHRU = -70;
	static final int CHRV = -71;
	static final int CHRW = -72;
	static final int CHRX = -73;
	static final int CHRY = -74;
	static final int CHRZ = -75;
	static final int CHR0 = -80;
	static final int CHR1 = -81;
	static final int CHR2 = -82;
	static final int CHR3 = -83;
	static final int CHR4 = -84;
	static final int CHR5 = -85;
	static final int CHR6 = -86;
	static final int CHR7 = -87;
	static final int CHR8 = -88;
	static final int CHR9 = -89;
	static final int SEMI_COLON = -90;
	static final int BACKSLASH = -91;
	static final int AT = -92;
	static final int SLASH = -93;
	static final int COLON = -94;
	static final int EQUALS = -95;
	static final int QUESTION = -96;

	public static final int ESC = -100;
	static final int SPACE = -101;
	static final int MOUSE_RIGHT = -102;
	static final int MOUSE_LEFT = -103;
	static final int MOUSE_UP = -104;
	static final int MOUSE_DOWN = -105;
	static final int MOUSE_LCLICK = -106;
	static final int MOUSE_RCLICK = -107;
	static final int MOUSE_WUP = -108;
	static final int MOUSE_WDOWN = -109;
	static final int CR = -110;
	static final int CUR_DOWN = -111;
	static final int CUR_UP = -112;
	static final int CUR_RIGHT = -113;
	static final int CUR_LEFT = -114;
	static final int NUM_KP_0 = -115;
	static final int NUM_KP_1 = -116;
	static final int NUM_KP_2 = -117;
	static final int NUM_KP_3 = -118;
	static final int NUM_KP_4 = -119;
	static final int NUM_KP_5 = -120;
	static final int NUM_KP_6 = -121;
	static final int NUM_KP_7 = -122;
	static final int NUM_KP_8 = -123;
	static final int NUM_KP_9 = -124;
	static final int PLUS = -125;
	static final int MINUS = -126;
	static final int TAB = -127;
	static final int PGUP = -128;
	static final int PGDOWN = -129;
	static final int COMMA = -130;
	static final int PERIOD = -131;
	static final int END = -132;
	static final int HOME = -133;
	static final int BACKSPACE = -134;
	static public final int DUMMY = -135;
	static public final int ZOOM_IN = -136;
	static public final int ZOOM_OUT = -137;
	static public final int ZOOM_RESET = -138;

	static public final int NORTH = -139;
	static public final int SOUTH = -140;
	static public final int WEST = -141;
	static public final int EAST = -142;
	static public final int NORTHWEST = -143;
	static public final int NORTHEAST = -144;
	static public final int SOUTHWEST = -145;
	static public final int SOUTHEAST = -146;

	private static final int CHECK_RELEASE = 0;
	private static final int CHECK_PRESS = 1;

	private static final int SENSORS_MOUSE = 0;
	private static final int SENSORS_CURSOR = 1;
	private static final int SENSORS_MOUSE_GAME = 2;

	private static final int MAX_KEYS = 200;

	private static final int DEFAULT_NUM_ROWS = 5;
	private static final int DEFAULT_NUM_COLS = 4;

	private static final int DEFAULT_MP_SIZE = 50;
	private static final float DEFAULT_TOUCH_PRESSURE = 0.7f;

	private static final int DEFAULT_LAYOUTS = 32;

	private static final long DEFAULT_TOUCH_TIME = 150;
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
	// The 'active pointer' is the one currently moving our object.
	private int mActivePointerId = INVALID_POINTER_ID;

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

	private int mode = MOUSE_NO_GESTURE;
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

	public static Paint mPaint;
	// private MaskFilter mEmboss;
	// private MaskFilter mBlur;

	public static int textCol;
	public static int backCol;
	public static int mousepadCol;
	public static int mousewheelCol;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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

		// } catch (Exception e) {debug(e.toString());}
		mode = MOUSE_NO_GESTURE;

		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		vibrateNotification = new Notification();
		vibrateNotification.vibrate = new long[] { 0, 40 };

		mySharedPreferences = getSharedPreferences("MY_PREFS",
				Activity.MODE_PRIVATE);

		// kpState = KEYPAD_STATE_TOUCH;

		keyPadView = new CustomDrawableView(this);
		keyPadView.setOnTouchListener(this);

		keyState = new int[MAX_KEYS];
		x1Pos = new int[MAX_KEYS];
		y1Pos = new int[MAX_KEYS];
		x2Pos = new int[MAX_KEYS];
		y2Pos = new int[MAX_KEYS];

		for (int i = 0; i < MAX_KEYS; i++) {
			keyState[i] = KEY_STATE_UP;
		}

		// Store available actions. Used when editing layouts.
		key_actions = new int[200];

		key_actions[0] = CUR_RIGHT;
		key_actions[1] = CUR_LEFT;
		key_actions[2] = CUR_UP;
		key_actions[3] = CUR_DOWN;
		key_actions[4] = MOUSE_RIGHT;
		key_actions[5] = MOUSE_LEFT;
		key_actions[6] = MOUSE_UP;
		key_actions[7] = MOUSE_DOWN;
		key_actions[8] = MOUSE_LCLICK;
		key_actions[9] = MOUSE_RCLICK;
		key_actions[10] = ESC;
		key_actions[11] = CR;
		key_actions[12] = NUM_KP_0;
		key_actions[13] = NUM_KP_1;
		key_actions[14] = NUM_KP_2;
		key_actions[15] = NUM_KP_3;
		key_actions[16] = NUM_KP_4;
		key_actions[17] = NUM_KP_5;
		key_actions[18] = NUM_KP_6;
		key_actions[19] = NUM_KP_7;
		key_actions[20] = NUM_KP_8;
		key_actions[21] = NUM_KP_9;

		key_actions[22] = SPACE;
		key_actions[23] = MOUSE_WUP;
		key_actions[24] = MOUSE_WDOWN;
		key_actions[25] = F1;
		key_actions[26] = F2;
		key_actions[27] = F3;
		key_actions[28] = F4;
		key_actions[29] = F5;
		key_actions[30] = F6;
		key_actions[31] = F7;
		key_actions[32] = F8;
		key_actions[33] = F9;
		key_actions[34] = F10;
		key_actions[35] = F11;
		key_actions[36] = F12;
		key_actions[37] = LCTRL;
		key_actions[38] = DEL;
		key_actions[39] = LSHIFT;
		key_actions[40] = LALT;
		key_actions[41] = INS;
		key_actions[42] = CHRA;
		key_actions[43] = CHRB;
		key_actions[44] = CHRC;
		key_actions[45] = CHRD;
		key_actions[46] = CHRE;
		key_actions[47] = CHRF;
		key_actions[48] = CHRG;
		key_actions[49] = CHRH;
		key_actions[50] = CHRI;
		key_actions[51] = CHRJ;
		key_actions[52] = CHRK;
		key_actions[53] = CHRL;
		key_actions[54] = CHRM;
		key_actions[55] = CHRN;
		key_actions[56] = CHRO;
		key_actions[57] = CHRP;
		key_actions[58] = CHRQ;
		key_actions[59] = CHRR;
		key_actions[60] = CHRS;
		key_actions[61] = CHRT;
		key_actions[62] = CHRU;
		key_actions[63] = CHRV;
		key_actions[64] = CHRW;
		key_actions[65] = CHRX;
		key_actions[66] = CHRY;
		key_actions[67] = CHRZ;
		key_actions[68] = CHR0;
		key_actions[69] = CHR1;
		key_actions[70] = CHR2;
		key_actions[71] = CHR3;
		key_actions[72] = CHR4;
		key_actions[73] = CHR5;
		key_actions[74] = CHR6;
		key_actions[75] = CHR7;
		key_actions[76] = CHR8;
		key_actions[77] = CHR9;
		key_actions[78] = PLUS;
		key_actions[79] = MINUS;
		key_actions[80] = TAB;
		key_actions[81] = PGUP;
		key_actions[82] = PGDOWN;
		key_actions[83] = COMMA;
		key_actions[84] = PERIOD;
		key_actions[85] = END;
		key_actions[86] = HOME;
		key_actions[87] = BACKSPACE;
		key_actions[88] = DUMMY;
		key_actions[89] = SEMI_COLON;
		key_actions[90] = BACKSLASH;
		key_actions[91] = AT;
		key_actions[92] = SLASH;
		key_actions[93] = COLON;
		key_actions[94] = EQUALS;
		key_actions[95] = QUESTION;
		key_actions[96] = ZOOM_IN;
		key_actions[97] = ZOOM_OUT;
		key_actions[98] = ZOOM_RESET;
		key_actions[99] = NORTH;
		key_actions[100] = SOUTH;
		key_actions[101] = WEST;
		key_actions[102] = EAST;
		key_actions[103] = NORTHWEST;
		key_actions[104] = NORTHEAST;
		key_actions[105] = SOUTHWEST;
		key_actions[106] = SOUTHEAST;

		number_of_keycodes = 107; // index+1

		EDIT_Names = new String[number_of_keycodes];

		for (int i = 0; i < number_of_keycodes; i++) {
			// debug(getActionName(key_actions[i]));
			EDIT_Names[i] = getActionName(key_actions[i]);
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
		setContentView(keyPadView);
		// }

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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	void setDefaultKeys() {
		debug("keys_layout: " + keys_layout);

		// SharedPreferences mySharedPreferences =
		// getSharedPreferences("MY_PREFS", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = mySharedPreferences.edit();

		if (mySharedPreferences.getInt("layout" + keys_layout + "key1", 99999) == 99999) // Set
																							// default
																							// values
																							// if
																							// layouts
																							// are
																							// empty
		{
			editor.putInt("layout" + keys_layout + "key1", ESC);
			editor.putInt("layout" + keys_layout + "key2", LCTRL);
			editor.putInt("layout" + keys_layout + "key3", CUR_UP);
			editor.putInt("layout" + keys_layout + "key4", CR);
			editor.putInt("layout" + keys_layout + "key5", SPACE);
			editor.putInt("layout" + keys_layout + "key6", CUR_LEFT);
			editor.putInt("layout" + keys_layout + "key7", DUMMY);
			editor.putInt("layout" + keys_layout + "key8", CUR_RIGHT);
			editor.putInt("layout" + keys_layout + "key9", TAB);
			editor.putInt("layout" + keys_layout + "key10", MOUSE_UP);
			editor.putInt("layout" + keys_layout + "key11", CUR_DOWN);
			editor.putInt("layout" + keys_layout + "key12", PLUS);
			editor.putInt("layout" + keys_layout + "key13", MOUSE_LEFT);
			editor.putInt("layout" + keys_layout + "key14", DUMMY);
			editor.putInt("layout" + keys_layout + "key15", MOUSE_RIGHT);
			editor.putInt("layout" + keys_layout + "key16", MINUS);
			editor.putInt("layout" + keys_layout + "key17", MOUSE_LCLICK);
			editor.putInt("layout" + keys_layout + "key18", MOUSE_DOWN);
			editor.putInt("layout" + keys_layout + "key19", MOUSE_RCLICK);
			editor.putInt("layout" + keys_layout + "key20", DUMMY);
			editor.commit();
		}
	}

	// Events not related to any views
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// debug("Activity *** onTouch *** MotionEvent: \n" + event.toString());

		int action = event.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			final float x = event.getX();
			final float y = event.getY();

			// debug("DOWN - X " + x + "Y " + y);

			break;
		}

		case MotionEvent.ACTION_UP: {

			final float x = event.getX();
			final float y = event.getY();

			// debug("UP - X " + x + "Y " + y);

		}
		}
		return super.onTouchEvent(event);

	}

	/** Creates the menu items **/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_MAIN, 0, "Virtual keyboard");
		menu.add(0, MENU_KP_TOUCH, 0, "Mouse/Keypad");
		menu.add(0, MENU_EDIT, 0, "Toggle edit");
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
					debug("1");
					debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					debug("2");
					debug(s.toString() + " start: " + start + " after: "
							+ after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					debug("3");
					debug(s.toString() + " start: " + start + " before: "
							+ before + " count: " + count);
					// String text = s.toString();
					if (count - before > 0) // Char added
					{
						char ch = s.charAt(start + count - 1);
						debug((int) ch);
						sendUDP("KBP" + (int) ch);
						sendUDP("KBR" + (int) ch);
					} else if (before - count == 1) {
						debug("backspace");
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
			setContentView(keyPadView);
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
			setContentView(keyPadView);
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
			keyPadView.invalidate();
			setContentView(keyPadView);
			return true;
		case MENU_LAYOUT:
			setContentView(layout_select);

			for (int i = 0; i < DEFAULT_LAYOUTS; i++) {
				LAYOUT_Names3[i] = mySharedPreferences.getString("layout_name"
						+ i, "Layout " + (i + 1));
			}
			ArrayAdapter<String> adapter_layout = new ArrayAdapter<String>(
					this, android.R.layout.simple_list_item_1, LAYOUT_Names3);
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
						.createFromResource(this, R.array.mouse_speed_array,
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
						.createFromResource(this, R.array.layout_mode_array,
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
					// debug("e1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("e2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("e3");
					// debug(s.toString() + " start: " + start + " before: " +
					// before + " count: " + count);
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
					// debug("e1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("e2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("e3");
					// debug(s.toString() + " start: " + start + " before: " +
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
					// debug("p1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("p2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("p3");
					// debug(s.toString() + " start: " + start + " before: " +
					// before + " count: " + count);
					passwd = (s.toString()).trim();
					SharedPreferences.Editor editor = mySharedPreferences
							.edit();
					editor.putString("password", passwd);
					editor.commit();
				}

			});

			mousepadentry.setText("" + mousepadRelativeSize);
			mousepadentry.addTextChangedListener(new TextWatcher() {

				public void afterTextChanged(Editable s) {
					// debug("m1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("m2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("m3");
					// debug(s.toString() + " start: " + start + " before: " +
					// before + " count: " + count);
					try {
						int val = Integer.parseInt((s.toString()).trim());
						if (val >= 0 & val <= 100) {
							mousepadRelativeSize = val;
							SharedPreferences.Editor editor = mySharedPreferences
									.edit();
							editor.putInt("mousepadRelativeSize" + keys_layout,
									mousepadRelativeSize);
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
					// debug("c1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("c2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("c3");
					// debug(s.toString() + " start: " + start + " before: " +
					// before + " count: " + count);
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
					// debug("r1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("r2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("r3");
					// debug(s.toString() + " start: " + start + " before: " +
					// before + " count: " + count);
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
					// debug("r1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("r2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("r3");
					// debug(s.toString() + " start: " + start + " before: " +
					// before + " count: " + count);
					try {
						float val = Float.parseFloat((s.toString()).trim());
						if (val > 0) {
							textSize = val;
							SharedPreferences.Editor editor = mySharedPreferences
									.edit();
							editor.putFloat("textSize" + keys_layout, textSize);
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
					// debug("r1");
					// debug(s.toString());

				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
					// debug("r2");
					// debug(s.toString() + " start: " + start + " after: " +
					// after + " count: " + count);
				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// debug("r3");
					// debug(s.toString() + " start: " + start + " before: " +
					// before + " count: " + count);
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
			setContentView(keyPadView);
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

	public void onNothingSelected(AdapterView parent) {
		// Do nothing.
		debug(lang_pos);
	}

	// Events from keyboard
	// Implement the OnKeyDown callback
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// debug("onKeyDown keyCode: "+ keyCode + "KeyEvent: " +
		// event.toString());
		// handleKey(keyCode, event.getAction());
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			debug("back");
			sendLanguage();
			setContentView(keyPadView);
		}
		}
		return false;
	}

	// Implement the OnKeyUp callback
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// debug("onKeyUp keyCode: "+ keyCode + "KeyEvent: " +
		// event.toString());
		// handleKey(keyCode, event.getAction());
		return false;
	}

	// Events from views

	// Implement the OnItemClickListener callback
	public void onItemClick(AdapterView v, View w, int i, long l) {
		// do something when the button is clicked
		debug("onItemClick: " + v.toString());
		debug("i:" + i + " l:" + l);
		if (v == choose_action) // Edit key action
		{
			debug("choose_action");
			debug(getActionName(key_actions[i]));

			// SharedPreferences mySharedPreferences =
			// getSharedPreferences("MY_PREFS", Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("layout" + keys_layout + "key" + key_to_edit,
					key_actions[i]);
			editor.commit();

			sendLanguage();
			setContentView(keyPadView);
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
			setContentView(keyPadView);
		}

	}

	private void reload_settings() {
		// Global values
		host = mySharedPreferences.getString("host", "192.168.10.184");
		port = mySharedPreferences.getInt("port", UDP_PORT);

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
		keyPadView.postInvalidate();
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

	// Implement the OnTouch callback

	public boolean onTouch(View v, MotionEvent touchEvent) {
		// debug("onTouch v:" + v.toString() + "MotionEvent: \n" +
		// ev.toString());

		if (v == keyPadView && calibrate == false) {
			return handleKeyPadViewTouch(v, touchEvent);
		} else if (v == keyPadView && calibrate == true) {
			// Store calibrated values
			debug(calibrate_x);
			debug(calibrate_y);
			calibrate = false;
			keyPadView.invalidate();
			return false; //
		} else // catch all
		{
			debug("onTouch: catch all");
			return false; //
		}
	}

	private boolean handleKeyPadViewTouch(View v, MotionEvent rawEvent) {
		// debug("handleKeyPadViewTouch");
		ev = WrapMotionEvent.wrap(rawEvent);

		action = ev.getAction();

		try { // Only for API 5 and up
				// debug("ev.getPointerCount():" + ev.getPointerCount());
			pointerCnt = ev.getPointerCount();
		} catch (Exception e) {// debug(e.toString());
		}

		for (int i = 0; i < pointerCnt; i++) // Repeat for each finger pressed
		{
			try { // Only for API 5 and up
				pointerIndex = ev.getPointerId(i); // Which pointer
				// debug("i: " + i + " pointerIndex: " + pointerIndex);
				xPosFloat = ev.getX(pointerIndex);
				yPosFloat = ev.getY(pointerIndex);
				xPos = (int) xPosFloat;
				yPos = (int) yPosFloat;
			} catch (Exception e) {
			} // debug("**** EXCEPTION ****");debug(e.toString());debug(ev.toString());}

			// debug("X: "+ xPosFloat + " Y: " + yPosFloat);

			if (enableMousePad) {
				if (yPos > keyPadView.getMeasuredHeight() - yOffsetBottom) // Mousepad
				{
					// debug("on mousepad");
					checkMouse();
				}
			}
			checkIfFingerUp(); // Make sure that an event based on a finger up
								// should never "hit" a key.
			checkIfOnKey(xPos, yPos); // Must check all keys even if on mousepad
										// since the user might have dragged a
										// finger from the keypad to the
										// mousepad.
		} // Repeat for all fingers
		checkIfAnyPendingKeys();
		return true;

	}

	private void checkIfAnyPendingKeys() {
		for (int i = 0; i < (numberOfKeyRows * numberOfKeyCols); i++) {
			if (keyState[i] == KEY_STATE_DOWN_PENDING_UP) {
				// debug("6  i:" + i);
				keyState[i] = KEY_STATE_UP;
				keyPadView.invalidate();
				handleKey(i + 1, MotionEvent.ACTION_UP);
			} else if (keyState[i] == KEY_STATE_DOWN_PENDING_CONFIRMED_DOWN) {
				// debug("7 i:" + i);
				keyState[i] = KEY_STATE_DOWN;
			}
		}
	}

	private void checkIfOnKey(int xPos, int yPos) {
		// debug("checkIfOnKey x: " + xPos + "y: " + yPos);
		for (int i = 0; i < (numberOfKeyRows * numberOfKeyCols); i++) {
			if ((xPos > x1Pos[i] + (keyWidth / 10) & xPos < x2Pos[i]
					- (keyWidth / 10))
					& (yPos > y1Pos[i] + (keyHeight / 10) & yPos < y2Pos[i]
							- (keyHeight / 10))) {
				// debug("Hit on key: " + i);
				if (keyState[i] == KEY_STATE_UP) // Key down from up
				{
					// debug("1 i:" + i);
					keyState[i] = KEY_STATE_DOWN_PENDING_CONFIRMED_DOWN;
					keyPadView.invalidate();
					handleKey(i + 1, MotionEvent.ACTION_DOWN);
				} else if (keyState[i] == KEY_STATE_DOWN) {
					// debug("2 i:" + i);
					keyState[i] = KEY_STATE_DOWN_PENDING_CONFIRMED_DOWN; // Stay
																			// down
																			// and
																			// make
																			// sure
																			// no
																			// other
																			// event
																			// triggers
																			// an
																			// UP.
				} else if (keyState[i] == KEY_STATE_DOWN_PENDING_UP) // Stay
																		// down
				{
					// debug("3 i:" + i);
					keyState[i] = KEY_STATE_DOWN_PENDING_CONFIRMED_DOWN; // Stay
																			// down
																			// and
																			// make
																			// sure
																			// no
																			// other
																			// event
																			// triggers
																			// an
																			// UP.
				} else if (keyState[i] == KEY_STATE_DOWN_PENDING_CONFIRMED_DOWN) // Ignore
				{
					// debug("4 i:" + i);
				}
			} else // No hit on key i. Mark it as candidate for an UP event.
			{
				// debug("No hit on key: " + i);
				if (keyState[i] == KEY_STATE_DOWN) {
					// debug("5 i:" + i);
					keyState[i] = KEY_STATE_DOWN_PENDING_UP;
				}
			}
		}
	}

	/* 	
 * 
 */
	private boolean handleKey(int key_num, int action) {
		// debug("handleKey key_num: " + key_num + " action: " + action);
		int key_value = getKeyValue(key_num);
		if (action == MotionEvent.ACTION_DOWN && key_value != 0) {
			// debug("handleKey key_num: " + key_num + " action: " + action);
			if (edit == EditMode.Binding) {
				debug("edit key binding");
				key_to_edit = key_num;
				setContentView(layout_action);
			} else if (edit == EditMode.Name) {
				debug("edit key name");
				key_to_edit = key_num;
				display_edit_name_layout();
			} else if (edit == EditMode.Color) {
				debug("edit key col");
				key_to_edit = key_num;
				showColorPicker(mPaint.getColor(), ColorElement.Button);
			} else {
				int key_sticky = getKeyValueSticky(key_num);
				if (key_sticky == UNSTUCK_KEY) {
					keyDown(key_value);
					if (sticky) // Save sticky state for this key
					{
						SharedPreferences.Editor editor = mySharedPreferences
								.edit();
						editor.putInt("layout" + keys_layout + "key_sticky"
								+ key_num, STUCK_KEY);
						editor.commit();
					}
				} else { // Stuck key should be released when pressed again
					keyUp(key_value);
					SharedPreferences.Editor editor = mySharedPreferences
							.edit();
					editor.putInt("layout" + keys_layout + "key_sticky"
							+ key_num, UNSTUCK_KEY_PENDING_RELEASE);
					editor.commit();
				}
			}
		} else if (action == MotionEvent.ACTION_UP && key_value != 0) {
			// debug("handleKey key_num: " + key_num + " action: " + action);
			if (edit == EditMode.None) // Normal keypad
			{
				int key_sticky = getKeyValueSticky(key_num);
				if (key_sticky == UNSTUCK_KEY) {
					keyUp(key_value);
				} else if (key_sticky == UNSTUCK_KEY_PENDING_RELEASE) // Ignore
																		// recently
																		// unstucked
																		// keys
				{
					SharedPreferences.Editor editor = mySharedPreferences
							.edit();
					editor.putInt("layout" + keys_layout + "key_sticky"
							+ key_num, UNSTUCK_KEY);
					editor.commit();
				} // Ignore stuck keys
			}
		}
		return false;
	}

	void checkMouse() {
		switch (action & MotionEvent.ACTION_MASK) // Check if first or second
													// finger down (for gesture)
		{
		case MotionEvent.ACTION_DOWN: // First finger
		{
			// debug("on mousepad 1");
			// int actionPointerId = (action &
			// MotionEvent.ACTION_POINTER_INDEX_MASK) >>
			// MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			// debug("actionPointerId: " + actionPointerId + " i: " + i);
			// int pId = ev.getPointerId(actionPointerId);
			mouseMovePointer = pointerIndex; // pId;
			xPosFirstFloat = xPosFloat;
			yPosFirstFloat = yPosFloat;
			accXFloatOld = 0;
			accYFloatOld = 0;
			mode = MOUSE_AWAITING_GESTURE;
			mouseMovePressure = 0;
			old_zoom = 0; // No zoom at the beginning of the gesture
			// mode = MOUSE_ZOOM; // Test
			break;
		}
		case MotionEvent.ACTION_POINTER_DOWN: // Second or more fingers
		{
			// debug("on mousepad 2");
			// Only trigger on the actual finger which caused the event
			int actionPointerId = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			// debug("actionPointerId: " + actionPointerId + " i: " + i);
			int pId = ev.getPointerId(actionPointerId);
			// debug("pId: " + pId);
			if (pId == pointerIndex) {
				if (yPos > keyPadView.getMeasuredHeight() - yOffsetBottom) // Mousepad
				{
					if (mode == MOUSE_AWAITING_GESTURE) {
						// debug("on mousepad 3");
						oldDist = spacing(ev);
						// debug(oldDist);
						// if (oldDist > 10f)
						// {
						// debug("on mousepad 3.5");
						mode = MOUSE_ZOOM;
						// }
					} else if (mode == MOUSE_ZOOM) {
						// debug("on mousepad mode==MOUSE_ZOOM");
					} else { // Second finger down but this is first finger on
								// mousepad
						// debug("Second finger down but this is first finger on mousepad");
						// debug(" pointerIndex: " + pointerIndex);
						mouseMovePointer = pointerIndex; // pId;
						xPosFirstFloat = xPosFloat;
						yPosFirstFloat = yPosFloat;
						accXFloatOld = 0;
						accYFloatOld = 0;
						mode = MOUSE_AWAITING_GESTURE;
						mouseMovePressure = 0;
						old_zoom = 0; // No zoom at the beginning of the gesture
					}
				} // End if on mousepad
				else {
					// debug("on mousepad 4");
				}
			}
		}
		case MotionEvent.ACTION_MOVE: {
			// debug("on mousepad 5");
			// Om samma finger som startade musmove så sätt mode =
			// MOUSE_NO_GESTURE;
			// Only trigger on the actual finger which caused the event by
			// looking at the event to get the pointer index for the pointer
			// that moved.
			int actionPointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			// debug("actionPointerId: " + actionPointerId + " i: " + i);
			int pId = ev.getPointerId(actionPointerIndex); // Get the pointerId
															// from the pointer
															// index.
			// debug("pId: " + pId);
			if (pId == pointerIndex) {
				// debug("pointerIndex: " + pointerIndex);
				if (mouseMovePointer == pointerIndex) {
					if (mode == MOUSE_AWAITING_GESTURE) {
						mode = MOUSE_NO_GESTURE;
					}
				}
			}
			break;
		}
		}

		// Check if mouse should be moved or for mouseclick.
		if ((mode == MOUSE_NO_GESTURE || mode == MOUSE_AWAITING_GESTURE)
				&& pointerIndex == mouseMovePointer) {
			// debug("on mousepad 7");
			// check distance to first touch
			accXFloat = (xPosFloat - xPosFirstFloat);
			accYFloat = (yPosFloat - yPosFirstFloat);
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

			down = ev.getDownTime(); // Time for first press event
			up = ev.getEventTime(); // Time for this event

			switch (action & MotionEvent.ACTION_MASK) // Check if move or
														// possible tap
			{
			case MotionEvent.ACTION_UP: {
				checkTap();
				break;
			}
			case MotionEvent.ACTION_POINTER_UP: {
				checkTap();
				break;
			}
			case MotionEvent.ACTION_CANCEL: {
				checkTap();
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				moveMouse();
				break;
			}
			}
		} // end- MOUSE_NO_GESTURE
		else if (mode == MOUSE_ZOOM) {
			//
			// debug("Zoom");
			float newDist = spacing(ev);
			// debug(newDist);
			float diff = newDist - oldDist;
			// debug(diff);
			int zoom = (int) diff / 30;
			// debug("old_zoom: " + old_zoom);
			// debug("new_zoom: " + zoom);
			int zoom_diff = zoom - old_zoom;
			if (zoom_diff != 0 && enablePinchZoom) {
				String msg = "MPZ" + zoom_diff; // Mouse Pinch Zoom.. Positive
												// values means zoom in and
												// negative zoom out.
				// debug("UDP msg: " + msg);
				sendUDP(msg);
				old_zoom = old_zoom + zoom_diff;
			}
		} // end- MOUSE_ZOOM
		else {
			debug("mode: " + mode + " pointerIndex: " + pointerIndex);
		}
	}

	void moveMouse() {
		if ((up - down) >= 0) // 25 Only move mouse if the event is long enough
		{

			if (mouseMovePressure == 0) {
				mouseMovePressure = ev.getPressure(mouseMovePointer);
			}
			currentMousePressure = ev.getPressure(mouseMovePointer);
			// debug("mouseMovePressure:" + mouseMovePressure);
			// debug("currentMousePressure:" + currentMousePressure);

			if (currentMousePressure < (mouseMovePressure * DEFAULT_TOUCH_PRESSURE)) // Only
																						// move
																						// if
																						// not
																						// too
																						// low
																						// pressure
			{
				// debug("Too low pressure ");
			} else {
				if (accX != 0) // Only move if there has been a movement
				{
					// store new distance
					accXFloatOld = accXFloat;
					if (enableMouseWheel
							& (yPos > keyPadView.getMeasuredHeight()
									- yOffsetBottom)
							& xPos > (keyPadView.getMeasuredWidth() - mouseWheelWidth)) // Mousepad
																						// wheel
					{
						// debug("on mouse wheel");
					} else {
						// debug("accX: "+ accX);
						String msg = "XMM" + accX * (mouse_speed_pos + 1);
						// debug("UDP msg: " + msg);
						sendUDP(msg);
						// store new distance
					}
				} else // No movement
				{
					// debug("No movement");
				}

				if (accY != 0) // Only move if there has been a movement
				{
					accYFloatOld = accYFloat;
					if (enableMouseWheel
							& (yPos > keyPadView.getMeasuredHeight()
									- yOffsetBottom)
							& xPos > (keyPadView.getMeasuredWidth() - mouseWheelWidth)) // Mousepad
																						// wheel
					{
						// debug("on mouse wheel");
						int newAccY = (int) (accYFloatDiff / 6);
						if (newAccY != 0) {
							sendUDP("MWS" + newAccY);
						} else // Make sure that something happens even for
								// small movements
						{
							if (accY > 0) {
								sendUDP("MWS1");
							} else {
								sendUDP("MWS-1");
							}
						}
					} else {
						// debug("accY: "+ accY);
						String msg = "YMM" + accY * (mouse_speed_pos + 1);
						// debug("UDP msg: " + msg);
						sendUDP(msg);
					}
				} else // No movement
				{
					// debug("No movement");
				}
			} // End check pressure
		} else {
			// debug("ignoring too short touch"); // Ok to ignore, mouse will
			// eventually move the right distance anyway.
		} // End check press duration
	}

	void checkTap() {
		// Check tap
		if (enableMouseWheel
				& xPos > (keyPadView.getMeasuredWidth() - mouseWheelWidth)
				|| !enableClickOnTap) // On mousepad wheel
		{
			// debug("tap? (on mousewheel)");
		} else {
			// debug("tap?");
			// debug("Down time: " + ev.getDownTime());
			// debug("Event time: " + ev.getEventTime());
			// long down = ev.getDownTime();
			// long up = ev.getEventTime();
			if ((up - down) < DEFAULT_TOUCH_TIME) {
				// debug("tap!");

				String msg = "MLC";
				// debug("UDP msg: " + msg);
				sendUDP(msg);
				msg = "MLR";
				// debug("UDP msg: " + msg);
				sendUDP(msg);
				if (enableVibrate) {
					nm.notify(1, vibrateNotification);
				}
			}
		}
	} // Check tap

	void checkIfFingerUp() // Stop mouse move and make sure that an event based
							// on a finger up should never "hit" a key.
	{
		// debug("checkIfFingerUp " + action);
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP: {
			// debug("checkIfFingerUp ACTION_UP");
			xPos = -1; // -1 means it will not hit a key
			yPos = -1;
			mode = MOUSE_NO_GESTURE;
			mouseMovePointer = -1;
			// last_up = ev.getEventTime(); // Time for this event
			break;
		}
		case MotionEvent.ACTION_POINTER_UP: {
			// debug("checkIfFingerUp ACTION_POINTER_UP");
			// Only trigger on the actual finger which caused the event
			int actionPointerId = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			// debug("actionPointerId: " + actionPointerId + " i: " + i);
			int pId = ev.getPointerId(actionPointerId);
			// debug("pId: " + pId);
			if (pId == pointerIndex) {
				if (yPos > keyPadView.getMeasuredHeight() - yOffsetBottom) // Mousepad
				{ // Only stop mouse if the finger up was on mousepad
					debug("ACTION_POINTER_UP mousepad - stop mouse");
					mouseMovePointer = -1;
					mode = MOUSE_NO_GESTURE;
					// last_up = ev.getEventTime(); // Time for this event
				}
				xPos = -1; // -1 means it will not hit a key
				yPos = -1;
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL: {
			// debug("checkIfFingerUp ACTION_CANCEL");
			break;
		}
		default:
			// debug("checkIfFingerUp Default: " + action);
		}
	}

	private void display_edit_name_layout() {
		setContentView(R.layout.edit_key_name);
		EditText name = (EditText) findViewById(R.id.keyboard_input);
		name.setText(mySharedPreferences.getString("nameOfKey" + "layout"
				+ keys_layout + "key" + key_to_edit,
				getActionName(getKeyValue(key_to_edit))));
		name.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				// debug("1");
				// debug(s.toString());
				// setContentView(keyPadView);

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				// debug("2");
				// debug(s.toString() + " start: " + start + " after: " + after
				// + " count: " + count);
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String name = (s.toString()).trim();
				// debug(name.toString());
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putString("nameOfKey" + "layout" + keys_layout + "key"
						+ key_to_edit, name);
				editor.commit();

				// String check = mySharedPreferences.getString("nameOfKey"
				// + "layout" + keys_layout + "key" + key_to_edit, "nada");
				// debug("nameOfKey" + "layout" + keys_layout + "key" +
				// key_to_edit);
				// debug("Res: " + check.toString());
				keyPadView.postInvalidate();
			}
		});
	}

	static public int getKeyValue(int key_num) {
		int value = mySharedPreferences.getInt("layout" + keys_layout + "key"
				+ key_num, DUMMY);
		return value;
	}

	static public int getKeyValueSticky(int key_num) {
		int value = mySharedPreferences.getInt("layout" + keys_layout
				+ "key_sticky" + key_num, UNSTUCK_KEY);

		return value;
	}

	private void keyDown(int key_value) {
		// debug("keyDown key_value: " + key_value);
		// debug("Action: " + getActionName(key_value));
		if (key_value == DUMMY) {
			// debug("Dummy");
		} else {
			String msg = getActionPress(key_value);
			// debug("UDP msg: " + msg);
			sendUDP(msg);
			if (enableVibrate) {
				nm.notify(1, vibrateNotification);
			}
		}
	}

	// try {
	// URL url = new URL("http://www.linuxfunkar.se/data");
	// URLConnection connection = url.openConnection();
	// HttpURLConnection httpConnection = (HttpURLConnection)connection;
	//
	// int responseCode = httpConnection.getResponseCode();
	// if (responseCode == HttpURLConnection.HTTP_OK)
	// {
	// InputStream in= httpConnection.getInputStream();
	// in.close();
	// }
	//
	// } catch (MalformedURLException e) {debug(e.toString());}
	// catch (IOException e) {debug(e.toString());}
	//

	private void keyUp(int key_value) {
		// debug("keyUp: " + key_value);
		// debug("Action: " + getActionName(key_value));
		if (key_value == DUMMY) {
			// debug("Dummy");
		} else if (key_value == MOUSE_WUP) {
			// debug("Dummy");
		} else if (key_value == MOUSE_WDOWN) {
			// debug("Dummy");
		} else if (key_value == ZOOM_IN) {
			// debug("Dummy");
		} else if (key_value == ZOOM_OUT) {
			// debug("Dummy");
		} else if (key_value == ZOOM_RESET) {
			// debug("Dummy");
		} else {
			String msg = getActionRelease(key_value);
			// debug("UDP msg: " + msg);
			sendUDP(msg);
		}
	}

	void sendUDP(String msg_plain) {
		String msg = passwd + msg_plain;
		// debug("sendUDP msg: " + msg);
		try {
			DatagramSocket s = new DatagramSocket();

			InetAddress local = InetAddress.getByName(host);

			int msg_length = msg.length();

			byte[] message = msg.getBytes();

			DatagramPacket p = new DatagramPacket(message, msg_length, local,
					port);

			s.send(p);
		} catch (Exception e) {
			debug(e.toString());
		}
	}

	// Translate internal keycode identifiers to protocol specific ones.
	String getActionPress(int id) {
		switch (id) {
		case CUR_LEFT:
			return new String("KBP" + 37);
		case CUR_RIGHT:
			return new String("KBP" + 39);
		case CUR_UP:
			return new String("KBP" + 38);
		case CUR_DOWN:
			return new String("KBP" + 40);
		case MOUSE_RIGHT:
			return new String("MMR");
		case MOUSE_LEFT:
			return new String("MML");
		case MOUSE_LCLICK:
			return new String("MLC");
		case MOUSE_RCLICK:
			return new String("MRC");
		case MOUSE_UP:
			return new String("MMU");
		case MOUSE_DOWN:
			return new String("MMD");
		case MOUSE_WUP:
			return new String("MWU");
		case MOUSE_WDOWN:
			return new String("MWD");
		case ESC:
			return new String("KBP" + 27);
		case SPACE:
			return new String("KBP" + 32);
		case CR:
			return new String("KBP" + 10);
		case NUM_KP_0:
			return new String("KBP" + 1015);
		case NUM_KP_1:
			return new String("KBP" + 1016);
		case NUM_KP_2:
			return new String("KBP" + 1017);
		case NUM_KP_3:
			return new String("KBP" + 1018);
		case NUM_KP_4:
			return new String("KBP" + 1019);
		case NUM_KP_5:
			return new String("KBP" + 1020);
		case NUM_KP_6:
			return new String("KBP" + 1021);
		case NUM_KP_7:
			return new String("KBP" + 1022);
		case NUM_KP_8:
			return new String("KBP" + 1023);
		case NUM_KP_9:
			return new String("KBP" + 1024);
		case F1:
			return new String("KBP" + 1101);
		case F2:
			return new String("KBP" + 1102);
		case F3:
			return new String("KBP" + 1103);
		case F4:
			return new String("KBP" + 1104);
		case F5:
			return new String("KBP" + 1105);
		case F6:
			return new String("KBP" + 1106);
		case F7:
			return new String("KBP" + 1107);
		case F8:
			return new String("KBP" + 1108);
		case F9:
			return new String("KBP" + 1109);
		case F10:
			return new String("KBP" + 1110);
		case F11:
			return new String("KBP" + 1111);
		case F12:
			return new String("KBP" + 1112);
		case LCTRL:
			return new String("KBP" + 1200);
		case DEL:
			return new String("KBP" + 1201);
		case LSHIFT:
			return new String("KBP" + 1202);
		case LALT:
			return new String("KBP" + 1203);
		case INS:
			return new String("KBP" + 1204);
		case PLUS:
			return new String("KBP" + 1205);
		case MINUS:
			return new String("KBP" + 1206);
		case TAB:
			return new String("KBP" + 1207);
		case BACKSPACE:
			return new String("KBP" + 1208);
		case PGUP:
			return new String("KBP" + 1209);
		case PGDOWN:
			return new String("KBP" + 1210);
		case COMMA:
			return new String("KBP" + 1215);
		case PERIOD:
			return new String("KBP" + 1216);
		case END:
			return new String("KBP" + 1217);
		case EQUALS:
			return new String("KBP" + 1218);
		case HOME:
			return new String("KBP" + 1219);
		case QUESTION:
			return new String("KBP" + 63);
		case SEMI_COLON:
			return new String("KBP" + 59);
		case BACKSLASH:
			return new String("KBP" + 1213);
		case ZOOM_IN:
			return new String("MPZ1");
		case ZOOM_OUT:
			return new String("MPZ-1");
		case ZOOM_RESET:
			return new String("MZR");
		case AT:
			return new String("KBP" + 1212);
		case SLASH:
			return new String("KBP" + 47);
		case COLON:
			return new String("KBP" + 58);
		case CHRA:
			return new String("KBP" + 97);
		case CHRB:
			return new String("KBP" + 98);
		case CHRC:
			return new String("KBP" + 99);
		case CHRD:
			return new String("KBP" + 100);
		case CHRE:
			return new String("KBP" + 101);
		case CHRF:
			return new String("KBP" + 102);
		case CHRG:
			return new String("KBP" + 103);
		case CHRH:
			return new String("KBP" + 104);
		case CHRI:
			return new String("KBP" + 105);
		case CHRJ:
			return new String("KBP" + 106);
		case CHRK:
			return new String("KBP" + 107);
		case CHRL:
			return new String("KBP" + 108);
		case CHRM:
			return new String("KBP" + 109);
		case CHRN:
			return new String("KBP" + 110);
		case CHRO:
			return new String("KBP" + 111);
		case CHRP:
			return new String("KBP" + 112);
		case CHRQ:
			return new String("KBP" + 113);
		case CHRR:
			return new String("KBP" + 114);
		case CHRS:
			return new String("KBP" + 115);
		case CHRT:
			return new String("KBP" + 116);
		case CHRU:
			return new String("KBP" + 117);
		case CHRV:
			return new String("KBP" + 118);
		case CHRW:
			return new String("KBP" + 119);
		case CHRX:
			return new String("KBP" + 120);
		case CHRY:
			return new String("KBP" + 121);
		case CHRZ:
			return new String("KBP" + 122);
		case CHR0:
			return new String("KBP" + 48);
		case CHR1:
			return new String("KBP" + 49);
		case CHR2:
			return new String("KBP" + 50);
		case CHR3:
			return new String("KBP" + 51);
		case CHR4:
			return new String("KBP" + 52);
		case CHR5:
			return new String("KBP" + 53);
		case CHR6:
			return new String("KBP" + 54);
		case CHR7:
			return new String("KBP" + 55);
		case CHR8:
			return new String("KBP" + 56);
		case CHR9:
			return new String("KBP" + 57);
		case NORTH:
			return new String("KBP" + 1300);
		case SOUTH:
			return new String("KBP" + 1301);
		case WEST:
			return new String("KBP" + 1302);
		case EAST:
			return new String("KBP" + 1303);
		case NORTHWEST:
			return new String("KBP" + 1304);
		case NORTHEAST:
			return new String("KBP" + 1305);
		case SOUTHWEST:
			return new String("KBP" + 1306);
		case SOUTHEAST:
			return new String("KBP" + 1307);

		default:
			return new String("NOP");
		}
	}

	String getActionRelease(int id) {
		switch (id) {
		case CUR_LEFT:
			return new String("KBR" + 37);
		case CUR_RIGHT:
			return new String("KBR" + 39);
		case CUR_UP:
			return new String("KBR" + 38);
		case CUR_DOWN:
			return new String("KBR" + 40);
		case MOUSE_RIGHT:
			return new String("MSR");
		case MOUSE_LEFT:
			return new String("MSL");
		case MOUSE_LCLICK:
			return new String("MLR");
		case MOUSE_RCLICK:
			return new String("MRR");
		case MOUSE_UP:
			return new String("MSU");
		case MOUSE_DOWN:
			return new String("MSD");
		case ESC:
			return new String("KBR" + 27);
		case SPACE:
			return new String("KBR" + 32);
		case CR:
			return new String("KBR" + 10);
		case NUM_KP_0:
			return new String("KBR" + 1015);
		case NUM_KP_1:
			return new String("KBR" + 1016);
		case NUM_KP_2:
			return new String("KBR" + 1017);
		case NUM_KP_3:
			return new String("KBR" + 1018);
		case NUM_KP_4:
			return new String("KBR" + 1019);
		case NUM_KP_5:
			return new String("KBR" + 1020);
		case NUM_KP_6:
			return new String("KBR" + 1021);
		case NUM_KP_7:
			return new String("KBR" + 1022);
		case NUM_KP_8:
			return new String("KBR" + 1023);
		case NUM_KP_9:
			return new String("KBR" + 1024);
		case F1:
			return new String("KBR" + 1101);
		case F2:
			return new String("KBR" + 1102);
		case F3:
			return new String("KBR" + 1103);
		case F4:
			return new String("KBR" + 1104);
		case F5:
			return new String("KBR" + 1105);
		case F6:
			return new String("KBR" + 1106);
		case F7:
			return new String("KBR" + 1107);
		case F8:
			return new String("KBR" + 1108);
		case F9:
			return new String("KBR" + 1109);
		case F10:
			return new String("KBR" + 1110);
		case F11:
			return new String("KBR" + 1111);
		case F12:
			return new String("KBR" + 1112);
		case LCTRL:
			return new String("KBR" + 1200);
		case DEL:
			return new String("KBR" + 1201);
		case LSHIFT:
			return new String("KBR" + 1202);
		case LALT:
			return new String("KBR" + 1203);
		case INS:
			return new String("KBR" + 1204);
		case PLUS:
			return new String("KBR" + 1205);
		case MINUS:
			return new String("KBR" + 1206);
		case TAB:
			return new String("KBR" + 1207);
		case BACKSPACE:
			return new String("KBR" + 1208);
		case PGUP:
			return new String("KBR" + 1209);
		case PGDOWN:
			return new String("KBR" + 1210);
		case COMMA:
			return new String("KBR" + 1215);
		case PERIOD:
			return new String("KBR" + 1216);
		case END:
			return new String("KBR" + 1217);
		case HOME:
			return new String("KBR" + 1219);
		case SEMI_COLON:
			return new String("KBR" + 59);
		case BACKSLASH:
			return new String("KBR" + 1213);
		case AT:
			return new String("KBR" + 1212);
		case SLASH:
			return new String("KBR" + 47);
		case COLON:
			return new String("KBR" + 58);
		case EQUALS:
			return new String("KBR" + 1218);
		case QUESTION:
			return new String("KBR" + 63);
		case CHRA:
			return new String("KBR" + 97);
		case CHRB:
			return new String("KBR" + 98);
		case CHRC:
			return new String("KBR" + 99);
		case CHRD:
			return new String("KBR" + 100);
		case CHRE:
			return new String("KBR" + 101);
		case CHRF:
			return new String("KBR" + 102);
		case CHRG:
			return new String("KBR" + 103);
		case CHRH:
			return new String("KBR" + 104);
		case CHRI:
			return new String("KBR" + 105);
		case CHRJ:
			return new String("KBR" + 106);
		case CHRK:
			return new String("KBR" + 107);
		case CHRL:
			return new String("KBR" + 108);
		case CHRM:
			return new String("KBR" + 109);
		case CHRN:
			return new String("KBR" + 110);
		case CHRO:
			return new String("KBR" + 111);
		case CHRP:
			return new String("KBR" + 112);
		case CHRQ:
			return new String("KBR" + 113);
		case CHRR:
			return new String("KBR" + 114);
		case CHRS:
			return new String("KBR" + 115);
		case CHRT:
			return new String("KBR" + 116);
		case CHRU:
			return new String("KBR" + 117);
		case CHRV:
			return new String("KBR" + 118);
		case CHRW:
			return new String("KBR" + 119);
		case CHRX:
			return new String("KBR" + 120);
		case CHRY:
			return new String("KBR" + 121);
		case CHRZ:
			return new String("KBR" + 122);
		case CHR0:
			return new String("KBR" + 48);
		case CHR1:
			return new String("KBR" + 49);
		case CHR2:
			return new String("KBR" + 50);
		case CHR3:
			return new String("KBR" + 51);
		case CHR4:
			return new String("KBR" + 52);
		case CHR5:
			return new String("KBR" + 53);
		case CHR6:
			return new String("KBR" + 54);
		case CHR7:
			return new String("KBR" + 55);
		case CHR8:
			return new String("KBR" + 56);
		case CHR9:
			return new String("KBR" + 57);
		case NORTH:
			return new String("KBR" + 1300);
		case SOUTH:
			return new String("KBR" + 1301);
		case WEST:
			return new String("KBR" + 1302);
		case EAST:
			return new String("KBR" + 1303);
		case NORTHWEST:
			return new String("KBR" + 1304);
		case NORTHEAST:
			return new String("KBR" + 1305);
		case SOUTHWEST:
			return new String("KBR" + 1306);
		case SOUTHEAST:
			return new String("KBR" + 1307);

		default:
			return new String("NOP");
		}
	}

	public static String getActionName(int id) {
		switch (id) {
		case CUR_LEFT:
			return new String("Left");
		case CUR_RIGHT:
			return new String("Right");
		case CUR_UP:
			return new String("Up");
		case CUR_DOWN:
			return new String("Down");
		case MOUSE_RIGHT:
			return new String("M-Right");
		case MOUSE_LEFT:
			return new String("M-Left");
		case MOUSE_LCLICK:
			return new String("M-LC");
		case MOUSE_RCLICK:
			return new String("M-RC");
		case MOUSE_UP:
			return new String("M-Up");
		case MOUSE_DOWN:
			return new String("M-Down");
		case MOUSE_WUP:
			return new String("M-WUp");
		case MOUSE_WDOWN:
			return new String("M-WDown");
		case ESC:
			return new String("Esc");
		case SPACE:
			return new String("Space");
		case CR:
			return new String("Enter");
		case NUM_KP_0:
			return new String("KP-0");
		case NUM_KP_1:
			return new String("KP-1");
		case NUM_KP_2:
			return new String("KP-2");
		case NUM_KP_3:
			return new String("KP-3");
		case NUM_KP_4:
			return new String("KP-4");
		case NUM_KP_5:
			return new String("KP-5");
		case NUM_KP_6:
			return new String("KP-6");
		case NUM_KP_7:
			return new String("KP-7");
		case NUM_KP_8:
			return new String("KP-8");
		case NUM_KP_9:
			return new String("KP-9");
		case F1:
			return new String("F1");
		case F2:
			return new String("F2");
		case F3:
			return new String("F3");
		case F4:
			return new String("F4");
		case F5:
			return new String("F5");
		case F6:
			return new String("F6");
		case F7:
			return new String("F7");
		case F8:
			return new String("F8");
		case F9:
			return new String("F9");
		case F10:
			return new String("F10");
		case F11:
			return new String("F11");
		case F12:
			return new String("F12");
		case LCTRL:
			return new String("Ctrl");
		case DEL:
			return new String("Del");
		case LSHIFT:
			return new String("Shift");
		case LALT:
			return new String("Alt");
		case INS:
			return new String("Ins");
		case CHRA:
			return new String(" a ");
		case CHRB:
			return new String(" b ");
		case CHRC:
			return new String(" c ");
		case CHRD:
			return new String(" d ");
		case CHRE:
			return new String(" e ");
		case CHRF:
			return new String(" f ");
		case CHRG:
			return new String(" g ");
		case CHRH:
			return new String(" h ");
		case CHRI:
			return new String(" i ");
		case CHRJ:
			return new String(" j ");
		case CHRK:
			return new String(" k ");
		case CHRL:
			return new String(" l ");
		case CHRM:
			return new String(" m ");
		case CHRN:
			return new String(" n ");
		case CHRO:
			return new String(" o ");
		case CHRP:
			return new String(" p ");
		case CHRQ:
			return new String(" q ");
		case CHRR:
			return new String(" r ");
		case CHRS:
			return new String(" s ");
		case CHRT:
			return new String(" t ");
		case CHRU:
			return new String(" u ");
		case CHRV:
			return new String(" v ");
		case CHRW:
			return new String(" w ");
		case CHRX:
			return new String(" x ");
		case CHRY:
			return new String(" y ");
		case CHRZ:
			return new String(" z ");
		case CHR0:
			return new String(" 0 ");
		case CHR1:
			return new String(" 1 ");
		case CHR2:
			return new String(" 2 ");
		case CHR3:
			return new String(" 3 ");
		case CHR4:
			return new String(" 4 ");
		case CHR5:
			return new String(" 5 ");
		case CHR6:
			return new String(" 6 ");
		case CHR7:
			return new String(" 7 ");
		case CHR8:
			return new String(" 8 ");
		case CHR9:
			return new String(" 9 ");
		case PLUS:
			return new String(" + ");
		case MINUS:
			return new String(" - ");
		case TAB:
			return new String("Tab");
		case PGUP:
			return new String("PgUp");
		case PGDOWN:
			return new String("PgDown");
		case COMMA:
			return new String(" , ");
		case PERIOD:
			return new String(" . ");
		case END:
			return new String("End");
		case HOME:
			return new String("Home");
		case BACKSPACE:
			return new String("Backspace");
		case DUMMY:
			return new String("n/c");
		case SEMI_COLON:
			return new String(" ; ");
		case BACKSLASH:
			return new String(" \\ ");
		case AT:
			return new String(" @ ");
		case SLASH:
			return new String(" / ");
		case EQUALS:
			return new String(" = ");
		case COLON:
			return new String(" : ");
		case QUESTION:
			return new String(" ? ");
		case ZOOM_IN:
			return new String("Zoom in");
		case ZOOM_OUT:
			return new String("Zoom out");
		case ZOOM_RESET:
			return new String("Zoom reset");
		case NORTH:
			return new String("North");
		case SOUTH:
			return new String("South");
		case WEST:
			return new String("West");
		case EAST:
			return new String("East");
		case NORTHWEST:
			return new String("North-W");
		case NORTHEAST:
			return new String("North-E");
		case SOUTHWEST:
			return new String("South-W");
		case SOUTHEAST:
			return new String("South-E");

		default:
			return new String("???");
			// char[] ch = new char[1];
			// ch[0] = (char)id;
			// return new String(ch);
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

	private float spacing(WrapMotionEvent ev) {
		float x = ev.getX(0) - ev.getX(1);
		float y = ev.getY(0) - ev.getY(1);
		return FloatMath.sqrt(x * x + y * y);
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
			setContentView(keyPadView);
		} else if (elem == ColorElement.Back) {
			backCol = color;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("background_col" + keys_layout, color);
			editor.commit();
			keyPadView.postInvalidate();
			setContentView(keyPadView);
		} else if (elem == ColorElement.MousePad) {
			mousepadCol = color;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("mousepad_col" + keys_layout, color);
			editor.commit();
			keyPadView.postInvalidate();
			setContentView(keyPadView);
		} else if (elem == ColorElement.MouseWheel) {
			mousewheelCol = color;
			SharedPreferences.Editor editor = mySharedPreferences.edit();
			editor.putInt("mousewheel_col" + keys_layout, color);
			editor.commit();
			keyPadView.postInvalidate();
			setContentView(keyPadView);
		}
	}

	@Override
	public void colorChanged(int color, int elem) {
		colorChanged_int(color, ColorElement.valueOf("" + elem));
	}

}
