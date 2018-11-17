package com.racer40.arduino;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.Rs232;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;
import com.racer40.sensor.SystemUtils;

/*
 * rs232 protocol(uno and mega):<br> 
 * Setup: 57600, parity none, databits 8, stop bit 1, no handshake ("57600,n,8,1") <br>
 * - PC -> Arduino:<br>
 *  . change output: S(et),<pin number as described above>,<value: 0x00 means low state, not 0x00 means high state, range 0x00 to 0xFF> <br>
 *  Ex (uno): 0x53 0x2C 0x0B 0x2C 0x0F -> 0x53 is 'S'et, 0x2C is comma separator, 0x0B stands for PD5, 0x0F is not 0x00 so pin state is set to hight <br>
 *  . reset date counter: R(eset) Ex: 0x52: where 0x52 is 'R'eset. The 4 bytes unsigned long counter are set to 0x00000000<br>
 *   . get pin status: G(et),<pin number, input or output> <br>
 *   Ex (uno): 0x47 0x2C 0x06: where 0x47 is 'G'et, 0x06 stands for PD4<br>
 *    . Arduino response: P(in),<pin number, as described above>,<date: internal 4 byte unsigned long counter>,<value: 0x00 for low state, 0xFF for hight state><br>
 *     Ex: (uno): 0x50 0x2C 0x06 0x2C 0x52362154 0x2C 0xFF means: pin 6 (PD4) is in high state (0xFF is not 0x00) at 1379279188 half of ms (or closest half ms tick) <br>
 *     . get date: D(ate) Ex : 0x44 Response: D,<date: internal 4 byte unsigned long counter> <br>
 * Ex: 0x44 0x2C 0x52362154<br>
 * - Arduino -> PC: <br>
 * . when an input change, then the arduino sends a change message to the PC: <br>
 * C(hange),<pin number, as described above>,<date: internal 4 byte unsigned long counter>,<value: 0x00 for low state, 0xFF for hight state> <br>
 * Ex: (uno): 0x43 0x2C 0x0D 0x2C 0x52362154 0x2C 0x00 means: pin 13 changed to low state at 1379279188 half of ms 
 * 
 * 
 */
// http://stackoverflow.com/questions/4436733/how-to-write-java-code-that-return-a-line-of-string-into-a-string-variable-from
public class ArduinoUno extends Rs232 {
	static final Logger logger = LoggerFactory.getLogger(ArduinoUno.class);

	public static final String UNO = "Uno";

	protected Map<String, Integer> waitget = new HashMap<>();

	boolean readPort = false;

