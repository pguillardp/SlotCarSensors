package com.racer40.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.racer40.sensor.Rs232;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

public class Scalex7042 extends Rs232 {
	private final Logger logger = LoggerFactory.getLogger(Scalex7042.class);

	public static final String GREEN_LED = "green LED";

	public static final String RED_LED = "red LED";

	public static final String LIGHT_PIN = "light 7042";

	private static final int MAX_CAR = 6;

	private static final int IN_DETECTED = 8;

	private static final int IN_CHECKSUM = 14;

	private static final int OUT_CHECKSUM = 8;

	private static final int OUT_LED = 7;

	private static final int OUT_MODE = 0;

	static final int C7042_BUFFER = 2048;

	/*
	 * 1st Handset+Track Status 2nd Handset #1 3rd Handset #2 4th Handset #3 5th
	 * Handset #4 6th Handset #5 7th Handset #6 8th Aux port current 9th CarID /
	 * Track # updated 10th Game or SF-line time(LSB) 11th Game or SF-line time 12th
	 * Game or SF-line time 13th Game or SF-line time(MSB) 14th extra 15th Checksum
	 */
	private byte[] from7042 = new byte[15];

	/*
	 * 1st Operation Mode 2nd Drive Packet #1 3rd Drive Packet #2 4th Drive Packet
	 * #3 5th Drive Packet #4 6th Drive Packet #5 7th Drive Packet #6 8th LED status
	 * 9th Checksum
	 */
	private byte[] to7042 = new byte[9];

	/*
	 * car speed in %, from 0 to 100
	 */
	private float carSpeed[] = new float[MAX_CAR];

	// checksum
	static final int CRC8_LOOK_UP_TABLE[] = new int[] { 0x00, 0x07, 0x0e, 0x09, 0x1c, 0x1b, 0x12, 0x15, 0x38, 0x3f,
			0x36, 0x31, 0x24, 0x23, 0x2a, 0x2d, 0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65, 0x48, 0x4F, 0x46, 0x41,
			0x54, 0x53, 0x5A, 0x5D, 0xE0, 0xE7, 0xEE, 0xE9, 0xFC, 0xFB, 0xF2, 0xF5, 0xD8, 0xDF, 0xD6, 0xD1, 0xC4, 0xC3,
			0xCA, 0xCD, 0x90, 0x97, 0x9E, 0x99, 0x8C, 0x8B, 0x82, 0x85, 0xA8, 0xAF, 0xA6, 0xA1, 0xB4, 0xB3, 0xBA, 0xBD,
			0xC7, 0xC0, 0xC9, 0xCE, 0xDB, 0xDC, 0xD5, 0xD2, 0xFF, 0xF8, 0xF1, 0xF6, 0xE3, 0xE4, 0xED, 0xEA, 0xB7, 0xB0,
			0xB9, 0xBE, 0xAB, 0xAC, 0xA5, 0xA2, 0x8F, 0x88, 0x81, 0x86, 0x93, 0x94, 0x9D, 0x9A, 0x27, 0x20, 0x29, 0x2E,
			0x3B, 0x3C, 0x35, 0x32, 0x1F, 0x18, 0x11, 0x16, 0x03, 0x04, 0x0D, 0x0A, 0x57, 0x50, 0x59, 0x5E, 0x4B, 0x4C,
			0x45, 0x42, 0x6F, 0x68, 0x61, 0x66, 0x73, 0x74, 0x7D, 0x7A, 0x89, 0x8E, 0x87, 0x80, 0x95, 0x92, 0x9B, 0x9C,
			0xB1, 0xB6, 0xBF, 0xB8, 0xAD, 0xAA, 0xA3, 0xA4, 0xF9, 0xFE, 0xF7, 0xF0, 0xE5, 0xE2, 0xEB, 0xEC, 0xC1, 0xC6,
			0xCF, 0xC8, 0xDD, 0xDA, 0xD3, 0xD4, 0x69, 0x6E, 0x67, 0x60, 0x75, 0x72, 0x7B, 0x7C, 0x51, 0x56, 0x5F, 0x58,
			0x4D, 0x4A, 0x43, 0x44, 0x19, 0x1E, 0x17, 0x10, 0x05, 0x02, 0x0B, 0x0C, 0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A,
			0x33, 0x34, 0x4E, 0x49, 0x40, 0x47, 0x52, 0x55, 0x5C, 0x5B, 0x76, 0x71, 0x78, 0x7F, 0x6A, 0x6D, 0x64, 0x63,
			0x3E, 0x39, 0x30, 0x37, 0x22, 0x25, 0x2C, 0x2B, 0x06, 0x01, 0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13, 0xAE, 0xA9,
			0xA0, 0xA7, 0xB2, 0xB5, 0xBC, 0xBB, 0x96, 0x91, 0x98, 0x9F, 0x8A, 0x8D, 0x84, 0x83, 0xDE, 0xD9, 0xD0, 0xD7,
			0xC2, 0xC5, 0xCC, 0xCB, 0xE6, 0xE1, 0xE8, 0xEF, 0xFA, 0xFD, 0xF4, 0xF3 };

