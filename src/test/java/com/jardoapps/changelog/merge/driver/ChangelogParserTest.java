package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

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
