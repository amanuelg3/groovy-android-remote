package com.linuxfunkar.mousekeysremote;

import android.util.SparseArray;

public class Constants {
	// Codes för keyboard layout
	public static final int F1 = -25;
	public static final int F2 = -26;
	public static final int F3 = -27;
	public static final int F4 = -28;
	public static final int F5 = -29;
	public static final int F6 = -30;
	public static final int F7 = -31;
	public static final int F8 = -32;
	public static final int F9 = -33;
	public static final int F10 = -34;
	public static final int F11 = -35;
	public static final int F12 = -36;
	public static final int LCTRL = -37;
	public static final int DEL = -38;
	public static final int LSHIFT = -39;
	public static final int LALT = -40;
	public static final int INS = -41;
	public static final int CHRA = -50;
	public static final int CHRB = -51;
	public static final int CHRC = -52;
	public static final int CHRD = -53;
	public static final int CHRE = -54;
	public static final int CHRF = -55;
	public static final int CHRG = -56;
	public static final int CHRH = -57;
	public static final int CHRI = -58;
	public static final int CHRJ = -59;
	public static final int CHRK = -60;
	public static final int CHRL = -61;
	public static final int CHRM = -62;
	public static final int CHRN = -63;
	public static final int CHRO = -64;
	public static final int CHRP = -65;
	public static final int CHRQ = -66;
	public static final int CHRR = -67;
	public static final int CHRS = -68;
	public static final int CHRT = -69;
	public static final int CHRU = -70;
	public static final int CHRV = -71;
	public static final int CHRW = -72;
	public static final int CHRX = -73;
	public static final int CHRY = -74;
	public static final int CHRZ = -75;
	public static final int CHR0 = -80;
	public static final int CHR1 = -81;
	public static final int CHR2 = -82;
	public static final int CHR3 = -83;
	public static final int CHR4 = -84;
	public static final int CHR5 = -85;
	public static final int CHR6 = -86;
	public static final int CHR7 = -87;
	public static final int CHR8 = -88;
	public static final int CHR9 = -89;
	public static final int SEMI_COLON = -90;
	public static final int BACKSLASH = -91;
	public static final int AT = -92;
	public static final int SLASH = -93;
	public static final int COLON = -94;
	public static final int EQUALS = -95;
	public static final int QUESTION = -96;
	public static final int ESC = -100;
	public static final int SPACE = -101;
	public static final int MOUSE_RIGHT = -102;
	public static final int MOUSE_LEFT = -103;
	public static final int MOUSE_UP = -104;
	public static final int MOUSE_DOWN = -105;
	public static final int MOUSE_LCLICK = -106;
	public static final int MOUSE_RCLICK = -107;
	public static final int MOUSE_WUP = -108;
	public static final int MOUSE_WDOWN = -109;
	public static final int CR = -110;
	public static final int CUR_DOWN = -111;
	public static final int CUR_UP = -112;
	public static final int CUR_RIGHT = -113;
	public static final int CUR_LEFT = -114;
	public static final int NUM_KP_0 = -115;
	public static final int NUM_KP_1 = -116;
	public static final int NUM_KP_2 = -117;
	public static final int NUM_KP_3 = -118;
	public static final int NUM_KP_4 = -119;
	public static final int NUM_KP_5 = -120;
	public static final int NUM_KP_6 = -121;
	public static final int NUM_KP_7 = -122;
	public static final int NUM_KP_8 = -123;
	public static final int NUM_KP_9 = -124;
	public static final int PLUS = -125;
	public static final int MINUS = -126;
	public static final int TAB = -127;
	public static final int PGUP = -128;
	public static final int PGDOWN = -129;
	public static final int COMMA = -130;
	public static final int PERIOD = -131;
	public static final int END = -132;
	public static final int HOME = -133;
	public static final int BACKSPACE = -134;
	public static final int DUMMY = -135;
	public static final int ZOOM_IN = -136;
	public static final int ZOOM_OUT = -137;
	public static final int ZOOM_RESET = -138;
	public static final int NORTH = -139;
	public static final int SOUTH = -140;
	public static final int WEST = -141;
	public static final int EAST = -142;
	public static final int NORTHWEST = -143;
	public static final int NORTHEAST = -144;
	public static final int SOUTHWEST = -145;
	public static final int SOUTHEAST = -146;