	protected static final int DISCOVERY_TIMEOUT = 20;

	private static final String Scalex7042LIGHT = "STATUS_LIGHT";

	private int ledStatus;

	public Scalex7042() {
		super();

		this.type = SensorConstants.SCALEX_DIG_7042;
		this.name = "Scalextric C7042";
		this.managedCars = 6;
		this.pinoutImage = "c7042_pinout.png";
		this.image = "C7042.jpg";
		this.ioPinList();
		this.digital = true;

		this.bauds = 19200;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;

	}

	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public boolean start() {
		startPoll = true;
		for (int i = 0; i < MAX_CAR; i++) {
			carSpeed[i] = 1f;
		}
		return super.start();
	}

	@Override
	public void stop() {
		super.stop();
	}

	/**
	 * CORE
	 */

	@Override
	protected void handleSerialEvent(SerialPortEvent event) {
		byte[] from7042 = event.getReceivedData();
		int read = from7042.length;

		// read serial port entry

		if (read < from7042.length) {
			pollNext(0x7F);
			return;
		}

		this.monitorHexaFrame(from7042);

		// checksum control
		int ckSum = checksum(from7042, 13);
		if (ckSum != (from7042[IN_CHECKSUM] & 0xff)) {
			pollNext(0xFF);
			return;
		}

		this.started = true;

		// complement middle bytes
		for (int i = 0; i < Scalex7042.MAX_CAR; i++) {
			to7042[i] = from7042[i + 1];
			from7042[i + 1] ^= 0xFF;
		}

		// extract fields

		// car detected?
		int car = from7042[IN_DETECTED] & 0x07;
		if (car >= 1 && car <= Scalex7042.MAX_CAR) {
			SensorPinImpl pin = this.getPin("car.in." + car);
			pin.setDetectionID(car);
			long timer = (((long) (from7042[12] & 0xff)) << 24) + (((long) (from7042[11] & 0xff)) << 16)
					+ (((long) (from7042[10] & 0xff)) << 8) + (from7042[9] & 0xff);
			timer = (long) (timer * 0.0064f);
			pin.setTimeEvent(timer, true);
			long nHour = (timer / 3600000);
			long nMinute = (timer / 60000) % 60;
			long nSecond = (timer / 1000) % 60;
			long lMilli = timer % 1000;
			String info = String.format("Detection of car: Car ID: %d at %02d:%02d.%03d (%d)", car, nMinute, nSecond,
					lMilli, timer);
			this.eventLogger.set(info);
			this.carDetected = true;
			this.notifyPinChanged(pin);
		}

		// poll next packet
		pollNext(0xFF);
	}

	/**
	 * request another data packet
	 * 
	 * @param mode
	 */
	private void pollNext(int mode) {
		to7042[OUT_MODE] = (byte) mode;
		to7042[OUT_LED] = (byte) ledStatus;
		to7042[OUT_CHECKSUM] = (byte) checksum(to7042, 7);
		try {
			this.writeToSerial(to7042);
		} catch (Exception e) {
			logger.error("{}", e);
		}
	}

