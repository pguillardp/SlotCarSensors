package sensorPhidget;

import org.junit.Test;

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

public class TestPhidget {

	public TestPhidget() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void phidget_jinput_conflict() {
		// new NativeLibrary("jinput") //
		// .register(Platform.Windows_x86, "jinput-dx8.dll") //
		// .register(Platform.Windows_x86, "jinput-raw.dll") //
		// .register(Platform.Windows_x64, "jinput-dx8_64.dll") //
		// .register(Platform.Windows_x64, "jinput-raw_64.dll") //
		// .register(Platform.Linux_x86, "libjinput-linux.so") //
		// .register(Platform.Linux_x64, "libjinput-linux64.so") //
		// .register(Platform.MacOS, "libjinput-osx.jnilib") //
		// .require(true) //
		// .deleteOnExit(true)//
		// .extractTo(new File("target", "jinput-natives")) //
		// .load(Loaders.JAVA_LIBRARY_PATH__LOADER);
	}

	@Test
	public void open_single_1012_output_channel() throws PhidgetException {
		DigitalOutput ch = new DigitalOutput();
		String javapath = System.getProperty("java.library.path");
		System.out.println(javapath);

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

			ch.setDeviceSerialNumber(80870);
			ch.setHubPort(-1);
			ch.setIsHubPortDevice(false);
			ch.setChannel(14);

			if (channelInfo.netInfo.isRemote) {
				ch.setIsRemote(channelInfo.netInfo.isRemote);
				if (channelInfo.netInfo.serverDiscovery) {
					try {
						Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
					} catch (PhidgetException e) {
						PhidgetHelperFunctions.PrintEnableServerDiscoveryErrorMessage(e);
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
			}

			double dutyCycle = 1.0;

			// Send the value to the device
			ch.setDutyCycle(dutyCycle);

			Thread.sleep(500);

			/***
			 * Perform clean up and exit
			 ***/
			System.out.println("\nDone Sampling...");

			System.out.println("Cleaning up...");
			ch.close();
			System.out.println("\nExiting...");
			return;

		} catch (PhidgetException | InterruptedException ex) {
			System.out.println(ex.getMessage());
		}
	}

}
