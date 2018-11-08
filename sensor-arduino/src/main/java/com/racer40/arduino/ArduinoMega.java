package com.racer40.arduino;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

// http://stackoverflow.com/questions/4436733/how-to-write-java-code-that-return-a-line-of-string-into-a-string-variable-from
public class ArduinoMega extends ArduinoUno {

	private final Logger logger = LoggerFactory.getLogger(ArduinoMega.class);

	public ArduinoMega() {
		super();
		this.type = SensorConstants.ARDUINO_MEGA;
		this.name = "Arduino Mega";
		this.managedCars = -1;
		this.pinoutImage = "arduinomega_pinout.png";
		this.image = "arduinomega.jpg";

		this.ioPinList();

		this.poll = 1500;
		this.bauds = 19200;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;
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
