package com.racer40.urcommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.racer40.utils.RandomHelper;

// TODO: test race from lan + gui
public class TestRace {

	@Test
	public void send_200_events_per_8_car_in_test_mode() {

		System.out.println("\n*** 200 events per car - 8 cars test");

		List<List<CarEvent>> events = createEvents(2, 10, 11000, 8000);

		long curtimer = 0;
		URCommand.main(new String[] {"starttest", "", "##" + curtimer});

		// starting sequence
		int nbevents = 0;
//		URCommand.main(new String[] {"go", ""});
//		for (; curtimer < 5000; curtimer += 100) {
//			URCommand.main(new String[] {"clock", "", "##" + curtimer});
//		}

		// offset all events after starting sequence
		for (List<CarEvent> lst : events) {
			for (CarEvent evt : lst) {
				evt.setTimer(evt.getTimer() + curtimer);
			}
		}

		// play all events per startpos
		for (List<CarEvent> nextlst = events.get(0); nextlst != null;) {

			// find next event to play => exit if all events played
			nextlst = null;
			for (List<CarEvent> lst : events) {
				if (!lst.isEmpty()) {
					nextlst = lst;
					break;
				}
			}
			if (nextlst == null) {
				break;
			}

			for (List<CarEvent> lst : events) {
				if (lst.isEmpty() || (lst.get(0).getTimer() > nextlst.get(0).getTimer())) {
					continue;
				}
				nextlst = lst;
			}

			// pop next event
			CarEvent event = nextlst.get(0);
			nbevents++;
			nextlst.remove(0);
			if (curtimer < 0) {
				curtimer = event.getTimer();
			}

			// send clock event from previous event to current one
			while (curtimer < event.getTimer()) {
				URCommand.main(new String[] {"clock", "", "##" + curtimer});
				curtimer += 50;
			}

			curtimer = event.getTimer();
			URCommand.main(new String[] {"slotcar", event.getCarid() + "", "##" + curtimer});
		}

		URCommand.main(new String[] {"stoptest", "", "##" + curtimer});

		System.out.println(nbevents + " events sent");


	}



	private List<List<CarEvent>> createEvents(int nbcar, int nblap, int avg, int delta) {
		List<List<CarEvent>> events = new ArrayList<List<CarEvent>>();

		for (int car = 1; car <= 8; car++) {

			List<CarEvent> carevents = new ArrayList<CarEvent>();
			events.add(carevents);
			long curdate = 0; // DateTimeHelper.getSystemTime();
			carevents.clear();

			for (int i = 0; i < nblap; i++) {

				// reaction time
				long lapDuration;
				if (i == 0) {
					lapDuration = 50 + RandomHelper.rnd(1000);
				} else {
					lapDuration = avg + RandomHelper.rnd(delta) - (delta / 2);
				}
				curdate += lapDuration;

				CarEvent event = new CarEvent(car, curdate);
				carevents.add(event);
			}
		}
		return events;
	}


	public class CarEvent {
		private int carid;
		private long timer;

		public CarEvent(int carid, long timer) {
			this.carid = carid;
			this.timer = timer;
		}

		public int getCarid() {
			return carid;
		}

		public void setCarid(int carid) {
			this.carid = carid;
		}

		public long getTimer() {
			return timer;
		}

		public void setTimer(long timer) {
			this.timer = timer;
		}

	}


}
