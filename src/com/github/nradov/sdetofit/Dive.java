package com.github.nradov.sdetofit;

import java.time.Instant;
import java.util.List;

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
	Instant getStart();

	/**
	 * Get the end time for the dive.
	 *
	 * @return time when the dive ended
	 */
	Instant getEnd();

	List<Record> getRecords();
	
	String getProductName();
	
	Integer getManufacturer();
	
	long getSerialNumber();
	
	/**
	 * {@inheritDoc}
	 *
	 * @return a negative integer, zero, or a positive integer as the start time
	 *         of this dive is before, equal to, or after the specified dive.
	 * @see #getStart()
	 */
	@Override
	int compareTo(Dive o);

}
