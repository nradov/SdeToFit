package com.github.nradov.sdetofit.suunto;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.garmin.fit.DateTime;
import com.garmin.fit.Manufacturer;
import com.github.nradov.sdetofit.Dive;
import com.github.nradov.sdetofit.Record;
import com.github.nradov.sdetofit.SdeToFit;

/**
 * <p>
 * Single dive profile in Suunto XML format. The format is simple with some
 * basic header information at the top followed by a series of data point
 * elements each containing a time, depth, and temperature (actual temperature
 * readings are only present in a subset of points).
 * </p>
 *
 * <p>
 * <strong>Note:</strong> This class is intended to process input from trusted
 * sources only and could be vulnerable to XML exploits.
 * </p>
 *
 * @author Nick Radov
 */
public class SuuntoXml implements Dive {

	private final ZoneOffset zoneOffset;
	private final DateTime start, end;
	private final String productName;
	private final long serialNumber;
	private final byte waterTemperatureMaxDepth;

	private final Element suunto;

	public SuuntoXml(final InputStream is, final ZoneOffset zoneOffset)
			throws ParserConfigurationException, SAXException, IOException {
		this.zoneOffset = zoneOffset;

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.parse(is);
		suunto = doc.getDocumentElement();

		final String date = suunto.getElementsByTagName("DATE").item(0).getTextContent();
		final int dayOfMonth = Integer.valueOf(date.substring(0, 2));
		final int month = Integer.valueOf(date.substring(3, 5))-1;
		final int year = Integer.valueOf(date.substring(6));
		final String time = suunto.getElementsByTagName("TIME").item(0).getTextContent();
		final int hourOfDay = Integer.valueOf(time.substring(0, 2));
		final int minute = Integer.valueOf(time.substring(3, 5));
		final int second = Integer.valueOf(time.substring(6));
		// TODO: support other time zones
		final Calendar startCalendar = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
		start = new DateTime((startCalendar.getTimeInMillis() - SdeToFit.OFFSET_MS) / 1000);
		final int diveTimeSec = Integer.valueOf(suunto.getElementsByTagName("DIVETIMESEC").item(0).getTextContent());
		end = new DateTime(((startCalendar.getTimeInMillis() - SdeToFit.OFFSET_MS) / 1000) + diveTimeSec);

		this.productName = suunto.getElementsByTagName("DEVICEMODEL").item(0).getTextContent();
		this.serialNumber = Long.valueOf(suunto.getElementsByTagName("WRISTOPID").item(0).getTextContent());
		this.waterTemperatureMaxDepth = Byte
				.valueOf(suunto.getElementsByTagName("WATERTEMPMAXDEPTH").item(0).getTextContent());
		populateDepths();
	}

	@Override
	public DateTime getStartTime() {
		return start;
	}

	@Override
	public DateTime getEndTime() {
		return end;
	}

	private List<Record> points = new ArrayList<Record>();;

	private void populateDepths() {
		final int sampleCount = Integer.valueOf(suunto.getElementsByTagName("SAMPLECNT").item(0).getTextContent());
		final int sampleInterval = Integer
				.valueOf(suunto.getElementsByTagName("SAMPLEINTERVAL").item(0).getTextContent());
		final int diveSeconds = sampleCount * sampleInterval;
		final NodeList samples = suunto.getElementsByTagName("SAMPLE");
		final DateTime dateTime = new DateTime(start);
		
		for (int i = 0; i < samples.getLength(); i++) {
			final Element sample = (Element) samples.item(i);
			final int sampleTime = Integer.valueOf(sample.getElementsByTagName("SAMPLETIME").item(0).getTextContent());
			if (sampleTime > diveSeconds) {
				throw new IllegalStateException("sample at time " + sampleTime + " doesn't match header");
			}

			final float depth = Float.valueOf(sample.getElementsByTagName("DEPTH").item(0).getTextContent());
			final byte temperature = Byte.valueOf(sample.getElementsByTagName("TEMPERATURE").item(0).getTextContent());
			// for most samples the temperature seems to be 0
			final byte adjustedTemperature = temperature == (byte) 0 ? waterTemperatureMaxDepth : temperature;
			final Record point = new Record(dateTime, depth, adjustedTemperature);
			points.add(point);
			dateTime.add(sampleInterval);
		}
	}

	public List<Record> getRecords() {
		return Collections.unmodifiableList(points);
	}

	@Override
	public int compareTo(final Dive o) {
		return getStartTime().compareTo(o.getStartTime());
	}

	@Override
	public String getProductName() {
		return productName;
	}

	@Override
	public Integer getManufacturer() {
		return Manufacturer.SUUNTO;
	}

	@Override
	public long getSerialNumber() {
		return serialNumber;
	}

	@Override
	public float getAvgDepth() {
		double sum = 0;
		for (final Record record : points) {
			sum += record.getDepthMeters();
		}
		return (float) (sum / (double) points.size());
	}

	@Override
	public float getMaxDepth() {
		float maxDepth = 0f;
		for (final Record record : points) {
			maxDepth = Math.max(maxDepth, record.getDepthMeters());
		}
		return maxDepth;
	}

	@Override
	public float getBottomTime() {
		return (float) (end.getTimestamp() - start.getTimestamp());
	}

}
