package com.racer40.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.racer40.sensor.Rs232;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

/**
 * org/projects/fslapcounter/browser/tags/release-0.64
 * Classes/SerialLapCounters.rbbas
 * 
 * @author Pierrick
 *
 */
public class Trackmate extends Rs232 {
	private final Logger logger = LoggerFactory.getLogger(Trackmate.class);

	private static final byte START_BYTE = (byte) 0xE0;
	private static final byte END_BYTE = (byte) 0xEB;
	private static final byte TOTAL_BYTES = 21;

	private static final int MAX_DETECTED_CARS = 8;

	private byte dataWord[] = new byte[TOTAL_BYTES], olddataWord[] = new byte[TOTAL_BYTES];
	private int byteCount, i, checksum;
	private boolean sync = false;
	private byte intc;

	private boolean newdata;
	private byte[] fromTM = new byte[TOTAL_BYTES];
	byte buffer[] = null;

	// DSxxx/UR30 timer offset
	private long timeOffset[] = new long[MAX_DETECTED_CARS];

	private int dsComOffset = 0; // used to manage several ds per computer.
	// Offset added to lane number

	private boolean processing;

	public Trackmate() {
		super();

		this.type = SensorConstants.TRACKMATE;
		this.name = "Trackmate";
		this.managedCars = 8;
		this.pinoutImage = "trackmate_pinout.png";
		this.image = "trackmate.jpg";

		this.ioPinList();

		bauds = 9600;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;
	}

	public int getDsComOffset() {
		return dsComOffset;
	}

	public void setDsComOffset(int dsComOffset) {
		this.dsComOffset = dsComOffset;
	}

	@Override
	public void reset() {
		for (int i = 0; i < MAX_DETECTED_CARS; i++) {
			this.timeOffset[i] = 0l;
		}
	}

	@Override
	public boolean start() {
		for (int i = 0; i < MAX_DETECTED_CARS; i++) {
			this.timeOffset[i] = 0l;
		}
		return super.start();
	}

	@Override
	public void stop() {
		super.stop();
	}

	@Override
	public void run() {
	}

	@Override
	protected void handleSerialEvent(SerialPortEvent event) {
		byte[] fromTM = event.getReceivedData();
		int read = fromTM.length;

		buffer = (buffer != null) ? ArrayUtils.addAll(buffer, fromTM) : fromTM;

		processBuffer(read);

		// check to see if there is data still to process
		while (buffer.length >= 15) {
			this.processBuffer(read);
		}

	}

