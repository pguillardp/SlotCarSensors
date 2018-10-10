package com.racer40.sensor.common;

import javafx.scene.shape.Rectangle;

public interface SensorPinInterface {

	public String getName();

	public String getPinIdentifier();

	public boolean isInput();

	public boolean isOutput();

	// pin change time, system time or sensor time converted into system time
	public long getEventTime();

	// manage values
	public int getPinValue();

	public void setOutputPinValue(int value);

	// additional optional pin information
	// information equals pin name by default
	public void setInformation(String information);

	public String getInformation();

	// for information only
	public long getSystemTime();

	public long getSensorTime();

	// pin bound in sensor pinout image (optional)
	public Rectangle getBounds();

}
