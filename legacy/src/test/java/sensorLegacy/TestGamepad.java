package sensorLegacy;

import org.junit.Test;

import com.github.strikerx3.jxinput.XInputButtons;
import com.github.strikerx3.jxinput.XInputCapabilities;
import com.github.strikerx3.jxinput.XInputComponents;
import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.XInputDevice14;
import com.github.strikerx3.jxinput.XInputLibraryVersion;
import com.github.strikerx3.jxinput.enums.XInputButton;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.github.strikerx3.jxinput.listener.SimpleXInputDeviceListener;
import com.github.strikerx3.jxinput.listener.XInputDeviceListener;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import com.studiohartman.jamepad.ControllerUnpluggedException;

public class TestGamepad {

	public TestGamepad() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void test_jamepad() {
		ControllerManager controllers = new ControllerManager();
		controllers.initSDLGamepad();

		controllers.update(); // If using ControllerIndex, you should call update() to check if a new
		// controller
		// was plugged in or unplugged at this index.

		while (true) {
			for (int i = 0; i < 4; i++) {
				ControllerState state = controllers.getState(i);
				if (!state.isConnected) {
					continue;
				}
				ControllerIndex currController = controllers.getControllerIndex(i);

				try {
					if (currController.isButtonPressed(ControllerButton.A)) {
						System.out.println("\"A\" on \"" + currController.getName() + "\" is pressed");
					}
					if (currController.isButtonPressed(ControllerButton.B)) {
						break;
					}
				} catch (ControllerUnpluggedException e) {
					break;
				}
			}
		}
	}

	@Test
	public void test_jxinput() {
		// Check if XInput 1.3 is available
		if (XInputDevice.isAvailable()) {
			System.out.println(" XInput 1.3 is available on this platform");
		}

		// Check if XInput 1.4 is available
		if (XInputDevice14.isAvailable()) {
			System.out.println(" XInput 1.4 is available on this platform");
		}

		XInputLibraryVersion libVersion = XInputDevice.getLibraryVersion();

		// Retrieve all devices
		XInputDevice14.setEnabled(true);
		try {
			XInputDevice14[] devices = XInputDevice14.getAllDevices();

			// Retrieve the device for player 1
			for (int i = 0; i < devices.length; i++) {
				XInputDevice14 device = XInputDevice14.getDeviceFor(i); // or devices[0]

				XInputComponents components = device.getComponents();
				XInputButtons buttons = components.getButtons();

				XInputCapabilities caps = device.getGamepadCapabilities();

				// The SimpleXInputDeviceListener allows us to implement only the methods we
				// actually need
				XInputDeviceListener listener = new SimpleXInputDeviceListener() {
					@Override
					public void connected() {
						// Resume the game
					}

					@Override
					public void disconnected() {
						// Pause the game and display a message
					}

					@Override
					public void buttonChanged(final XInputButton button, final boolean pressed) {
						System.out.println("button changed: " + button.name());
					}
				};
				device.addListener(listener);

				// Whenever the device is polled, listener events will be fired as long as there
				// are changes
				XInputDevice.getDeviceFor(i).poll();

			}

			while (true) {
				try {
					for (int i = 0; i < devices.length; i++) {
						XInputDevice.getDeviceFor(i).poll();
					}
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (XInputNotLoadedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
