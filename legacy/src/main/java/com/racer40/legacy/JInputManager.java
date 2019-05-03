package com.racer40.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.SensorInterface;

import javafx.beans.property.ObjectProperty;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

/**
 * class shared between mouse, keyboard and joystick sensors to detect events
 * http://www.java-gaming.org/topics/getting-started-with-jinput/16866/view.html
 * https://theuzo007.wordpress.com/2012/09/02/joystick-in-java-with-jinput/
 *
 */
public class JInputManager {
	static final Logger logger = LoggerFactory.getLogger(JInputManager.class);

	private static final int POLL_MS = 100;

	private static Map<String, Joystick> sensorMap = new HashMap<>();

	private static boolean exitThread = false;

	private static Controller[] controllers = null;
	private static JInputManager instance = null;

	public static synchronized JInputManager getInstance() {
		if (instance == null) {
			instance = new JInputManager();
			final Thread parent = Thread.currentThread();
			instance.startJInputManagerPoll(parent);
		}
		return instance;
	}

	/**
	 * start thread if not running
	 * 
	 * @param parent
	 */
	public static void startJInputManagerPoll(Thread parent) {
		if (controllers != null) {
			return;
		}

		Runnable runnable = () -> {
			try {
				exitThread = false;
				while (!exitThread && parent.isAlive()) {

					/* Get the available controllers */
					controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
					if (controllers.length == 0) {
						continue;
					}

					for (int i = 0; i < controllers.length; i++) {

						// focus only on managed controllers
						String setup = controllers[i].getName().toLowerCase();
						if (!sensorMap.containsKey(setup)) {
							continue;
						}

						/* Remember to poll each one */
						controllers[i].poll();

						/* Get the controllers event queue */
						EventQueue queue = controllers[i].getEventQueue();

						/* Create an event object for the underlying plugin to populate */
						Event event = new Event();

						/* For each object in the queue */
						while (queue.getNextEvent(event)) {
							Component comp = event.getComponent();

							// Check the type of the component and display an appropriate value
							if (comp.getName().startsWith("Button ")) {
								sensorMap.get(setup).handleJInputEvent(event);
							}
						}
					}

					/*
					 * Sleep for POLL_MS milliseconds, in here only so the example doesn't thrash
					 * the system.
					 */
					try {
						Thread.sleep(POLL_MS);
					} catch (InterruptedException e) {
						logger.debug("{}", e);
					}
				}

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
		for (Controller ctrl : this.controllers) {

			if (Controller.Type.KEYBOARD.equals(ctrl.getType())) {
				sensor = new KeyboardSensor();

			} else if (Controller.Type.GAMEPAD.equals(ctrl.getType())) {
				sensor = new Joystick();

			} else if (Controller.Type.STICK.equals(ctrl.getType())) {
				sensor = new Joystick();

			} else if (Controller.Type.MOUSE.equals(ctrl.getType())) {
				sensor = new MouseSensor();

			} else {
				continue;
			}
			if (sensor.getType() == searchedType.getType()) {
				sensor.setPort("USB");
				sensor.setSetup(ctrl.getName());
				found.set(sensor);
				searchedType.getEventLogger().set(String.format("%s - name: %s - port: %s - type: %s", sensor.getName(),
						ctrl.getName(), ctrl.getPortType(), ctrl.getType()));
			}
		}
	}

	/**
	 * start stop a sensor unsing the underlaying jiput poll
	 * 
	 * @param sensor
	 */
	public synchronized void start(Joystick sensor) {
		String setup = sensor.getSetup().toLowerCase();
		if (!sensorMap.containsKey(setup)) {
			sensorMap.put(setup, sensor);
		}
	}

	public synchronized void stop(Joystick sensor) {
		String setup = sensor.getSetup().toLowerCase();
		if (sensorMap.containsKey(setup)) {
			sensorMap.remove(setup, sensor);
		}
	}

	public synchronized boolean isRunning(Joystick sensor) {
		String setup = sensor.getSetup().toLowerCase();
		return sensorMap.containsKey(setup);
	}

	/**
	 * returns controller button list
	 * 
	 * @param sensor
	 * @return
	 */
	public synchronized List<String> getKeyList(Joystick sensor) {
		List<String> keys = new ArrayList<>();
		if (controllers != null) {
			for (Controller ctrl : controllers) {
				String setup = ctrl.getName().toLowerCase();
				if (setup.equalsIgnoreCase(sensor.getSetup())) {

					/* Get this controllers components (buttons and axis) */
					Component[] components = ctrl.getComponents();
					logger.debug("Component Count: {}", components.length);

					for (int j = 0; j < components.length; j++) {

						if (!components[j].isAnalog()) {
							String key = components[j].getIdentifier().getName() + "," + components[j].getName()
									+ ",in";
							keys.add(key);
							/* Get the components name */
							logger.debug("Component: {} ", j + ": " + components[j].getName());
							logger.debug("    Identifier: {}", components[j].getIdentifier().getName());
						}
					}
					break;
				}
			}
		}
		return keys;
	}

	/**
	 * get button status
	 */
	public synchronized boolean getButtonState(Joystick joystick, String button) {
		if (controllers != null) {
			for (Controller ctrl : controllers) {
				String setup = ctrl.getName().toLowerCase();
				if (setup.equalsIgnoreCase(joystick.getSetup())) {
					Component[] components = ctrl.getComponents();
					for (int j = 0; j < components.length; j++) {
						if (!components[j].isAnalog()
								&& components[j].getIdentifier().getName().equalsIgnoreCase(button)) {
							return components[j].getPollData() > 0.0f;
						}
					}
					break;
				}
			}
		}
		return false;
	}
}
