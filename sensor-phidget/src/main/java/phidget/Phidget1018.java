package phidget;

import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

public class Phidget1018 extends PhidgetIK {

	public Phidget1018() {
		super(8, 8, 8);

		this.type = SensorConstants.PHIDGET_1018;
		this.name = "Phidget 1018";
		this.managedCars = -1;
		this.pinoutImage = "phidget1018_pinout.png";
		this.image = "phidget1018.jpg";
	}

	// add display graphic info
	@Override
	protected void ioPinList() {
		super.ioPinList();
		for (int i = 0; i < 8; i++) {
			SensorPinImpl p = (SensorPinImpl) pins.get(i);
			p.setLocationIngrid(i + 1, 10, i + 1, 20);
		}
	}

}
