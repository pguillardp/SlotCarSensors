package com.racer40.legacy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorImpl;
import com.racer40.sensor.SensorPinImpl;
import com.racer40.sensor.SystemUtils;

public class Serial extends SensorImpl {
	final Logger logger = LoggerFactory.getLogger(Serial.class);

	protected static final int MAX_COM_PORT = 64;
	protected SerialPort serialPort;
	private SerialRunnable serialRunnable = null;
	private Map<String, Integer> waitget = new HashMap<>();

	public Serial() {
		super();
		this.type = SensorConstants.COM_PORT;
		this.name = "Serial port pins";
		this.managedCars = -1;
		this.pinoutImage = "serial9_pinout.png";
		this.image = "serial.jpg";
		this.serial = true;

		this.port = "COM1";
		this.ioPinList();
	}

	/**
	 * start wraps a command line https://alvinalexander.com/java/edu/pj/pj010016
	 */
	@Override
	public boolean start() {
		super.start();

		String error = null;
		this.waitget.clear();

		// check port exists first
		boolean found = false;
		SerialPort[] ports = SerialPort.getCommPorts();
		for (SerialPort port : ports) {
			if (port.getSystemPortName().equalsIgnoreCase(this.port)) {
				found = true;
				break;
			}
		}
		if (!found) {
			error = "Port " + this.port + " not found";
			Serial.this.eventLogger.set(null);
			this.eventLogger.set(error);
			return false;
		}
		if (this.serialRunnable == null) {
			this.serialRunnable = new SerialRunnable(Thread.currentThread());
		}

		// stop it
		this.serialRunnable.stop();

		// start it
		Thread thread = new Thread(this.serialRunnable, "SerialPiner - " + this.port);
		thread.start();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.debug("{}", e);
		}
		long waitend = System.currentTimeMillis() + 10000;
		while (!this.serialRunnable.isStarted() && System.currentTimeMillis() < waitend)
			;
		return this.serialRunnable.isStarted();
	}

	@Override
	public boolean isStarted() {
		return this.serialRunnable != null && this.serialRunnable.isStarted();
	}

	@Override
	public void run() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void stop() {
		if (this.serialRunnable != null) {
			this.serialRunnable.stop();
		}
	}

	@Override
	public boolean setOutputPinValue(String pinIdentifier, int value) {
		final String[] fields = pinIdentifier.split("\\.");
		this.serialRunnable.write("s," + fields[2] + "," + (value > 0 ? 1 : 0));
		return value > 0;
	}

	@Override
	public int getPinValue(String pinIdentifier) {
		if (!this.started) {
			return -1;
		}
		String[] fields = pinIdentifier.split("\\.");
		synchronized (this.waitget) {
			this.waitget.remove(pinIdentifier);
		}
		this.serialRunnable.write("g," + fields[2] + "\n");
		int pinval = -1;
		int retry = 10;
		while (retry > 0) {
			synchronized (this.waitget) {
				if (this.waitget.containsKey(pinIdentifier)) {
					pinval = this.waitget.get(pinIdentifier);
					pinval = pinval > 0 ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF;
					break;
				}
				try {
					Thread.currentThread().wait(50);
				} catch (InterruptedException e) {
					logger.debug("{}", e);
				}
				retry--;
			}
		}
		return pinval;
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
		SerialPort[] commports = SerialPort.getCommPorts();
		for (SerialPort port : commports) {
			if (!port.getDescriptivePortName().toLowerCase().contains("uno")
					&& !port.getDescriptivePortName().toLowerCase().contains("mega")) {
				ports.add(port.getSystemPortName().toUpperCase());
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

	/**
	 * asynchrounous serial port polling
	 * 
	 * @author Pierrick
	 *
	 */
	private class SerialRunnable implements Runnable {
		private Thread fxThread;
		private InputStream out;
		private OutputStream in;
		private Process process;

		private SerialRunnable(Thread fxThread) {
			this.fxThread = fxThread;
		}

		public void write(String string) {
			try {
				in.write(string.getBytes(StandardCharsets.UTF_8));
				in.flush();
				if (Serial.this.isDebugMode()) {
					Serial.this.eventLogger.set(null);
					Serial.this.eventLogger.set(string);
				}
			} catch (IOException e) {
				logger.debug("{}", e);
			}
		}

		/**
		 * stop polling thread
		 */
		private void stop() {
			if (this.in != null) {
				try {
					in.write("bye\n".getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
					logger.debug("{}", e);
				}
				// while (this.process.isAlive()) {
				// try {
				// Thread.currentThread().sleep(50);
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				// }
				this.process.destroy();
			}
		}

		private boolean isStarted() {
			return this.process != null && this.process.isAlive();
		}

		@Override
		public void run() {
			try {

				String exepath = SystemUtils.getLibraryFolder() + "serialPiner.exe";
				exepath = exepath.replaceAll("/", "");
				ProcessBuilder builder = new ProcessBuilder(exepath, Serial.this.port);
				builder.redirectErrorStream(true); // so we can ignore the error stream
				this.process = builder.start();

				this.out = process.getInputStream();
				this.in = process.getOutputStream();
				byte[] buffer = new byte[4000];

				while (this.fxThread.isAlive() && this.process.isAlive()) {
					int no;
					no = out.available();

					if (no > 0) {
						int n = out.read(buffer, 0, Math.min(no, buffer.length));
						String fromSerial = new String(buffer, 0, n);
						if (Serial.this.isDebugMode()) {
							Serial.this.eventLogger.set(null);
							Serial.this.eventLogger.set(fromSerial);
						}
						fromSerial = fromSerial.toLowerCase().replaceAll("\n", "");
						logger.debug("{}", fromSerial);
						String[] fields = fromSerial.split(",");
						if (fields.length == 4 && "c".equals(fields[0])) {
							String pinIdentifier = "digital.in." + fields[1];
							SensorPinImpl pin = getPin(pinIdentifier);
							if (pin != null) {
								pin.setPinValueForNotification(
										"0".equals(fields[3]) ? SensorConstants.PIN_OFF : SensorConstants.PIN_ON,
										Long.parseLong(fields[2]), false, true);
							}
						} else if (fields.length == 3 && "g".equals(fields[0])) {
							String pinIdentifier = "digital.in." + fields[1];
							SensorPinImpl pin = getPin(pinIdentifier);
							if (pin != null) {
								synchronized (waitget) {
									waitget.put(pinIdentifier, Integer.parseInt(fields[2]));
								}
							}
						} else {
							Serial.this.eventLogger.set(null);
							Serial.this.eventLogger.set(fromSerial);
						}
					}

					int ni = System.in.available();
					if (ni > 0) {
						int n = System.in.read(buffer, 0, Math.min(ni, buffer.length));
						in.write(buffer, 0, n);
						in.flush();
					}

					Thread.sleep(50);
				}

				if (!this.fxThread.isAlive() && this.process.isAlive()) {
					this.process.destroy();
				}

				logger.debug("serial exit: {}", process.exitValue());
				this.process = null;
				this.in = null;

			} catch (IOException | InterruptedException e) {
				logger.debug("{}", e);
				Serial.this.eventLogger.set(null);
				Serial.this.eventLogger.set(e.getMessage());
				if (this.process == null) {
					Serial.this.eventLogger.set("Failed to start process on port " + Serial.this.port);
					Serial.this.eventLogger.set("It might be used by another application");
				}
			}

		}

	}

}
