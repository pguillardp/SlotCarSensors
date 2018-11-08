package com.racer40.legacy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorImpl;
import com.racer40.sensor.SensorPinImpl;

import net.java.games.input.Event;

// http://www.hytherion.com/beattidp/comput/pport.htm
public class Gamepad extends SensorImpl {
	final Logger logger = LoggerFactory.getLogger(Gamepad.class);

	public Gamepad() {
		super();
		this.type = SensorConstants.JOYSTICK_USB;
		this.name = "USB joystick";
		this.managedCars = -1;
		this.pinoutImage = "gamepad_pinout.png";
		this.image = "gamepad.jpg";

		this.ioPinList();
	}

	public void setPoll(int pollms) {
		JInputManager.getInstance().setPoll(pollms);
	}

	@Override
	public boolean start() {
		super.start();
		JInputManager.getInstance().start(this);
		return true;
	}

	protected void closePort() {
		JInputManager.getInstance().stop();
	}

	@Override
	public boolean isStarted() {
		return JInputManager.getInstance().isRunning();
	}

	@Override
	public void reset() {

	}

	/*
	 * event received from JInputManager
	 */
	protected void handleJInputEvent(Event event) {
		String identifier = event.getComponent().getIdentifier().getName();
		SensorPinImpl pin = this.getPin("digital.in." + (Integer.parseInt(identifier) + 1));
		if (pin != null) {
			pin.setPinValueForNotification(event.getValue() > 0 ? SensorConstants.PIN_ON : SensorConstants.PIN_OFF,
					event.getNanos() / 1000000, false, true);
		}
	}

	@Override
	public void stop() {
		JInputManager.getInstance().stop();
	}

	@Override
	public boolean setOutputPinValue(String pin, int value) {
		return false;
	}

	@Override
	public void run() {
	}

	@Override
	public int getPinValue(String pinIdentifier) {
		int index = Integer.parseInt(pinIdentifier.split("\\.")[2]) - 1;
		return JInputManager.getInstance().getButtonState(this, index + "") ? SensorConstants.PIN_ON
				: SensorConstants.PIN_OFF;
	}

	@Override
	public void discover(long timeout) {
		JInputManager.discoverSetup(this.discoveredInterface, this);
	}

	@Override
	protected void ioPinList() {
		List<String> querypins = JInputManager.getInstance().getKeyList(this);
		SensorPinImpl p;
		if (!querypins.isEmpty()) {
			this.pins.clear();
			int size = querypins.size();
			int size2 = size / 2;
			for (int i = 0; i < size; i++) {
				String btn[] = querypins.get(i).split(",");
				p = new SensorPinImpl(this, "digital.in." + (Integer.parseInt(btn[0]) + 1), btn[1]);
				if (i < size2) {
					p.setBounds(283, 85 + 32 * i, 41, 20);

				} else {
					p.setBounds(337, 85 + 32 * (i - size2), 41, 20);
				}
				pins.add(p);
			}

		} else {
			this.pins.clear();
			for (int i = 1; i <= 12; i++) {
				p = new SensorPinImpl(this, "digital.in." + i + "", "Button " + i);
				if (i <= 6) {
					p.setBounds(283, 85 + 32 * (i - 1), 41, 20);

				} else {
					p.setBounds(337, 85 + 32 * (i - 7), 41, 20);
				}
				pins.add(p);
			}
		}

	}

}
