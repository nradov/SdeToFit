package com.github.nradov.sdetofit;

import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;

/**
 * A single data point in the dive profile.
 * 
 * @author Nick Radov
 */
public final class Record {

	private final DateTime timeStamp;

	/** Depth in meters. */
	private final float depth;

	/** Temperature in degrees Celsius. */
	private final byte temperature;

	public Record(final DateTime timeStamp, final float depth, final byte temperature) {
		this.timeStamp = new DateTime(timeStamp);
		this.depth = depth;
		this.temperature = temperature;
	}

	public DateTime getTimeStamp() {
		return timeStamp;
	}

	public float getDepthMeters() {
		return depth;
	}

	public byte getTemperatureDegreesCelsius() {
		return temperature;
	}

	public RecordMesg toMesg() {
		final var recordMesg = new RecordMesg();
		recordMesg.setTimestamp(timeStamp);
		recordMesg.setDepth(depth);
		recordMesg.setTemperature(temperature);
		return recordMesg;
	}
}