	protected void processBuffer(int read) {
		// For the Trackmate hardware there are two known response packets
		// To start the lap counter timer you must send the following command
		// 01 3F 2C 32 30 32 2C 30 2C 31 31 2C 0D 0A
		// Each packet ends with a Control Line Feed, 0D 0A

		// Two types of packets are sent from the lap counter, one is a time
		// packet sent periodically and the other is a transponder packet
		// All packets start with 01 and end with 0D 0A

		// The time packet will have byte 2 set to 23, the # character

		// The transponder packet with have byte 2 set to 40, the @ character
		// The fields in the transponder packet are seperated by 09, the Tab
		// character
		// Field 1 is the header, 01 40
		// Field 2 is unknown
		// Field 3 is the transponder number
		// Field 4 is the time stamp, convert to a number and multiply by 1000
		// to get the time in milliseconds
		// Field 5 is the number of hits the transponder had
		int flagStart = -1;
		int flagEnd = -1;
		int uids;
		byte[] subbuf;
		List<byte[]> splitbuf;
		int splitCount;
		int tempUID;
		double tempTime;

		this.monitorHexaFrame(fromTM);

		// Make sure we are not already analyzing data
		if (processing == false) {

			processing = true;

			// Check to see if we have enough data in the buffer to process
			if (read < 4) {

				// Not enough data to care
				processing = false;
				return;
			}

			// Check to see if we have a valid packet
			flagStart = Arrays.binarySearch(buffer, (byte) 0x01);
			flagEnd = Arrays.binarySearch(buffer, (byte) 0x0A);

			// If 0 is returned) then we do not have a beginning of a packet,
			// data can be discarded
			if (flagStart == 0) {

				// no good data,
				// flush it all
				buffer = null;
				processing = false;
				return;

			} else if (flagEnd == 0) {

				// we do not have a complete packet return and wait
				processing = false;
				return;
			}

			// Make sure we do not have an end of a packet stuck in the buffer,
			// if so discard that data and start over
			if (flagEnd < flagStart) {

				// oops we have bad data before flagstart and need to discard
				// that data check to see if we have another end
				buffer = Arrays.copyOfRange(buffer, buffer.length - flagStart + 1, buffer.length - 1);

				flagEnd = Arrays.binarySearch(buffer, (byte) 0x0A);

				if (flagEnd == 0) {

					// Not enough data to continue yet
					processing = false;
					return;
				}
			}

			// make sure we have a transponder packet
			// If not get rid of the packet
			if (buffer[flagStart] != ((byte) '@') && flagEnd < buffer.length) {

				// We must have some other packet not a transponder packet
				// Discard this data and continue one
				buffer = Arrays.copyOfRange(buffer, buffer.length - flagEnd, buffer.length - 1);
				processing = false;
				return;

			} else if ((buffer[flagStart] != ((byte) '@')) && flagEnd == (buffer.length - 1)) {
				buffer = null;
				processing = false;
				return;
			}

			// take the substring to work with
			// subbuf = MidB(buffer, flagStart, flagEnd - flagStart + 1);
			subbuf = Arrays.copyOfRange(buffer, flagStart, flagEnd - flagStart + 1);

			// Split it out
			splitbuf = splitByteBuffer(subbuf, (byte) 0x09);

			splitCount = splitbuf.size();

			// Check to see that we have enough elements in array, if so then
			// use the transponder id, otherwise discard the data
			if (splitCount > 4) {
				uids = splitbuf.get(3)[0];
				// Time in packet is in seconds, we need to send it in
				// microseconds
				String time = splitbuf.get(4).toString();
				tempTime = Double.parseDouble(time) * 1000000.0;

			} else {
				// Not a valid transponder packet we need to discard it
				if (flagEnd == buffer.length - 1) {
					buffer = null;
					processing = false;
					return;
				} else {
					buffer = Arrays.copyOfRange(buffer, buffer.length, buffer.length - flagEnd);
					processing = false;
					return;
				}

			}

			// Clear out the processed data
			if (flagEnd < buffer.length - 1) {
				// buffer = RightB(buffer, buffer.length - flagEnd);
				buffer = Arrays.copyOfRange(buffer, buffer.length - flagEnd, buffer.length - 1);
			} else {
				buffer = null;
			}

			if (uids >= 1 && uids <= MAX_DETECTED_CARS) {
				SensorPinImpl pin = this.getPin("car.in." + uids);
				pin.setDetectionID(uids);
				pin.setTimeEvent((long) (tempTime / 1000), true);
				this.notifyPinChanged(pin);
				this.eventLogger.set(String.format("Car %d detectedt at %d", uids, (long) (tempTime / 1000)));
				started = true;
			}

		} else {
			// We are getting data but no command was issued, discard data
			buffer = null;

		}

		// done processing set to false
		processing = false;
	}

	/*
	 * split an array into sub arrays
	 */
	private List<byte[]> splitByteBuffer(byte[] subbuf, byte separator) {
		List<byte[]> split = new ArrayList<>();
		int first = 0, last = subbuf.length - 1;
		int i = Arrays.binarySearch(subbuf, separator);
		while (i > 0) {
			split.add(Arrays.copyOfRange(subbuf, first, i - 1));
			first = i + 1;
			i = Arrays.binarySearch(subbuf, first, last, separator);
		}
		split.add(Arrays.copyOfRange(subbuf, first, last));
		return split;
	}

	@Override
	public boolean setOutputPinValue(String pin, int value) {
		// NA: box
		return false;
	}

	@Override
	protected void ioPinList() {
		pins.clear();

		for (int i = 1; i <= MAX_DETECTED_CARS; i++) {
			SensorPinImpl p = new SensorPinImpl(this, "car.in." + i, "Car # " + i);
			pins.add(p);

			p.setLocationIngrid(1, 10 + i, i + 1, 11 + i);
		}

	}

	@Override
	public void discover(long timeout) {
		// TODO Auto-generated method stub

	}

}