	static int checksum(byte[] dataInOut, int nSize) {
		int crc8;
		int i;

		// Routine for the CRC
		i = dataInOut[0] & 0xff;
		crc8 = CRC8_LOOK_UP_TABLE[i]; // first byte

		// for incoming packet, data length is 9 byte, the routine should be
		// should be looped for 7 times
		// for outgoing packet, data length is 14 byte, the routine should be
		// should be looped for 14 times
		for (i = 1; i <= nSize; i++) // loop for 7 times for incoming packet
		{
			crc8 = CRC8_LOOK_UP_TABLE[crc8 ^ (dataInOut[i] & 0xff)];
		}

		return crc8;
	}

	// manage LED status
	private void onLED(int bLED) {
		ledStatus |= bLED;
	}

	private void offLED(int j) {
		ledStatus &= 0xFF ^ j;
	}

	private boolean getLED(int j) {
		return ((ledStatus & j) != 0) ? true : false;
	}

	private void setPin(int nLED, int nValue) {
		if (nValue != 0)
			onLED(0x01 << nLED);
		else
			offLED(0x01 << nLED);
	}

	private boolean getPin(int nLED /* nPinNumber */) {
		return getLED(0x01 << nLED);
	}

	public void setRed(boolean bOn) {
		if (bOn)
			onLED(0x40);
		else
			offLED(0x40);
	}

	public boolean getRed() {
		return getLED((byte) 0x40);
	}

	public void setGreen(boolean bOn) {
		if (bOn)
			onLED(0x80);
		else
			offLED(0x80);
	}

	public boolean getGreen() {
		return getLED((byte) 0x80);
	}

	private void stopGameTimer() {
		ledStatus = 0xC0;
	}

	private void startGameTimer() {
		ledStatus = 0x80 | (ledStatus & 0x3F);
	}

	// manage cars and lights
	@Override
	public boolean setOutputPinValue(String pin, int value) {
		if ("greenled.out.0".equals(pin)) {
			this.setGreen(value > 0);
			return true;
		}

		if ("redled.out.0".equals(pin)) {
			this.setRed(value > 0);
			return true;
		}

		boolean found = false;
		for (int i = 1; i <= MAX_CAR; i++) {
			if (("carlight.out." + i).equals(pin)) {
				found = true;
				if (value > 0) {
					this.onLED(i - 1);
				} else {
					this.offLED(i - 1);
				}
			}
		}
		if (found) {
			return true;
		}

		// if (pin.startsWith(SensorConstants.DIGITAL_SPEED_PIN)) {
		// String s = pin.replace("_", "").replace(SensorConstants.DIGITAL_SPEED_PIN,
		// "");
		// int car = Integer.parseInt(s);
		// if (car >= 0 && car < MAX_CAR) {
		// carSpeed[car] = value / (float) SensorConstants.PIN_ON;
		// if (carSpeed[car] < 0) {
		// carSpeed[car] = 0;
		// }
		// if (carSpeed[car] > 1) {
		// carSpeed[car] = 1;
		// }
		// }
		// return true;
		// }

		return false;
	}

	@Override
	protected void ioPinList() {
		SensorPinImpl pin;
		for (int i = 1; i <= MAX_CAR; i++) {
			pin = new SensorPinImpl(this, "car.in." + i, "car #" + i);
			pin.setBounds(191 + (i - 1) * 16, 82, 10, 10);
			this.pins.add(pin);
		}

		for (int i = 1; i <= MAX_CAR; i++) {
			pin = new SensorPinImpl(this, "carlight.out." + i, "light #" + i);
			pin.setBounds(191 + (i - 1) * 16, 100, 10, 10);
			this.pins.add(pin);
		}

		pin = new SensorPinImpl(this, "redled.out.0" + MAX_CAR + 1, Scalex7042.RED_LED);
		pin.setBounds(377, 91, 10, 10);
		this.pins.add(pin);

		pin = new SensorPinImpl(this, "greenled.out.0" + MAX_CAR + 2, Scalex7042.GREEN_LED);
		pin.setBounds(403, 91, 10, 10);
		this.pins.add(pin);
	}

	@Override
	public void discover(long timeout) {
		// TODO Auto-generated method stub

	}

}