	protected final static int[] UNO_OUT_PINS = new int[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	public final static int[] UNO_IN_PINS = new int[] { 12, 13, 14, 15, 16, 17, 18, 19 };

	private final Thread mainThread;
	private SerialPort comPort = null;
	private String comPortName = "";

	private byte rxBuffer[] = new byte[128];
	private int rxPos;

	public boolean isProgramming = false;

	public ArduinoUno() {
		super();

		this.type = SensorConstants.ARDUINO_UNO;
		this.name = "Arduino Uno";
		this.managedCars = -1;
		this.pinoutImage = "arduinouno_pinout.png";
		this.image = "arduinouno.jpg";

		this.ioPinList();

		this.poll = 1500;
		this.bauds = 57600;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;

		mainThread = new Thread(new MainRunnable());
	}

	protected boolean isArduinoUno() {
		return this instanceof ArduinoUno;
	}

	protected boolean isArduinoMega() {
		return this instanceof ArduinoMega;
	}

	@Override
	public boolean start() {

		closePort();

		comPortName = this.port;
		if (!"".equals(this.port)) {
			SerialPort ports[] = SerialPort.getCommPorts();
			for (SerialPort port : ports) {
				if (port.getSystemPortName().equals(comPortName)) {
					comPort = port;
					comPort.addDataListener(new RxHandler());
					openPort();
					break;
				}
			}
		}

		mainThread.start();
		return true;
	}

	private class RxHandler implements SerialPortDataListener {

		@Override
		public int getListeningEvents() {
			return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
		}

		@Override
		public void serialEvent(SerialPortEvent event) {
			byte[] data = event.getReceivedData();
			for (int i = 0; i < data.length; ++i) {
				rxBuffer[rxPos] = data[i];
				if ((char) rxBuffer[0] != 'P' && (char) rxBuffer[0] != 'C' && (char) rxBuffer[0] != 'D') {
					rxPos = 0;
				} else {
					rxPos++;
				}

				switch ((char) rxBuffer[0]) {
				case 'C':
					if (rxPos == 10) {
						rxPos = 0;
						String pinIdentifier = "digital.in." + ((int) rxBuffer[2]);
						SensorPinImpl pin = getPin(pinIdentifier);
						if (pin != null) {
							long l = 0;
							l |= rxBuffer[4] & 0xFF;
							l <<= 8;
							l |= rxBuffer[5] & 0xFF;
							l <<= 8;
							l |= rxBuffer[6] & 0xFF;
							l <<= 8;
							l |= rxBuffer[7] & 0xFF;
							pin.setPinValueForNotification(
									(rxBuffer[2] != 0) ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF, l, false,
									true);
						}
					}
					break;
				case 'P':
					if (rxPos == 10) {
						rxPos = 0;
						synchronized (waitget) {
							String pin = "" + (int) rxBuffer[2];
							waitget.put(pin, ((int) rxBuffer[9]));
						}
					}
					break;
				case 'D':
					if (rxPos == 6) {
						rxPos = 0;
					}
					break;
				}
			}
		}
	}

	@Override
	public boolean setOutputPinValue(String pinIdentifier, int value) {
		String[] s = pinIdentifier.split("\\.");
		String pin = s[2].length() == 1 ? "0" + s[2] : s[2];
		this.sendToArduino("s," + pin + ",0" + (value != 0 ? 1 : 0));
		return true;
	}

	@Override
	public int getPinValue(String pinIdentifier) {
		String[] s = pinIdentifier.split("\\.");
		String pin = s[2].length() == 1 ? "0" + s[2] : s[2];
		long endwait = DateTimeHelper.getSystemTime() + 100;
		waitget.put(pin, -1);
		this.sendToArduino("g," + pin);
		while (DateTimeHelper.getSystemTime() < endwait) {
			synchronized (this.waitget) {
				if (this.waitget.get(pin) != -1) {
					return this.waitget.get(pin);
				}
			}
		}
		return -1;
	}

	/*
	 * send command to arduino
	 */
	protected void sendToArduino(String command) {
		if (command.length() < 1) {
			this.eventLogger.set(null);
			this.eventLogger.set("Empty command");
			return;
		}

		int pin;
		int value;

		try {
			byte bytes[] = null;

			switch (command.charAt(0)) {
			case 'S':
			case 's':
				if (command.length() != 7 || command.charAt(1) != ',' || command.charAt(4) != ',') {
					this.eventLogger.set(null);
					this.eventLogger.set("Wrong command. Correct format: S,00,0");
					return;
				}

				try {
					pin = Integer.parseInt(command.substring(2, 4));
				} catch (Exception ex) {
					pin = -1;
				}

				if ((isArduinoMega() && !contains(ArduinoMega.MEGA_OUT_PINS, pin)
						|| isArduinoUno() && !contains(ArduinoUno.UNO_OUT_PINS, pin))) {
					this.eventLogger.set(null);
					this.eventLogger.set("Wrong output pin number: " + String.valueOf(pin));
					return;
				}

				try {
					value = Integer.parseInt(command.substring(5, 7));
				} catch (Exception ex) {
					value = -1;
				}

				bytes = new byte[5];
				bytes[0] = (byte) 'S';
				bytes[1] = (byte) ',';
				bytes[2] = (byte) pin;
				bytes[3] = (byte) ',';
				bytes[4] = (byte) value;

				break;
			case 'r':
			case 'R':
				bytes = new byte[1];
				bytes[0] = (byte) 'R';
				break;
			case 'g':
			case 'G':
				if (command.length() != 4 || command.charAt(1) != ',') {
					this.eventLogger.set(null);
					this.eventLogger.set("Wrong command. Correct format: G,00");
					return;
				}

				try {
					pin = Integer.parseInt(command.substring(2, 4));
				} catch (Exception ex) {
					pin = -1;
				}

				if ((isArduinoMega() && !contains(ArduinoMega.MEGA_OUT_PINS, pin)
						&& !contains(ArduinoMega.MEGA_IN_PINS, pin))
						|| (isArduinoUno() && !contains(UNO_OUT_PINS, pin) && !contains(UNO_IN_PINS, pin))) {
					this.eventLogger.set(null);
					this.eventLogger.set("Wrong output pin number: " + String.valueOf(pin));
					return;
				}

				bytes = new byte[3];
				bytes[0] = (byte) 'G';
				bytes[1] = (byte) ',';
				bytes[2] = (byte) pin;

				break;
			case 'd':
			case 'D':
				bytes = new byte[1];
				bytes[0] = (byte) 'D';
				break;
			default:
				this.eventLogger.set(null);
				this.eventLogger.set("Wrong command");
				return;
			}

			this.eventLogger.set(null);
			this.eventLogger.set("Hex command:     " + bytesToHex(bytes));

			if (comPort == null) {
				this.eventLogger.set(null);
				this.eventLogger.set("No COM port selected");
				return;
			}

			comPort.writeBytes(bytes, bytes.length);

		} catch (Exception ex) {
			this.eventLogger.set(null);
			this.eventLogger.set("Failed to send command [" + command + "]");
		}
	}

	private boolean contains(int[] data, final int key) {
		return Arrays.stream(data).anyMatch(i -> i == key);
	}

	private String bytesToHex(byte[] bytes) {
		String result = "";

		for (int i = 0; i < bytes.length; i++) {
			result += String.format("%02X ", bytes[i]);
			result += " ";
		}

		return result.trim();
	}

	protected void uploadSketch() {
		if (comPort == null || "".equals(comPort)) {
			this.eventLogger.set(null);
			this.eventLogger.set("COM Port not selected");
			return;
		}

		if (isProgramming) {
			this.eventLogger.set(null);
			this.eventLogger.set("isProgramming in progress");
			return;
		}

		Thread thread;
		thread = new Thread() {
			@Override
			public void run() {
				isProgramming = true;
				try {
					closePort();

					String toolsFolder = SystemUtils.getToolsFolder();

					String AVRDudePath = toolsFolder + "avrdude.exe";
					String confPath = toolsFolder + "avrdude.conf";
					String workingDir = toolsFolder;
					String port = comPortName;
					String hexPath = "";
					String command = "";

					if (isArduinoMega()) {
						hexPath = toolsFolder + ("Mega.hex");
						command = AVRDudePath + " -C" + confPath + " -V -D -p ATmega2560 -c wiring -P " + port
								+ " -b 115200 -D -U flash:w:" + hexPath + ":i";
					} else if (isArduinoUno()) {
						hexPath = toolsFolder + ("Uno.hex");
						command = AVRDudePath + " -C" + confPath + " -V -D -p ATmega328p -c arduino -P " + port
								+ " -b 115200 -D -U flash:w:" + hexPath + ":i";
					}
					eventLogger.set(null);
					eventLogger.set("HEX write command: " + command);
					Runtime rt = Runtime.getRuntime();
					Process proc;
					proc = rt.exec(command, null, new File(workingDir));
					int exitVal = proc.waitFor();
					logger.debug("Process exitValue: {}", exitVal);
					eventLogger.set(null);
					eventLogger.set("HEX write result: " + (exitVal == 0 ? "OK" : "ERROR " + String.valueOf(exitVal)));
					openPort();
					isProgramming = false;

				} catch (Exception ex) {
					eventLogger.set(null);
					eventLogger.set("HEX write error: " + ex.getMessage());
					logger.error("{}", ex);
				}
				isProgramming = false;
			}
		};

		thread.start();
	}

	private void openPort() {
		comPort.setComPortParameters(this.bauds, this.databit, this.stopbit, this.parity);
		if (!comPort.openPort()) {
			closePort();
		}
		rxPos = 0;
	}

	private void closePort() {
		if (comPort == null) {
			return;
		}
		this.readPort = false;
		comPort.closePort();
	}

	private class MainRunnable implements Runnable {

		@Override
		public void run() {
			readPort = true;
			while (readPort) {
				if ("".equals(comPortName) || comPort == null) {
					continue;
				}
				try {
					this.wait(10);
				} catch (InterruptedException e) {
					logger.error("{}");
				}
				if (!comPort.isOpen()) {
					comPort.openPort();
				}
			}
		}
	}

	@Override
	protected void parseFrame(byte[] frame, int numRead) {
		// TODO Auto-generated method stub

	}

	/**
	 * discover all connected arduino boards
	 */
	@Override
	public void discover(long timeout) {

		com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();
		this.eventLogger.set("Search for connected arduino cards (mega and uno)");

		for (com.fazecast.jSerialComm.SerialPort port : ports) {
			String portname = port.getDescriptivePortName().toLowerCase();
			ArduinoUno sensor = null;
			if (portname.contains("uno")) {
				String name = portname.substring(portname.lastIndexOf("(") + 1, portname.length() - 1);
				sensor = new ArduinoUno();
			} else if (portname.contains("mega")) {
				String name = portname.substring(portname.lastIndexOf("(") + 1, portname.length() - 1);
				sensor = new ArduinoMega();
			}
			if (sensor != null) {
				sensor.setPort(port.getSystemPortName());
				sensor.comPort = port;
				this.discoveredInterface.set(sensor);
				this.eventLogger.set("Initialize " + portname + " on port " + sensor.getPort());
				sensor.uploadSketch();
			}
		}
	}

	@Override
	protected void ioPinList() {
		pins.clear();

		String pinName;
		for (int j = 0; j < UNO_IN_PINS.length; j++) {
			int i = UNO_IN_PINS[j];
			String identifier = "digital.in." + i;
			if (i >= 14 && i <= 19) {
				pinName = "-A" + (i - 14);
			} else {
				pinName = i + "";
			}
			SensorPinImpl p = new SensorPinImpl(this, identifier, i + " " + pinName);
			pins.add(p);
			p.setBounds(50 + (j % 10) * 25, 100 + (j / 10) * 25, 20, 20);
		}

		for (int j = 0; j < UNO_OUT_PINS.length; j++) {
			int i = UNO_OUT_PINS[j];
			String identifier = "digital.out." + i;
			if (i >= 14 && i <= 19) {
				pinName = "-A" + (i - 14);
			} else {
				pinName = i + "";
			}
			SensorPinImpl p = new SensorPinImpl(this, identifier, i + " " + pinName);
			pins.add(p);
			p.setBounds(50 + (j % 10) * 25, 200 + (j / 10) * 25, 20, 20);
		}
	}

}
