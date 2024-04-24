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

			if (StringUtils.startsWith(line, VERSION_MARKER)) {
				processVersionLine(line, lineNumber);
			} else if (StringUtils.startsWith(line, SECTION_MARKER)) {
				processSectionLine(line, lineNumber);
			} else {
				processGenericLine(line, lineNumber);
			}
		}

		finalizeCurrentVersion(lineNumber);
	}

	private void processVersionLine(String line, int lineNumber) {

		finalizeCurrentVersion(lineNumber);

		currentVersion = Version.builder();

		int versionStart = line.indexOf('[') + 1;
		int versionEnd = line.indexOf(']');

		currentVersion.name(line.substring(versionStart, versionEnd));

		int dateStart = line.indexOf(" - ") + 3;
		currentVersion.releaseDate(line.substring(dateStart));
	}

	private void processSectionLine(String line, int lineNumber) {

		finalizeCurrentSection(lineNumber);

		currentSection = Section.builder();
		currentSection.name(line.substring(SECTION_MARKER.length()));
	}

	private void processGenericLine(String line, int lineNumber) {

		if (currentSection != null) {
			currentSection.line(line);
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
