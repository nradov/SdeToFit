package com.github.nradov.sdetofit;

import java.util.NavigableSet;

/**
 * Source of zero or more dive profiles. A dive profile is a series of depths at
 * points in time.
 *
 * @author Nick Radov
 */
public interface DivesSource {

	NavigableSet<Dive> getDives();
	
}
