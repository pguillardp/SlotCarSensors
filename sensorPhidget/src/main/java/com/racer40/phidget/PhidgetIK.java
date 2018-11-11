package com.racer40.phidget;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.phidget22.DigitalInput;
import com.phidget22.DigitalInputStateChangeEvent;
import com.phidget22.DigitalInputStateChangeListener;
import com.phidget22.DigitalOutput;
import com.phidget22.Manager;
import com.phidget22.ManagerAttachEvent;
import com.phidget22.ManagerDetachEvent;
import com.phidget22.Phidget;
import com.phidget22.PhidgetException;
import com.phidget22.VoltageRatioInput;
import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorImpl;
import com.racer40.sensor.SensorPinImpl;

import javafx.event.EventDispatchChain;
import javafx.event.EventTarget;

public abstract class PhidgetIK extends SensorImpl implements EventTarget {
	static final Logger logger = LoggerFactory.getLogger(PhidgetIK.class);

	private static final int ATTACH_TIMEOUT_MS = 5000;

	// phidget device channels
	private DigitalInput[] dinput = null;
	private DigitalOutput[] doutput = null;
	private VoltageRatioInput[] vinput = null;

	public PhidgetIK(int din, int dout, int vin) {
		super("USB", "");
		this.dinput = new DigitalInput[din];
		this.doutput = new DigitalOutput[dout];
		this.vinput = new VoltageRatioInput[vin];
	}

	/**
	 * discover phidgets IK
	 * 
	 * @return
	 */
	public static String libraryVersion() {
		try {
			return com.phidget22.Phidget.getLibraryVersion();
		} catch (PhidgetException e) {
			logger.error("{}", e);
		} finally {
		}
		return "???";
	}

	@Override
	public void run() {
		// NA: interrupt driven
	}

	/**
	 * build io list
	 */
	@Override
	protected void ioPinList() {
		this.pins.clear();

		for (int i = 0; i < this.dinput.length; i++) {
			pins.add(new SensorPinImpl(this, "digital.in." + i, "in " + i));
		}
		for (int i = 0; i < this.doutput.length; i++) {
			pins.add(new SensorPinImpl(this, "digital.out." + i, "out " + i));
		}
	}

	@Override
	public void reset() {
		if (this.dinput == null) {
			return;
		}
	}

	private DigitalInputStateChangeListener digitalInputStateChangeListener = (DigitalInputStateChangeEvent e) -> {
		logger.debug("State changed: {}", e.getState());
		String pinIdentifier;
		try {
			pinIdentifier = "digital.in." + e.getSource().getChannel();
			SensorPinImpl pin = this.getPin(pinIdentifier);
			if (pin != null) {
				pin.setPinValueForNotification(e.getState() ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF,
						DateTimeHelper.getSystemTime(), false, true);
			}
		} catch (PhidgetException e1) {
			logger.error("{}", e1);
		} finally {
		}
	};

	/**
	 * starts a phidget
	 */
	@Override
	public boolean start() {
		super.start();
		if (this.dinput != null) {
			stop();
		}
		String error = "";
		String no = "";
		this.started = false;

		try {

			int serial = StringUtils.isNumeric(this.getSetup()) ? Integer.parseInt(this.getSetup()) : 0;

			// open channels
			for (int i = 0; i < this.doutput.length; i++) {
				if (this.doutput[i] == null) {
					this.doutput[i] = new DigitalOutput();
					this.doutput[i].setDeviceSerialNumber(serial);
					this.doutput[i].setHubPort(-1);
					this.doutput[i].setIsHubPortDevice(false);
					this.doutput[i].setChannel(i);
				}
				this.doutput[i].open(PhidgetIK.ATTACH_TIMEOUT_MS);
			}

			for (int i = 0; i < this.dinput.length; i++) {
				if (this.dinput[i] == null) {
					this.dinput[i] = new DigitalInput();
					this.dinput[i].setDeviceSerialNumber(serial);
					this.dinput[i].setHubPort(-1);
					this.dinput[i].setIsHubPortDevice(false);
					this.dinput[i].setChannel(i);
				}
				this.dinput[i].addStateChangeListener(digitalInputStateChangeListener);
				this.dinput[i].open(PhidgetIK.ATTACH_TIMEOUT_MS);
			}

			this.started = true;

		} catch (NumberFormatException | PhidgetException e) {
			logger.error("{}", e);
			this.stop();
		} finally {
		}
		if (!this.started) {
			this.eventLogger.set("Unable to attach phidget sn:" + no + "\nError: " + error + "\n"
					+ "Check:\n-serial number\n-phidget setup with the phidget controller\n- phidget controller is closed. ");
		}
		return this.started;
	}

