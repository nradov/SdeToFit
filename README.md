# SdeToFit
Java command-line application to convert Suunto SDE dive log files as [exported](https://www.suunto.com/en-us/Support/faq-articles/dm5/how-do-i-import--export-dive-logs-to-dm5/) from Suunto Dive Manager to [ANT](https://www.thisisant.com/) Flexible and Interoperable Data Transport (FIT) format for [import](https://connect.garmin.com/modern/import-data) into Garmin Connect. I wrote this application after switching from a Suunto D3 to a Garmin [Descent Mk2](https://www.garmin.com/en-US/p/633356/pn/010-02132-00) dive computer and wanted to view all of my old logs in one platform. Using it requires downloading the free Garmin [Flexible and Interoperable Data Transfer (FIT) SDK](https://developer.garmin.com/fit/overview/); it is not available through a Maven repository and is not open source so I can't redistribute it here.

# Instructions
These instructions are written for Linux and assume that you have a current Java Runtime Environment installed. Steps might be slightly different on other platforms.
1. Export Suunto dives to SDE: ![SDM_Export](https://user-images.githubusercontent.com/6307271/208802543-bf036a41-f08b-4b08-a1bb-66d536198147.png)
2. Compile and create fat jar: ` mvn clean compile assembly:single`
3. Run the converter using a command like this, specifying the export file created in the previous steps as the first argument and the output directory as the second argument: 
```bash
java -cp ./target/SdeToFit-1.0-SNAPSHOT-jar-with-dependencies.jar  com.github.nradov.sdetofit.SdeToFit  ./Divelogs.SDE  output/
Converting dive log: "0.xml"
Converting dive log: "1.xml"
Converting dive log: "2.xml"
Converting dive log: "3.xml"
```
4. Import the FIT files into Garmin Connect on web.

# Limitations
* No support for time zone offsets. All times are treated as being in the local time zone.
* No support for tissue loading (decompression) or tank pressures (air integration).
* No support for bookmarks (such as "Slow"). In theory those could probably be converted to FIT `event` messages, but Garmin Connect and Garmin Dive don't display those anyway so it seems pointless.
* Only tested with dive logs recorded using the Suunto Mosquito and exported from SDM 3.1.0; may not work correctly for other dive computers or SDM versions.
