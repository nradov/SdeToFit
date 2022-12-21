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

	List<Record> getRecords();

	String getProductName();

	Integer getManufacturer();

	long getSerialNumber();

	long getDiveNumber();
	
	float getAvgDepth();
	
	float getMaxDepth();
	
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
