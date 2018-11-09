package com.racer40.legacy;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorImpl;
import com.racer40.sensor.SensorPinImpl;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

public class Serial extends SensorImpl {
	final Logger logger = LoggerFactory.getLogger(Serial.class);

	protected static final int MAX_COM_PORT = 64;
	protected SerialPort serialPort;
	protected CommPort commPort = null;
	protected Timer sensorTimer;

	protected int poll = 10;
	private boolean db[] = new boolean[10];

	public Serial() {
		super();
		this.poll = 1;
		this.type = SensorConstants.COM_PORT;
		this.name = "Serial port pins";
		this.managedCars = -1;
		this.pinoutImage = "serial9_pinout.png";
		this.image = "serial.jpg";
		this.serial = true;

		port = "COM1";
		this.ioPinList();
	}

	@Override
	public boolean setOutputPinValue(String pinIdentifier, int value) {
		boolean pinval = value > 0 ? true : false;
		if (serialPort == null) {
			return false;
		}
		switch (pinIdentifier) {
		case "7":
			serialPort.setRTS(pinval);
			break;
		case "4":
			serialPort.setDTR(pinval);
			break;
		}
		this.getPin(pinIdentifier).setPinValueForNotification(
				value > 0 ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF, DateTimeHelper.getSystemTime(), false,
				true);

		return pinval;
	}

	ActionListener sensorTask = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			run();
		}
	};

	@Override
	public boolean start() {
		super.start();
		if (this.openPort()) {
			sensorTimer = new Timer(this.poll, sensorTask);
			sensorTimer.setRepeats(true);
			sensorTimer.start();
			return true;
		}
		return false;
	}

	private void pinChanged(String pinIdentifier, boolean status, long timer) {
		int pinno = Integer.parseInt(pinIdentifier);
		if (db[pinno] != status) {
			db[pinno] = status;
			this.getPin(pinIdentifier).setPinValueForNotification(
					status ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF, timer, false, true);
		}
	}

	@Override
	public void run() {
		SerialPort serial = (SerialPort) commPort;
		started = true;
		long timer = DateTimeHelper.getSystemTime();

		pinChanged("8", serial.isCTS(), timer);
		pinChanged("6", serial.isDSR(), timer);
		pinChanged("1", serial.isCD(), timer);
		pinChanged("9", serial.isRI(), timer);
	}

	@Override
	public void reset() {
	}

	@Override
	public void stop() {
		if (sensorTimer != null) {
			sensorTimer.stop();
		}
		started = false;
		closePort();
	}

	public void closePort() {
		if (commPort != null) {
			commPort.close();
			commPort = null;
		}
	}

	// open serial port
	protected boolean openPort() {
		CommPortIdentifier portIdentifier = null;
		boolean isopened = false;
		String error = null;

		// check port exists first
		java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
		boolean found = false;
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier comidt = portEnum.nextElement();
			if (CommPortIdentifier.PORT_SERIAL == comidt.getPortType()
					&& comidt.getName().equalsIgnoreCase(this.port)) {
				found = true;
				break;
			}
		}
		if (!found) {
			error = "Port " + this.port + " not found";
			this.eventLogger.set(error);
			return false;
		}

		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			if (portIdentifier.isCurrentlyOwned()) {
				error = "Error: port is currently in use";
				this.eventLogger.set(error);
				logger.debug(error);

			} else {
				commPort = portIdentifier.open(this.getClass().getName(), 2000);

				if (commPort instanceof SerialPort) {
					serialPort = (SerialPort) commPort;
					isopened = true;

				} else {
					error = "Unable to open port. Check it exists.";
					this.eventLogger.set(error);
					logger.debug(error);
				}
			}
		} catch (NoSuchPortException | PortInUseException e) {
			closePort();
			logger.error("{}", e.getMessage());
		} finally {
			if (error == null) {
				logger.error("{}", "Unable to open port " + port + ". Check it exists");
				this.eventLogger.set(error);
			} else if (!"".equals(error)) {
				this.eventLogger.set(error);
			}
		}
		return isopened;

	}

	@Override
	public int getPinValue(String pinIdentifier) {
		if (!this.started) {
			return -1;
		}

		// pin input changes
		boolean pinval = false;

		switch (pinIdentifier) {
		case "digital.in.1":
			pinval = serialPort.isCD();
			break;
		case "digital.in.6":
			pinval = serialPort.isDSR();
			break;
		case "digital.in.8":
			pinval = serialPort.isCTS();
			break;
		case "digital.in.9":
			pinval = serialPort.isRI();
			break;
		}

		return pinval ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF;
	}

	@Override
	protected void ioPinList() {

		this.pins.clear();

		pins.add(new SensorPinImpl(this, "digital.in.1", "DCD"));
		pins.add(new SensorPinImpl(this, "digital.out.4", "DTR"));
		pins.add(new SensorPinImpl(this, "digital.in.6", "DSR"));
		pins.add(new SensorPinImpl(this, "digital.out.7", "RTS"));
		pins.add(new SensorPinImpl(this, "digital.in.8", "CTS"));
		pins.add(new SensorPinImpl(this, "digital.in.9", "IN"));

		for (int i = 0; i < pins.size(); i++) {
			((SensorPinImpl) pins.get(i)).setLocationIngrid(i + 1, 10, i + 1, 12);
		}
	}

	/**
	 * returns list of serial port names upper case
	 * 
	 * @return
	 */
	protected List<String> getSerialPortList() {
		List<String> ports = new ArrayList<>();
		java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();

		while (portEnum.hasMoreElements()) {
			CommPortIdentifier portIdentifier = portEnum.nextElement();

			// serial
			if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				ports.add(portIdentifier.getName().toUpperCase());
			}
		}
		return ports;
	}

	@Override
	public void discover(long timeout) {
		List<String> ports = this.getSerialPortList();
		for (String port : ports) {
			Serial com = new Serial();
			com.setPort(port);
			this.discoveredInterface.set(com);
			logger.debug("found serial: {}", port);
		}
	}

}
