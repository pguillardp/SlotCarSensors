package com.racer40.sensor.legacy;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.common.SensorPinImpl;

public class DS045 extends DSxxx {

	public DS045() {
		super();

		this.type = 1042;
		this.name = "DS045";
		this.managedCars = -1;
		this.image = "DS045.jpg";
		this.pinoutImage = "ds045_pinout.png";
		this.ioPinList();

		this.poll = 10;
		bauds = 9600;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;
	}

	@Override
	protected void ioPinList() {
		this.pins.clear();

		SensorPinImpl p;
		for (int i = 1; i <= 8; i++) {
			p = new SensorPinImpl(this, "lane.in." + i, "lane #" + i);
			pins.add(p);
			p.setBounds(57 + (i - 1) * 26, 276, 20, 41);
		}

		p = new SensorPinImpl(this, "go.in.0", "go");
		p.setBounds(295, 276, 20, 41);

		p = new SensorPinImpl(this, "pause.in.0", "pause");
		p.setBounds(321, 276, 20, 41);

		p = new SensorPinImpl(this, "resume.in.0", "resume");
		p.setBounds(347, 276, 20, 41);

		p = new SensorPinImpl(this, "abort.in.0", "abort");
		p.setBounds(373, 276, 20, 41);
	}
}
