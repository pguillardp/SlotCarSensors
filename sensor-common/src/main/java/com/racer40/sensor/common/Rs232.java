package com.racer40.sensor.common;

import java.io.IOError;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.concurrent.Task;

// http://rxtx.qbang.org/wiki/index.php/Two_way_communcation_with_the_serial_port
// http://fizzed.com/oss/rxtx-for-java
public abstract class Rs232 extends SensorImpl {
	static final Logger logger = LoggerFactory.getLogger(Rs232.class);

	protected static final int DISCOVERY_TIMEOUT = 20;

	protected InputStream in;
	protected OutputStream out;

	// serial port default settings
	protected int bauds = 9600;
	protected int databit = 8;
	protected int stopbit = SerialPort.ONE_STOP_BIT;
	protected int parity = SerialPort.NO_PARITY;

	protected int poll = 10;

	private Task<Void> rs232task;

	protected Task<Void> search;

	private SerialPort comPort = null;

	public Rs232() {
		super();
		port = "COM1";
	}

	public void setPoll(int pollms) {
		this.poll = pollms;
		this.stop();
		this.start();
	}

	protected List<String> getSerialPortList() {
		List<String> comList = new ArrayList<>();
		for (SerialPort c : SerialPort.getCommPorts()) {
			comList.add(c.getSystemPortName());
		}
		return comList;
	}

	@Override
	public boolean start() {
		super.start();
		if (this.rs232task == null) {
			this.rs232task = new Task<Void>() {
				@Override
				protected Void call() {

					// open port
					if (openPort()) {

						// read/write
						while (true) {
							if (isCancelled()) {
								updateMessage("Cancelled");
								break;
							}

							// Block the thread for a short time, but be sure
							// to check the InterruptedException for cancellation
							try {
								Thread.sleep(500);
							} catch (InterruptedException interrupted) {
								if (isCancelled()) {
									updateMessage("Cancelled");
									break;
								}
							}
						}
					}
					return null;
				}
			};
			this.rs232task.run();
		}
		return true;
	}

	private boolean openPort() {
		boolean isopened = false;

		try {
			comPort.setComPortParameters(this.bauds, this.databit, this.stopbit, this.parity);
			comPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 1, 1);
			comPort.openPort();
			this.in = comPort.getInputStream();
			this.out = comPort.getOutputStream();
			isopened = true;
		} catch (IOError ex) {
			logger.error("{}", ex);
			return false;
		}

		comPort.addDataListener(new SerialPortDataListener() {
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
			}

			@Override
			public void serialEvent(SerialPortEvent event) {
				if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
					return;
				byte[] newData = new byte[comPort.bytesAvailable()];
				int numRead = comPort.readBytes(newData, newData.length);
				parseFrame(newData, numRead);
				logger.debug("rs232 read {} bytes", numRead);
			}
		});

		return isopened;
	}

	protected abstract void parseFrame(byte[] frame, int numRead);

	protected void writeData(byte[] data) {
		try {
			this.out.write(data);
			this.out.flush();

		} catch (Exception e) {
			logger.error("{}", e);
		}
	}

	@Override
	public void stop() {
		if (this.rs232task != null) {
			this.rs232task.cancel();
			while (!this.rs232task.isCancelled())
				;
			if (comPort != null && comPort.isOpen()) {
				comPort.closePort();
				comPort = null;
			}
			this.rs232task = null;
		}
	}

	@Override
	public void run() {
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
	public void discover(long timeout) {
		if (this.search == null) {
			List<String> ports = getSerialPortList();
			List<SensorInterface> sensors = new ArrayList<>();
			long endDate = DateTimeHelper.getSystemTime() + timeout;
			this.search = new Task<Void>() {

				@Override
				protected Void call() throws Exception {

					// initialization
					for (String port : ports) {
						SensorInterface sensor = createSensor();
						sensor.setPort(port);
						sensors.add(sensor);
						sensor.start();
						Rs232.this.eventLogger.set("search " + sensor.getName() + "on port " + port);
					}

					// search loop
					while (DateTimeHelper.getSystemTime() < endDate) {
						if (isCancelled()) {
							this.cancelDiscovery();
							break;
						}

						// Block the thread for a short time, but be sure
						// to check the InterruptedException for cancellation
						try {
							Thread.sleep(500);
						} catch (InterruptedException interrupted) {
							if (isCancelled()) {
								this.cancelDiscovery();
								break;
							}
						}
					}
					search = null;
					sensors.clear();
					return null;
				}

				private void cancelDiscovery() {
					for (SensorInterface sensor : sensors) {
						sensor.stop();
					}
				}
			};
			this.search.run();
		}

	}

	@Override
	public String getPort() {
		return this.port;
	}

	@Override
	public String getSetup() {
		return this.bauds + "," + this.databit + "," + this.parity + "," + this.stopbit;
	}

}
