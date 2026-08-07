package com.jardoapps.changelog.merge.driver;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.jardoapps.changelog.merge.driver.Changelog.ChangelogBuilder;
import com.jardoapps.changelog.merge.driver.Changelog.Section;
import com.jardoapps.changelog.merge.driver.Changelog.Section.SectionBuilder;
import com.jardoapps.changelog.merge.driver.Changelog.Version;
import com.jardoapps.changelog.merge.driver.Changelog.Version.VersionBuilder;

public class ChangelogParser {

	private static final String CHANGELOG_NAME_MARKER = "# ";
	private static final String VERSION_MARKER = "## ";
	private static final String SECTION_MARKER = "### ";

	/** The unbracketed version names the README lists under "Marking Unreleased Versions". */
	private static final String[] UNRELEASED_MARKERS = { "Unreleased", "Snapshot" };

	private final ChangelogBuilder changelog = Changelog.builder();

	private VersionBuilder currentVersion;

	private SectionBuilder currentSection;

	public Changelog getChangelog() {
		return changelog.build();
	}

	public void parse(BufferedReader reader) throws IOException {

		String line = reader.readLine();
		if (StringUtils.startsWith(line, CHANGELOG_NAME_MARKER)) {
			changelog.name(line.substring(CHANGELOG_NAME_MARKER.length()));
		} else {
			throw new IllegalArgumentException("Expected changelog file to start with '" + CHANGELOG_NAME_MARKER + "'");
		}

		int lineNumber = 1;
		while ((line = reader.readLine()) != null) {

			lineNumber++;

			if (isVersionLine(line)) {
				processVersionLine(line, lineNumber);
			} else if (StringUtils.startsWith(line, SECTION_MARKER)) {
				processSectionLine(line, lineNumber);
			} else {
				processGenericLine(line, lineNumber);
			}
		}

		finalizeCurrentVersion(lineNumber);
	}

	/**
	 * A level 2 heading is a version heading if it carries a bracketed version name, or if it is
	 * one of the unbracketed unreleased markers the README documents ("## Unreleased", "## SNAPSHOT").
	 * Changelogs use level 2 headings for prose as well (e.g. "## Older versions"); those are kept
	 * verbatim as content instead.
	 */
	private static boolean isVersionLine(String line) {

		if (!StringUtils.startsWith(line, VERSION_MARKER)) {
			return false;
		}

		int versionStart = line.indexOf('[');
		if (versionStart >= 0 && line.indexOf(']', versionStart) > versionStart) {
			return true;
		}

		return StringUtils.equalsAnyIgnoreCase(headingText(line), UNRELEASED_MARKERS);
	}

	private void processVersionLine(String line, int lineNumber) {

		finalizeCurrentVersion(lineNumber);

		currentVersion = Version.builder();

		int versionStart = line.indexOf('[');
		if (versionStart < 0) {
			currentVersion.name(headingText(line));
			return;
		}

		int versionEnd = line.indexOf(']', versionStart);

		currentVersion.name(line.substring(versionStart + 1, versionEnd));

		String releaseDate = parseReleaseDate(line.substring(versionEnd + 1));
		if (!releaseDate.isEmpty()) {
			currentVersion.releaseDate(releaseDate);
		}
	}

	private static String headingText(String line) {
		return line.substring(VERSION_MARKER.length()).trim();
	}

	/**
	 * Strips the separator between version name and release date. Rather than matching the
	 * canonical " - ", any dash is accepted, so that an en or em dash does not cost the release
	 * date. The printer writes the canonical separator back.
	 *
	 * @param afterVersionName everything following the closing bracket of the version name
	 * @return the release date, or an empty string if the heading carries none
	 */
	private static String parseReleaseDate(String afterVersionName) {

		int dateStart = skipWhitespace(afterVersionName, 0);

		if (dateStart < afterVersionName.length()
				&& Character.getType(afterVersionName.charAt(dateStart)) == Character.DASH_PUNCTUATION) {
			dateStart = skipWhitespace(afterVersionName, dateStart + 1);
		}

		return afterVersionName.substring(dateStart);
	}

	private static int skipWhitespace(String text, int from) {

		int index = from;
		while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
			index++;
		}

		return index;
	}

	private void processSectionLine(String line, int lineNumber) {

		finalizeCurrentSection(lineNumber);

		currentSection = Section.builder();
		currentSection.name(line.substring(SECTION_MARKER.length()));
	}

	private void processGenericLine(String line, int lineNumber) {

		if (currentSection != null) {
			currentSection.line(line);
		} else if (currentVersion != null) {
			currentVersion.headerLine(line);
		} else if (currentVersion == null) {
			changelog.headerLine(line);
		}
	}

	private void finalizeCurrentVersion(int lineNumber) {
		finalizeCurrentSection(lineNumber);
		if (currentVersion != null) {
			Version builtVersion = currentVersion.build();
			if (builtVersion.isReleased()) {
				changelog.releasedVersion(builtVersion);
			} else {
				changelog.unreleasedVersion(builtVersion);
			}
		}
	}

	private void finalizeCurrentSection(int lineNumber) {
		if (currentSection != null) {
			Section builtSection = currentSection.build();
			currentSection = null;
			if (currentVersion != null) {
				currentVersion.section(builtSection);
			} else {
				throw new IllegalStateException("Line " + lineNumber + ": Found a section outside of version.");
			}
		}
	}
}
