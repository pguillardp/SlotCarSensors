package com.racer40.legacy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorImpl;
import com.racer40.sensor.SensorPinImpl;
import com.racer40.sensor.SystemUtils;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class Serial extends SensorImpl {
	final Logger logger = LoggerFactory.getLogger(Serial.class);

	protected static final int MAX_COM_PORT = 64;
	protected SerialPort serialPort;
	protected CommPort commPort = null;

	public Serial() {
		super();
		this.type = SensorConstants.COM_PORT;
		this.name = "Serial port pins";
		this.managedCars = -1;
		this.pinoutImage = "serial9_pinout.png";
		this.image = "serial.jpg";
		this.serial = true;

		port = "COM1";
		this.ioPinList();
	}

	/**
	 * start wraps a command line https://alvinalexander.com/java/edu/pj/pj010016
	 */
	@Override
	public boolean start() {
		super.start();

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
		String command = SystemUtils.getAsoluteAppFolder() + SystemUtils.PLUGINS + "//"
				+ (SystemUtils.isWindows64bits() ? "x64" : "x32") + "//serialPiner.exe";

		ProcessBuilder pb = new ProcessBuilder(command, this.port);
		logger.debug("{}", command + " " + this.port);
		Process process;
		String s;
		try {
			process = pb.start();

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			;
			// process.getOutputStream()

			// read the output from the command
			logger.debug("{}", "Here is the standard output of the command:\n");
			while ((s = stdInput.readLine()) != null) {
				logger.debug("{}", s);
			}

			// read any errors from the attempted command
			logger.debug("{}", "Here is the standard error of the command (if any):\n");
			while ((s = stdError.readLine()) != null) {
				logger.debug("{}", s);
			}

			// logger.debug("{}", "Echo command executed, any errors? " + (errCode == 0 ?
			// "No" : "Yes"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void run() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void stop() {
		// if (sensorTimer != null) {
		// sensorTimer.stop();
		// }
		started = false;
		closePort();
	}

	public void closePort() {
		if (commPort != null) {
			commPort.close();
			commPort = null;
		}
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
