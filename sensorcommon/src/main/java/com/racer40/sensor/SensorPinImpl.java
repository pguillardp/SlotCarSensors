package com.racer40.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.shape.Rectangle;

/**
 * sensor pin details
 * 
 * 
 * @author Pierrick
 *
 */
public class SensorPinImpl implements SensorPinInterface {
	final Logger logger = LoggerFactory.getLogger(SensorPinImpl.class);

	// the pin belongs to
	private SensorImpl sensor;

	// pin internal identifier
	// sensor pin identifier as defined in sensor pin list. depends of the sensor
	// - 0/pinIdentifier pin device identifier: used to make the JOIN with the rms
	// event (RMSEventSetup.pin) Button xxx, 1..N, In_xx/Out_xx...
	// this identifier is NORMALIZED as follow for all sensors: <detection
	// type>.<in|out>.<index> where:<br>
	// detection type is digital, or car, or lane... depends of the event detected
	// by the sensor
	// in|out: in, from the world to sensor, out: from sensor to the world
	// index: 0 by default, 1 to N when it is a car, or a start light, or 0 to N for
	// digital board inputs/outputs
	// always LOWER CASE, 3 keywords, dot separated
	//
	// NORMALIZED pin.type<br>
	// bestlap.out.
	// bestspeed.out.
	// cancel.in.
	// carover.out.
	// checkeredflag.out.
	// supply.out.
	// car.in.
	// end.in.
	// falsestart.out.
	// go.in.
	// gostop.in
	// greenflag.out.
	// interlap.in.
	// leader.out.
	// fuelout.out.
	// pause.in.
	// pitin.in.
	// pitout.in.
	// redflag.out.
	// redstartlight.out.
	// resume.in.
	// stop.in.
	// whiteflag.out.
	// yellowflag.out.
	private String pinIdentifier = "";

	// - 1/pin name, as known by the device and human readable
	// sensor pin name: for information only
	private String name = "";

	// pin HW event status information
	// race time the event occured on pin. 3 values as the sensor may or may not
	// manage time.
	// systemTime: system time (ms)
	// eventTime is the value to use. It is equal to systemTime + timeOffset
	// timeOffset is computed when the first event is detected (timeOffset !=
	// Long.MIN_VALUE (ms)) and equals 0 when
	// the sensor does not manage time, (system time - sensor time) when it does.
	// It allows the keep lap times equals to sensor detected laps when sensor
	// manages time ;)
	// systemTime and sensorTime are provided for information ONLY
	private long eventTime;
	private long systemTime;
	private long sensorTime;

	// pin status: PIN_ON or PIN_OFF or analog value
	private int pinValueForNotification;

	// additional information provided by the sensor if any
	// detection attribute: digital chip ID (digital) or lane number (analog like
	// ds)
	private int detectionID = -1;
	private int lap;
	private String information;

	// pin bounds in sensor pinout image (optional use)
	private Rectangle bounds = new Rectangle();

	// constructors
	public SensorPinImpl() {
	}

	public SensorPinImpl(SensorInterface sensor, String pinIdentifier, String name) {
		this.sensor = (SensorImpl) sensor;
		this.pinIdentifier = pinIdentifier;
		this.name = name;
		this.information = name;
	}

	@Override
	public SensorInterface getSensor() {
		return this.sensor;
	}

	@Override
	public boolean isInput() {
		return this.pinIdentifier.contains(".in.");
	}

	@Override
	public boolean isOutput() {
		return !isInput();
	}

	/**
	 * interface implementation is the same as sensor call
	 */
	@Override
	public int getPinValue() {
		return this.sensor.getPinValue(pinIdentifier);
	}

	@Override
	public void setOutputPinValue(int value) {
		this.sensor.setOutputPinValue(pinIdentifier, value);
	}

	// get/set pin values
	public int getPinValueForNotification() {
		return pinValueForNotification;
	}

	// pin value change methods: trigger a sensor pin change notification
	// pin values can be changed by:
	// - sensor: for INPUT pins
	// - rms: for OUTPUT pin
	// sequence of calls used by all sensors when a pin value is changed
	@Override
	public void setPinValueForNotification(int pinValue, long eventTime, boolean isSensorTime, boolean notify) {
		this.pinValueForNotification = pinValue;
		this.setTimeEvent(eventTime, isSensorTime);
		if (notify) {
			sensor.notifyPinChanged(this);
		}
	}

	/**
	 * time management. use eventTime to date all events. All other time are
	 * provided for information only
	 */
	@Override
	public long getEventTime() {
		return eventTime;
	}

	public void setTimeEvent(long timeEventMs, boolean isSensorDate) {
		this.systemTime = DateTimeHelper.getSystemTime();
		if (isSensorDate) {
			if (this.sensor.getTimeOffset() == SensorImpl.TIME_OFFSET_UNDEFINED) {
				this.sensor.setTimeOffset(this.systemTime - timeEventMs);
			}
			this.eventTime = timeEventMs + this.sensor.getTimeOffset();
			this.sensorTime = timeEventMs;

		} else {
			if (this.sensor.getTimeOffset() == SensorImpl.TIME_OFFSET_UNDEFINED) {
				this.sensor.setTimeOffset(0);
			}
			this.eventTime = timeEventMs;
			this.sensorTime = -1l;
		}
	}

	@Override
	public long getSystemTime() {
		return systemTime;
	}

	@Override
	public long getSensorTime() {
		return sensorTime;
	}

	// getters & setters
	@Override
	public String getPinIdentifier() {
		return pinIdentifier;
	}

	public void setPinIdentifier(String pinIdentifier) {
		this.pinIdentifier = pinIdentifier;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDetectionID() {
		return detectionID;
	}

	public void setDetectionID(int detectionID) {
		this.detectionID = detectionID;
	}

	public int getLap() {
		return lap;
	}

	public void setLap(int lap) {
		this.lap = lap;
	}

	public String getLogMessage() {
		return "pin identifier = " + this.getPinIdentifier() + " - name = " + this.getName();
	}

	@Override
	public void setInformation(String information) {
		this.information = information;
	}

	@Override
	public String getInformation() {
		return this.information;
	}

	@Override
	public Rectangle getBounds() {
		return this.bounds;
	}

	public void setBounds(double left, double top, double width, double height) {
		this.bounds.setLayoutX(left);
		this.bounds.setLayoutY(top);
		this.bounds.setWidth(width);
		this.bounds.setHeight(height);
	}

	// TODO: replace by file value readings
	private static int CELL = 30;

	public void setLocationIngrid(int rowb, int colb, int rowl, int coll) {
		this.setBounds(colb * CELL, rowb * CELL, coll * CELL, rowl * CELL);
	}

	// a sensor has virtual pins when it managesfull detection logic like dsxx or
	// digital system.
	// serial ports or phidgets have physical digitalios, thus no virtual pins
	@Override
	public boolean isVirtual() {
		return !this.pinIdentifier.startsWith("digital.");
	}

}