	/**
	 * stop & close channel
	 */
	@Override
	public void stop() {
		if (this.started) {
			try {
				// close channels
				for (int i = 0; i < this.doutput.length; i++) {
					this.doutput[i].close();
				}

				for (int i = 0; i < this.dinput.length; i++) {
					this.dinput[i].removeStateChangeListener(digitalInputStateChangeListener);
					this.dinput[i].close();
				}
			} catch (PhidgetException e) {
				logger.error("{}", e);
			} finally {
				this.started = false;
			}
		}
	}

	/**
	 * get pin value
	 */
	@Override
	public int getPinValue(String pinIdentifier) {
		boolean inputpin = pinIdentifier.contains(".in.");
		String pinstr = pinIdentifier.split("\\.")[2];
		int pinNo = Integer.parseInt(pinstr);
		try {
			if (inputpin) {
				if (pinNo >= 0 && pinNo < this.dinput.length && this.dinput[pinNo] != null) {
					return this.dinput[pinNo].getState() ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF;
				}
			} else {
				if (pinNo >= 0 && pinNo < this.doutput.length && this.doutput[pinNo] != null) {
					return this.doutput[pinNo].getState() ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF;
				}
			}
		} catch (PhidgetException e) {
			logger.error("{}", e);
			return -1;
		}
		return -1;
	}

	/**
	 * set output pin value
	 */
	@Override
	public boolean setOutputPinValue(String pinIdentifier, int value) {
		try {
			SensorPinImpl pin = this.getPin(pinIdentifier);
			if (pin != null) {
				int pinNo = Integer.parseInt(pinIdentifier.split("\\.")[2]);
				if (pinNo >= 0 && pinNo < this.doutput.length && this.doutput[pinNo] != null) {
					this.doutput[pinNo].setState(value != 0);
					pin.setPinValueForNotification((value != 0) ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF,
							DateTimeHelper.getSystemTime(), false, true);
					return true;
				}
			}
		} catch (PhidgetException e) {
			logger.error("{}", e);
			return false;
		}
		return false;
	}

	/**
	 * discover all connected phigets
	 */
	@Override
	public void discover(long timeout) {
		String version = PhidgetIK.libraryVersion();
		if (version != null && !"".equals(version)) {
			this.eventLogger.set("Search for phidget Interface Kits");
			this.eventLogger.set("Library version: " + version + "");
			Set<String> foundSetup = new HashSet<>();

			// Create and open the manager
			Manager manager = null;
			try {
				manager = new Manager();

				// called once per i/o => filter
				manager.addAttachListener((ManagerAttachEvent ev) -> {
					Phidget phidget = ev.getChannel();
					try {
						String message = "Found); " + phidget.getDeviceName() + " Serial Number: "
								+ phidget.getDeviceSerialNumber();
						int k = phidget.getChannel();
						System.out.println(k);
						if (!foundSetup.contains(message)) {
							logger.debug(message);
							foundSetup.add(message);

							SensorImpl sensor = null;
							if (phidget.getDeviceName().contains("0/16/16")) {
								sensor = new Phidget1012();
							} else if (phidget.getDeviceName().contains("8/8/8")) {
								sensor = new Phidget1018();
							} else if (phidget.getDeviceName().contains("0/0/8")) {
								sensor = new Phidget1017();
							} else if (phidget.getDeviceName().contains("0/0/4")) {
								sensor = new Phidget1014();
							}

							if (sensor != null) {
								// sensor.setVersion(phidget.getDeviceVersion() + "");
								sensor.setPort("USB");
								sensor.setSetup("" + phidget.getDeviceSerialNumber());
								this.discoveredInterface.set(sensor);
							}
						}
					} catch (PhidgetException ex) {
						logger.debug(ex.getDescription());
					}
				});

				manager.addDetachListener((ManagerDetachEvent ev) -> {
					Phidget phid = ev.getChannel();
					try {
						logger.debug("Close device: " + phid.getDeviceName() + ", Serial Number: "
								+ phid.getDeviceSerialNumber());
					} catch (PhidgetException ex) {
						logger.debug(ex.getDescription());
					}
				});

				manager.open();

				// Allow the Phidgets time to attach
				Thread.sleep(ATTACH_TIMEOUT_MS);

				// Close the manager
				manager.close();

			} catch (PhidgetException | InterruptedException e) {
				logger.error("{}", e);
			}
		}
	}

	@Override
	public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
		return null;
	}

}
