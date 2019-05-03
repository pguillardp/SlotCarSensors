package com.racer40.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.Rs232;
import com.racer40.sensor.SensorPinImpl;

public abstract class DSxxx extends Rs232 {
	private final Logger logger = LoggerFactory.getLogger(DSxxx.class);

	private static final byte START_BYTE = (byte) 0xE0;
	private static final byte END_BYTE = (byte) 0xEB;
	private static final byte TOTAL_BYTES = 21;

	private static final int MAX_DETECTED_CARS_CARS = 8;

	private byte dataWord[] = new byte[TOTAL_BYTES * 3];
	private byte frame[] = new byte[TOTAL_BYTES];
	private int frameno = -1;
	private int byteCount, i, checksum;
	private boolean sync = false;
	private byte intc;
	private int received = 0;

	private boolean newdata;

	private boolean ds200 = false;
	private boolean ds300 = false;

	private int dsComOffset = 0; // used to manage several ds per computer.
									// Offset added to lane number

	public DSxxx() {
		super();

		this.automaticDiscovery = false;

		this.bauds = 4800;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;
	}

	public int getDsComOffset() {
		return dsComOffset;
	}

	public void setDsComOffset(int dsComOffset) {
		this.dsComOffset = dsComOffset;
	}

	@Override
	public boolean start() {
		this.received = 0;
		this.frameno = Integer.MIN_VALUE;
		return super.start();
	}

	@Override
	protected void handleSerialEvent(SerialPortEvent event) {
		byte[] fromDS = event.getReceivedData();
		for (int i = 0; i < fromDS.length; i++) {
			this.dataWord[this.received + i] = fromDS[i];
			this.received++;
		}

		// try to parse frame
		int i = 0;
		while (this.received >= TOTAL_BYTES) {
			for (; (i <= this.received - TOTAL_BYTES); i++) {
				if (this.dataWord[i] == START_BYTE && this.dataWord[i + TOTAL_BYTES - 1] == END_BYTE
						&& this.dataWord[i + 2] == TOTAL_BYTES) {
					this.parseFrame(i);
					this.received -= TOTAL_BYTES;
					i += (TOTAL_BYTES - 1);
				} else {
					this.received--;
				}
			}

		}
		// shift buffer beginning
		for (int j = i; j < this.received; j++) {
			this.dataWord[j - i] = this.dataWord[j];
		}
	}

