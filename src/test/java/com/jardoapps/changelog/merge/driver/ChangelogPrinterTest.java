package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class ChangelogPrinterTest {

	private ChangelogPrinter changelogPrinter = new ChangelogPrinter();

	@Test
	void testPrint() throws Exception {

		Changelog changelog = Changelog.builder()
				.name("Changelog")
				.headerLine("")
				.headerLine("All notable changes to this project will be documented in this file.")
				.headerLine("")
				.headerLine("The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),")
				.headerLine("and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).")
				.headerLine("")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.2.0")
						.releaseDate("SNAPSHOT")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("")
								.line("- v1.1 Brazilian Portuguese translation.")
								.line("- v1.1 German Translation")
								.line("")
								.build())
						.section(Changelog.Section.builder()
								.name("Changed")
								.line("")
								.line("- Use frontmatter title & description in each language version template")
								.line("- Replace broken OpenGraph image with an appropriately-sized Keep a Changelog")
								.line("  image that will render properly (although in English for all languages)")
								.line("")
								.build())
						.section(Changelog.Section.builder()
								.name("Removed")
								.line("")
								.line("- Trademark sign previously shown after the project description in version")
								.line("0.3.0")
								.line("")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.1.1")
						.releaseDate("2023-03-05")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("")
								.line("- Arabic translation (#444).")
								.line("- v1.1 French translation.")
								.line("")
								.build())
						.section(Changelog.Section.builder()
								.name("Fixed")
								.line("")
								.line("- Improve French translation (#377).")
								.line("- Improve id-ID translation (#416).")
								.line("")
								.build())
						.section(Changelog.Section.builder()
								.name("Changed")
								.line("")
								.line("- Upgrade dependencies: Ruby 3.2.1, Middleman, etc.")
								.line("")
								.build())
						.section(Changelog.Section.builder()
								.name("Removed")
								.line("")
								.line("- Unused normalize.css file.")
								.line("- Identical links assigned in each translation file.")
								.line("")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("2019-02-15")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("")
								.line("- Danish translation (#297).")
								.line("- Georgian translation from (#337).")
								.line("")
								.build())
						.section(Changelog.Section.builder()
								.name("Fixed")
								.line("")
								.line("- Italian translation (#332).")
								.line("- Indonesian translation (#336).")
								.build())
						.build())
				.build();

		// print changelog

		try (StringWriter stringWriter = new StringWriter(); BufferedWriter writer = new BufferedWriter(stringWriter)) {
			changelogPrinter.print(changelog, writer);
			writer.flush();
			String result = stringWriter.toString();
			assertThat(result).isEqualTo(EXPECTED_CHANGELOG);
		}
	}

	private static final String EXPECTED_CHANGELOG = """
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
