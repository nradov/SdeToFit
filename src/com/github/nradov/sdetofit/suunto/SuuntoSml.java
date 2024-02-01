package com.github.nradov.sdetofit.suunto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

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
import com.github.nradov.sdetofit.DivesSource;
import com.github.nradov.sdetofit.Record;
import com.github.nradov.sdetofit.SdeToFit;

/**
 * Single dive profile in Suunto SML format.
 */
public class SuuntoSml implements Dive, DivesSource {

	private final DateTime start, end;
	private final String productName;
	private final long serialNumber;
	private final byte waterTemperatureMaxDepth;
	private final long diveNumber;
	private final float maxDepth, meanDepth;

	private final Element suunto;

	/* XML document root element name. */
	private static final String DOCUMENT_ELEMENT_NAME = "sml";

	public SuuntoSml(final Path file) throws ParserConfigurationException, SAXException, IOException {
		this(new FileInputStream(file.toFile()));
	}
	
	public SuuntoSml(final InputStream is) throws ParserConfigurationException, SAXException, IOException {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.parse(is);
		suunto = doc.getDocumentElement();
		
		if (!DOCUMENT_ELEMENT_NAME.equals(suunto.getTagName())) {
			throw new IllegalArgumentException(
					"Document element name is " + suunto.getLocalName() + " instead of " + DOCUMENT_ELEMENT_NAME);
		}



		// final int sampleCnt = Integer.valueOf(suunto.getElementsByTagName("SAMPLECNT").item(0).getTextContent());
		final int sampleCnt = 1;


		final String date = suunto.getElementsByTagName("DateTime").item(0).getTextContent();
		final LocalDateTime dateTime = LocalDateTime.parse(date);

		final int dayOfMonth = dateTime.getDayOfMonth();
		final int month = dateTime.getMonthValue();
		final int year = dateTime.getYear();
		final int hourOfDay = dateTime.getHour();
		final int minute = dateTime.getMinute();
		final int second = dateTime.getSecond();


		final int sampleInterval = Integer.valueOf(suunto.getElementsByTagName("SampleInterval").item(0).getTextContent());
		final Calendar startCalendar = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
		start = new DateTime((startCalendar.getTimeInMillis() - SdeToFit.OFFSET_MS) / 1000);
		final var diveTimeSecText = suunto.getElementsByTagName("Duration").item(0).getTextContent().trim();
		final int diveTimeSec;
		// the DIVETIMESEC element may or may not be populated depending on the SDM
		// version
		if (diveTimeSecText.length() > 0) {
			diveTimeSec = Integer.valueOf(diveTimeSecText);
		} else {
			diveTimeSec = sampleCnt * sampleInterval;
		}
		end = new DateTime(((startCalendar.getTimeInMillis() - SdeToFit.OFFSET_MS) / 1000) + diveTimeSec);

		this.maxDepth = Float.parseFloat(suunto.getElementsByTagName("Max").item(0).getTextContent());
		this.meanDepth = Float.parseFloat(suunto.getElementsByTagName("Avg").item(0).getTextContent());

		// the LOGTITLE element content is formatted like "367. 2019-11-16 11:11:00"
		// where the first number is the dive number
		// final var logTitle = null; //suunto.getElementsByTagName("LOGTITLE").item(0).getTextContent();
		this.diveNumber = Integer.valueOf(suunto.getElementsByTagName("NumberInSeries").item(0).getTextContent());

		this.productName = suunto.getElementsByTagName("Name").item(0).getTextContent();
		this.serialNumber = Long.valueOf(suunto.getElementsByTagName("SerialNumber").item(0).getTextContent());
		this.waterTemperatureMaxDepth = 0;
		// 		Byte.valueOf(suunto.getElementsByTagName("WATERTEMPMAXDEPTH").item(0).getTextContent());
		populateRecords();
	}

	@Override
	public DateTime getStartTime() {
		return start;
	}

	@Override
	public DateTime getEndTime() {
		return end;
	}

	private List<Record> records = new ArrayList<Record>();;

	private void populateRecords() {

		final NodeList samples = suunto.getElementsByTagName("Sample");

		final int sampleCount = samples.getLength()+1;
		final int sampleInterval = Integer
				.valueOf(suunto.getElementsByTagName("SampleInterval").item(0).getTextContent());
		final int diveSeconds = sampleCount * sampleInterval;
		final DateTime dateTime = new DateTime(start);

		for (int i = 0; i < samples.getLength(); i++) {
			final var sample = (Element) samples.item(i);
			final int sampleTime = Integer.valueOf(sample.getElementsByTagName("Time").item(0).getTextContent());
			if (sampleTime > diveSeconds) {
				throw new IllegalStateException("sample at time " + sampleTime + " doesn't match header");
			}

			if (sample.getElementsByTagName("Depth").item(0) == null) {
				continue;
			}

			final float depth = Float.valueOf(sample.getElementsByTagName("Depth").item(0).getTextContent());
			final float temperature = Float.valueOf(sample.getElementsByTagName("Temperature").item(0).getTextContent()) - 273.15f;
			final var record = new Record(dateTime, depth, (byte) temperature);
			records.add(record);
			dateTime.add(sampleInterval);
		}
	}

	public List<Record> getRecords() {
		return Collections.unmodifiableList(records);
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
		return meanDepth;
	}

	@Override
	public float getMaxDepth() {
		return maxDepth;
	}

	@Override
	public float getBottomTime() {
		return (float) (end.getTimestamp() - start.getTimestamp());
	}

	@Override
	public long getDiveNumber() {
		return diveNumber;
	}

	@Override
	public NavigableSet<Dive> getDives() {
		return new TreeSet<Dive>(Collections.singleton(this));
	}

}
