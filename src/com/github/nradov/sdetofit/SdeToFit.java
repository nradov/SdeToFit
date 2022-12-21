package com.github.nradov.sdetofit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DeveloperDataIdMesg;
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DiveSummaryMesg;
import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventType;
import com.garmin.fit.File;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.LapMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

/**
 * Main class for converting Suunto Dive Export (.sde) files to Garmin Flexible
 * and Interoperable (.fit) files.
 * 
 * @author Nick Radov
 *
 */
public final class SdeToFit {

	/** This number is taken from {@code Profile.xlsx}. */
	private static final int MESG_NUM_SESSION = 18;

	private final Path input, output;

	/**
	 * Offset between the FIT Epoch and Unix Epoch.
	 * 
	 * @see <a href="https://developer.garmin.com/fit/cookbook/datetime/">Working
	 *      with Dates</a>
	 */
	public static final long OFFSET_MS = 631065600000L;

	public SdeToFit(final Path input, final Path output) {
		this.input = input;
		this.output = output;
	}

	public SdeToFit(final String input, final String output) {
		this(Paths.get(input), Paths.get(output));
	}

	public static void main(final String[] args)
			throws ZipException, IOException, ParserConfigurationException, SAXException {
		if (args.length != 2) {
			throw new IllegalArgumentException("Usage: input output");
		}

		final var converter = new SdeToFit(args[0], args[1]);
		converter.convert();
	}

	public void convert() throws ZipException, IOException, ParserConfigurationException, SAXException {
		final DivesSource divesSource = DivesSourceFactory.create(input);
		for (final Dive dive : divesSource.getDives()) {
			createDiveFitFile(dive);
		}
	}

	/**
	 * File extension for the Garmin Flexible and Interoperabile Data Transfer (FIT)
	 * format.
	 */
	private static final String FIT_FILE_EXTENSION = ".fit";

	private static final String PATTERN = "yyyy-MM-dd-HH-mm-ss";

	// It is a BEST PRACTICE to reuse the same Guid for all FIT files created by
	// your platform
	// TODO: make a new GUID
	private static final byte[] APPLICATION_ID = new byte[] { 0x1, 0x1, 0x2, 0x3, 0x5, 0x8, 0xD, 0x15, 0x22, 0x37, 0x59,
			(byte) 0x90, (byte) 0xE9, 0x79, 0x62, (byte) 0xDB };

	private void createDiveFitFile(final Dive dive) {
		final var sdf = new SimpleDateFormat(PATTERN);
		final String filename = output.toString() + java.io.File.separator + sdf.format(dive.getStartTime().getDate())
				+ FIT_FILE_EXTENSION;

		final var messages = new ArrayList<Mesg>();

		// The combination of file type, manufacturer id, product id, and serial number
		// should be unique.

		// Every FIT file MUST contain a File ID message
		final var fileIdMesg = new FileIdMesg();
		fileIdMesg.setType(File.ACTIVITY);
		fileIdMesg.setManufacturer(Manufacturer.SUUNTO);
		fileIdMesg.setTimeCreated(dive.getStartTime());
		fileIdMesg.setSerialNumber(dive.getSerialNumber());
		messages.add(fileIdMesg);

		// A Device Info message is a BEST PRACTICE for FIT ACTIVITY files
		final var deviceInfoMesg = new DeviceInfoMesg();
		deviceInfoMesg.setDeviceIndex(DeviceIndex.CREATOR);
		deviceInfoMesg.setManufacturer(dive.getManufacturer());
		deviceInfoMesg.setProductName(dive.getProductName()); // Max 20 Chars
		deviceInfoMesg.setSerialNumber(dive.getSerialNumber());
		deviceInfoMesg.setTimestamp(dive.getStartTime());
		messages.add(deviceInfoMesg);

		// Timer Events are a BEST PRACTICE for FIT ACTIVITY files
		final var eventMesg = new EventMesg();
		eventMesg.setTimestamp(dive.getStartTime());
		eventMesg.setEvent(Event.TIMER);
		eventMesg.setEventType(EventType.START);
		messages.add(eventMesg);

		// Create the Developer Id message for the developer data fields.
		final var developerIdMesg = new DeveloperDataIdMesg();
		for (int i = 0; i < APPLICATION_ID.length; i++) {
			developerIdMesg.setApplicationId(i, APPLICATION_ID[i]);
		}

		developerIdMesg.setDeveloperDataIndex((short) 0);
		messages.add(developerIdMesg);

		dive.getRecords().forEach(e -> messages.add(e.toMesg()));

		// Timer Events are a BEST PRACTICE for FIT ACTIVITY files
		final var eventMesgStop = new EventMesg();
		eventMesgStop.setTimestamp(dive.getEndTime());
		eventMesgStop.setEvent(Event.TIMER);
		eventMesgStop.setEventType(EventType.STOP_ALL);
		messages.add(eventMesgStop);

		// Every FIT ACTIVITY file MUST contain at least one Lap message
		final var lapMesg = new LapMesg();
		lapMesg.setMessageIndex(0);
		lapMesg.setTimestamp(dive.getStartTime());
		lapMesg.setStartTime(dive.getStartTime());
		final var elapsedTime = (float) (dive.getEndTime().getTimestamp() - dive.getStartTime().getTimestamp());
		lapMesg.setTotalElapsedTime(elapsedTime);
		lapMesg.setTotalTimerTime(elapsedTime);
		messages.add(lapMesg);

		final var diveSummaryMesg1 = new DiveSummaryMesg();
		diveSummaryMesg1.setTimestamp(dive.getStartTime());
		diveSummaryMesg1.setAvgDepth(dive.getAvgDepth());
		diveSummaryMesg1.setMaxDepth(dive.getMaxDepth());
		diveSummaryMesg1.setDiveNumber(dive.getDiveNumber());
		diveSummaryMesg1.setBottomTime(dive.getBottomTime());
		diveSummaryMesg1.setReferenceMesg(MESG_NUM_SESSION);
		diveSummaryMesg1.setReferenceIndex(0);
		messages.add(diveSummaryMesg1);

		// Every FIT ACTIVITY file MUST contain at least one Session message
		final var sessionMesg = new SessionMesg();
		sessionMesg.setMessageIndex(0);
		sessionMesg.setTimestamp(dive.getStartTime());
		sessionMesg.setStartTime(dive.getStartTime());
		sessionMesg.setTotalElapsedTime(elapsedTime);
		sessionMesg.setTotalTimerTime(elapsedTime);
		sessionMesg.setSport(Sport.DIVING);
		sessionMesg.setSubSport(SubSport.GAUGE_DIVING);
		sessionMesg.setFirstLapIndex(0);
		sessionMesg.setNumLaps(1);
		messages.add(sessionMesg);

		// Every FIT ACTIVITY file MUST contain EXACTLY one Activity message
		final var activityMesg = new ActivityMesg();
		activityMesg.setTimestamp(dive.getStartTime());
		activityMesg.setNumSessions(1);
		activityMesg.setTotalTimerTime(elapsedTime);
		messages.add(activityMesg);

		// Create the output stream
		final var encode = new FileEncoder(new java.io.File(filename), Fit.ProtocolVersion.V2_0);
		messages.forEach(m -> encode.write(m));
		encode.close();
	}

}
