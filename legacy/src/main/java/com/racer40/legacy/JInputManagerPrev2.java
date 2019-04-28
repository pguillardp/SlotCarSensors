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

/**
 * class shared between mouse, keyboard and gamepad sensors to detect events
 * http://www.java-gaming.org/topics/getting-started-with-jinput/16866/view.html
 * https://theuzo007.wordpress.com/2012/09/02/joystick-in-java-with-jinput/
 *
 */
public class JInputManagerPrev2 {
	static final Logger logger = LoggerFactory.getLogger(JInputManagerPrev2.class);

	private static final int POLL_MS = 100;

	private static JInputManagerPrev2 instance = null;
	private String lock = "";

	private JInputWrapper wrapper = null;
	private static boolean polling = false;

	/**
	 * instance managed as thread
	 */
	private JInputManagerPrev2() {
		this.wrapper = new JInputWrapper(Thread.currentThread());
		Thread t = new Thread(this.wrapper);
		t.setDaemon(true);
		t.start();
		// while (!polling)
		// ;
	}

	public static JInputManagerPrev2 getInstance() {
		if (instance == null) {
			instance = new JInputManagerPrev2();
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
		wrapper.found = found;
		wrapper.searchedType = searchedType;
		wrapper.request = JInputWrapper.GETBUTTONS;
	}

	public void start(Gamepad sensor) {
		JInputManagerPrev2.getInstance().wrapper.setPolling(sensor, true);
	}

	public void stop(Gamepad sensor) {
		JInputManagerPrev2.getInstance().wrapper.setPolling(sensor, false);
	}

	public boolean isRunning(Gamepad sensor) {
		return JInputManagerPrev2.getInstance().wrapper.isPolling(sensor);
	}

	public void getKeyList(Gamepad sensor, List<String> keys) {
		wrapper.sensor = sensor;
		wrapper.keys = keys;
		wrapper.request = JInputWrapper.GETBUTTONS;
	}

	public boolean getButtonState(Gamepad sensor, String button) {
		wrapper.sensor = sensor;
		wrapper.button = button;
		wrapper.request = JInputWrapper.BUTTONSTATE;
		return wrapper.buttonState;
	}

	/**
	 * jinput runnable wrapper to load jnis on the runnable thread
	 * 
	 * @author Pierrick
	 *
	 */
	private class JInputWrapper implements Runnable {
		private boolean buttonState;
		private static final String DISCOVER = "discover";
		private static final String BUTTONSTATE = "buttonstate";
		private static final String GETBUTTONS = "getbuttons";
		private Map<String, SensorInterface> sensorMap = new HashMap<>();
		private Thread parentthread;
		private SensorInterface sensor;
		private String button;
		private List<String> keys;
		private SensorInterface searchedType;
		private ObjectProperty<SensorInterface> found;
		private String request = "";

		private JInputWrapper(Thread thread) {
			parentthread = thread;
		}

		public boolean isPolling(SensorInterface sensor) {
			return sensorMap.values().contains(sensor);
		}

		public void setPolling(SensorInterface sensor, boolean poll) {
			if (poll) {
				this.sensorMap.put(sensor.getSetup().toLowerCase(), sensor);
			} else {
				this.sensorMap.remove(sensor.getSetup().toLowerCase());
			}
		}

		@Override
		public void run() {
			Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
			// ControllerEnvironment.getDefaultEnvironment();
			// while (this.parentthread.isAlive()) {
			// Controller[] controllers =
			// ControllerEnvironment.getDefaultEnvironment().getControllers();
			//
			// synchronized (lock) {
			// switch (request) {
			// case JInputWrapper.DISCOVER:
			// break;
			// case JInputWrapper.GETBUTTONS:
			// getSensorKeys(ControllerEnvironment.getDefaultEnvironment().getControllers());
			// break;
			// case JInputWrapper.BUTTONSTATE:
			// getButtonState(ControllerEnvironment.getDefaultEnvironment().getControllers());
			// break;
			// default:
			// break;
			// }
			// }
			//
			// // timer
			// if (!polling) {
			// polling = true;
			// }
			// try {
			// Thread.sleep(POLL_MS);
			// } catch (InterruptedException e) {
			// logger.debug("{}", e);
			// }
			//
			// // go aheat if no sensor nor controller to poll
			// if (this.sensorMap.isEmpty() || controllers.length == 0) {
			// continue;
			// }
			//
			// // poll controllers to detect changes
			// for (Controller ctrl : controllers) {
			// String setup = ctrl.getName().toLowerCase();
			// if (this.sensorMap.containsKey(setup)) {
			// ctrl.poll();
			//
			// EventQueue queue = ctrl.getEventQueue();
			//
			// Event event = new Event();
			//
			// while (queue.getNextEvent(event)) {
			// Component comp = event.getComponent();
			// if (!comp.isAnalog()) {
			// ((Gamepad) this.sensorMap.get(setup)).handleJInputEvent(event);
			// }
			// }
			// }
			// }
			// }
		}

		private void getSensorKeys(Controller[] ca) {
			for (Controller ctrl : ca) {
				if (ctrl.getName().equalsIgnoreCase(sensor.getSetup())) {

					/* Get this controllers components (buttons and axis) */
					Component[] components = ctrl.getComponents();
					logger.debug("Component Count: {}", components.length);

					for (int j = 0; j < components.length; j++) {

						if (!components[j].isAnalog()) {
							String key = components[j].getIdentifier().getName() + "," + components[j].getName()
									+ ",in";
							keys.add(key);
							/* Get the components name */
							logger.debug("Component {}", j + ": " + components[j].getName());
							logger.debug("    Identifier: {}", components[j].getIdentifier().getName());
						}
					}
					break;
				}
			}
		}

		private boolean getButtonState(Controller[] ca) {
			for (Controller ctrl : ca) {
				if (ctrl.getName().equalsIgnoreCase(sensor.getSetup())) {
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

		private void discoverSetup(Controller[] ca) {

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
					searchedType.getEventLogger().set(String.format("%s - name: %s - port: %s - type: %s",
							sensor.getName(), ctrl.getName(), ctrl.getPortType(), ctrl.getType()));
				}
			}
		}
	}
}
