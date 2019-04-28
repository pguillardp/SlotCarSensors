package com.racer40.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.SensorInterface;

import javafx.beans.property.ObjectProperty;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

/**
 * class shared between mouse, keyboard and gamepad sensors to detect events
 * http://www.java-gaming.org/topics/getting-started-with-jinput/16866/view.html
 * https://theuzo007.wordpress.com/2012/09/02/joystick-in-java-with-jinput/
 *
 */
public class JInputManager {
	static final Logger logger = LoggerFactory.getLogger(JInputManager.class);

	private static final int POLL_MS = 100;

	private Map<String, Gamepad> sensorMap = null;

	private static Controller[] ca;
	private static JInputManager[] instance = new JInputManager[1];

	public static synchronized JInputManager getInstance() {
		if (instance[0] == null) {
			final Thread parent = Thread.currentThread();

			Thread thread = new Thread(() -> {
				final JInputManager jInputManager = new JInputManager();
				instance[0] = jInputManager;
				instance[0].sensorMap = new HashMap<>();
				ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
				while (parent.isAlive()) {
					try {
						Thread.currentThread().sleep(POLL_MS);
					} catch (InterruptedException e) {
						logger.debug("{}", e);
					}
					instance[0].run();
				}
			});
			thread.setDaemon(true);
			thread.start();
			try {
				while (instance[0] == null) {
					Thread.sleep(POLL_MS);
				}
			} catch (InterruptedException e) {
				logger.debug("{}", e);
			}
		}
		return instance[0];
	}

	/**
	 * returns a search sensor runnable
	 * 
	 * @param searchCtrl
	 * @param typeToSearch
	 * @return
	 */
	public synchronized void discoverSetup(ObjectProperty<SensorInterface> found, SensorInterface searchedType) {

		SensorInterface sensor = null;
		for (Controller ctrl : ca) {

			if (Controller.Type.KEYBOARD.equals(ctrl.getType())) {
				sensor = new KeyboardSensor();

			} else if (Controller.Type.GAMEPAD.equals(ctrl.getType())) {
				sensor = new Gamepad();

			} else if (Controller.Type.STICK.equals(ctrl.getType())) {
				sensor = new Gamepad();

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

	public synchronized void start(Gamepad sensor) {
		String setup = sensor.getSetup().toLowerCase();
		if (!this.sensorMap.containsKey(setup)) {
			this.sensorMap.put(setup, sensor);
		}
	}

	public synchronized void stop(Gamepad sensor) {
		String setup = sensor.getSetup().toLowerCase();
		if (this.sensorMap.containsKey(setup)) {
			this.sensorMap.remove(setup, sensor);
		}
	}

	public synchronized boolean isRunning(Gamepad sensor) {
		String setup = sensor.getSetup().toLowerCase();
		return this.sensorMap.containsKey(setup);
	}

	/**
	 * core
	 */
	public void run() {
		if (ca.length == 0 || this.sensorMap.isEmpty()) {
			return;
		}

		for (Controller ctrl : ca) {
			String setup = ctrl.getName().toLowerCase();
			if (this.sensorMap.containsKey(setup)) {
				ctrl.poll();
				EventQueue queue = ctrl.getEventQueue();
				Event event = new Event();
				while (queue.getNextEvent(event)) {
					Component comp = event.getComponent();
					if (!comp.isAnalog()) {
						this.sensorMap.get(setup).handleJInputEvent(event);
					}
				}
			}
		}
	}

	public synchronized List<String> getKeyList(Gamepad sensor) {
		List<String> keys = new ArrayList<>();

		for (Controller ctrl : ca) {
			String setup = ctrl.getName().toLowerCase();
			if (setup.equalsIgnoreCase(sensor.getSetup())) {

				/* Get this controllers components (buttons and axis) */
				Component[] components = ctrl.getComponents();
				logger.debug("Component Count: " + components.length);

				for (int j = 0; j < components.length; j++) {

					if (!components[j].isAnalog()) {
						String key = components[j].getIdentifier().getName() + "," + components[j].getName() + ",in";
						keys.add(key);
						/* Get the components name */
						logger.debug("Component: {} ", j + ": " + components[j].getName());
						logger.debug("    Identifier: {}", components[j].getIdentifier().getName());
					}
				}
				break;
			}
		}
		return keys;
	}

	/*
	 * get button status
	 */
	public synchronized boolean getButtonState(Gamepad gamepad, String button) {

		for (Controller ctrl : ca) {
			String setup = ctrl.getName().toLowerCase();
			if (setup.equalsIgnoreCase(gamepad.getSetup())) {
				Component[] components = ctrl.getComponents();
				for (int j = 0; j < components.length; j++) {
					if (!components[j].isAnalog() && components[j].getIdentifier().getName().equalsIgnoreCase(button)) {
						return components[j].getPollData() > 0.0f;
					}
				}
				break;
			}
		}
		return false;
	}
}
