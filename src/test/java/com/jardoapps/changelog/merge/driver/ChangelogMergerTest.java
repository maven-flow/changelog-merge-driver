package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jardoapps.changelog.merge.driver.Changelog.Version;

class ChangelogMergerTest {

	private ChangelogMerger changelogMerger = new ChangelogMerger();

	@Test
	void testMerge() {

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.headerLine("Header line 1")
				.headerLine("Header line 2")
				.unreleasedVersion(Changelog.Version.builder()
						.name("Unreleased")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("Line U1")
								.line("Line U2")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.0.0")
						.releaseDate("2020-01-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("Line A1")
								.line("Line A2")
								.build())
						.build())
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.headerLine("Header line 1")
				.headerLine("Header line 2")
				.unreleasedVersion(Changelog.Version.builder()
						.name("Unreleased")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("Line U1")
								.line("Line U2")
								.line("Line U3")
								.line("Line U4")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.2.0")
						.releaseDate("2020-02-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("Line C1")
								.line("Line C2")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("2020-02-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("Line B1")
								.line("Line B2")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.0.0")
						.releaseDate("2020-01-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("Line A1")
								.line("Line A2")
								.build())
						.build())
				.build();

		// Merge the changelogs

		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		assertThat(mergedChangelog.getName()).isEqualTo("Changelog");
		assertThat(mergedChangelog.getHeaderLines()).containsExactly("Header line 1", "Header line 2");

		// Check the unreleased version

		Version unreleasedVersion = mergedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getName()).isEqualTo("Unreleased");
		assertThat(unreleasedVersion.getSections()).hasSize(1);
		assertThat(unreleasedVersion.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly(
				"Line U1",
				"Line U2",
				"Line B1",
				"Line B2",
				"Line C1",
				"Line C2",
				"Line U3",
				"Line U4");

		// Check the released versions

		assertThat(mergedChangelog.getReleasedVersions()).extracting(Version::getName).containsExactly("1.2.0", "1.1.0", "1.0.0");
	}

	@Test
	void testMergeVersions() {

		Changelog.Version ourVersion = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("Line 1")
						.line("Line 2")
						.build())
				.section(Changelog.Section.builder()
						.name("Changed")
						.line("Change 1")
						.line("Change 2")
						.build())
				.build();

		Changelog.Version theirVersion = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("Line 1")
						.line("Line 2")
						.line("Line 3")
						.line("Line 4")
						.build())
				.section(Changelog.Section.builder()
						.name("Fixed")
						.line("Fix 1")
						.line("Fix 2")
						.build())
				.build();

		Changelog.Version mergedVersion = changelogMerger.mergeVersions(ourVersion, theirVersion);

		assertThat(mergedVersion.getName()).isEqualTo("1.0.0");
		assertThat(mergedVersion.getReleaseDate()).isEqualTo("2020-01-01");
		assertThat(mergedVersion.getSections()).hasSize(3);
		assertThat(mergedVersion.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(mergedVersion.getSections().get(0).getLines()).containsExactly("Line 1", "Line 2", "Line 3", "Line 4");
		assertThat(mergedVersion.getSections().get(1).getName()).isEqualTo("Changed");
		assertThat(mergedVersion.getSections().get(1).getLines()).containsExactly("Change 1", "Change 2");
		assertThat(mergedVersion.getSections().get(2).getName()).isEqualTo("Fixed");
		assertThat(mergedVersion.getSections().get(2).getLines()).containsExactly("Fix 1", "Fix 2");
	}

	@Test
	void testMergeSections() {

		Changelog.Section ourSection = Changelog.Section.builder()
				.name("Section")
				.line("Line 1")
				.line("Line 2")
				.build();

		Changelog.Section theirSection = Changelog.Section.builder()
				.name("Section")
				.line("Line 1")
				.line("Line 2")
				.line("Line 3")
				.line("Line 4")
				.build();

		Changelog.Section mergedSection = changelogMerger.mergeSections(ourSection, theirSection);

		assertThat(mergedSection.getName()).isEqualTo("Section");
		assertThat(mergedSection.getLines()).containsExactly("Line 1", "Line 2", "Line 3", "Line 4");
	}
}
