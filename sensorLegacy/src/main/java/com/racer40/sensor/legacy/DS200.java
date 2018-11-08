package com.racer40.sensor.legacy;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.common.SensorPinImpl;

public class DS200 extends DSxxx {

	public DS200() {
		super();

		this.type = 1040;
		this.name = "DS200";
		this.managedCars = -1;
		this.pinoutImage = "ds200_pinout.png";
		this.image = "DS200.jpg";
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
		for (int i = 1; i <= 2; i++) {
			p = new SensorPinImpl(this, "lane.in." + i, "lane #" + i);
			pins.add(p);
			p.setBounds(137 + (i - 1) * 26, 276, 20, 41);
		}

		p = new SensorPinImpl(this, "go.in.0", "go");
		p.setBounds(216, 276, 20, 41);

		p = new SensorPinImpl(this, "pause.in.0", "pause");
		p.setBounds(242, 276, 20, 41);

		p = new SensorPinImpl(this, "resume.in.0", "resume");
		p.setBounds(268, 276, 20, 41);

		p = new SensorPinImpl(this, "abort.in.0", "abort");
		p.setBounds(294, 276, 20, 41);
	}

}
