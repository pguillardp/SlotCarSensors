package com.racer40.legacy;

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
public class JInputManagerPrev implements Runnable {
	static final Logger logger = LoggerFactory.getLogger(JInputManagerPrev.class);

	private static final int POLL_MS = 100;

	private static JInputManagerPrev instance = null;
	private boolean polling = false;
	private Map<String, Gamepad> sensorMap = new HashMap<>();
	private Thread thread;

	/**
	 * instance managed as thread
	 */
	private JInputManagerPrev() {
	}

	public static JInputManagerPrev getInstance() {
		if (instance == null) {
			instance = new JInputManagerPrev();
			instance.thread = Thread.currentThread();

			new Thread(instance).start();
			try {
				Thread.currentThread().sleep(2 * POLL_MS);
			} catch (InterruptedException e) {
				logger.debug("{}", e);
			}
		}
		return instance;
	}

	/**
	 * returns a search sensor runnable
	 * 
	 * @param searchCtrl
	 * @param typeToSearch
	 * @return
	 */
	public void discoverSetup(ObjectProperty<SensorInterface> found, SensorInterface searchedType) {
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
		if (!polling) {
			this.sensorMap.clear();
			polling = true;
		}
		this.sensorMap.put(sensor.getSetup(), sensor);
	}

	public void stop() {
		if (polling) {
			polling = false;
		}
	}

	public boolean isRunning() {
		return polling;
	}

	/**
	 * core
	 */

	@Override
	public void run() {
		while (true && thread.isAlive()) {
			try {
				Thread.sleep(POLL_MS);
			} catch (InterruptedException e) {
				logger.debug("{}", e);
			}
			if (!polling) {
				continue;
			}
			Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
			if (controllers.length == 0) {
				continue;
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
	}

	public void getKeyList(Gamepad sensor, List<String> keys) {

		synchronized (keys) {

			Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();

			for (Controller ctrl : ca) {
				if (this.buildSetup(ctrl).equalsIgnoreCase(sensor.getSetup())) {

					/* Get this controllers components (buttons and axis) */
					Component[] components = ctrl.getComponents();
					logger.debug("Component Count: " + components.length);

					for (int j = 0; j < components.length; j++) {

						if (!components[j].isAnalog()) {
							String key = components[j].getIdentifier().getName() + "," + components[j].getName()
									+ ",in";
							keys.add(key);
							/* Get the components name */
							logger.debug("Component " + j + ": " + components[j].getName());
							logger.debug("    Identifier: " + components[j].getIdentifier().getName());
						}
					}

					break;
				}
			}
		}
	}

	/*
	 * get button status
	 */
	public boolean getButtonState(Gamepad gamepad, String button) {
		synchronized (button) {
			Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
			for (Controller ctrl : ca) {
				if (this.buildSetup(ctrl).equalsIgnoreCase(gamepad.getSetup())) {
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
			return false;
		}
	}
}
