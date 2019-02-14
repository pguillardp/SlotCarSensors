package com.racer40.corerms.fsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * all internal event types managed by the rmsfsm.
 */
public enum RMSEventType {

	// DETECTION
	// slotcar detected
	EVT_DETECT_SLOTCAR(1500, true, "chrono.png", "onslotcar", getMaxStartingPos(), RMSEventType.DETECTION_GRP,
			"Detection", 1, true),

	// detects interlap event for interlap car times (not managed)
	EVT_INTERLAP(1900, true, "chrono.png", "oninterlap", getMaxStartingPos(), RMSEventType.OTHER_GRP, "Interlap", 50,
			true),

	// FUELLING
	// Control car power supply
	EVT_CTR_SUPPLY(1550, false, "supply.png", "", getMaxStartingPos(), RMSEventType.SUPPLY_GRP, "Power control", 5,
			true),

	// pit in
	EVT_PITIN(9250, true, "refuel.png", "onpitin", getMaxStartingPos(), RMSEventType.FUELPIT_GRP, "Pit in", 10, true),

	// pit out
	EVT_PITOUT(9300, true, "refuel.png", "onpitout", getMaxStartingPos(), RMSEventType.FUELPIT_GRP, "Pit out", 15,
			true),

	// out of fuel car light
	EVT_OUTFUEL(1700, false, "refuel.png", "", getMaxStartingPos(), RMSEventType.FUELPIT_GRP, "Out of fuel light", 20,
			true),

	// START LIGHTS
	// starting red lights- 5 light manager
	EVT_RED_START_LIGHT(4000, false, "light.png", "", 5, RMSEventType.RACELIGHT_GRP, "Start light #", 1, false),

	// RACE FLAGS
	// flag man the end of a heat
	EVT_CHECKERED_FLAG(4014, false, "scenery.png", "", 1, RMSEventType.RACEFLAG_GRP, "Checkered flag", 1, false),

	// one lap before end of heat
	EVT_WHITE_FLAG(4016, false, "scenery.png", "", 1, RMSEventType.RACEFLAG_GRP, "White flag", 4, false),

	// trackcall (== pause/resume)
	EVT_RED_FLAG(4018, false, "scenery.png", "", 1, RMSEventType.RACEFLAG_GRP, "Red flag", 2, false),

	// car trackcall
	EVT_YELLOW_FLAG(4020, false, "scenery.png", "", 1, RMSEventType.RACEFLAG_GRP, "Yellow flag", 5, false),

	// heat starts after red light sequence
	EVT_GREEN_FLAG(4021, false, "scenery.png", "", 1, RMSEventType.RACEFLAG_GRP, "Green flag", 3, false),

	// RACE STATUS LIGHTS
	// race leader light
	EVT_LEADRACE(1600, false, "chrono.png", "", getMaxStartingPos(), RMSEventType.RACELIGHT_GRP, "Leader light", 20,
			true),

	// car heat over car light
	EVT_CAROVER(1650, false, "scenery.png", "", getMaxStartingPos(), RMSEventType.RACELIGHT_GRP, "Heat over", 25, true),

	// best lap car light
	EVT_BESTLAP(1750, false, "light.png", "", getMaxStartingPos(), RMSEventType.RACELIGHT_GRP, "Best lap", 30, true),

	// best speed car light
	EVT_BESTSPEED(1800, false, "light.png", "", getMaxStartingPos(), RMSEventType.RACELIGHT_GRP, "Best speed", 35,
			true),

	// false start car light
	EVT_FALSE_START(1850, false, "light.png", "", getMaxStartingPos(), RMSEventType.RACELIGHT_GRP, "False start", 40,
			true),

	// COMMANDS
	// start heat
	EVT_GO(9203, true, "chrono.png", "ongo", 1, RMSEventType.COMMAND_GRP, "Start heat", 1, false),

	// cancel heat
	EVT_STOP(9202, true, "chrono.png", "onstop", 1, RMSEventType.COMMAND_GRP, "Abort heat", 3, false),

	// go/cancel race event
	EVT_GOSTOP(4015, true, "chrono.png", "ongostop", 1, RMSEventType.COMMAND_GRP, "Start/cancel heat", 5, false),

	// cancel heat
	EVT_CANCEL(9207, true, "chrono.png", "oncancel", 1, RMSEventType.COMMAND_GRP, "Cancel heat", 7, false),

	// end heat normally (!= abort)
	EVT_ENDHEAT(9206, true, "chrono.png", "onend", 1, RMSEventType.COMMAND_GRP, "End heat normally", 9, false),

