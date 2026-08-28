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
	static final String SECTION_MARKER = "### ";

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
	 * A level 2 heading is a version heading if it starts with a bracketed version name, or if it is
	 * one of the unbracketed unreleased markers the README documents ("## Unreleased", "## SNAPSHOT").
	 * Changelogs use level 2 headings for prose as well (e.g. "## Older versions"); the parser keeps
	 * those as content of the enclosing section or version. Note that content kept as version header
	 * lines is still subject to the merger's handling of version descriptions, which currently does
	 * not carry header lines into the merged result.
	 */
	private static boolean isVersionLine(String line) {

		if (!StringUtils.startsWith(line, VERSION_MARKER)) {
			return false;
		}

		String headingText = headingText(line);

		if (StringUtils.startsWith(headingText, "[") && headingText.indexOf(']') > 1) {
			return true;
		}

		return StringUtils.equalsAnyIgnoreCase(headingText, UNRELEASED_MARKERS);
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
		if (versionEnd < 0) {
			currentVersion.name(headingText(line));
			return;
		}

		currentVersion.name(line.substring(versionStart + 1, versionEnd));

		String afterVersionName = line.substring(versionEnd + 1);

		int restStart = 0;

		int linkEnd = findLinkEnd(afterVersionName);
		if (linkEnd >= 0) {
			String link = afterVersionName.substring(1, linkEnd);
			// an empty target ("## [1.0.0]()") is no link, the same way an absent release date is null
			if (!link.isEmpty()) {
				currentVersion.link(link);
			}
			restStart = linkEnd + 1;
		}

		restStart = skipWhitespace(afterVersionName, restStart);

		String releaseDate = parseReleaseDate(afterVersionName, restStart);
		if (!releaseDate.isEmpty()) {
			currentVersion.releaseDate(releaseDate);
		}
	}

	/**
	 * Locates the link target of a version name written as an inline link:
	 * "## [1.0.0](https://.../compare/v0.9.0...v1.0.0) - 2019-02-15". Markdown requires the "("
	 * to follow the closing bracket immediately, so a parenthetical separated from it by whitespace
	 * ("## [1.0.0] (yanked)") is literal text, not a link, and is left to the release date parser.
	 * An unclosed "(" is not treated as a link either, for the same reason.
	 *
	 * Parentheses inside the link target are matched as pairs, so a target that contains a balanced
	 * pair ("https://example.com/foo_(bar)") is not truncated at its first ")".
	 *
	 * @param afterVersionName everything following the closing bracket of the version name
	 * @return the index of the closing parenthesis of the link, or -1 if the heading has no link
	 */
	private static int findLinkEnd(String afterVersionName) {

		if (afterVersionName.isEmpty() || afterVersionName.charAt(0) != '(') {
			return -1;
		}

		int depth = 0;

		for (int i = 0; i < afterVersionName.length(); i++) {

			char c = afterVersionName.charAt(i);

			if (c == '(') {
				depth++;
			} else if (c == ')' && --depth == 0) {
				return i;
			}
		}

		// unbalanced "(": not a link, so the rest of the heading stays readable as a release date
		return -1;
	}

	private static String headingText(String line) {
		return line.substring(VERSION_MARKER.length()).trim();
	}

	/**
	 * Strips the separator between version name and release date. Rather than matching the
	 * canonical " - ", any dash is accepted, so that an en or em dash does not cost the release
	 * date. The printer writes the canonical separator back.
	 * <p>
	 * Whatever follows the separator is taken as the release date without checking that it is a
	 * date: the field is free-form by design ("[SNAPSHOT]" is a documented placeholder, and Keep a
	 * Changelog appends "[YANKED]" to the date of a withdrawn release), and whether a version counts
	 * as released is decided by {@link Version#isReleased()} from the marker words alone.
	 *
	 * @param afterVersionName everything following the closing bracket of the version name
	 * @param from the index at which the release date would start, past the link if the heading has one
	 * @return the release date with surrounding whitespace removed, or an empty string if the
	 *         heading carries none
	 */
	private static String parseReleaseDate(String afterVersionName, int from) {

		int dateStart = from;

		if (dateStart < afterVersionName.length() && isDash(afterVersionName.charAt(dateStart))) {
			dateStart = skipWhitespace(afterVersionName, dateStart + 1);
		}

		return afterVersionName.substring(dateStart).strip();
	}

	/**
	 * Dash punctuation (hyphen-minus, en dash, em dash, ...) plus U+2212 MINUS SIGN, which is
	 * mathematical symbol punctuation but auto-substituted for a dash by some editors.
	 */
	private static boolean isDash(char c) {
		return Character.getType(c) == Character.DASH_PUNCTUATION || c == '−';
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
