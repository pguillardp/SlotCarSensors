/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.racer40.joystickrmiserver;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

/**
 * command line to send key/values to UR40. These key/values trigger heat start,
 * stop, pause (...) + slotcar detection events The arguments are dictionary
 * keys
 * 
 */
public class JoystickRMIServer extends UnicastRemoteObject implements RMIInterface {

	public static final String JOYSTICK_RMI_SERVER = "//localhost/JoystickRMIServer";
	private static final long serialVersionUID = -6686283144071959434L;
	private Controller[] ca = null;

	protected JoystickRMIServer() throws RemoteException {
		super();
	}

	/**
	 * returns component list. Format:<br>
	 * controller.type.component name
	 */
	@Override
	public void getComponents(List<String> componentList) throws RemoteException {
		componentList.clear();

		this.ca = ControllerEnvironment.getDefaultEnvironment().getControllers();

		for (int i = 0; i < ca.length; i++) {

			/* Get the name of the controller */
			System.out.println(ca[i].getName());
			System.out.println("Type: " + ca[i].getType().toString());

			/* Get this controllers components (buttons and axis) */
			Component[] components = ca[i].getComponents();
			System.out.println("Component Count: " + components.length);
			for (int j = 0; j < components.length; j++) {

				/* Get the components name */
				System.out.println("Component " + j + ": " + components[j].getName());
				System.out.println("Identifier: " + components[j].getIdentifier().getName());
				System.out.println("ComponentType: " + ca[i].getType());

				if (Controller.Type.KEYBOARD.equals(ca[i].getType()) || Controller.Type.GAMEPAD.equals(ca[i].getType())
						|| Controller.Type.STICK.equals(ca[i].getType())
						|| Controller.Type.MOUSE.equals(ca[i].getType())) {
					final String button = ca[i].getName() + "." + ca[i].getType() + "." + components[j].getName();
					componentList.add(button);
					System.out.println(button);

				} else {
					continue;
				}

				if (components[j].isRelative()) {
					System.out.println("Relative");
				} else {
					System.out.println("Absolute");
				}
				if (components[j].isAnalog()) {
					System.out.println(" Analog");
				} else {
					System.out.println(" Digital");
				}
			}
		}
	}

	@Override
	public void getEvents(List<String> events) throws RemoteException {
		events.clear();
		if (ca != null) {
			for (Controller ctrl : ca) {
				ctrl.poll();
				EventQueue queue = ctrl.getEventQueue();
				Event event = new Event();
				while (queue.getNextEvent(event)) {
					Component comp = event.getComponent();
					if (!comp.isAnalog()) {
						String evt = ctrl.getName() + "." + ctrl.getType() + "." + comp.getName() + "."
								+ (comp.getPollData() == 1f ? "1" : "0") + "." + (long) (event.getNanos() / 1000f);
						events.add(evt);
					}
				}
			}
		}
	}

	@Override
	public void closeRMIServer() throws RemoteException {
		System.exit(0);
	}

	public static void main(String[] args) {
		try {
			Naming.rebind(JoystickRMIServer.JOYSTICK_RMI_SERVER, new JoystickRMIServer());
			System.err.println("JoystickRMIServer ready");

		} catch (Exception e) {
			System.err.println("JoystickRMIServer exception: " + e.toString());
			e.printStackTrace();

		}

	}

}
