package com.github.nradov.sdetofit;

import java.time.Instant;


public class Point {

	private final Instant instant;

	/** Depth in meters. */
	private final float depth;
	
	/** Temperature in degrees Celsius. */
	private final float temperature;

	public Point(final Instant instant, final float depth, final float temperature) {
		this.instant = instant;
		this.depth = depth;
		this.temperature = temperature;
	}

	public Instant getInstant() {
		return instant;
	}

	public float getDepthMeters() {
		return depth;
	}

	public float getTemperatureDegreesCelsius() {
		return temperature;
	}
}
