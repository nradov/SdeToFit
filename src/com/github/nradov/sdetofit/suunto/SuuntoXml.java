package com.github.nradov.sdetofit.suunto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
 * Single dive profile in Suunto XML format. The format is not documented so I
 * had to reverse engineer it. This class may not work correctly for some
 * versions of Suunto Dive Manager. The format is simple with some basic header
 * information at the top followed by a series of data point elements each
 * containing a time, depth, and temperature (actual temperature readings are
 * only present in a subset of points).
 *
 * <p>
 * <strong>Note:</strong> This class is intended to process input from trusted
 * sources only and could be vulnerable to XML exploits.
 * </p>
 *
 * @author Nick Radov
 */
public class SuuntoXml implements Dive, DivesSource {

	private final DateTime start, end;
	private final String productName;
	private final long serialNumber;
	private final byte waterTemperatureMaxDepth;
	private final long diveNumber;
    private final long surfaceTime;
	private final float maxDepth, meanDepth;

	private final Element suunto;

	/* XML document root element name. */
	private static final String DOCUMENT_ELEMENT_NAME = "SUUNTO";

	public SuuntoXml(final Path file) throws ParserConfigurationException, SAXException, IOException {
		this(new FileInputStream(file.toFile()));
	}
	
	public SuuntoXml(final InputStream is) throws ParserConfigurationException, SAXException, IOException {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.parse(is);
		suunto = doc.getDocumentElement();
		
		final int sampleCnt = Integer.valueOf(suunto.getElementsByTagName("SAMPLECNT").item(0).getTextContent());
		final String date = suunto.getElementsByTagName("DATE").item(0).getTextContent();
		final int dayOfMonth = Integer.valueOf(date.substring(0, 2));
		final int month = Integer.valueOf(date.substring(3, 5)) - 1;
		final int year = Integer.valueOf(date.substring(6));
		final String time = suunto.getElementsByTagName("TIME").item(0).getTextContent();
		final int hourOfDay = Integer.valueOf(time.substring(0, 2));
		final int minute = Integer.valueOf(time.substring(3, 5));
		final int second = Integer.valueOf(time.substring(6));
		final int sampleInterval = Integer.valueOf(suunto.getElementsByTagName("SAMPLEINTERVAL").item(0).getTextContent());
		final Calendar startCalendar = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
		start = new DateTime((startCalendar.getTimeInMillis() - SdeToFit.OFFSET_MS) / 1000);
		final var diveTimeSecText = suunto.getElementsByTagName("DIVETIMESEC").item(0).getTextContent().trim();
		final int diveTimeSec;
		// the DIVETIMESEC element may or may not be populated depending on the SDM
		// version
		if (diveTimeSecText.length() > 0) {
			diveTimeSec = Integer.valueOf(diveTimeSecText);
		} else {
			diveTimeSec = sampleCnt * sampleInterval;
		}
		end = new DateTime(((startCalendar.getTimeInMillis() - SdeToFit.OFFSET_MS) / 1000) + diveTimeSec);

		this.maxDepth = Float.parseFloat(suunto.getElementsByTagName("MAXDEPTH").item(0).getTextContent());
		this.meanDepth = Float.parseFloat(suunto.getElementsByTagName("MEANDEPTH").item(0).getTextContent());

		// the LOGTITLE element content is formatted like "367. 2019-11-16 11:11:00"
		// where the first number is the dive number
		final var logTitle = suunto.getElementsByTagName("LOGTITLE").item(0).getTextContent();
		this.diveNumber = Long.parseLong(logTitle.substring(0, logTitle.indexOf('.')));
		String surfaceTime_string = suunto.getElementsByTagName("SURFACETIME").item(0).getTextContent() != "" ? suunto.getElementsByTagName("SURFACETIME").item(0).getTextContent() : "0";
		this.surfaceTime = Integer.valueOf(surfaceTime_string);
		this.productName = suunto.getElementsByTagName("DEVICEMODEL").item(0).getTextContent();
		this.serialNumber = Long.valueOf(suunto.getElementsByTagName("WRISTOPID").item(0).getTextContent());
		this.waterTemperatureMaxDepth = Byte
				.valueOf(suunto.getElementsByTagName("WATERTEMPMAXDEPTH").item(0).getTextContent());
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
		final int sampleCount = Integer.valueOf(suunto.getElementsByTagName("SAMPLECNT").item(0).getTextContent());
		final int sampleInterval = Integer
				.valueOf(suunto.getElementsByTagName("SAMPLEINTERVAL").item(0).getTextContent());
		final int diveSeconds = (sampleCount + 1) * sampleInterval;
		final NodeList samples = suunto.getElementsByTagName("SAMPLE");
		final DateTime dateTime = new DateTime(start);

		for (int i = 0; i < samples.getLength(); i++) {
			final var sample = (Element) samples.item(i);
			final int sampleTime = Integer.valueOf(sample.getElementsByTagName("SAMPLETIME").item(0).getTextContent());
			if (sampleTime > diveSeconds) {
				throw new IllegalStateException("sample at time " + sampleTime + " doesn't match header");
			}

			final float depth = Float.valueOf(sample.getElementsByTagName("DEPTH").item(0).getTextContent());
			final byte temperature = sample.getElementsByTagName("TEMPERATURE").item(0).getTextContent() != "" ? (byte) Math.round(Float.valueOf(sample.getElementsByTagName("TEMPERATURE").item(0).getTextContent())) : (byte) 0;
			// for most samples the temperature seems to be 0
			final byte adjustedTemperature = temperature == (byte) 0 ? waterTemperatureMaxDepth : temperature;
			final var record = new Record(dateTime, depth, adjustedTemperature);
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
    public long getSurfaceTime() {
        return surfaceTime;
    }

	@Override
	public NavigableSet<Dive> getDives() {
		return new TreeSet<Dive>(Collections.singleton(this));
	}

}
