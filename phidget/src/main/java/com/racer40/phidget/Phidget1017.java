package com.racer40.phidget;

import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

public class Phidget1017 extends PhidgetIK {

	public Phidget1017() {
		super(0, 8, 0);

		this.type = SensorConstants.PHIDGET_1017;
		this.name = "Phidget 1017";
		this.managedCars = -1;
		this.pinoutImage = "phidget1017_pinout.png";
		this.image = "phidget1017.jpg";
	}

	// add display graphic info
	@Override
	protected void ioPinList() {
		super.ioPinList();
		for (int i = 0; i < 4; i++) {
			SensorPinImpl p = (SensorPinImpl) pins.get(i);
			p.setLocationIngrid(i + 1, 10, i + 1, 20);
		}
	}

}
