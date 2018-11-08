package com.racer40.legacy;

import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

public class KeyboardSensor extends Gamepad {

	public KeyboardSensor() {
		super();
		this.type = SensorConstants.KEYBOARD_USB;
		this.name = "USB keyboard";
		this.managedCars = -1;
		this.pinoutImage = "keyboard.jpg";
		this.image = "keyboard.jpg";

		this.pins.clear();
		for (int i = 1; i <= 12; i++) {
			pins.add(new SensorPinImpl(this, "digital.in." + i, "Button " + i));
		}
	}

}
