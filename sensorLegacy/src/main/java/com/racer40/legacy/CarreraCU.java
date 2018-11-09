package com.racer40.legacy;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.Rs232;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

/**
 * ref: https://github.com/tkem/carreralib/tree/master/carreralib
 * 
 * @author Pierrick
 *
 */
public class CarreraCU extends Rs232 {
	private static final int DISCOVERY_TIMEOUT = 5;

	private final Logger logger = LoggerFactory.getLogger(CarreraCU.class);

	// pool at the end of proc cos polling thread is called every xxx ms
	// with PC unit
	private static final int SRPORT_D132_BUFFER = 1024;
	private static final int D132_QUERY_STATUS = 0x3f22; // "?
	private static final int D132_QUERY_VERSION = 0x3022; // "0
	private static final int D132_QUERY_CMD = 0x3f; // ?
	private static final int D132_STATUS_PACKET = 0x3a3f; // ?:
	private static final int D132_QUERY = 0x233f; // ?#
	private static final int DI32_VERSION = 0x30; // 0
	private static final int D132_END_FRAME = 0x24; // $

	private static final int D132_CAR1 = 0x313f; // ?1
	private static final int D132_CAR2 = 0x323f; // ?2
	private static final int D132_CAR3 = 0x333f; // ?3
	private static final int D132_CAR4 = 0x343f; // ?4
	private static final int D132_CAR5 = 0x353f; // ?5
	private static final int D132_CAR6 = 0x363f; // ?6
	private static final int D132_CAR7 = 0x373f; // ?7
	private static final int D132_CAR8 = 0x383f; // ?8

	private static final int D132_MAX_SIZE = 20; // max packet size

	private static final int D132_MAX_SPEED_CMD = 0x30; // 0
	private static final int D132_BREAK_CMD = 0x31; // 1
	private static final int D132_FUEL_LIMIT_CMD = 0x32; // 2
	private static final int D132_CHANGE_ID_CMD = 0x34; // 4
	private static final int D132_REFUEL_CMD = 0x35; // 5
	private static final int D132_SET_POSITION_CMD = 0x36; // 6
	private static final int D132_FUEL_CMD = 0x3a; // 10

	private long dateOffset = 0L;
	private long firstDetection = 0L;

	private final byte queryStatus[] = toByteArray(D132_QUERY_STATUS);

	public CarreraCU() {
		super();

		this.type = SensorConstants.CARRERA_DIGITAL_CU;
		this.name = "Carrera Control Unit";
		this.managedCars = 6;
		this.image = "carreraCU.bmp";
		this.pinoutImage = "carreraCU_pinout.png";
		this.digital = true;
		

		this.poll = 10;
		this.bauds = 19200;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;

		ioPinList();
	}

	// packet checksum control
	private boolean D132Checksum(byte frame[], int start, int end) {

		// checksum control
		int bChecksum, i;
		for (bChecksum = 0x00, i = start; i <= end; i++) {
			bChecksum = (bChecksum + (frame[i] & 0x0f)) & 0x0f;
		}
		bChecksum |= 0x30;

		// poll & exit if fails
		if ((frame[end + 3] & 0xff) != D132_END_FRAME) {
			return false;
		}

		return true;
	}

	// skip an unknown packet
	/*
	 * private int[ SkipPacket(int fromCU[], int bMax) { unsigned short *p =
	 * (unsigned short*) fromCU;
	 * 
	 * while ((*p != D132_QUERY) && (*p != D132_QUERY_STATUS) && (fromCU < bMax))
	 * fromCU++; return fromCU; }
	 */

	/**
	 * utils
	 */
	private byte[] toByteArray(int value) {
		byte array[] = new byte[2];
		array[0] = (byte) (value & 0xff);
		array[1] = (byte) ((value & 0xff00) >> 8);
		return array;
	}

	/**
	 * check packet type - generic
	 * 
	 * @param packet
	 * @param value
	 * @return
	 */
	private boolean isEqual(byte packet[], int value) {
		if (packet[0] != (value & 0xff) || (packet[1] & 0xff) != ((value & 0xff00) >> 8)) {
			return false;
		}
		return true;
	}

	/**
	 * cross line data packet
	 * 
	 * @param packet
	 * @return
	 */
	private boolean isCrossLine(byte packet[]) {
		if (isEqual(packet, D132_CAR1) || isEqual(packet, D132_CAR2) || isEqual(packet, D132_CAR3)
				|| isEqual(packet, D132_CAR4) || isEqual(packet, D132_CAR5) || isEqual(packet, D132_CAR6)
				|| isEqual(packet, D132_CAR7) || isEqual(packet, D132_CAR8)) {
			return true;
		}
		return false;
	}

