

/**
 * This class is intended for use alongside PhidgetHelperFunctions.java to store
 * the information collected by those functions for later use. If you are not
 * using PhidgetHelperFunctions.java in your project, it is likely you do not
 * need this class in your project either.
 */

public class ChannelInfo {

	public class NetInfo {
		public boolean isRemote;
		public boolean serverDiscovery;
		public String hostname;
		public int port;
		public String password;

		public NetInfo() {
			isRemote = false;
			serverDiscovery = false;
			hostname = "";
			port = 0;
			password = "";
		}
	};

	public int deviceSerialNumber;
	public int hubPort;
	public int channel;
	public boolean isHubPortDevice;
	boolean isVINT;
	public NetInfo netInfo;

	public ChannelInfo() {
		deviceSerialNumber = 0;
		hubPort = -1;
		channel = -1;
		isHubPortDevice = false;
		isVINT = false;
		netInfo = new NetInfo();
	}
};