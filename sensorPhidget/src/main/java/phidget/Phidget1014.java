package phidget;

import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

public class Phidget1014 extends PhidgetIK {

	public Phidget1014() {
		super(0, 4, 0);
		this.type = SensorConstants.PHIDGET_1014;
		this.name = "Phidget 1014";
		this.managedCars = -1;
		this.pinoutImage = "phidget1014_pinout.png";
		this.image = "phidget1014.jpg";

		this.ioPinList();
	}

	// add display graphic info
	@Override
	protected void ioPinList() {
		super.ioPinList();
		for (int i = 0; i < 4; i++) {
			SensorPinImpl p = (SensorPinImpl) pins.get(i);
			p.setLocationIngrid(i + 1, 1, i + 1, 2);
		}
	}

}
