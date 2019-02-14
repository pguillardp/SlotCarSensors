package com.racer40.test.joystickrmiserver;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.racer40.joystickrmiserver.JoystickRMIServer;
import com.racer40.joystickrmiserver.RMIInterface;

/**
 * Unit test for simple App.
 */
public class TestMain {
	@Test
	public void test_main() throws MalformedURLException, RemoteException, NotBoundException {
		JoystickRMIServer.main(new String[] { "" });
	}

	@Test
	public void get_all_controller_components() throws MalformedURLException, RemoteException, NotBoundException {
		RMIInterface testJoystick;

		testJoystick = (RMIInterface) Naming.lookup(JoystickRMIServer.JOYSTICK_RMI_SERVER);
		List<String> components = new ArrayList<>();
		testJoystick.getComponents(components);
	}
}
