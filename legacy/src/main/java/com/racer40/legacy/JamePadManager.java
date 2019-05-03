package com.racer40.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.SensorInterface;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import javafx.beans.property.ObjectProperty;

/**
 * class shared between mouse, keyboard and gamepad sensors to detect events
 * http://www.java-gaming.org/topics/getting-started-with-jinput/16866/view.html
 * https://theuzo007.wordpress.com/2012/09/02/joystick-in-java-with-jinput/
 *
 */
public class JamePadManager {
	static final Logger logger = LoggerFactory.getLogger(JamePadManager.class);

	private static final int POLL_MS = 50;

	private static Map<String, Gamepad> sensorMap = new HashMap<>();

	private static boolean exitThread = false;

	private static ControllerManager controllers = null;
	private static JamePadManager instance = null;

	public static synchronized JamePadManager getInstance() {
		if (instance == null) {
			instance = new JamePadManager();
			final Thread parent = Thread.currentThread();
			instance.startManager(parent);
		}
		return instance;
	}

	/**
	 * start thread if not running
	 * 
	 * @param parent
	 */
	public static void startManager(Thread parent) {
		if (controllers != null) {
			return;
		}

		Runnable runnable = () -> {
			try {
				exitThread = false;
				controllers = new ControllerManager();
				controllers.initSDLGamepad();

				ControllerState cstate = controllers.getState(0);
				for (int i = 0; i < controllers.getNumControllers(); i++) {
					ControllerState currState = controllers.getState(i);
				}

				controllers.update(); // If using ControllerIndex, you should call update() to check if a new
				// controller

				while (!exitThread && parent.isAlive()) {

					/* Get the available controllers */
					for (int i = 0; i < controllers.getNumControllers(); i++) {
						try {
							// logger.debug("Gamepad: {}", controllers.getControllerIndex(i).getName());
							ControllerState currState = controllers.getState(i);

							if (!currState.isConnected || currState.b) {
								break;
							}
							if (currState.a) {
								System.out.println("\"A\" on \"" + currState.controllerType + "\" is pressed");
							}

							/*
							 * Sleep for POLL_MS milliseconds, in here only so the example doesn't thrash
							 * the system.
							 */
							Thread.sleep(POLL_MS);
						} catch (InterruptedException e) {
							logger.debug("{}", e);
						}
					}
				}

				controllers.quitSDLGamepad();
				exitThread = true;
				controllers = null;

			} catch (Exception e) {
				logger.debug("{}", e);
			}
		};

		Thread thread = new Thread(runnable);
		thread.start();

		// synchronous timed out start
		long timeout = DateTimeHelper.getSystemTime() + 2000;
		while (controllers == null && DateTimeHelper.getSystemTime() < timeout)
			;
		if (DateTimeHelper.getSystemTime() > timeout) {
			exitThread = true;
		}
	}

	public static void stopJInputManagerPoll() {
		long timeout = DateTimeHelper.getSystemTime() + 2000;
		exitThread = true;
		while (DateTimeHelper.getSystemTime() < timeout)
			;
	}

	/**
	 * returns a search sensor runnable
	 * 
	 * @param searchCtrl
	 * @param typeToSearch
	 * @return
	 */
	public synchronized void discoverSetup(ObjectProperty<SensorInterface> found, SensorInterface searchedType) {
		if (controllers == null) {
			return;
		}
		SensorInterface sensor = null;
		for (int i = 0; i < controllers.getNumControllers(); i++) {
			// mngr.
			//
			// if (Controller.Type.KEYBOARD.equals(ctrl.getType())) {
			// sensor = new KeyboardSensor();
			//
			// } else if (Controller.Type.GAMEPAD.equals(ctrl.getType())) {
			// sensor = new Gamepad();
			//
			// } else if (Controller.Type.STICK.equals(ctrl.getType())) {
			// sensor = new Gamepad();
			//
			// } else if (Controller.Type.MOUSE.equals(ctrl.getType())) {
			// sensor = new MouseSensor();
			//
			// } else {
			// continue;
			// }
			// if (sensor.getType() == searchedType.getType()) {
			// sensor.setPort("USB");
			// sensor.setSetup(ctrl.getName());
			// found.set(sensor);
			// searchedType.getEventLogger().set(String.format("%s - name: %s - port: %s -
			// type: %s", sensor.getName(),
			// ctrl.getName(), ctrl.getPortType(), ctrl.getType()));
			// }
		}
	}

	/**
	 * start stop a sensor unsing the underlaying jiput poll
	 * 
	 * @param sensor
	 */
	public synchronized void start(Gamepad sensor) {
		String setup = sensor.getSetup().toLowerCase();
		if (!sensorMap.containsKey(setup)) {
			sensorMap.put(setup, sensor);
		}
	}

	public synchronized void stop(Gamepad sensor) {
		String setup = sensor.getSetup().toLowerCase();
		if (sensorMap.containsKey(setup)) {
			sensorMap.remove(setup, sensor);
		}
	}

	public synchronized boolean isRunning(Gamepad sensor) {
		String setup = sensor.getSetup().toLowerCase();
		return sensorMap.containsKey(setup);
	}

	/**
	 * returns controller button list
	 * 
	 * @param sensor
	 * @return
	 */
	public synchronized List<String> getKeyList(Gamepad sensor) {
		List<String> keys = new ArrayList<>();
		if (controllers != null) {
			// for (Controller ctrl : controllers) {
			// String setup = ctrl.getName().toLowerCase();
			// if (setup.equalsIgnoreCase(sensor.getSetup())) {
			//
			// /* Get this controllers components (buttons and axis) */
			// Component[] components = ctrl.getComponents();
			// logger.debug("Component Count: {}", components.length);
			//
			// for (int j = 0; j < components.length; j++) {
			//
			// if (!components[j].isAnalog()) {
			// String key = components[j].getIdentifier().getName() + "," +
			// components[j].getName()
			// + ",in";
			// keys.add(key);
			// /* Get the components name */
			// logger.debug("Component: {} ", j + ": " + components[j].getName());
			// logger.debug(" Identifier: {}", components[j].getIdentifier().getName());
			// }
			// }
			// break;
			// }
			// }
		}
		return keys;
	}

	/**
	 * get button status
	 */
	public synchronized boolean getButtonState(Gamepad gamepad, String button) {
		// if (controllers != null) {
		// for (Controller ctrl : controllers) {
		// String setup = ctrl.getName().toLowerCase();
		// if (setup.equalsIgnoreCase(gamepad.getSetup())) {
		// Component[] components = ctrl.getComponents();
		// for (int j = 0; j < components.length; j++) {
		// if (!components[j].isAnalog()
		// && components[j].getIdentifier().getName().equalsIgnoreCase(button)) {
		// return components[j].getPollData() > 0.0f;
		// }
		// }
		// break;
		// }
		// }
		// }
		return false;
	}
}
