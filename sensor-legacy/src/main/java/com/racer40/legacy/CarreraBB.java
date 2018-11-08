package com.racer40.legacy;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.SensorConstants;

public class CarreraBB extends CarreraCU {

	public CarreraBB() {
		super();

		this.type = SensorConstants.CARRERA_DIGITAL_BB;
		this.name = "Carrera Black Box";
		this.managedCars = 6;
		this.image = "carreraBB.bmp";
		this.pinoutImage = "carreraBB_pinout.png";

		this.poll = 10;
		this.bauds = 19200;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;

	}

}