	/**
	 * parse a newly received frame
	 * 
	 * @param offset
	 */
	private void parseFrame(int offset) {

		this.framefound = true;

		for (int j = 0; j < TOTAL_BYTES; j++) {
			this.frame[j] = this.dataWord[offset + j];
		}
		if (isDebugMode()) {
			this.eventLogger.set(null);
			this.eventLogger.set(bytesToHex("<- from sensor", frame, Rs232.HEX_COLUMNS));
		}

		// consistency checks
		if (this.frameno == this.frame[1]) {
			return;
		}
		this.frameno = this.frame[1];

		// detection pin
		SensorPinImpl pin = null;

		// allow structure to forward this event to the sensor
		// assumes it will be released by its parent ;)
		int lane = 0;

		// Display results of data word. => DS revision
		if (this.frame[3] == 2) {
			this.eventLogger.set("DS200 found");
			this.ds200 = true;
			this.ds300 = false;
		} else if (this.frame[3] == 3) {
			this.eventLogger.set("DS300 found");
			this.ds200 = false;
			this.ds300 = true;
		}

		// extract & Display type of data
		switch (this.frame[7]) {
		case 0x00:
			this.eventLogger.set("Function ");
			break;
		case 0x1B:
			this.eventLogger.set("Timing data ");
			break;
		case 0x1C:
			this.eventLogger.set("Final record data ");
			break;
		case 0x3A:
			this.eventLogger.set("Programmed by time ");
			break;
		case 0x3B:
			this.eventLogger.set("Programmed by laps (total) ");
			break;
		case 0x3C:
			this.eventLogger.set("Programmed by laps (individual) ");
			break;
		case 0x3D:
			this.eventLogger.set("Programmed by F1 ");
			break;
		case 0x3F:
			this.eventLogger.set("Press start race button");
			break;
		default:
			this.eventLogger.set(String.format("Unknown data %02x ", this.frame[7]));
			break;
		}

		// Display type of function (assuming we have a function).
		if (this.frame[7] == 0x00) {
			switch (this.frame[8]) {
			case (byte) 0xA1:
				this.eventLogger.set("Start of race, phase 1 "); // not
				break;
			case (byte) 0xA2:
				this.eventLogger.set("Start of race, phase 2 ");
				break;
			case (byte) 0xA3:
				this.eventLogger.set("Start of race, phase 3 ");
				break;
			case (byte) 0xA4:
				this.eventLogger.set("End of race ");
				break;

			case (byte) 0xA5:
				this.eventLogger.set("Start of pause ");
				pin = this.getPin("pause.in.0");
				break;

			case (byte) 0xA6:
				this.eventLogger.set("End of pause ");
				pin = this.getPin("resume.in.0");
				break;

			case (byte) 0xA7:
				this.eventLogger.set("Abort race ");
				pin = this.getPin("abort.in.0");
				break;

			default:
				this.eventLogger.set(String.format("Unknown function %02x ", this.frame[8]));
				break;
			}
		}

		// start race
		if (this.frame[8] == 0xA1) {

			/*
			 * If start of race (0xA1), next two bytes have different meaning.
			 */
			pin = this.getPin("go.in.0");
			this.eventLogger.set(String.format("Program values: %02x and %02x\n", this.frame[9], this.frame[10]));

			// on going race event
		} else {

			/* Display Identifiers. */
			switch (this.frame[9]) {
			case (byte) 0xA8:
				/*
				 * Documentation say this should be "1st position. Actual use looks like this is
				 * really fast lap.
				 */
				this.eventLogger.set("Fast lap ");
				break;
			case (byte) 0xA9:
				this.eventLogger.set("Fast lap ");
				break;
			default:
				break;
			}

			/* timing data */
			if (this.frame[7] == 0x1B) {

				/* Display Lane number. */
				switch (this.frame[10]) {
				case (byte) 0x80:
					lane = 1;
					break;
				case 0x40:
					lane = 2;
					break;
				case 0x20:
					lane = 3;
					break;
				case 0x10:
					lane = 4;
					break;
				case 0x08:
					lane = 5;
					break;
				case 0x04:
					lane = 6;
					break;
				case 0x02:
					lane = 7;
					break;
				case 0x01:
					lane = 8;
					break;
				default:
					this.eventLogger.set(String.format("Unknown lane number %02x\n", this.frame[10]));
					break;
				}
				if (lane > 0) {
					int pinlane = lane;

					// ds200 & ds300: 2 virtual detection pins but lane number is between 1 and 8
					// when large detection bridges are used
					if (this instanceof DS200 || this instanceof DS300) {
						pinlane = (lane & 1) + 1;
					}
					this.eventLogger.set("lane " + lane);

					pin = this.getPin(pinlane + "");
					pin.setDetectionID(lane);
				}
			}
		}

		/* Display Number of laps. */
		int nLap = (this.frame[11] >> 4) * 1000 + (this.frame[11] & 0xf) * 100 + (this.frame[12] >> 4) * 10
				+ (this.frame[12] & 0xf);
		this.eventLogger.set("Laps " + nLap);

		/* Display hours, minutes, seconds. */
		this.eventLogger.set(String.format("HH:MM:SS %1d%1d:%1d%1d:%1d%1d.", this.frame[13] >> 4, this.frame[13] & 0xf,
				this.frame[14] >> 4, this.frame[14] & 0xf, this.frame[15] >> 4, this.frame[15] & 0xf));
		int nHour = 0, nMinute = 0, nSecond = 0;
		long lMilli = 0L;
		if (nLap > 1) {
			nHour = (((this.frame[13] & 0xff) >> 4) * 10 + (this.frame[13] & 0xf)) % 24;
			nMinute = ((this.frame[14] >> 4) * 10 + (this.frame[14] & 0xf)) % 60;
			nSecond = ((this.frame[15] >> 4) * 10 + (this.frame[15] & 0xf)) % 60;
			lMilli = ((((this.frame[16] & 0xff) >> 4) * 100) + (((this.frame[16] & 0xf)) * 10)
					+ ((this.frame[17] & 0xff) >> 4)) % 1000;
			if (lMilli < 0) {
				long l = (((this.frame[16] >> 4) * 100) + ((this.frame[16] & 0xf) * 10) + (this.frame[17] >> 4));
				this.eventLogger.set("negative: " + l);
			}
		}

		// Display fractions of a second
		// nb : the DSxxx event timer and ur3 timer are not
		// synchronised => do it when needed
		this.eventLogger.set(String.format("%1d%1d%1d%1d.", this.frame[16] >> 4, this.frame[16] & 0xf,
				this.frame[17] >> 4, this.frame[17] & 0xf));
		if (pin != null) {
			pin.setTimeEvent(((long) nHour * 3600 + (long) nMinute * 60 + nSecond) * 1000 + lMilli, true);
		}
		// Test checksum.
		for (i = 1, checksum = 0; i < 18; i++) {
			checksum += this.frame[i];
		}
		checksum += this.frame[19];
		if ((checksum & 0xff) != this.frame[18]) {
			this.eventLogger.set(String.format("Warning: Checksum failure. Was %02x not %02x.\n", (checksum & 0xff),
					this.frame[18]));
		}

		// new data parsed => forward event to sensor if parsing is
		// correct
		if (pin != null) {
			this.notifyPinChanged(pin);
		}
	}

