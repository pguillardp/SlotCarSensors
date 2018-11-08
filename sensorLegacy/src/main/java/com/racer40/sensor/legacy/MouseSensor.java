package com.racer40.sensor.legacy;

import com.racer40.sensor.common.SensorPinImpl;

public class MouseSensor extends Gamepad {

	public MouseSensor() {
		super();

		this.type = 1050;
		this.name = "USB mouse";
		this.managedCars = -1;
		this.pinoutImage = "mouse_pinout.png";
		this.image = "mouse.jpg";

		this.pins.clear();
		for (int i = 1; i <= 3; i++) {
			pins.add(new SensorPinImpl(this, "digital.in." + i, "Button " + i));
		}
	}

}
