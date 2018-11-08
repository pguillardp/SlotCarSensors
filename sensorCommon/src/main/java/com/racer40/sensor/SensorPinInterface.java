package com.racer40.sensor;

import javafx.scene.shape.Rectangle;

public interface SensorPinInterface {

	public SensorInterface getSensor();

	public String getName();

	public String getPinIdentifier();

	public boolean isInput();

	public boolean isOutput();

	// pin change time, system time or sensor time converted into system time
	public long getEventTime();

	// manage values
	public int getPinValue();

	public void setOutputPinValue(int value);

	// this method is used only to reset internal pin status and trigger status
	// change event
	public void setPinValueForNotification(int pinValue, long eventTime, boolean isSensorTime, boolean notify);

	// additional optional pin information
	// information equals pin name by default
	public void setInformation(String information);

	public String getInformation();

	// for information only
	public long getSystemTime();

	public long getSensorTime();

	// pin bound in sensor pinout image (optional)
	public Rectangle getBounds();

	// a sensor has virtual pins when it managesfull detection logic like dsxx or
	// digital system.
	// serial ports or phidgets or gamepads have physical digital ios, thus no
	// virtual pins
	public boolean isVirtual();

}
