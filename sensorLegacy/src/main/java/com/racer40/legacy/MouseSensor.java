package com.racer40.legacy;

import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

public class MouseSensor extends Gamepad {

	public MouseSensor() {
		super();

		this.type = SensorConstants.MOUSE_USB;
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
