package com.racer40.arduino;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

// http://stackoverflow.com/questions/4436733/how-to-write-java-code-that-return-a-line-of-string-into-a-string-variable-from
public class ArduinoMega extends ArduinoUno {

	private final Logger logger = LoggerFactory.getLogger(ArduinoMega.class);

	public final static int[] MEGA_OUT_PINS = new int[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 22, 23, 24, 25, 26,
			27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39 };
	public final static int[] MEGA_IN_PINS = new int[] { 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
			56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69 };

	public ArduinoMega() {
		super();
		this.type = SensorConstants.ARDUINO_MEGA;
		this.name = "Arduino Mega";
		this.managedCars = -1;
		this.pinoutImage = "arduinomega_pinout.png";
		this.image = "arduinomega.jpg";

		this.ioPinList();

		this.bauds = 57600;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;
	}

	@Override
	protected void ioPinList() {
		pins.clear();

		String pinName;
		for (int j = 0; j < MEGA_IN_PINS.length; j++) {
			int i = MEGA_IN_PINS[j];
			String identifier = "digital.in." + i;
			if (i >= 14 && i <= 19) {
				pinName = "-A" + (i - 14);
			} else {
				pinName = i + "";
			}
			SensorPinImpl p = new SensorPinImpl(this, identifier, i + " " + pinName);
			pins.add(p);
			p.setBounds(50 + (j % 10) * 30, 100 + (j / 10) * 30, 20, 20);
		}

		for (int j = 0; j < MEGA_OUT_PINS.length; j++) {
			int i = MEGA_OUT_PINS[j];
			String identifier = "digital.out." + i;
			if (i >= 14 && i <= 19) {
				pinName = "-A" + (i - 14);
			} else {
				pinName = i + "";
			}
			SensorPinImpl p = new SensorPinImpl(this, identifier, i + " " + pinName);
			pins.add(p);
			p.setBounds(50 + (j % 10) * 30, 250 + (j / 10) * 30, 20, 20);
		}
	}
}
