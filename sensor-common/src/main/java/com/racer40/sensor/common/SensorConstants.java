package com.racer40.sensor.common;

public class SensorConstants {

	// SENSOR CONSTANTS
	public static final int COM_PORT = 1010;
	public static final int SCALEX_8143 = 1020;
	public static final int DS200 = 1040;
	public static final int DS300 = 1041;
	public static final int DS045 = 1042;
	public static final int JOYSTICK_USB = 1055;
	public static final int NINCO_PB_DIGITAL = 1060;
	public static final int CARRERA_DIGITAL_BB = 1061;
	public static final int SCALEX_DIG_7042 = 1064;
	public static final int DAVIC_DIGITAL = 1066;
	public static final int NINCOMLE_DIGITAL = 1067;
	public static final int CARRERA_DIGITAL_CU = 1072;
	public static final int TRACKMATE = 1073;
	public static final int PHIDGET_1012 = 1100;
	public static final int PHIDGET_1014 = 1110;
	public static final int PHIDGET_1017 = 1120;
	public static final int PHIDGET_1018 = 1130;
	public static final int KEYBOARD_USB = 1140;
	public static final int MOUSE_USB = 1150;
	public static final int KEYBOARD = 1154;

	public static final int ARDUINO_MEGA = 1161;
	public static final int ARDUINO_UNO = 1160;

	// PIN CONSTANTS
	public static final int PIN_ON = 255;
	public static final int PIN_OFF = 0;

	public static final String ON_STR = "on";
	public static final String OFF_STR = "off";

	public static final String IN = "in";
	public static final String OUT = "out";

	// special pin identifier used by boxes like ds or carrera to define box virtual
	// pins detected inputs
	public static final String DETECTION = "detection";
	public static final String DETECTION_1 = "detection1";
	public static final String DETECTION_2 = "detection2";
	public static final String DETECTION_3 = "detection3";
	public static final String DETECTION_4 = "detection4";
	public static final String DETECTION_5 = "detection5";
	public static final String DETECTION_6 = "detection6";

	// virtual pin for digital systems
	public static final String DIGITAL_DETECTION_PIN = "DigitalDetection";
	public static final String DIGITAL_LIGHT_PIN = "DigitalLight";
	public static final String DIGITAL_SPEED_PIN = "DigitalSpeed";
}
