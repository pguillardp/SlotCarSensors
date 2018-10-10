package com.racer40.sensor.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.common.Rs232;
import com.racer40.sensor.common.SensorPinImpl;

public abstract class DSxxx extends Rs232 {
	private final Logger logger = LoggerFactory.getLogger(DSxxx.class);

	private static final byte START_BYTE = (byte) 0xE0;
	private static final byte END_BYTE = (byte) 0xEB;
	private static final byte TOTAL_BYTES = 21;

	private static final int MAX_STARTPOS = 8;

	protected static final String PAUSE = "PAUSE";
	protected static final String START = "START";
	protected static final String RESUME = "RESUME";
	protected static final String ABORT = "ABORT";

	private byte dataWord[] = new byte[TOTAL_BYTES], olddataWord[] = new byte[TOTAL_BYTES];
	private int byteCount, i, checksum;
	private boolean sync = false;
	private byte intc;

	private boolean newdata;

	private boolean ds200 = false;
	private boolean ds300 = false;

	private int dsComOffset = 0; // used to manage several ds per computer.
									// Offset added to lane number

	public DSxxx() {
		super();
		this.poll = 10;
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
	public void stop() {
		super.stop();
	}

	@Override
	protected void parseFrame(byte[] fromDS, int numRead) {
		byte c = fromDS[0];
		intc = (byte) (c & 0xFF); /*
									 * This solves some leading FF problems with negative numbers.
									 */

		// If not in sync, check for synchronization to 21 byte data word.
		if (!sync) {
			if (intc == START_BYTE) {
				byteCount = 0;
				dataWord[byteCount++] = intc;
				sync = true;
			} else {
				logger.debug(String.format("c is %02x not %02x.\n", intc, START_BYTE));
				if (this.isDebugMode()) {
					this.eventLogger.set("Unknown received frame:");
					this.monitorHexaFrame(fromDS);
				}
			}

			// parsed synchronized data
		} else {
			dataWord[byteCount++] = c;
			if (byteCount >= TOTAL_BYTES) {
				byteCount = 0;

				/*
				 * See if dataWord is new. DS300 has a habit of spitting out three identical
				 * dataWords in a row. We don't need to look at it three times.
				 */
				newdata = false;
				for (i = 0; i < TOTAL_BYTES; i++) {
					if (olddataWord[i] != dataWord[i]) {
						newdata = true;
						break;
					}
				}

				// parse new data
				if (newdata) {
					started = true;
					parseFrame();
				}
			} // end of total bytes found
		}

	}

	/**
	 * parse a newly received frame
	 */
	private void parseFrame() {
		StringBuilder information = new StringBuilder();

		monitorHexaFrame(this.dataWord);

		// detection pin
		SensorPinImpl pin = null;
		boolean detection = false;
		boolean notify = true;

		// allow structure to forward this event to the sensor
		// assumes it will be released by its parent ;)
		int lane = 0;

		// Display results of data word. => DS revision
		if (dataWord[3] == 2) {
			logger.debug("DS200: \n");
			information.append("DS200");
			this.ds200 = true;
			this.ds300 = false;
		} else if (dataWord[3] == 3) {
			logger.debug("DS300: \n");
			information.append("DS300");
			this.ds200 = false;
			this.ds300 = true;
		}

		// extract & Display type of data
		switch (dataWord[7]) {
		case 0x00:
			logger.debug("Function ");
			break;
		case 0x1B:
			logger.debug("Timing data ");
			information.append(" - Timing data");
			break;
		case 0x1C:
			logger.debug("Final record data ");
			information.append(" - Final record data");
			break;
		case 0x3A:
			logger.debug("Programmed by time ");
			information.append(" - Programmed by time");
			break;
		case 0x3B:
			logger.debug("Programmed by laps (total) ");
			information.append(" - Programmed by laps (total)");
			break;
		case 0x3C:
			logger.debug("Programmed by laps (individual) ");
			information.append(" - Programmed by laps (indi))");
			break;
		case 0x3D:
			logger.debug("Programmed by F1 ");
			information.append(" - Programmed by F1");
			break;
		case 0x3F:
			information.append(" - Press start race button");
			logger.debug("Press start race button");
			break;
		default:
			information.append(" - Unknown data");
			logger.debug(String.format("Unknown data %02x ", dataWord[7]));
			break;
		}

		// Display type of function (assuming we have a function).
		if (dataWord[7] == 0x00) {
			switch (dataWord[8]) {
			case (byte) 0xA1:
				logger.debug("Start of race, phase 1 "); // not
															// managed
															// by DS
				information.append(" - Start#1");
				break;
			case (byte) 0xA2:
				logger.debug("Start of race, phase 2 "); // not
															// managed
															// by DS
				information.append(" - Start#2");
				break;
			case (byte) 0xA3:
				logger.debug("Start of race, phase 3 "); // not
															// managed
															// by DS
				information.append(" - Start#3");
				break;
			case (byte) 0xA4:
				logger.debug("End of race "); // not managed by DS
				information.append(" - End of race");
				break;
			case (byte) 0xA5:
				logger.debug("Start of pause ");
				information.append(" - Pause");
				pin = this.getPin(DSxxx.PAUSE);
				break;
			case (byte) 0xA6:
				logger.debug("End of pause ");
				information.append(" - End pause");
				pin = this.getPin(DSxxx.RESUME);
				break;
			case (byte) 0xA7:
				logger.debug("Abort race ");
				information.append(" - Abort");
				pin = this.getPin(DSxxx.ABORT);
				break;
			default:
				information.append(" - Unknown");
				logger.debug(String.format("Unknown function %02x ", dataWord[8]));
				break;
			}
		}

		// start race
		if (dataWord[8] == 0xA1) {

			/*
			 * If start of race (0xA1), next two bytes have different meaning.
			 */
			pin = this.getPin(DSxxx.START);
			logger.debug(String.format("Program values: %02x and %02x\n", dataWord[9], dataWord[10]));

			// on going race event
		} else {

			/* Display Identifiers. */
			switch (dataWord[9]) {
			case (byte) 0xA8:
				/*
				 * Documentation say this should be "1st position. Actual use looks like this is
				 * really fast lap.
				 */
				/* logger.debug("1st position "); */
				logger.debug("Fast lap ");
				break;
			case (byte) 0xA9:
				logger.debug("Fast lap ");
				break;
			default:
				/* logger.debug("Identifier %02x ", dataWord[9]); */
				break;
			}

			/* timing data */
			if (dataWord[7] == 0x1B) {

				/* Display Lane number. */
				switch (dataWord[10]) {
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
					logger.debug(String.format("Unknown lane number %02x\n", dataWord[10]));
					break;
				}
				int bridge = lane;
				if (lane > 0) {
					int pinlane = lane;

					// ds200 & ds300: 2 virtual detection pins but lane number is between 1 and 8
					// when large detection bridges are used
					if (this instanceof DS200 || this instanceof DS300) {
						pinlane = (lane & 1) + 1;
					}
					logger.debug("lane " + lane);

					pin = this.getPin(pinlane + "");
					pin.setDetectionID(lane);
					detection = true;
				}

				// display information
				information.append(String.format(" - Bridge  lane %d - DS lane %d", bridge, lane));
			}
		}

		/* Display Number of laps. */
		int nLap = (dataWord[11] >> 4) * 1000 + (dataWord[11] & 0xf) * 100 + (dataWord[12] >> 4) * 10
				+ (dataWord[12] & 0xf);
		logger.debug("Laps " + nLap);
		information.append(String.format(" - lap %d", nLap));

		/* Display hours, minutes, seconds. */
		logger.debug(String.format("HH:MM:SS %1d%1d:%1d%1d:%1d%1d.", dataWord[13] >> 4, dataWord[13] & 0xf,
				dataWord[14] >> 4, dataWord[14] & 0xf, dataWord[15] >> 4, dataWord[15] & 0xf));
		int nHour = 0, nMinute = 0, nSecond = 0;
		long lMilli = 0L;
		if (nLap > 1) {
			nHour = (((dataWord[13] & 0xff) >> 4) * 10 + (dataWord[13] & 0xf)) % 24;
			nMinute = ((dataWord[14] >> 4) * 10 + (dataWord[14] & 0xf)) % 60;
			nSecond = ((dataWord[15] >> 4) * 10 + (dataWord[15] & 0xf)) % 60;
			lMilli = ((((dataWord[16] & 0xff) >> 4) * 100) + (((dataWord[16] & 0xf)) * 10)
					+ ((dataWord[17] & 0xff) >> 4)) % 1000;
			if (lMilli < 0) {
				long l = (((dataWord[16] >> 4) * 100) + ((dataWord[16] & 0xf) * 10) + (dataWord[17] >> 4));
				logger.debug("negative: " + l);
			}
		}

		information.append(String.format(" - %02d:%02d:%02d.%03d", nHour, nMinute, nSecond, lMilli));

		// Display fractions of a second
		// nb : the DSxxx event timer and ur3 timer are not
		// synchronised => do it when needed
		logger.debug(String.format("%1d%1d%1d%1d.", dataWord[16] >> 4, dataWord[16] & 0xf, dataWord[17] >> 4,
				dataWord[17] & 0xf));
		// pin.setHour(nHour);
		// pin.setMinute(nMinute);
		// pin.setSecond(nSecond);
		pin.setTimeEvent(((long) nHour * 3600 + (long) nMinute * 60 + nSecond) * 1000 + lMilli, true);

		// Test checksum.
		for (i = 1, checksum = 0; i < 18; i++) {
			checksum += dataWord[i];
		}
		checksum += dataWord[19];
		if ((checksum & 0xff) != dataWord[18]) {
			logger.debug(
					String.format("Warning: Checksum failure.  Was %02x not %02x.\n", (checksum & 0xff), dataWord[18]));
		}

		/* Display any errors in data word. */
		if (dataWord[2] != TOTAL_BYTES) {
			logger.debug(
					String.format("Warning: Word length incorrect.  Was %02x not %02x.\n", dataWord[2], TOTAL_BYTES));
			notify = false;
		}
		if (dataWord[TOTAL_BYTES - 1] != END_BYTE) {
			logger.debug(String.format("Warning: End byte incorrect.  Was %02x not %02x.\n", dataWord[TOTAL_BYTES - 1],
					END_BYTE));
			notify = false;
		}

		// Save dataWord.
		for (i = 0; i < TOTAL_BYTES; i++) {
			olddataWord[i] = dataWord[i];
		}

		// new data parsed => forward event to sensor if parsing is
		// correct
		if (notify) {
			this.eventLogger.set(information.toString());
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

	@Override
	public void discover(long timeout) {
		// TODO Auto-generated method stub

	}
}
