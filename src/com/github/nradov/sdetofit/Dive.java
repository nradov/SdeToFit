package com.github.nradov.sdetofit;

import java.util.List;

import com.garmin.fit.DateTime;

/**
 * Represents a single dive profile as a series of depths at points in time.
 *
 * @author Nick Radov
 */
public interface Dive extends Comparable<Dive> {

	/**
	 * Get the start time for the dive.
	 *
	 * @return time when the dive started
	 */
	DateTime getStartTime();

	/**
	 * Get the end time for the dive.
	 *
	 * @return time when the dive ended
	 */
	DateTime getEndTime();

	/** Get the dive profile records. */
	List<Record> getRecords();

	/** Get the dive computer model name. */
	String getProductName();

	/** Get the dive computer manufacturer number. Values are defined in {@code Profile.xlsx}. */
	Integer getManufacturer();

	/** Get the dive computer serial number. */
	long getSerialNumber();

	/** Get the dive log number. */
	long getDiveNumber();
	
	/** Get the average dive depth in meters. */
	float getAvgDepth();
	
	/** Get the maximum dive depth in meters. */
	float getMaxDepth();
	
	/** Get the dive run time in seconds. */
	float getBottomTime();
	
	/**
	 * {@inheritDoc}
	 *
	 * @return a negative integer, zero, or a positive integer as the start time of
	 *         this dive is before, equal to, or after the specified dive.
	 * @see #getStartTime()
	 */
	@Override
	int compareTo(Dive o);

}
