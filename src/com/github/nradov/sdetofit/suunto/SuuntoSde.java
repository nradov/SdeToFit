package com.github.nradov.sdetofit.suunto;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.Enumeration;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.github.nradov.sdetofit.Dive;
import com.github.nradov.sdetofit.DivesSource;

/**
 * Read data from Suunto
 * <a href="http://www.suunto.com/en-US/Support/Suunto-DM5/" target="_">Dive
 * Manager</a> {@code .sde} export files. A {@code .sde} file is actually a Zip
 * file containing one XML file per dive profile.
 *
 * @author Nick Radov
 */
public class SuuntoSde implements DivesSource {

	private static final Logger LOGGER = Logger.getLogger(SuuntoSde.class.getName());

	private final ZoneOffset zoneOffset;
	private final ZipFile zipFile;

	private final NavigableSet<Dive> dives = new TreeSet<>();

	public SuuntoSde(final String pathname, final ZoneOffset zoneOffset)
			throws ZipException, IOException, ParserConfigurationException, SAXException {
		this(Paths.get(pathname), zoneOffset);
	}

	public SuuntoSde(final Path file, final ZoneOffset zoneOffset)
			throws ZipException, IOException, ParserConfigurationException, SAXException {
		this.zoneOffset = zoneOffset;
		zipFile = new ZipFile(file.toFile());
		final Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			final ZipEntry entry = entries.nextElement();
			LOGGER.log(Level.FINE, "processing dive profile: \"" + entry.getName() + "\"");
			System.err.println("processing dive profile: \"" + entry.getName() + "\"");
			dives.add(new SuuntoXml(zipFile.getInputStream(entry), this.zoneOffset));
		}
		
	}

	@Override
	public NavigableSet<Dive> getDives() {
		return dives;
	}

}
