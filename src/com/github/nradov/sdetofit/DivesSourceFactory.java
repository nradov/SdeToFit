package com.github.nradov.sdetofit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.github.nradov.sdetofit.suunto.SuuntoSde;

/**
 * Utility class for creating {@link DivesSource} objects.
 *
 * @author Nick Radov
 */
public final class DivesSourceFactory {

	private DivesSourceFactory() {
		// not to be instantiated
	}

	/** Constants for supported dive log file extensions. */
	private static final class FileExtension {

		/** Suunto dive export. */
		final static String SUUNTO_DIVE_EXPORT = ".sde";

	}

	/**
	 * Factory method to automatically create the right source of dive profiles
	 * based on the file extension. Currently only Suunto Dive Manager (.sde) files
	 * are supported. supported.
	 *
	 * @param file       dive profiles
	 * @param zoneOffset time zone offset for the logged data; this is typically not
	 *                   present in most data files and has to be specified
	 *                   separately
	 * @return source of zero or more dive profiles
	 * @throws ZipException                 if an error occurs while reading a
	 *                                      compressed dive log file
	 * @throws IOException                  if an error occurs while reading a dive
	 *                                      log file
	 * @throws ParserConfigurationException if an error occurs while reading an XML
	 *                                      dive log file
	 * @throws SAXException                 if an error occurs while reading an XML
	 *                                      dive log file
	 */
	public static DivesSource create(final Path file, final ZoneOffset zoneOffset)
			throws ZipException, IOException, ParserConfigurationException, SAXException {
		final String lowerCaseFile = file.toString().toLowerCase(Locale.US);
		if (lowerCaseFile.endsWith(FileExtension.SUUNTO_DIVE_EXPORT)) {
			return new SuuntoSde(file, zoneOffset);
		}
		// TODO: add support for other file formats

		throw new IllegalArgumentException("unrecognized file format: \"" + file + "\"");
	}

	public static DivesSource create(final String file, final ZoneOffset zoneOffset)
			throws ZipException, IOException, ParserConfigurationException, SAXException {
		return create(Paths.get(file), zoneOffset);
	}

}
