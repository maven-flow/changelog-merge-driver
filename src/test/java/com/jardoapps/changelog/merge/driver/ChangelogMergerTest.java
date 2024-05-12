package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
								.line("- Line U1")
								.line("- Line U2")
								.line("- Line released later")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.0.0")
						.releaseDate("2020-01-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Line A1")
								.line("- Line A2")
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
								.line("- Line U1")
								.line("- Line U2")
								.line("- Line U3")
								.line("- Line U4")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.2.0")
						.releaseDate("2020-02-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Line C1")
								.line("- Line C2")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("2020-02-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Line B1")
								.line("- Line B2")
								.line("- Line released later")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.0.0")
						.releaseDate("2020-01-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Line A1")
								.line("- Line A2")
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
				"- Line U1",
				"- Line U2",
				"- [from `1.1.0`] Line released later",
				"- [from `1.1.0`] Line B1",
				"- [from `1.1.0`] Line B2",
				"- [from `1.2.0`] Line C1",
				"- [from `1.2.0`] Line C2",
				"- Line U3",
				"- Line U4");

		// Check the released versions

		assertThat(mergedChangelog.getReleasedVersions()).extracting(Version::getName).containsExactly("1.2.0", "1.1.0", "1.0.0");
	}

	/**
	 * Model situation:
	 *   <ul>
	 *     <li>Maintaining 2 versions: 1.0.x and 2.0.x</li>
	 *     <li>A fix "Fix 1" has been made in version 1.0.1-SNAPSHOT and merged to 2.0.1-SNAPSHOT</li>
	 *     <li>Version 2.0.1 has been released (but 1.0.1 still has not)</li>
	 *     <li>Another fix "Fix 2" has been made in version 1.0.1-SNAPSHOT (theirs) and is now being merged to 2.0.2-SNAPSHOT (ours)</li>
	 *     <li>Fix "Fix 1" should not be repeated again in the unreleased section of ours</li>
	 *   </ul>
	 */
	@Test
	void testMerge_unreleasedItemDuplication() {

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(Changelog.Version.builder()
						.name("2.0.2")
						.releaseDate("[SNAPSHOT]")
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("2.0.1")
						.releaseDate("2024-04-12")
						.section(Changelog.Section.builder()
								.name("Fixed")
								.line("- Fix 1")
								.build())
						.build())
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.0.1")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Fixed")
								.line("- Fix 1")
								.line("- Fix 2")
								.build())
						.build())
				.build();

		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		Version unreleasedVersion = mergedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getSections()).hasSize(1);
		assertThat(unreleasedVersion.getSections().get(0).getName()).isEqualTo("Fixed");
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly("- Fix 2");
	}

	/**
	 * Model situation:
	 *   <ul>
	 *     <li>Maintaining 3 versions: 1.0.0, 2.0.0, 3.0.0</li>
	 *     <li>A fix has been made in version 1.0.1-SNAPSHOT and merged to all other versions.</li>
	 *     <li>Version 1.0.1 has been released and merged to 2.0.1-SNAPSHOT. The fix in 2.0.1-SNAPSHOT has been marked with [from `1.0.1`].</li>
	 *     <li>Changes from 2.0.1-SNAPSHOT are now being merged to 3.0.1-SNAPSHOT</li>
	 *   </ul>
	 */
	@Test
	void testMerge_mergingAddedFromLabel() {

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(Changelog.Version.builder()
						.name("3.0.1")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Fixed")
								.line("- Fix in 1.0.1")
								.build())
						.build())
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(Changelog.Version.builder()
						.name("2.0.1")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Fixed")
								.line("- [from `1.0.1`] Fix in 1.0.1")
								.build())
						.build())
				.build();

		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		Version unreleasedVersion = mergedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getName()).isEqualTo("3.0.1");
		assertThat(unreleasedVersion.getReleaseDate()).isEqualTo("[SNAPSHOT]");
		assertThat(unreleasedVersion.getSections()).hasSize(1);
		assertThat(unreleasedVersion.getSections().get(0).getName()).isEqualTo("Fixed");
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly("- [from `1.0.1`] Fix in 1.0.1");
	}

	@Test
	void testMergeVersions() {

		Changelog.Version ourVersion = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("- Line 1")
						.line("- Line 2")
						.build())
				.section(Changelog.Section.builder()
						.name("Changed")
						.line("- Change 1")
						.line("- Change 2")
						.build())
				.build();

		Changelog.Version theirVersion = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("- Line 1")
						.line("- Line 2")
						.line("- Line 3")
						.line("- Line 4")
						.build())
				.section(Changelog.Section.builder()
						.name("Fixed")
						.line("- Fix 1")
						.line("- Fix 2")
						.build())
				.build();

		Changelog.Version mergedVersion = changelogMerger.mergeVersions(ourVersion, theirVersion, false);

		assertThat(mergedVersion.getName()).isEqualTo("1.0.0");
		assertThat(mergedVersion.getReleaseDate()).isEqualTo("2020-01-01");
		assertThat(mergedVersion.getSections()).hasSize(3);
		assertThat(mergedVersion.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(mergedVersion.getSections().get(0).getLines()).containsExactly("- Line 1", "- Line 2", "- Line 3", "- Line 4");
		assertThat(mergedVersion.getSections().get(1).getName()).isEqualTo("Changed");
		assertThat(mergedVersion.getSections().get(1).getLines()).containsExactly("- Change 1", "- Change 2");
		assertThat(mergedVersion.getSections().get(2).getName()).isEqualTo("Fixed");
		assertThat(mergedVersion.getSections().get(2).getLines()).containsExactly("- Fix 1", "- Fix 2");
	}

	@Test
	void testMergeVersions_withFromLabel() {

		Changelog.Version ourVersion = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("- Line 1")
						.line("- Line 2")
						.build())
				.section(Changelog.Section.builder()
						.name("Changed")
						.line("- Change 1")
						.line("- Change 2")
						.build())
				.build();

		Changelog.Version theirVersion = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("- Line 1")
						.line("- Line 2")
						.line("- Line 3")
						.line("- Line 4")
						.build())
				.section(Changelog.Section.builder()
						.name("Fixed")
						.line("- Fix 1")
						.line("- Fix 2")
						.build())
				.build();

		Changelog.Version mergedVersion = changelogMerger.mergeVersions(ourVersion, theirVersion, true);

		assertThat(mergedVersion.getName()).isEqualTo("1.0.0");
		assertThat(mergedVersion.getReleaseDate()).isEqualTo("2020-01-01");
		assertThat(mergedVersion.getSections()).hasSize(3);
		assertThat(mergedVersion.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(mergedVersion.getSections().get(0).getLines()).containsExactly("- Line 1", "- Line 2", "- [from `1.0.0`] Line 3", "- [from `1.0.0`] Line 4");
		assertThat(mergedVersion.getSections().get(1).getName()).isEqualTo("Changed");
		assertThat(mergedVersion.getSections().get(1).getLines()).containsExactly("- Change 1", "- Change 2");
		assertThat(mergedVersion.getSections().get(2).getName()).isEqualTo("Fixed");
		assertThat(mergedVersion.getSections().get(2).getLines()).containsExactly("- [from `1.0.0`] Fix 1", "- [from `1.0.0`] Fix 2");
	}

	@Test
	void testMergeSections() {

		Changelog.Section ourSection = Changelog.Section.builder()
				.name("Section")
				.line("- Line 1")
				.line("- Line 2")
				.build();

		Changelog.Section theirSection = Changelog.Section.builder()
				.name("Section")
				.line("- Line 1")
				.line("- Line 2")
				.line("- Line 3")
				.line("- Line 4")
				.build();

		Changelog.Section mergedSection = changelogMerger.mergeSections(ourSection, theirSection, "");

		assertThat(mergedSection.getName()).isEqualTo("Section");
		assertThat(mergedSection.getLines()).containsExactly("- Line 1", "- Line 2", "- Line 3", "- Line 4");
	}

	@Test
	void testMergeSections_withFromLabel() {

		Changelog.Section ourSection = Changelog.Section.builder()
				.name("Section")
				.line("- Line 1")
				.line("- Line 2")
				.build();

		Changelog.Section theirSection = Changelog.Section.builder()
				.name("Section")
				.line("- Line 1")
				.line("- Line 2")
				.line("- Line 3")
				.line("- Line 4")
				.build();

		Changelog.Section mergedSection = changelogMerger.mergeSections(ourSection, theirSection, "[from `1.0.0`] ");

		assertThat(mergedSection.getName()).isEqualTo("Section");
		assertThat(mergedSection.getLines()).containsExactly("- Line 1", "- Line 2", "- [from `1.0.0`] Line 3", "- [from `1.0.0`] Line 4");
	}

	@Test
	void testAddMissingFromLabels() {

		Changelog.Version unreleasedVersion = Changelog.Version.builder()
				.name("3.0.0")
				.releaseDate("Unreleased")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("- Feature 1")
						.line("- Feature 2")
						.line("- Feature 3")
						.line("- Feature 4")
						.line("- Feature 5")
						.line("- Feature 6")
						.line("  second line of feature 6")
						.line("- Feature 7")
						.build())
				.section(Changelog.Section.builder()
						.name("Changed")
						.line("- Change 1")
						.line("- Change 2")
						.line("- Change 3")
						.line("- Change 4")
						.build())
				.section(Changelog.Section.builder()
						.name("Fixed")
						.line("- Fix 1")
						.line("- Fix 2")
						.build())
				.build();

		Changelog.Version releasedVersion2 = Changelog.Version.builder()
				.name("2.0.0")
				.releaseDate("2024-04-12")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("- Feature 5")
						.line("- Feature 6")
						.build())
				.section(Changelog.Section.builder()
						.name("Changed")
						.line("- Change 4")
						.build())
				.section(Changelog.Section.builder()
						.name("Fixed")
						.line("- Fix 3")
						.line("- Fix 4")
						.build())
				.build();

		Changelog.Version releasedVersion1 = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2024-03-12")
				.section(Changelog.Section.builder()
						.name("Added")
						.line("- Feature 3")
						.line("- Feature 4")
						.build())
				.section(Changelog.Section.builder()
						.name("Changed")
						.line("- Change 2")
						.build())
				.section(Changelog.Section.builder()
						.name("Removed")
						.line("- Feature 1")
						.build())
				.build();

		Version result = changelogMerger.addMissingFromLabels(unreleasedVersion, List.of(releasedVersion2, releasedVersion1));

		assertThat(result.getName()).isEqualTo("3.0.0");
		assertThat(result.getReleaseDate()).isEqualTo("Unreleased");
		assertThat(result.getSections()).hasSize(3);
		assertThat(result.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(result.getSections().get(0).getLines()).containsExactly(
				"- Feature 1",
				"- Feature 2",
				"- [from `1.0.0`] Feature 3",
				"- [from `1.0.0`] Feature 4",
				"- [from `2.0.0`] Feature 5",
				"- [from `2.0.0`] Feature 6",
				"  second line of feature 6",
				"- Feature 7"
		);
		assertThat(result.getSections().get(1).getName()).isEqualTo("Changed");
		assertThat(result.getSections().get(1).getLines()).containsExactly(
				"- Change 1",
				"- [from `1.0.0`] Change 2",
				"- Change 3",
				"- [from `2.0.0`] Change 4"
		);
		assertThat(result.getSections().get(2).getName()).isEqualTo("Fixed");
		assertThat(result.getSections().get(2).getLines()).containsExactly(
				"- Fix 1",
				"- Fix 2"
		);
	}
}