	// request to start pause
	EVT_PAUSE(9204, true, "chrono.png", "onpause", 1, RMSEventType.COMMAND_GRP, "Pause", 10, false),

	// request to end pause
	EVT_RESUME(9205, true, "chrono.png", "onresume", 1, RMSEventType.COMMAND_GRP, "Resume", 15, false),

	// pause/resume event
	EVT_TRACKCALL(4013, true, "chrono.png", "ontrackcall", 1, RMSEventType.COMMAND_GRP, "Track call", 20, false),

	// car track call
	EVT_SLOTCAR_TRACK_CALL(2000, true, "chrono.png", "oncartrackcall", getMaxStartingPos(), RMSEventType.COMMAND_GRP,
			"Car track call", 22, true), //

	// request to start next heat
	EVT_NEXT(9208, true, "chrono.png", "onnext", 1, RMSEventType.COMMAND_GRP, "Next heat", 35, false),

	// car deslot
	EVT_DESLOT(1950, true, "ambulance.png", "ondeslot", getMaxStartingPos(), RMSEventType.COMMAND_GRP, "Deslot", 60,
			true),

	// car reslot
	EVT_RESLOT(2050, true, "ambulance.png", "onreslot", getMaxStartingPos(), RMSEventType.COMMAND_GRP, "Reslot", 65,
			true),

	// OTHER
	// scenery input - disabled
	EVT_SCENERY_INPUT(5000, true, "scenery.png", "onscenery", 0, RMSEventType.OTHER_GRP, "Scenery", 50, false),

	// scenery output - disabled
	EVT_SCENERY_OUTPUT(5500, false, "scenery.png", "", 0, RMSEventType.OTHER_GRP, "Scenery", 55, false),

	// Scalextric RMS blue button
	EVT_BLUE_BTN(9210, true, "chrono.png", "", 1, RMSEventType.OTHER_GRP, "Blue button", 45, false),

	// internal clock
	EVT_CLOCK(4017, false, "chrono.png", "onclock", 1, RMSEventType.OTHER_GRP, "", 95, false),

	// internal use commands
	EVT_STARTTEST(8886, false, "scenery.png", "onstarttest", 1, RMSEventType.OTHER_GRP, "", 90, false),

	EVT_STOPTEST(8887, false, "scenery.png", "onstoptest", 1, RMSEventType.OTHER_GRP, "", 90, false),

	// command sent to update all observers (internal use)
	EVT_UPDATE(8888, false, "scenery.png", "onupdate", 1, RMSEventType.OTHER_GRP, "", 90, false),

	UNDEFINED(-1, false, "", "", 0, RMSEventType.OTHER_GRP, "", 100, false);

	private static final Logger logger = LoggerFactory.getLogger(RMSEventType.class);

	private static Map<String, RMSEventType> command2type = new HashMap<>();

	private static final int MAX_STARTING_POS = 32;

	public static final String IN = "in";
	public static final String OUT = "out";

	// group the events belong to
	public static final String DETECTION_GRP = "detection";
	public static final String FUELPIT_GRP = "refueling";
	public static final String SUPPLY_GRP = "supply";
	public static final String COMMAND_GRP = "command";
	public static final String RACELIGHT_GRP = "racelight";
	public static final String RACEFLAG_GRP = "raceflag";
	public static final String OTHER_GRP = "other";

	// attributes
	private int id;
	private boolean input;
	private String picture;
	private String group;
	private String rmsCommand;
	private int range;
	private int priority = 100; // from 1 (highest priority) to 100 (lowest)
	private boolean carEvent = false;
	private String fullName;

	// constructor
	private RMSEventType(int id, boolean input, String picture, String rmsCommand, int range, String group,
			String fullName, int priority, boolean carEvent) {
		this.id = id;
		this.input = input;
		this.picture = picture;
		this.rmsCommand = rmsCommand;
		this.range = range;
		this.group = group;
		this.fullName = fullName;
		this.priority = priority;
		this.carEvent = carEvent;
	}

	private static int getMaxStartingPos() {
		return MAX_STARTING_POS;
	}

	public int getRange() {
		return this.range;
	}

	public int getRange(int managedCars) {
		if (carEvent && range > 1) {
			return managedCars;
		}
		return range;
	}

	public boolean isInput() {
		return input;
	}

	public boolean isOutput() {
		return !input;
	}

	public String getIO() {
		return input ? IN : OUT;
	}

	public String getPicture() {
		return picture;
	}

	public String getRmsCommand() {
		return this.rmsCommand;
	}

