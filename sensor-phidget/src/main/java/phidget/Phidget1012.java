package phidget;

import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

public class Phidget1012 extends PhidgetIK {

	public Phidget1012() {
		super(16, 16, 0);
		this.type = SensorConstants.PHIDGET_1012;
		this.name = "Phidget 1012";
		this.managedCars = -1;
		this.pinoutImage = "phidget1012_pinout.png";
		this.image = "phidget1012.jpg";

		this.ioPinList();
	}

	// add display graphic info
	@Override
	protected void ioPinList() {
		super.ioPinList();
		for (int i = 0; i < 16; i++) {
			((SensorPinImpl) this.pins.get(i)).setBounds(50 + i * 30, 60, 20, 20);
			((SensorPinImpl) this.pins.get(i + 16)).setBounds(50 + i * 30, 100, 20, 20);
		}
	}

}
