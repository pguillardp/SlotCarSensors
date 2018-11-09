package com.racer40.arduino;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.Rs232;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

// http://stackoverflow.com/questions/4436733/how-to-write-java-code-that-return-a-line-of-string-into-a-string-variable-from
public class ArduinoUno extends Rs232 {
	static final Logger logger = LoggerFactory.getLogger(ArduinoUno.class);

	public static final String UNO = "Uno";

	private String avrdudeFolder;
	private String avrdude;
	private String avrconf;
	private String hex;

	protected Map<String, String> input = new HashMap<>();
	protected Map<String, String> output = new HashMap<>();

	private List<String> stack = new ArrayList<>();
	private String received = "";

	private byte[] buffer = new byte[1024];

	private String version = "";
	private long top;

	public ArduinoUno() {
		super();

		this.type = SensorConstants.ARDUINO_UNO;
		this.name = "Arduino Uno";
		this.managedCars = -1;
		this.pinoutImage = "arduinouno_pinout.png";
		this.image = "arduinouno.jpg";

		this.ioPinList();

		this.poll = 1500;
		this.bauds = 19200;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;
	}

	public synchronized void sendToArduino(String data) {
		synchronized (this.stack) {
			this.stack.add(data);
		}
	}

	@Override
	public void run() {

		// check for errors + init dialog with unit
		try {
			if (this.in.available() > 0) {
				logger.debug("data available: {}", this.in.available());
				int len = 0, data;
				while ((data = in.read()) > -1) {
					if (data == '\n') {
						break;
					}
					buffer[len++] = (byte) data;
				}
				String buffer_string = new String(buffer, 0, len);
				buffer_string = buffer_string.replace("\r", "");
				this.parseBuffer(buffer_string);

			}
		} catch (IOException e) {
			logger.error("{}", e);
		}

		// push next command to serial
		synchronized (this.stack) {
			if (this.stack.size() > 0) {
				try {
					String data = this.stack.get(0);
					this.out.write(data.getBytes());
					this.out.flush();
					this.out.write(" ".getBytes());
					this.out.flush();
				} catch (IOException e) {
					logger.error("{}", e);
				}
				this.stack.remove(0);
			}
		}
	}

	protected synchronized void parseBuffer(String buffer) {
		logger.debug("buffer: " + buffer);
		received = buffer;
		logger.debug("received: " + received);
		if (received.contains("OK") || received.length() == 0) {
			return;
		}

		// logger.debug("elapsed: {}", DateTimeHelper.getSystemTime() - top);

		switch (received.charAt(0)) {

		// IO change
		case 'C':
			String data[] = received.split(",");
			String pinIdentifier = data[0].substring(1, data[0].length() - 1);
			String val = data[0].substring(data[0].length() - 1, data[0].length());
			if (output.containsKey(pinIdentifier)) {
				output.put(pinIdentifier, val + "," + data[1]);
			}
			if (input.containsKey(pinIdentifier)) {
				String prev[] = input.get(pinIdentifier).split(",");

				// input change => trigger event
				if (prev.length == 0 || !val.equals(prev[0])) {

					// date offset
					long lDate = Long.parseLong(data[1]);

					this.getPin(pinIdentifier).setPinValueForNotification(
							"1".equals(val) ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF, lDate, true, true);

					this.eventLogger.set(String.format("Pin change: %s at %d ticks", val, lDate));

				}
				input.put(pinIdentifier, val + "," + data[1]);
			}
			break;

		// version
		case 'V':
			this.version = received;
			break;

		// mode change
		case 'I':
			pinIdentifier = received.substring(1, received.length() - 1);
			if (received.endsWith("I")) {
				if (output.containsKey(pinIdentifier)) {
					output.remove(pinIdentifier);
				}
				if (!input.containsKey(pinIdentifier)) {
					input.put(pinIdentifier, "");
				}
			} else {
				if (!output.containsKey(pinIdentifier)) {
					output.put(pinIdentifier, "");
				}
				if (input.containsKey(pinIdentifier)) {
					input.remove(pinIdentifier);
				}
			}
			break;

		default:
			break;
		}
	}

	@Override
	public boolean start() {
		boolean b = super.start();
		this.initIO();
		return b;
	}

	/*
	 * init board IOs
	 */
	private void initIO() {
		for (int i = 2; i <= 19; i++) {
			// this.sendToArduino("I" + i);
		}
	}