	public static String groupIdToGroupName(String groupid) {
		switch (groupid) {
		case DETECTION_GRP:
			return "Car detection";
		case FUELPIT_GRP:
			return "Fuel management";
		case SUPPLY_GRP:
			return "Supply control";
		case COMMAND_GRP:
			return "Commands";
		case RACELIGHT_GRP:
			return "Lightning";
		case RACEFLAG_GRP:
			return "Flags";
		}
		return "Miscellaneous";
	}

	public int getPriority() {
		return priority;
	}

	public int getFirstId() {
		return this.id;
	}

	public int getLastId() {
		return this.id + this.range - 1;
	}

	/*
	 * converts a rms command into rmseventtype
	 */
	protected static void fillCommandToTypeMap() {
		if (RMSEventType.command2type.isEmpty()) {
			for (RMSEventType t : RMSEventType.values()) {
				if (!"".equals(t.rmsCommand)) {
					RMSEventType.command2type.put(t.rmsCommand.toLowerCase(), t);
				}
			}
		}
	}

	public static RMSEventType commandToType(String command) {
		fillCommandToTypeMap();
		if (RMSEventType.command2type.containsKey(command.toLowerCase())) {
			return RMSEventType.command2type.get(command.toLowerCase());
		}
		return RMSEventType.UNDEFINED;
	}

	public static RMSEventType fromId(int typeValue) {
		for (RMSEventType t : RMSEventType.values()) {
			if (typeValue >= t.getFirstId() && typeValue < (t.getFirstId() + t.getRange())) {
				return t;
			}
		}
		return UNDEFINED;
	}

	public static int getTypeIndex(int typeValue) {
		for (RMSEventType t : RMSEventType.values()) {
			if (typeValue >= t.getFirstId() && typeValue < (t.getFirstId() + t.getRange())) {
				return typeValue - t.getFirstId();
			}
		}
		return -1;
	}

	public static List<String> getCommandKeywords() {
		fillCommandToTypeMap();
		return new ArrayList<String>(command2type.keySet());
	}

	/*
	 * returns a group of feature events
	 */
	public static List<RMSEventType> getFeatureGroupEventList(String group) {
		List<RMSEventType> eventList = new ArrayList<RMSEventType>();
		for (RMSEventType t : RMSEventType.values()) {
			if (t.group.equals(group)) {
				eventList.add(t);
			}
		}
		// Sorting
		Collections.sort(eventList, new Comparator<RMSEventType>() {
			@Override
			public int compare(RMSEventType type1, RMSEventType type2) {
				if (type1.priority > type2.priority) {
					return 1;
				} else if (type1.priority < type2.priority) {
					return -1;
				}
				return 0;
			}
		});
		return eventList;
	}

	/**
	 * returns list of car events having a range > 1
	 * 
	 * @return
	 */
	public static List<RMSEventType> getMultipleCarEventList() {
		List<RMSEventType> eventList = new ArrayList<RMSEventType>();
		for (RMSEventType t : RMSEventType.values()) {
			if (t.getRange() > 1 && t.isCarEvent()) {
				eventList.add(t);
			}
		}
		return eventList;
	}

	/**
	 * build filter for sql queries
	 */
	public static String getSQLFilter() {
		StringBuffer eventFilter = new StringBuffer();
		eventFilter.append("(");
		for (RMSEventType t : RMSEventType.values()) {
			for (int i = 0; i < t.getRange(); i++) {
				eventFilter.append(t.getFirstId() + i);
				eventFilter.append(",");
			}
		}
		String filter = eventFilter.toString();
		filter = filter.substring(0, filter.length() - 1) + ")";
		return filter;
	}

	public static RMSEventType fromName(String name) {
		for (RMSEventType t : RMSEventType.values()) {
			if (t.name().equalsIgnoreCase(name)) {
				return t;
			}
		}
		return RMSEventType.UNDEFINED;
	}

	public String getFullName() {
		if (this != RMSEventType.UNDEFINED) {
			return this.fullName;
		}
		return "";
	}

	/*
	 * returns a set of all expected rms event ids when nbcars are managed, based on
	 * ranges
	 */
	public static Set<Integer> getRMSEventMap(int nbcars) {
		Set<Integer> types = new HashSet<Integer>();
		for (RMSEventType t : RMSEventType.values()) {
			int range = t.getRange(nbcars);
			if (range == 1) {
				Integer id = new Integer(t.id);
				if (!types.contains(id)) {
					types.add(id);
				} else {
					logger.debug("Duplicated event type: " + id);
				}
			} else if (range > 1) {
				for (int i = 0; i < range; i++) {
					types.add(new Integer(t.id + i));
				}
			}
		}
		return types;
	}

	/**
	 * true if car event
	 * 
	 * @return
	 */
	public boolean isCarEvent() {
		return carEvent;
	}

}
