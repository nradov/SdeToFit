package com.github.nradov.sdetofit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.DeveloperDataIdMesg;
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventType;
import com.garmin.fit.File;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.LapMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;
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

	public SdeToFit(final java.io.File input, final java.io.File output) {

	}

	public static void main(final String[] args) throws ZipException, IOException, ParserConfigurationException, SAXException {
		if (args.length != 2) {
			throw new IllegalArgumentException("Usage: input output");
		}

		final DivesSource divesSource = DiveSourceFactory.create(args[0], null);
		for(final Dive dive: divesSource.getDives()) {
			createDiveFitFile(dive);
		}
		
		
	}

	public static void createDiveFitFile(final Dive dive) {
		final String filename = "ActivityEncodeRecipe.fit";

		final List<Mesg> messages = new ArrayList<Mesg>();

		// The starting timestamp for the activity
		final DateTime startTime = new DateTime(new Date());

		// Timer Events are a BEST PRACTICE for FIT ACTIVITY files
		final EventMesg eventMesg = new EventMesg();
		eventMesg.setTimestamp(startTime);
		eventMesg.setEvent(Event.TIMER);
		eventMesg.setEventType(EventType.START);
		messages.add(eventMesg);

		// Create the Developer Id message for the developer data fields.
		final DeveloperDataIdMesg developerIdMesg = new DeveloperDataIdMesg();
		// It is a BEST PRACTICE to reuse the same Guid for all FIT files created by
		// your platform
		byte[] appId = new byte[] { 0x1, 0x1, 0x2, 0x3, 0x5, 0x8, 0xD, 0x15, 0x22, 0x37, 0x59, (byte) 0x90, (byte) 0xE9,
				0x79, 0x62, (byte) 0xDB };

		for (int i = 0; i < appId.length; i++) {
			developerIdMesg.setApplicationId(i, appId[i]);
		}

		developerIdMesg.setDeveloperDataIndex((short) 0);
		messages.add(developerIdMesg);

		// Every FIT ACTIVITY file MUST contain Record messages
		final DateTime timestamp = new DateTime(startTime);

		// Create one hour (3600 seconds) of Record data
		for (int i = 0; i <= 3600; i++) {
			// Create a new Record message and set the timestamp
			RecordMesg recordMesg = new RecordMesg();
			recordMesg.setTimestamp(timestamp);

			// Fake Record Data of Various Signal Patterns
			recordMesg.setDepth(1f);
			recordMesg.setAltitude((float) (Math.abs((double) i % 255.0) - 127.0)); // Triangle

			// Write the Record message to the output stream
			messages.add(recordMesg);

			// Increment the timestamp by one second
			timestamp.add(1);
		}

		// Timer Events are a BEST PRACTICE for FIT ACTIVITY files
		EventMesg eventMesgStop = new EventMesg();
		eventMesgStop.setTimestamp(timestamp);
		eventMesgStop.setEvent(Event.TIMER);
		eventMesgStop.setEventType(EventType.STOP_ALL);
		messages.add(eventMesgStop);

		// Every FIT ACTIVITY file MUST contain at least one Lap message
		LapMesg lapMesg = new LapMesg();
		lapMesg.setMessageIndex(0);
		lapMesg.setTimestamp(timestamp);
		lapMesg.setStartTime(startTime);
		lapMesg.setTotalElapsedTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
		lapMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
		messages.add(lapMesg);

		// Every FIT ACTIVITY file MUST contain at least one Session message
		SessionMesg sessionMesg = new SessionMesg();
		sessionMesg.setMessageIndex(0);
		sessionMesg.setTimestamp(timestamp);
		sessionMesg.setStartTime(startTime);
		sessionMesg.setTotalElapsedTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
		sessionMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
		sessionMesg.setSport(Sport.DIVING);
		sessionMesg.setSubSport(SubSport.GAUGE_DIVING);
		sessionMesg.setFirstLapIndex(0);
		sessionMesg.setNumLaps(1);
		messages.add(sessionMesg);

		// Every FIT ACTIVITY file MUST contain EXACTLY one Activity message
		final ActivityMesg activityMesg = new ActivityMesg();
		activityMesg.setTimestamp(timestamp);
		activityMesg.setNumSessions(1);
		final TimeZone timeZone = TimeZone.getDefault();
		long timezoneOffset = (timeZone.getRawOffset() + timeZone.getDSTSavings()) / 1000;
		activityMesg.setLocalTimestamp(timestamp.getTimestamp() + timezoneOffset);
		activityMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
		messages.add(activityMesg);

		createActivityFile(messages, filename, startTime);
	}

	public static void createActivityFile(final List<Mesg> messages, final String filename, final DateTime startTime)
			throws FitRuntimeException {
		// The combination of file type, manufacturer id, product id, and serial number
		// should be unique.
		// When available, a non-random serial number should be used.
		final float softwareVersion = 1.0f;

		Random random = new Random();
		int serialNumber = random.nextInt();

		// Every FIT file MUST contain a File ID message
		final FileIdMesg fileIdMesg = new FileIdMesg();
		fileIdMesg.setType(File.ACTIVITY);
		fileIdMesg.setManufacturer(Manufacturer.SUUNTO);
		fileIdMesg.setTimeCreated(startTime);
		fileIdMesg.setSerialNumber((long) serialNumber);

		// A Device Info message is a BEST PRACTICE for FIT ACTIVITY files
		final DeviceInfoMesg deviceInfoMesg = new DeviceInfoMesg();
		deviceInfoMesg.setDeviceIndex(DeviceIndex.CREATOR);
		deviceInfoMesg.setManufacturer(Manufacturer.SUUNTO);
		deviceInfoMesg.setProductName("D3"); // Max 20 Chars
		deviceInfoMesg.setSerialNumber((long) serialNumber);
		deviceInfoMesg.setSoftwareVersion(softwareVersion);
		deviceInfoMesg.setTimestamp(startTime);

		// Create the output stream
		final FileEncoder encode = new FileEncoder(new java.io.File(filename), Fit.ProtocolVersion.V2_0);

		encode.write(fileIdMesg);
		encode.write(deviceInfoMesg);

		for (Mesg message : messages) {
			encode.write(message);
		}

		// Close the output stream
		encode.close();

		System.out.println("Encoded FIT Activity file " + filename);
	}

}
