# SdeToFit
Java command-line application to convert Suunto SDE dive log files as [exported](https://www.suunto.com/en-us/Support/faq-articles/dm5/how-do-i-import--export-dive-logs-to-dm5/) from Suunto Dive Manager to [ANT](https://www.thisisant.com/) Flexible and Interoperable Data Transport (FIT) format for [import](https://connect.garmin.com/modern/import-data) into Garmin Connect. I wrote this application after switching from a Suunto D3 to a Garmin [Descent Mk2](https://www.garmin.com/en-US/p/633356/pn/010-02132-00) dive computer and wanted to view all of my old logs in one platform. Using it requires downloading the free Garmin [Flexible and Interoperable Data Transfer (FIT) SDK](https://developer.garmin.com/fit/overview/); it is not available through a Maven repository and is not open source so I can't redistribute it here.

# Instructions
These instructions are written for Windows and assume that you have a current Java Runtime Environment installed. Steps might be slightly different on other platforms.
1. Download and install the Garmin [Flexible and Interoperable Data Transfer (FIT) SDK](https://developer.garmin.com/fit/overview/).
2. Update the `CLASSPATH` environment variable to include `fit.jar` file from the SDK in the previous step. ![Environment_Variables](https://user-images.githubusercontent.com/6307271/208803154-8b6ce48b-a54f-4e7d-bfa0-8b91a27ee588.png)
3. Launch Suunto Dive Manager.
4. In the Logbook pane, select all of the dives that you want to convert.
5. On the menu select File, Export... ![SDM_Export](https://user-images.githubusercontent.com/6307271/208802543-bf036a41-f08b-4b08-a1bb-66d536198147.png)
6. Click the Browse button.
7. In the Save As dialog box, select a destination directory.
8. Click the Save button.
9. Click the Export button.
10. Launch Command Prompt.
11. Run the converter using a command like this, specifying the export file created in the previous steps as the first argument and the output directory as the second argument: `C:\Users\nrado\git\SdeToFit\target>java.exe -cp %CLASSPATH%;SdeToFit-1.0-SNAPSHOT.jar com.github.nradov.sdetofit.SdeToFit C:\Users\nrado\OneDrive\Documents\Divelogs.SDE C:\Users\nrado\OneDrive\Documents`.
12. Launch a web browser.
13. Log in to Garmin Connect and navigate to the [Import Data](https://connect.garmin.com/modern/import-data) page.
14. Drop or select the converted FIT files.
15. Click the Import Data button.

# Limitations
* No support for time zone offsets. All times are treated as being in the local time zone.
* No support for tissue loading (decompression) or tank pressures (air integration).
* No support for bookmarks (such as "Slow"). In theory those could probably be converted to FIT `event` messages, but Garmin Connect and Garmin Dive don't display those anyway so it seems pointless.
* No calculation of surface intervals.
* Only tested with dive logs recorded using the Suunto D3 and exported from SDM 3.1.0; may not work correctly for other dive computers or SDM versions.
