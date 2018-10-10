package com.racer40.sensor.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeHelper {
	private static long systemTime = 0L; // wraps system time in ms when
											// required
	// (junit tests)
	private static boolean useSystemTime = true; // by default, returns system
													// time

	public DateTimeHelper() {
	}

	public static long getSystemTime() {
		if (useSystemTime) {
			return System.currentTimeMillis();
		}
		return systemTime;
	}

	public static void setSystemTime(long systemTime) {
		DateTimeHelper.systemTime = systemTime;
	}

	public static void addSystemTime(long addMilliSec) {
		DateTimeHelper.systemTime += addMilliSec;
	}

	public static boolean isUseSystemTime() {
		return useSystemTime;
	}

	public static void setUseSystemTime(boolean useSystemTime) {
		DateTimeHelper.useSystemTime = useSystemTime;
	}

	/**
	 * seconds to hhmmss
	 * 
	 * @param totalSecs
	 * @return
	 */
	static public String secondsToHHMMSS(int totalSecs) {
		int hours = totalSecs / 3600;
		int minutes = (totalSecs % 3600) / 60;
		int seconds = totalSecs % 60;

		String timeString = "00:00";
		if (hours > 0) {
			timeString = String.format("%d:%02d:%02d", hours, minutes, seconds);
		} else {
			timeString = String.format("%d:%02d", minutes, seconds);
		}
		return timeString;
	}

	static public String secondsToChronoHHMMSS(int totalSecs) {
		int hours = totalSecs / 3600;
		int minutes = (totalSecs % 3600) / 60;
		int seconds = totalSecs % 60;

		String timeString = "00:00";
		if (hours > 0) {
			timeString = String.format("%2d:%02d:%02d", hours, minutes, seconds);
		} else if (minutes > 0) {
			timeString = String.format("%d:%02d", minutes, seconds);
		} else {
			timeString = String.format("00:%02d", seconds);
		}
		return timeString;
	}

	public static String msToChronoHHMMSSmmm(Long timeInMs) {
		int ms = (int) (timeInMs % 1000);
		int sec = (int) (timeInMs / 1000);
		return secondsToChronoHHMMSS(sec) + "." + String.format("%03d", ms);
	}

	public static String dateToString(java.sql.Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy");
		if (date != null) {
			return sdf.format(date);
		}
		return "...";
	}

	/**
	 * converts date time to normalized format
	 * 
	 * @param date
	 * @return
	 */
	public static String dateToString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy");
		if (date != null) {
			return sdf.format(date);
		}
		return "...";
	}

	/**
	 * converts date to noarmalized format
	 * 
	 * @param date
	 * @return
	 */
	public static String dateTimeToString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy - HH:mm:ss");
		if (date != null) {
			return sdf.format(date);
		}
		return "...";
	}

	/**
	 * Convert time string to integer (seconds)
	 * 
	 * @param string
	 * @return time
	 */
	public static int convertTimetoInteger(String string) {
		int time = 0;
		string = string.toLowerCase();
		String[] timeSplit = string.split(":");
		try {
			if (timeSplit.length >= 2) {
				time = Integer.parseInt(timeSplit[0]) * 3600 + Integer.parseInt(timeSplit[1]) * 60
						+ Integer.parseInt(timeSplit[2]);
			} else if (timeSplit.length >= 1) {
				time = Integer.parseInt(timeSplit[0]) * 60 + Integer.parseInt(timeSplit[1]);
			} else {
				time = Integer.parseInt(timeSplit[0]);
			}
		} catch (Exception e) {
			time = 0;
		} finally {
		}
		return time;
	}
}
