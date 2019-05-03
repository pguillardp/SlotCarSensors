package com.racer40.sensor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

/**
 * 
 * caution: model class sensorSetup is used to save settings in database, not to
 * create or delete sensors Sensors are not used as if (abstract class). They
 * are instancied per type => NO active records
 *
 * ref: http://www.flipsideracing.org/projects/fslapcounter/
 * http://www.flipsideracing.org/projects/fslapcounter/browser/trunk/Classes?rev=1248&order=name
 *
 * @author Pierrick
 *
 */
public abstract class SensorImpl implements SensorInterface {
	final Logger logger = LoggerFactory.getLogger(SensorImpl.class);

	public static final long TIME_OFFSET_UNDEFINED = Long.MIN_VALUE;

	public static final String SENSOR_RUNNING = "running";
	public static final String SENSOR_NOTRUNNING = "not running";

	// keyword used to advise the interface controller a rmss event has been
	// detected on notified port
	// used to identify port when it cannot be found (workaround)
	public static final String DETECTED_ON_PORT = "Port: ";

	// SENSOR ATTRIBUTES
	// general sensor attributes
	// type: used by ur40 to uniquely identifies a sensor type
	protected int type = -1;

	// sensor ID optionnal
	protected String userId = "";

	// sensor configuration parameters
	protected String port = "USB";
	protected String setup = "";

	// box only: number of cars the sensor can detect
	protected int managedCars = -1;

	// sensor name
	protected String name = "undefined";

	// sensor image
	protected String image = "nopicture.jpg";

	// sensor inout image
	protected String pinoutImage = "nopicture.jpg";

	// digital like carrera boxes
	protected boolean digital = false;

	// sensor pin list
	protected List<SensorPinInterface> pins = new ArrayList<>();
	protected Map<String, SensorPinImpl> pinMap = new HashMap<>();

	// pin change property used by rms to listen hardware input pin changes, and by
	// sensor to manage output pin changes. Must be called after the pin status has
	// been modified
	protected SimpleObjectProperty<SensorPinInterface> pinChanged = new SimpleObjectProperty<>();

	// sensor message logger: used by rms to monitor the sensor
	protected StringProperty eventLogger = new SimpleStringProperty();

	// sensor discovery
	protected SimpleObjectProperty<SensorInterface> discoveredInterface = new SimpleObjectProperty<>();
	protected boolean automaticDiscovery = true;

	// time management
	private long timeOffset = SensorImpl.TIME_OFFSET_UNDEFINED;

	// version
	protected String version = "-";

	// misc
	private Boolean debugMode = new Boolean(false);
	protected boolean carDetected = false;
	protected boolean startPoll = false;
	protected boolean started = false;
	protected boolean serial = false;

	// constructors
	public SensorImpl() {
		this.addInternalListeners();
		SystemUtils.initializeEnvironment();
	}

	public SensorImpl(String port, String setup) {
		this.configure(port, setup);
		this.addInternalListeners();
		SystemUtils.initializeEnvironment();
	}

