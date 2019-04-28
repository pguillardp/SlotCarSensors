package sensorPhidget;

import java.util.Scanner; //Required for Text Input

import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.DeviceClass;
import com.phidget22.DigitalOutput;
import com.phidget22.ErrorEvent;
import com.phidget22.ErrorListener;
import com.phidget22.Net;
import com.phidget22.Phidget;
import com.phidget22.PhidgetException;
import com.phidget22.ServerType;

public class DigitalOutputExample {

	static Scanner s = new Scanner(System.in);

	public static void main(String[] args) throws Exception {

		/***
		 * Allocate a new Phidget Channel object
		 ***/
		DigitalOutput ch = new DigitalOutput();

		/**
		 * Displays info about the attached Phidget channel. Fired when a Phidget
		 * channel with onAttachHandler registered attaches
		 */
		ch.addAttachListener(new AttachListener() {
			@Override
			public void onAttach(AttachEvent ae) {

				try {
					// If you are unsure how to use more than one Phidget channel with this event,
					// we recommend going to
					// www.phidgets.com/docs/Using_Multiple_Phidgets for information

					System.out.print("\nAttach Event:");

					DigitalOutput ph = (DigitalOutput) ae.getSource();

					/**
					 * Get device information and display it.
					 **/
					int serialNumber = ph.getDeviceSerialNumber();
					String channelClass = ph.getChannelClassName();
					int channel = ph.getChannel();

					DeviceClass deviceClass = ph.getDeviceClass();
					if (deviceClass != DeviceClass.VINT) {
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: "
								+ serialNumber + "\n\t-> Channel:  " + channel + "\n");
					} else {
						int hubPort = ph.getHubPort();
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: "
								+ serialNumber + "\n\t-> Hub Port: " + hubPort + "\n\t-> Channel:  " + channel + "\n");
					}
				} catch (PhidgetException e) {
					PhidgetHelperFunctions.DisplayError(e, "Getting Channel Informaiton");
				}

			}

		});

		/**
		 * Displays info about the detached Phidget channel. Fired when a Phidget
		 * channel with onDetachHandler registered detaches
		 */
		ch.addDetachListener(new DetachListener() {
			@Override
			public void onDetach(DetachEvent de) {
				try {
					// If you are unsure how to use more than one Phidget channel with this event,
					// we recommend going to
					// www.phidgets.com/docs/Using_Multiple_Phidgets for information

					System.out.print("\nAttach Event:");

					Phidget ph = de.getSource();

					/**
					 * Get device information and display it.
					 **/
					int serialNumber = ph.getDeviceSerialNumber();
					String channelClass = ph.getChannelClassName();
					int channel = ph.getChannel();

					DeviceClass deviceClass = ph.getDeviceClass();
					if (deviceClass != DeviceClass.VINT) {
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: "
								+ serialNumber + "\n\t-> Channel:  " + channel + "\n");
					} else {
						int hubPort = ph.getHubPort();
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: "
								+ serialNumber + "\n\t-> Hub Port: " + hubPort + "\n\t-> Channel:  " + channel + "\n");
					}
				} catch (PhidgetException e) {
					PhidgetHelperFunctions.DisplayError(e, "Getting Channel Informaiton");
				}
			}
		});

		/**
		 * Writes Phidget error info to stderr. Fired when a Phidget channel with
		 * onErrorHandler registered encounters an error in the library
		 */
		ch.addErrorListener(new ErrorListener() {
			@Override
			public void onError(ErrorEvent ee) {
				System.out.println("Error: " + ee.getDescription());
			}
		});

		try {

			/***
			 * Set matching parameters to specify which channel to open
			 ***/

			// You may remove these lines and hard-code the addressing parameters to fit
			// your application
			ChannelInfo channelInfo = new ChannelInfo(); // Information from AskForDeviceParameters(). May be removed
															// when hard-coding parameters.
			PhidgetHelperFunctions.AskForDeviceParameters(channelInfo, ch);

			ch.setDeviceSerialNumber(channelInfo.deviceSerialNumber);
			ch.setHubPort(channelInfo.hubPort);
			ch.setIsHubPortDevice(channelInfo.isHubPortDevice);
			ch.setChannel(channelInfo.channel);

			if (channelInfo.netInfo.isRemote) {
				ch.setIsRemote(channelInfo.netInfo.isRemote);
				if (channelInfo.netInfo.serverDiscovery) {
					try {
						Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
					} catch (PhidgetException e) {
						PhidgetHelperFunctions.PrintEnableServerDiscoveryErrorMessage(e);
						throw new Exception("Program Terminated: EnableServerDiscovery Failed", e);
					}
				} else {
					Net.addServer("Server", channelInfo.netInfo.hostname, channelInfo.netInfo.port,
							channelInfo.netInfo.password, 0);
				}
			}

			/***
			 * Open the channel with a timeout
			 ***/
			System.out.println("\nOpening and Waiting for Attachment...");

			try {
				ch.open(5000);
			} catch (PhidgetException e) {
				PhidgetHelperFunctions.PrintOpenErrorMessage(e, ch);
				throw new Exception("Program Terminated: Open Failed", e);
			}

			/***
			 * To find additional functionality not included in this example, be sure to
			 * check the API for your device.
			 ***/

			System.out.print("--------------------\n"
					+ "\n  | The output of a DigitalOutput channel can be controlled by setting its Duty Cycle.\n"
					+ "  | The Duty Cycle can be a number from 0.0 or 1.0\n"
					+ "  | Some devices only accept Duty Cycles of 0 and 1.\n" +

					"\nInput a desired duty cycle from 0.0 or 1.0 and press ENTER\n"
					+ "Input Q and press ENTER to quit\n");

			/*
			 * To find additional functionality not included in this example, be sure to
			 * check the API for your device.
			 */

			boolean end = false;
			while (!end) {

				// Get user input
				String buf = s.nextLine();

				if (buf.length() == 0)
					continue;

				// Process user input
				if (buf.charAt(0) == 'Q' || buf.charAt(0) == 'q') {
					end = true;
					continue;
				}

				double dutyCycle;
				try {
					dutyCycle = Double.parseDouble(buf);
				} catch (NumberFormatException e) {
					System.out.println("Duty Cycle must be between 0.0 and 1.0");
					continue;
				}

				if (dutyCycle > 1.0 || dutyCycle < 0.0) {
					System.out.println("Duty Cycle must be between 0.0 and 1.0");
					continue;
				}

				// Send the value to the device
				ch.setDutyCycle(dutyCycle);
			}

			/***
			 * Perform clean up and exit
			 ***/
			System.out.println("\nDone Sampling...");

			System.out.println("Cleaning up...");
			ch.close();
			System.out.println("\nExiting...");
			return;

		} catch (PhidgetException ex) {
			System.out.println(ex.getDescription());
		}
	}

}