	private static final Object[][] key_map = { { F1, "F1", 1101 },
			{ F2, "F2", 1102 }, { F3, "F3", 1103 }, { F4, "F4", 1104 },
			{ F5, "F5", 1105 }, { F6, "F6", 1106 }, { F7, "F7", 1107 },
			{ F8, "F8", 1108 }, { F9, "F9", 1109 }, { F10, "F10", 1110 },
			{ F11, "F11", 1111 }, { F12, "F12", 1112 },
			{ LCTRL, "Ctrl", 1200 }, { DEL, "Del", 1201 },
			{ LSHIFT, "Shift", 1202 }, { LALT, "Alt", 1203 },
			{ INS, "Ins", 1204 }, { CHRA, "a", 97 }, { CHRB, "b", 98 },
			{ CHRC, "c", 99 }, { CHRD, "d", 100 }, { CHRE, "e", 101 },
			{ CHRF, "f", 102 }, { CHRG, "g", 103 }, { CHRH, "h", 104 },
			{ CHRI, "i", 105 }, { CHRJ, "j", 106 }, { CHRK, "k", 107 },
			{ CHRL, "l", 108 }, { CHRM, "m", 109 }, { CHRN, "n", 110 },
			{ CHRO, "o", 111 }, { CHRP, "p", 112 }, { CHRQ, "q", 113 },
			{ CHRR, "r", 114 }, { CHRS, "s", 115 }, { CHRT, "t", 116 },
			{ CHRU, "u", 117 }, { CHRV, "v", 118 }, { CHRW, "w", 119 },
			{ CHRX, "x", 120 }, { CHRY, "y", 121 }, { CHRZ, "z", 122 },
			{ CHR0, "0", 48 }, { CHR1, "1", 49 }, { CHR2, "2", 50 },
			{ CHR3, "3", 51 }, { CHR4, "4", 52 }, { CHR5, "5", 53 },
			{ CHR6, "6", 54 }, { CHR7, "7", 55 }, { CHR8, "8", 56 },
			{ CHR9, "9", 57 }, { SEMI_COLON, ";", 59 },
			{ BACKSLASH, "\\", 1213 }, { AT, "@", 1212 }, { SLASH, "/", 47 },
			{ COLON, ":", 58 }, { EQUALS, "=", 1218 }, { QUESTION, "?", 63 },
			{ ESC, "ESC", 27 }, { SPACE, "Space", 32 },
			{ MOUSE_RIGHT, "M-Right", "MMR" }, { MOUSE_LEFT, "M-Left", "MML" },
			{ MOUSE_UP, "M-Up", "MMU" }, { MOUSE_DOWN, "M-Down", "MMD" },
			{ MOUSE_LCLICK, "M-LClick", "MLC" },
			{ MOUSE_RCLICK, "M-RClick", "MRC" },
			{ MOUSE_WUP, "Wheel up", "MWU" },
			{ MOUSE_WDOWN, "Wheel down", "MWD" }, { CR, "Return", 10 },
			{ CUR_DOWN, "Down", 40 }, { CUR_UP, "Up", 38 },
			{ CUR_RIGHT, "Right", 39 }, { CUR_LEFT, "Left", 37 },
			{ NUM_KP_0, "NUM 0", 1015 }, { NUM_KP_1, "NUM 1", 1016 },
			{ NUM_KP_2, "NUM 2", 1017 }, { NUM_KP_3, "NUM 3", 1018 },
			{ NUM_KP_4, "NUM 4", 1019 }, { NUM_KP_5, "NUM 5", 1020 },
			{ NUM_KP_6, "NUM 6", 1021 }, { NUM_KP_7, "NUM 7", 1022 },
			{ NUM_KP_8, "NUM 8", 1023 }, { NUM_KP_9, "NUM 9", 1024 },
			{ PLUS, "+", 1205 }, { MINUS, "-", 1206 }, { TAB, "Tab", 1207 },
			{ PGUP, "PgUp", 1209 }, { PGDOWN, "PgDn", 1210 },
			{ COMMA, ",", 1215 }, { PERIOD, ".", 1216 }, { END, "End", 1217 },
			{ HOME, "Home", 1219 }, { BACKSPACE, "Back", 1208 },
			{ DUMMY, "NOP", "NOP" }, { ZOOM_IN, "Zoom in", "MPZ1" },
			{ ZOOM_OUT, "Zoom out", "MPZ-1" },
			{ ZOOM_RESET, "Zoom Reset", "MZR" }, { NORTH, "(N)", 1300 },
			{ SOUTH, "(S)", 1301 }, { WEST, "(W)", 1302 },
			{ EAST, "(E)", 1303 }, { NORTHWEST, "(NW)", 1304 },
			{ NORTHEAST, "(NE)", 1305 }, { SOUTHWEST, "(SW)", 1306 },
			{ SOUTHEAST, "(SE)", 1307 } };

	private static final SparseArray<String> actionNames = new SparseArray<String>();
	private static final SparseArray<String> action = new SparseArray<String>();

	static {
		for (Object[] entry : key_map) {
			actionNames.put(Integer.parseInt(entry[0].toString()),
					entry[1].toString());
			action.put(Integer.parseInt(entry[0].toString()),
					entry[2].toString());
		}
	}

	public static String getActionName(int id) {
		return actionNames.get(id);
	}

	public static String getActionPress(int id) {
		if (action.indexOfKey(id) < 0)
			return "NOP";
		Object act = action.get(id);
		try {
			return "KBP" + Integer.valueOf(act.toString());
		} catch (NumberFormatException ex) {
			return act.toString();
		}
	}

	public static String getActionRelease(int id) {
		if (action.indexOfKey(id) < 0)
			return "NOP";
		Object act = action.get(id);
		try {
			return "KBR" + Integer.valueOf(act.toString());
		} catch (NumberFormatException ex) {
			return act.toString() + "R";
		}
	}

	public static String getServer() {
		return "";
	}

	static final int UDP_PORT = 5555;
}