	public void configure(String port, String setup) {
		this.port = port;
		this.setup = setup;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isSerial() {
		return serial;
	}

	public SensorStatus checkStatus() {
		return new SensorStatus();
	}

	// returns pin from its identifier: a map is initialized here to faster find
	// sensor pin
	// from case unsensitive pin identifier
	@Override
	public SensorPinImpl getPin(String pinIdentifier) {
		if (this.pinMap.isEmpty()) {
			for (SensorPinInterface p : this.pins) {
				this.pinMap.put(p.getPinIdentifier().toLowerCase(), (SensorPinImpl) p);
			}
		}
		return this.pinMap.get(pinIdentifier.toLowerCase());
	}

	/*
	 * called when a pin status has been changed either from hardware to rms (input
	 * pin) or from rms to hardware (output pin)
	 */
	private ChangeListener<SensorPinInterface> pinChangeListener = new ChangeListener<SensorPinInterface>() {

		@Override
		public void changed(ObservableValue<? extends SensorPinInterface> observable, SensorPinInterface oldpin,
				SensorPinInterface pin) {
			if (pin != null) {
				String message = SensorImpl.this.name + " (" + port + " - " + setup + " ) pin change: " + pin.getName()
						+ " - " + pin.getPinIdentifier()
						+ (((SensorPinImpl) pin).getPinValueForNotification() > 0 ? " -> 1" : " -> 0");

				// force listeners - dirty
				if (message.equals(eventLogger.get())) {
					eventLogger.set(null);
				}
				eventLogger.set(message);
			}
		}
	};

	private ChangeListener<SensorInterface> discoverListener = new ChangeListener<SensorInterface>() {

		@Override
		public void changed(ObservableValue<? extends SensorInterface> observable, SensorInterface oldsensor,
				SensorInterface sensor) {
			if (sensor != null) {
				String message = "Found sensor: " + sensor.getName() + " (port: " + sensor.getPort() + " - setup: "
						+ sensor.getSetup() + ")";
				eventLogger.set(message);
			}
		}
	};

	private void addInternalListeners() {
		this.pinChanged.addListener(pinChangeListener);
		this.discoveredInterface.addListener(discoverListener);
	}

	@Override
	public SimpleObjectProperty<SensorInterface> getDiscoveredInterface() {
		return discoveredInterface;
	}

	/*
	 * called when a pin status has been changed either from hardware to rms (input
	 * pin) or from rms to hardware (output pin)
	 */
	public void notifyPinChanged(String pinIdentifier) {
		this.notifyPinChanged(this.getPin(pinIdentifier));
	}

	public void notifyPinChanged(SensorPinImpl pin) {
		if (pin == null) {
			return;
		}
		if (this.pinChanged.get() == pin) {
			// dirty way to force pin change notification
			this.pinChanged.set(null);
		}
		this.pinChanged.set(pin);
	}

	public long getTimeOffset() {
		return timeOffset;
	}

	public void setTimeOffset(long timeOffset) {
		this.timeOffset = timeOffset;
	}

	@Override
	public Image getPinout() {
		return new Image("/images/" + this.pinoutImage);
	}

	@Override
	public Image getSensorImage() {
		return new Image("/images/" + this.image);
	}

	@Override
	public int getType() {
		return this.type;
	}

	@Override
	public int getManagedCars() {
		return this.managedCars;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public List<SensorPinInterface> getPinList() {
		return this.pins;
	}

	@Override
	public String getPort() {
		return port;
	}

	@Override
	public void setPort(String port) {
		if (port == null) {
			this.port = "";
		} else {
			this.port = port;
		}
	}

	@Override
	public String getSetup() {
		return setup;
	}

	@Override
	public void setSetup(String setup) {
		if (setup == null) {
			this.setup = "";
		} else {
			this.setup = setup;
		}
	}

	@Override
	public StringProperty getEventLogger() {
		return eventLogger;
	}

	@Override
	public SimpleObjectProperty<SensorPinInterface> pinChangeProperty() {
		return this.pinChanged;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	@Override
	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public boolean isDebugMode() {
		return debugMode;
	}

	@Override
	public void setDebugMode(boolean debugMode) {
		synchronized (this.debugMode) {
			this.debugMode = debugMode;
		}
	}

	public boolean isCarDetected() {
		return this.carDetected;
	}

	protected abstract void ioPinList();

	@Override
	public boolean isDigital() {
		return this.digital;
	}

	@Override
	public void reset() {
		this.stop();
		this.start();
	}

	@Override
	public boolean start() {
		this.timeOffset = SensorImpl.TIME_OFFSET_UNDEFINED;
		return true;
	}

	@Override
	public SensorInterface createSensor() {
		Constructor<?> ctor;
		SensorInterface sensor = null;
		try {
			ctor = this.getClass().getConstructor();
			sensor = (SensorInterface) ctor.newInstance(new Object[] {});
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			logger.error("{}", e);
		}
		return sensor;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String getConfigurationHelp() {
		return "";
	}

	@Override
	public String toString() {
		return this.getName();
	}

	/*
	 * sensor search utilities (non-Javadoc)
	 * 
	 * @see com.racer40.sensor.SensorInterface#isAutomaticDiscovery()
	 */
	@Override
	public boolean isAutomaticDiscovery() {
		return automaticDiscovery;
	}

	@Override
	public boolean isDiscoveryRunning() {
		return false;
	}

	@Override
	public void stopDiscovery() {

	}

	/**
	 * sensor status
	 *
	 * @author Pierrick
	 *
	 */
	public class SensorStatus {
		private String status = "";
		private boolean error = false;

		public SensorStatus() {
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public boolean isError() {
			return error;
		}

		public void setError(boolean error) {
			this.error = error;
		}
	}

}