	// ---------------------
	// main management proc
	// ---------------------
	/*
	 * // 2003037?>1=$ -> 03037?>1 ->Hex: 0373EF ->Dez: 226287 LCDataIn =
	 * "203037?>1=$"; // 203037?>1=$ Data = LCDataIn.Remove(0, 1); // 03037?>1=$
	 * Data = Data.Remove(8, 2); // 03037?>1
	 * 
	 * LCDataInByte = StringToByteArray(Data);
	 * 
	 * lDate += (fromCU[1] & 15) * 268435456; // 2^28 lDate += (fromCU[0] & 15) *
	 * 16777216; // 2^24 lDate += (fromCU[3] & 15) * 1048576; // 2^20 lDate +=
	 * (fromCU[2] & 15) * 65536; // 2^16 lDate += (fromCU[5] & 15) * 4096; // 2^12
	 * lDate += (fromCU[4] & 15) * 256; // 2^8 lDate += (fromCU[7] & 15) * 16; //
	 * 2^4 lDate += (fromCU[6] & 15); // 2^0
	 * 
	 * Time = 808515358
	 */
	@Override
	protected void parseFrame(byte[] fromCU, int read) {
		// check for errors + init dialog with unit
		try {
			if (this.in.available() == 0) {
				this.out.write(this.queryStatus);
				return;
			}

			if (read > 4) {
				this.monitorHexaFrame(fromCU);
			}

			// --- status packet
			if (isEqual(fromCU, D132_STATUS_PACKET)) {
				CarreraCU.this.started = true;

				// checksum error => jump to the next packet
				if (D132Checksum(fromCU, 2, 8)) {

					// extract fuel car ranges
					int nFuel = 0;
					for (int i = 0; i < 6; i++) {
						nFuel = fromCU[i + 2] & 0x0f;
					}

					logger.debug(String.format("Status: fuel %d %d %d %d %d %d-%c-%c", fromCU[2] & 0x0f,
							fromCU[3] & 0x0f, fromCU[4] & 0x0f, fromCU[5] & 0x0f, fromCU[6] & 0x0f, fromCU[7] & 0x0f,
							(char) fromCU[10], (char) fromCU[11]));
				}

				// --- cross line packet
			} else if (isCrossLine(fromCU)) {
				this.started = true;

				// car ID
				int carId = fromCU[1] - '0';

				// date in ms => use low nibbles
				long lDate = (fromCU[1 + 2] & 0x0f) * 268435456; // 2^28
				lDate += (fromCU[0 + 2] & 0x0f) * 16777216; // 2^24
				lDate += (fromCU[3 + 2] & 0x0f) * 1048576; // 2^20
				lDate += (fromCU[2 + 2] & 0x0f) * 65536; // 2^16
				lDate += (fromCU[5 + 2] & 0x0f) * 4096; // 2^12
				lDate += (fromCU[4 + 2] & 0x0f) * 256; // 2^8
				lDate += (fromCU[7 + 2] & 0x0f) * 16; // 2^4
				lDate += (fromCU[6 + 2] & 0x0f); // 2^0

				// date offset
				if (dateOffset == 0) {
					dateOffset = DateTimeHelper.getSystemTime() - lDate;
					this.firstDetection = lDate;
				}
				lDate += dateOffset;

				// allow structure to forward this event to the sensor
				// assumes it will be released by its parent ;)
				if (carId <= 6 && carId >= 1) {
					SensorPinImpl pin = this.getPin("car.in." + carId);
					pin.setTimeEvent(lDate, true);
					pin.setDetectionID(carId);
					this.notifyPinChanged(pin);
					this.eventLogger.set(String.format("Detection of car: Car ID: %d at %s  (%d ticks)", carId,
							DateTimeHelper.msToChronoHHMMSSmmm(lDate - this.firstDetection), lDate));

				}
				// }

				// --- revision packet
			} else if (isEqual(fromCU, DI32_VERSION)) {
				this.started = true;

				// checksum error => jump to the next packet
				if (D132Checksum(fromCU, 2, 5) == false) {

					logger.debug(String.format("%c%c%c%c", (char) fromCU[1], (char) fromCU[2], (char) fromCU[3],
							(char) fromCU[4]));

				}
			}

			fromCU = null;

			// poll status before leaving
			this.out.write(this.queryStatus);

		} catch (IOException e) {
			logger.error("{}", e);
		}

	}

	@Override
	public void stop() {
		super.stop();
	}

	@Override
	public void reset() {
		super.reset();
		this.dateOffset = 0l;
	}

	// build sensor pin list
	// digital box => use virtual pins correspoding to digital box events
	@Override
	protected void ioPinList() {
		pins.clear();

		for (int i = 0; i < 6; i++) {
			int j = i + 1;
			SensorPinImpl p = new SensorPinImpl(this, "car.in." + j, "Car ID #" + j);
			pins.add(p);
			p.setBounds(124 + i * 26, 276, 20, 41);
		}
	}
}
