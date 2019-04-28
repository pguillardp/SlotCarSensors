package com.racer40.sensor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public abstract class Rs232 extends SensorImpl {
	static final Logger logger = LoggerFactory.getLogger(Rs232.class);

	protected static final int DISCOVERY_TIMEOUT = 20;

	// serial port default settings
	protected int bauds = 9600;
	protected int databit = 8;
	protected int stopbit = SerialPort.ONE_STOP_BIT;
	protected int parity = SerialPort.NO_PARITY;

	protected SerialPort comPort = null;

	protected int rxPos;

	protected SerialPortDataListener dataListener;

	protected static final int HEX_COLUMNS = 16;

	public Rs232() {
		super();
		port = "COM1";
		this.serial = true;
		this.dataListener = new SerialPortDataListener() {
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
			}

			@Override
			public void serialEvent(SerialPortEvent event) {
				handleSerialEvent(event);
			}
		};

	}

	protected List<String> getSerialPortList() {
		List<String> comList = new ArrayList<>();
		for (SerialPort c : SerialPort.getCommPorts()) {
			comList.add(c.getSystemPortName());
		}
		return comList;
	}

	protected String getSystemPortName(String descriptivePortName) {
		String portName = "";
		SerialPort ports[] = SerialPort.getCommPorts();
		for (SerialPort port : ports) {
			if (port.getDescriptivePortName().equals(descriptivePortName)) {
				portName = port.getSystemPortName();
				break;
			}
		}
		return portName;
	}

	// CORE: manage received data packets
	protected abstract void handleSerialEvent(SerialPortEvent event);

	@Override
	public boolean start() {
		this.stop();
		this.comPort = SerialPort.getCommPort(this.port.toUpperCase());
		if (this.comPort != null) {
			if (this.comPort.isOpen()) {
				this.comPort.closePort();
			}
			comPort.setComPortParameters(this.bauds, this.databit, this.stopbit, this.parity);
			this.comPort.removeDataListener();

			rxPos = 0;
			if (comPort.openPort()) {
				this.comPort.addDataListener(this.dataListener);
				return true;
			}
		}
		return false;
	}

	@Override
	public void run() {
	}

	@Override
	public void stop() {
		if (this.comPort == null) {
			return;
		}
		try {
			comPort.removeDataListener();
			if (this.comPort.isOpen()) {
				comPort.closePort();
			}
		} catch (Exception e) {
			logger.error("{}", e);
		}
	}

	@Override
	public boolean isStarted() {
		if (this.comPort != null) {
			return this.comPort.isOpen();
		}
		return false;
	}

	protected void monitorHexaFrame(byte[] dataWord) {
		if (this.isDebugMode()) {
			StringBuilder debugInfo = new StringBuilder();

			// put frame in debug buffer
			for (byte d : dataWord) {
				debugInfo.append(String.format("%02x", d));
				debugInfo.append(" ");
			}
			this.eventLogger.set(debugInfo.toString());
		}
	}

	protected boolean contains(int[] data, final int key) {
		// return Arrays.stream(data).anyMatch(i -> i == key);
		for (int i : data) {
			if (i == key) {
				return true;
			}
		}
		return false;
	}

	protected String bytesToHex(byte[] bytes) {
		String result = "";
		for (int i = 0; i < bytes.length; i++) {
			result += String.format("%02X ", bytes[i]);
			result += " ";
		}
		return result.trim();
	}

	protected String bytesToHex(String note, byte[] bytes, int cols) {
		StringBuilder result = new StringBuilder();
		if (!StringUtils.isEmpty(note)) {
			result.append(note);
			result.append("\n");
		}
		for (int i = 0; i < bytes.length; i++) {
			if (i != 0 && ((i % cols) == 0)) {
				result.append("\n");
			}
			result.append(String.format("%02X ", bytes[i]));
			result.append(" ");
		}
		return result.toString().trim();
	}

	protected void writeToSerial(byte[] bytes) {
		this.comPort.writeBytes(bytes, bytes.length);
		if (this.isDebugMode()) {
			this.eventLogger.set(null);
			this.eventLogger.set(this.bytesToHex("-> to sensor", bytes, Rs232.HEX_COLUMNS));
		}
	}

	@Override
	public boolean setOutputPinValue(String pinIdentifier, int value) {
		// NA
		return false;
	}

	@Override
	public int getPinValue(String pinIdentifier) {
		// NA
		return 0;
	}

	@Override
	public String getPort() {
		return this.port;
	}

	@Override
	public String getSetup() {
		return this.bauds + "," + this.databit + "," + this.parity + "," + this.stopbit;
	}

	@Override
	public void discover(long timeout) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSetup(String setup) {
		super.setSetup(setup);
		String[] s = setup.replace(" ", "").split(",");
		try {
			if (s.length >= 1) {
				this.bauds = Integer.parseInt(s[0]);
			}
			if (s.length >= 2) {
				this.databit = Integer.parseInt(s[1]);
			}
			if (s.length >= 3) {
				this.parity = Integer.parseInt(s[2]);
			}
			if (s.length >= 4) {
				this.stopbit = Integer.parseInt(s[3]);
			}
		} catch (Exception e) {

		} finally {

		}
	}

	@Override
	protected void ioPinList() {
		// TODO Auto-generated method stub

	}

}
