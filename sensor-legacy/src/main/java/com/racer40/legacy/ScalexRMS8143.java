package com.racer40.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.racer40.sensor.DateTimeHelper;
import com.racer40.sensor.Rs232;
import com.racer40.sensor.SensorConstants;
import com.racer40.sensor.SensorPinImpl;

/* protocol description :
 Paramètres de communication port série : 9600,N,8,1, RTS/CTS ou NONE

 Valeurs envoyées du RMS vers le PC :
 HEX      Texte
 Piste 1 :            31          1
 Piste 2 :            32          2 
 Piste 3 :            33          3 
 Piste 4 :            34          4 
 Piste 5 :            35          5 
 Piste 6 :            36          6 
 Bouton Bleu			37          7  

 */
public class ScalexRMS8143 extends Rs232 {
	protected static final int DISCOVERY_TIMEOUT = 20;

	private static final int MAX_STARTPOS = 6;

	private static final String BLUE_BUTTON = "BLUE_BUTTON";

	private final Logger logger = LoggerFactory.getLogger(ScalexRMS8143.class);

	private byte fromRMS[] = new byte[256];

	public ScalexRMS8143() {
		super();

		this.type = SensorConstants.SCALEX_8143;
		this.name = "Scalextric RMS C8143";
		this.managedCars = MAX_STARTPOS;
		this.pinoutImage = "rms8143_pinout.png";
		this.image = "rms8143.jpg";
		

		this.poll = 10;
		this.bauds = 9600;
		databit = 8;
		stopbit = SerialPort.ONE_STOP_BIT;
		parity = SerialPort.NO_PARITY;

		ioPinList();
	}

	@Override
	protected void parseFrame(byte[] fromRMS, int numRead) {
		// it's in the expected range => advise parent window
		byte carByte = fromRMS[0];
		int car = carByte - 0x31 + 1;
		SensorPinImpl pin;

		if (car >= 0) {
			String message;
			long time = DateTimeHelper.getSystemTime();
			if (car < MAX_STARTPOS) {
				pin = this.getPin("car.in." + car);
				pin.setDetectionID(car);
				pin.setTimeEvent(time, false);
				message = String.format("Interrupt RMS : %d - %d", car, time);
			} else {
				pin = this.getPin("car.in.7");
				pin.setTimeEvent(time, false);
				message = String.format("Interrupt RMS : blue button at: %d", time);
			}

			this.notifyPinChanged(pin);
			this.eventLogger.set(message);

			started = true;
		}
	}

	@Override
	protected void ioPinList() {
		pins.clear();

		for (int i = 0; i <= MAX_STARTPOS; i++) {
			SensorPinImpl p;
			if (i < MAX_STARTPOS) {
				p = new SensorPinImpl(this, "car.in." + (i + 1), "Car #" + (i + 1));
				p.setBounds(124 + i * 26, 276, 20, 41);
			} else {
				p = new SensorPinImpl(this, "digital.in.0", "Blue Button");
				p.setBounds(306, 276, 20, 41);
			}
			pins.add(p);
		}
	}
}
