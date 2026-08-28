package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.jardoapps.changelog.merge.driver.Changelog.Section;
import com.jardoapps.changelog.merge.driver.Changelog.Version;

class ChangelogParserTest {

	@Test
	void testParse() throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader(CHANGELOG_TO_PARSE))) {
			parser.parse(reader);
		}

		Changelog changelog = parser.getChangelog();

		assertThat(changelog.getName()).isEqualTo("Changelog");
		assertThat(changelog.getHeaderLines()).containsExactly(
				"",
				"All notable changes to this project will be documented in this file.",
				"",
				"The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),",
				"and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).",
				""
		);

		Version version = changelog.getUnreleasedVersion();
		assertThat(version.getName()).isEqualTo("1.2.0");
		assertThat(version.getReleaseDate()).isEqualTo("SNAPSHOT");

		Section section = version.getSections().get(0);
		assertThat(section.getName()).isEqualTo("Added");
		assertThat(section.getLines()).containsExactly(
				"",
				"- v1.1 Brazilian Portuguese translation.",
				"- v1.1 German Translation",
				""
		);

		section = version.getSections().get(1);
		assertThat(section.getName()).isEqualTo("Changed");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Use frontmatter title & description in each language version template",
				"- Replace broken OpenGraph image with an appropriately-sized Keep a Changelog",
				"  image that will render properly (although in English for all languages)",
				""
		);

		section = version.getSections().get(2);
		assertThat(section.getName()).isEqualTo("Removed");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Trademark sign previously shown after the project description in version",
				"0.3.0",
				""
		);

		version = changelog.getReleasedVersions().get(0);
		assertThat(version.getName()).isEqualTo("1.1.1");
		assertThat(version.getReleaseDate()).isEqualTo("2023-03-05");

		assertThat(version.getHeaderLines()).containsExactly(
				"",
				"General version description.",
				""
		);

		section = version.getSections().get(0);
		assertThat(section.getName()).isEqualTo("Added");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Arabic translation (#444).",
				"- v1.1 French translation.",
				""
		);

		section = version.getSections().get(1);
		assertThat(section.getName()).isEqualTo("Fixed");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Improve French translation (#377).",
				"- Improve id-ID translation (#416).",
				""
		);

		section = version.getSections().get(2);
		assertThat(section.getName()).isEqualTo("Changed");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Upgrade dependencies: Ruby 3.2.1, Middleman, etc.",
				""
		);

		section = version.getSections().get(3);
		assertThat(section.getName()).isEqualTo("Removed");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Unused normalize.css file.",
				"- Identical links assigned in each translation file.",
				""
		);

		version = changelog.getReleasedVersions().get(1);
		assertThat(version.getName()).isEqualTo("1.1.0");
		assertThat(version.getReleaseDate()).isEqualTo("2019-02-15");

		section = version.getSections().get(0);
		assertThat(section.getName()).isEqualTo("Added");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Danish translation (#297).",
				"- Georgian translation from (#337).",
				""
		);

		section = version.getSections().get(1);
		assertThat(section.getName()).isEqualTo("Fixed");
		assertThat(section.getLines()).containsExactly(
				"",
				"- Italian translation (#332).",
				"- Indonesian translation (#336)."
		);

	}

	@Test
	void testParse_keepsNonVersionHeadingsAsContent() throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader(CHANGELOG_WITH_NON_VERSION_HEADING))) {
			parser.parse(reader);
		}

		Changelog changelog = parser.getChangelog();

		assertThat(changelog.getReleasedVersions()).hasSize(1);

		Version version = changelog.getReleasedVersions().get(0);
		assertThat(version.getName()).isEqualTo("1.0.0");

		Section section = version.getSections().get(0);
		assertThat(section.getLines()).containsExactly(
				"",
				"- Everything.",
				"",
				"## Older versions",
				"",
				"See the v0.9 tag.",
				"",
				"## Notes [see #12]",
				"",
				"Bracketed prose stays too."
		);
	}

	@Test
	void testParse_printRoundTripWithNonVersionHeadings() throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader(CHANGELOG_WITH_NON_VERSION_HEADING))) {
			parser.parse(reader);
		}

		assertThat(print(parser.getChangelog())).isEqualTo(CHANGELOG_WITH_NON_VERSION_HEADING);
	}

	@Test
	void testParse_versionWithoutReleaseDate() throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader("# Changelog\n\n## [Unreleased]\n"))) {
			parser.parse(reader);
		}

		Version version = parser.getChangelog().getUnreleasedVersion();
		assertThat(version.getName()).isEqualTo("Unreleased");
		assertThat(version.getReleaseDate()).isNull();
	}

	/**
	 * The unbracketed forms the README lists under "Marking Unreleased Versions".
	 */
	@ParameterizedTest
	@ValueSource(strings = { "Unreleased", "UNRELEASED", "Snapshot", "SNAPSHOT" })
	void testParse_unbracketedUnreleasedVersion(String versionName) throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader("# Changelog\n\n## " + versionName + "\n"))) {
			parser.parse(reader);
		}

		Version version = parser.getChangelog().getUnreleasedVersion();
		assertThat(version.getName()).isEqualTo(versionName);
		assertThat(version.getReleaseDate()).isNull();

		assertThat(print(parser.getChangelog())).isEqualTo("# Changelog\n\n## [" + versionName + "]\n");
	}

	/**
	 * A version name written as a link, the form Keep a Changelog recommends, must round-trip: the
	 * link is parsed into the version and written back by the printer.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"## [1.0.0](https://example.com/compare/v0.9.0...v1.0.0) - 2019-02-15 | https://example.com/compare/v0.9.0...v1.0.0 | 2019-02-15",
			"## [1.0.0](https://example.com/compare/v0.9.0...v1.0.0) | https://example.com/compare/v0.9.0...v1.0.0 | ",
			"## [Unreleased](https://example.com/compare/v1.0.0...HEAD) | https://example.com/compare/v1.0.0...HEAD | ",
			"## [1.0.0](https://example.com/foo_(bar)) - 2019-02-15 | https://example.com/foo_(bar) | 2019-02-15"
	})
	void testParse_linkedVersionHeading(String versionHeading, String expectedLink, String expectedReleaseDate) throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader("# Changelog\n\n" + versionHeading + "\n"))) {
			parser.parse(reader);
		}

		Changelog changelog = parser.getChangelog();
		Version version = changelog.getReleasedVersions().isEmpty()
				? changelog.getUnreleasedVersion()
				: changelog.getReleasedVersions().get(0);

		assertThat(version.getName()).isEqualTo(StringUtils.substringBetween(versionHeading, "[", "]"));
		assertThat(version.getLink()).isEqualTo(expectedLink);
		assertThat(version.getReleaseDate()).isEqualTo(expectedReleaseDate);

		assertThat(print(changelog)).isEqualTo("# Changelog\n\n" + versionHeading + "\n");
	}

	/**
	 * An unclosed "(" is not a link, so the rest of the heading is still read as the release date.
	 */
	@Test
	void testParse_versionHeadingWithUnclosedLink() throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader("# Changelog\n\n## [1.0.0](2019-02-15\n"))) {
			parser.parse(reader);
		}

		Version version = parser.getChangelog().getReleasedVersions().get(0);
		assertThat(version.getLink()).isNull();
		assertThat(version.getReleaseDate()).isEqualTo("(2019-02-15");
	}

	/**
	 * Markdown only makes a link out of "(...)" when it follows the closing bracket immediately, so
	 * a parenthetical separated from it by whitespace stays literal text. It is read as the release
	 * date, and the printer writes its canonical separator in front of it - the same normalisation
	 * any other non-dash leading content gets - rather than gluing it to the bracket as a link.
	 */
	@Test
	void testParse_versionHeadingWithParentheticalIsNotALink() throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader("# Changelog\n\n## [1.0.0] (yanked) - 2019-02-15\n"))) {
			parser.parse(reader);
		}

		Changelog changelog = parser.getChangelog();

		Version version = changelog.getReleasedVersions().get(0);
		assertThat(version.getLink()).isNull();
		assertThat(version.getReleaseDate()).isEqualTo("(yanked) - 2019-02-15");

		assertThat(print(changelog)).isEqualTo("# Changelog\n\n## [1.0.0] - (yanked) - 2019-02-15\n");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"## [1.0.0] - 2019-02-15 | 2019-02-15",
			"## [1.0.0] – 2019-02-15 | 2019-02-15",
			"## [1.0.0] — 2019-02-15 | 2019-02-15",
			"## [1.0.0] − 2019-02-15 | 2019-02-15",
			"## [1.0.0] 2019-02-15   | 2019-02-15",
			"'## [1.0.0] - 2019-02-15   ' | 2019-02-15",
			"## [1.0.0](https://example.com/compare/v0.9...v1.0.0) - 2019-02-15 | 2019-02-15",
			"## [0.5.0] - [SNAPSHOT] | [SNAPSHOT]",
			"## [0.0.5] - 2014-12-13 [YANKED] | 2014-12-13 [YANKED]"
	})
	void testParse_releaseDateSeparators(String versionHeading, String expectedReleaseDate) throws Exception {

		ChangelogParser parser = new ChangelogParser();

		try (BufferedReader reader = new BufferedReader(new StringReader("# Changelog\n\n" + versionHeading + "\n"))) {
			parser.parse(reader);
		}

		Changelog changelog = parser.getChangelog();
		Version version = changelog.getReleasedVersions().isEmpty()
				? changelog.getUnreleasedVersion()
				: changelog.getReleasedVersions().get(0);

		assertThat(version.getName()).isEqualTo(StringUtils.substringBetween(versionHeading, "[", "]"));
		assertThat(version.getReleaseDate()).isEqualTo(expectedReleaseDate);
	}

	private static String print(Changelog changelog) throws Exception {
		StringWriter stringWriter = new StringWriter();
		try (BufferedWriter writer = new BufferedWriter(stringWriter)) {
			new ChangelogPrinter().print(changelog, writer);
		}
		return stringWriter.toString().replace(System.lineSeparator(), "\n");
	}

	private static final String CHANGELOG_WITH_NON_VERSION_HEADING = """
			# Changelog

			## [Unreleased]

			### Added

			- Something.

			## [1.0.0] - 2019-02-15

			### Added

			- Everything.

			## Older versions

			See the v0.9 tag.

			## Notes [see #12]

			Bracketed prose stays too.
			""";

	private static final String CHANGELOG_TO_PARSE = """
			# Changelog

			All notable changes to this project will be documented in this file.

			The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
			and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

			## [1.2.0] - SNAPSHOT

			### Added

			- v1.1 Brazilian Portuguese translation.
			- v1.1 German Translation

			### Changed

			- Use frontmatter title & description in each language version template
			- Replace broken OpenGraph image with an appropriately-sized Keep a Changelog
			  image that will render properly (although in English for all languages)

			### Removed

			- Trademark sign previously shown after the project description in version
			0.3.0

			## [1.1.1] - 2023-03-05

			General version description.

			### Added

			- Arabic translation (#444).
			- v1.1 French translation.

			### Fixed

			- Improve French translation (#377).
			- Improve id-ID translation (#416).

			### Changed

			- Upgrade dependencies: Ruby 3.2.1, Middleman, etc.

			### Removed

			- Unused normalize.css file.
			- Identical links assigned in each translation file.

			## [1.1.0] - 2019-02-15

			### Added

			- Danish translation (#297).
			- Georgian translation from (#337).

			### Fixed

			- Italian translation (#332).
			- Indonesian translation (#336).
			""";
}
