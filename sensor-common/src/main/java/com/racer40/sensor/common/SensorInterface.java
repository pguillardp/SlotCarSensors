package com.racer40.sensor.common;

import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

public interface SensorInterface {

	// operations
	public boolean start();

	public boolean isStarted();

	public void run();

	public void stop();

	public void reset();

	// sensor type and configuration
	public int getType();

	public String getPort();

	public String getSetup();

	public void setPort(String port);

	public void setSetup(String setup);

	// sensor informations: name, pinout & sensor images and FX controls to display
	// pins
	public String getName();

	public Image getPinout();

	public Image getSensorImage();

	public boolean isDigital();

	// internal use
	public String getUserId();

	public void setUserId(String id);

	public int getManagedCars();

	// sensor pin management
	// sensors manage 2 kind of pins:
	// - real pins like in legacy ports (serial), io boards (phidgets, arduino with
	// right hex)
	// - virtual pins: created and used by box sensors like dsxxx, carrera cu ...
	// argument is set when number of pins differs between same sensor types
	// pin changes
	public boolean setOutputPinValue(String pinIdentifier, int value);

	public int getPinValue(String pinIdentifier);

	public List<SensorPinInterface> getPinList();

	public SensorPinInterface getPin(String pinIdentifier);

	// attach listeners to get pin changes
	// monitoring note: pin change messages are logged by the sensor and pushed to
	// the event logger
	public SimpleObjectProperty<SensorPinInterface> pinChangeProperty();

	// sensor logger properties: use listeners to collect all sensor event messages
	// like pin chages, status, rs232 sensor frames...
	public StringProperty getEventLogger();

	// discover same type sensors: a sensor can be used to discover same sensor
	// types.
	// This method creates one sensor instance with right port and setup and changes
	// property each time a sensor is discovered
	// port and setup must be set
	// monitoring note: discovery messages are logged by the sensor and pushed to
	// the event logger
	// call discover to find similar installed sensors, listen to
	// getDiscoveredInterface() to get found sensors
	public void discover(long timeout);

	public SimpleObjectProperty<SensorInterface> getDiscoveredInterface();

	// create sensor of the same type
	public SensorInterface createSensor();

}
