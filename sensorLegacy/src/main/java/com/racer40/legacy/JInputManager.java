package com.racer40.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

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
	private static final int POLL_MS = 100;

	static final Logger logger = LoggerFactory.getLogger(JInputManager.class);

	private Timer sensorTimer;
	private Map<String, Gamepad> sensorMap = new HashMap<>();

	private JInputManager() {
	}

	/** Holder */
	private static class SingletonHolder {

		private static final JInputManager instance = new JInputManager();
	}

	public static JInputManager getInstance() {
		return SingletonHolder.instance;
	}

	/**
	 * returns a search sensor runnable
	 * 
	 * @param searchCtrl
	 * @param typeToSearch
	 * @return
	 */
	public static void discoverSetup(ObjectProperty<SensorInterface> found, SensorInterface searchedType) {
		Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();

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

	private String buildSetup(Controller ctrl) {
		return ctrl.getName();
	}

	public void start(Gamepad sensor) {
		if (sensorTimer == null) {
			this.sensorMap.clear();
			sensorTimer = new Timer(JInputManager.POLL_MS, (e) -> run());
			sensorTimer.setRepeats(true);
			sensorTimer.start();
		}
		this.sensorMap.put(sensor.getSetup(), sensor);
	}

	public void stop() {
		if (sensorTimer != null) {
			this.sensorMap.clear();
			sensorTimer.stop();
			sensorTimer = null;
		}
	}

	public boolean isRunning() {
		return sensorTimer != null;
	}

	/**
	 * core
	 */
	public void run() {
		Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		if (controllers.length == 0) {
			return;
		}

		for (Controller ctrl : controllers) {
			String setup = this.buildSetup(ctrl);
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

	public List<String> getKeyList(Gamepad sensor) {
		List<String> keys = new ArrayList<>();

		Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();

		for (Controller ctrl : ca) {
			if (this.buildSetup(ctrl).equalsIgnoreCase(sensor.getSetup())) {

				/* Get this controllers components (buttons and axis) */
				Component[] components = ctrl.getComponents();
				logger.debug("Component Count: " + components.length);

				for (int j = 0; j < components.length; j++) {

					if (!components[j].isAnalog()) {
						String key = components[j].getIdentifier().getName() + "," + components[j].getName() + ",in";
						keys.add(key);
						/* Get the components name */
						logger.debug("Component " + j + ": " + components[j].getName());
						logger.debug("    Identifier: " + components[j].getIdentifier().getName());
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
	public boolean getButtonState(Gamepad gamepad, String button) {
		Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
		for (Controller ctrl : ca) {
			if (this.buildSetup(ctrl).equalsIgnoreCase(gamepad.getSetup())) {
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