	/*
	 * check board is configured
	 */
	public boolean isConfigured() {
		sendToArduino("V");
		if (this.version.startsWith("V")) {
			return true;
		}
		return false;
	}

	public String getVersion() {
		this.sendToArduino("V");
		return this.version;
	}

	/*
	 * returns pin list and update input & output pin maps
	 */
	// @Override
	// public List<SensorPin> getPinList(SensorSetup sensorSetup) {
	// try {
	// String name;
	// if (input.size() + output.size() < 18) {
	// for (int i = 2; i <= 19; i++) {
	// this.sendToArduino("I" + i);
	// }
	// }
	// for (int i = 2; i <= 19; i++) {
	// String pin = "" + i;
	// if (i >= 14 && i <= 19) {
	// name = "-A" + (i - 14);
	// } else {
	// name = "";
	// }
	// if (input.containsKey(pin)) {
	// pins.add(0, new SensorPin(i + "", i + " " + name, IN));
	// } else {
	// pins.add(0, new SensorPin(i + "", i + " " + name, OUT));
	// }
	// }
	//
	// } catch (Exception e) {
	// logger.error("{}", e);
	// ;
	// }
	// return super.getPinList(sensorSetup);
	// }

	/*
	 * returns the port list where arduino uno or mega is configured
	 */
	public static List<String> getArduinoComPort(String type) {
		List<String> comms = new ArrayList<>();

		com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();

		for (com.fazecast.jSerialComm.SerialPort port : ports) {
			String portname = port.getDescriptivePortName();
			if (portname.contains(type)) {
				String name = portname.substring(portname.lastIndexOf("(") + 1, portname.length() - 1);
				comms.add(name);
			}
		}

		return comms;
	}

	/*
	 * upload arduino uno .hex
	 */
	public void uploadHex(String port) throws IOException, InterruptedException {
		Path avrpath = Paths.get("media\\avrdude");

		Path realavrpath = avrpath.toRealPath(LinkOption.NOFOLLOW_LINKS);
		avrdudeFolder = realavrpath.toString();

		avrdudeFolder = avrdudeFolder.replace("\\", "\\\\");
		avrdude = avrdudeFolder + "\\\\avrdude.exe";
		avrconf = avrdudeFolder + "\\\\avrdude.conf";
		hex = avrdudeFolder + "\\\\serialStrUno.ino.hex";
		runCommand(new String[] { avrdude, "-C" + avrconf, "-v", "-v", "-v", "-v", "-patmega328p", "-carduino",
				"-P" + port, "-b115200", "-D", "-Uflash:w:" + hex + ":i" });

	}

	private void runCommand(String[] cmd) {
		try {

			Runtime.getRuntime().exec(cmd);

		} catch (IOException e) {
			logger.error("{}", e);
		}
	}

	protected void setPinValue(String pinstr, int value) {
		int pin = Integer.parseInt(pinstr);
		try {
			this.sendToArduino("S" + pin + (value > 0 ? "1" : "0"));
		} catch (Exception e) {
			logger.error("{}", e);
		}
	}

	@Override
	public int getPinValue(String pin) {
		this.sendToArduino("G" + pin);
		String data = "";
		if (output.containsKey(pin)) {
			data = output.get(pin);
		} else if (input.containsKey(pin)) {
			data = input.get(pin);
		}
		if (data.contains(",")) {
			String val[] = data.split(",");
			return Integer.parseInt(val[0]);
		}
		return -1;
	}

	@Override
	protected void parseFrame(byte[] frame, int numRead) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void ioPinList() {
		pins.clear();
		String pinName;
		for (int i = 2; i <= 19; i++) {
			String identifier = "" + i;
			if (i >= 14 && i <= 19) {
				pinName = "-A" + (i - 14);
			} else {
				pinName = "";
			}
			SensorPinImpl p;
			if (input.containsKey(identifier)) {
				p = new SensorPinImpl(this, identifier, i + " " + pinName);
				pins.add(p);
				p.setLocationIngrid(i + 1, 10, i + 1, 20);
			} else {
				p = new SensorPinImpl(this, i + "", i + " " + pinName);
				pins.add(p);
				p.setLocationIngrid(i + 1, 10, i + 1, 20);
			}
		}
	}
}
