package com.linuxfunkar.mousekeysremote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	private static Preferences preferences;
	private SharedPreferences prefs;

	public static Preferences getInstance(Context context) {
		if (preferences == null)
			preferences = new Preferences(context);
		return preferences;
	}

	private Preferences(Context context) {
		prefs = context.getSharedPreferences("MY_PREFS", Activity.MODE_PRIVATE);
	}

	public boolean isButtonSticky(int key_num) {
		return prefs.getBoolean("KeySticky" + "layout" + getLayout() + "key"
				+ key_num, false);
	}

	public void setButtonSticky(int key_num, boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("KeySticky" + "layout" + getLayout() + "key"
				+ key_num, value);
		editor.commit();
	}

	public boolean isButtonVisible(int key_num) {
		return prefs.getBoolean("KeyVisible" + "layout" + getLayout() + "key"
				+ key_num, true);
	}

	public void setButtonVisible(int key_num, boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("KeyVisible" + "layout" + getLayout() + "key"
				+ key_num, value);
		editor.commit();
	}

	public String getHost() {
		return prefs.getString("host", "192.168.10.184");
	}

	public int getPort() {
		return prefs.getInt("port", Constants.UDP_PORT);
	}

	public String getPassword() {
		return prefs.getString("password", "");
	}

	public int getLayout() {
		return prefs.getInt("layout", Constants.DUMMY);
	}

	public int getKeyValue(int key_num) {
		int value = prefs.getInt("layout" + getLayout() + "key" + key_num,
				Constants.DUMMY);
		return value;
	}

	public String getKeyLabel(int key_num) {
		return prefs.getString("layout" + getLayout() + "label" + key_num,
				Constants.getActionName(getKeyValue(key_num)));
	}

	public int getKeyColor(int key_num) {
		return prefs.getInt("KeyColor" + "layout" + getLayout() + "key"
				+ key_num, -1);
	}

	public String getKeyCustomCommand(int key_num) {
		return prefs.getString("KeyCustom" + "layout" + getLayout() + "key"
				+ key_num, "");
	}

	public void setKeyCustomCommand(int key_num, String value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(
				"KeyCustom" + "layout" + getLayout() + "key" + key_num, value);
		editor.commit();
	}
}