	@Override
	public boolean setOutputPinValue(String pin, int value) {
		// NA: box
		return false;
	}

	public boolean isDs200() {
		return ds200;
	}

	public boolean isDs300() {
		return ds300;
	}

	private List<DSxxx> todiscover = new ArrayList<>();
	private List<DSxxx> found = new ArrayList<>();
	private Timer timer = null;
	protected boolean framefound = false;
	private long prevsec = -1;
	private long prevdate = -1;
	private TimerTask repeatedTask = null;

	@Override
	public boolean isDiscoveryRunning() {
		return this.timer != null;
	}

	@Override
	public void stopDiscovery() {
		if (timer != null) {
			this.stopDiscovery("Search cancelled");
		}
	}

	private void stopDiscovery(String log) {
		if (timer != null) {
			this.timer.cancel();
			this.timer = null;
			this.repeatedTask.cancel();
			this.repeatedTask = null;
			this.eventLogger.set(log);
			this.todiscover.forEach(ds -> ds.stop());
			this.todiscover.clear();
		}
	}

	@Override
	public void discover(long timeout) {

		if (this.timer == null) {

			// start one sensor per available port
			this.eventLogger.set(null);
			this.eventLogger.set("Search for DSxxx during " + ((int) (timeout / 1000))
					+ "s. Trigger detection during this timeframe");
			this.todiscover.clear();
			this.found.clear();

			// one sensor per port
			List<String> ports = this.getSerialPortList();
			for (String port : ports) {
				DSxxx ds = (DSxxx) this.createSensor();
				this.todiscover.add(ds);
				ds.setPort(port);
				ds.framefound = false;
				ds.start();
			}

			// timer to countdown search seconds
			long startdate = DateTimeHelper.getSystemTime();
			prevsec = -99;
			this.repeatedTask = new TimerTask() {
				@Override
				public void run() {
					long diff = DateTimeHelper.getSystemTime() - startdate;
					if (diff > timeout) {
						stopDiscovery("Timed out");
						return;
					}
					long sec = (timeout - diff) / 1000;
					if (sec < 0) {
						sec = 0;
					}
					if (prevsec != sec) {
						prevsec = sec;
						eventLogger.set("Detecting ........ " + sec + "s");
					}
					for (DSxxx ds : todiscover) {
						if (ds.framefound && !found.contains(ds)) {
							found.add(ds);
							discoveredInterface.set(ds);
						}
					}
				}
			};
			timer = new Timer("Timer");
			timer.scheduleAtFixedRate(repeatedTask, 0, 1000);

		} else {

			this.stopDiscovery();
		}
	}
}
