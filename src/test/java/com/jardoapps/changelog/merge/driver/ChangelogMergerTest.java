package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
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
								.line("")
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
								.line("")
								.line("- Fix 1")
								.line("- Fix 2")
								.build())
						.build())
				.build();

		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		Version unreleasedVersion = mergedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getSections()).hasSize(1);
		assertThat(unreleasedVersion.getSections().get(0).getName()).isEqualTo("Fixed");
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly("", "- Fix 2");
	}

	/**
	 * Model situation:
	 *   <ul>
	 *     <li>Maintaining 2 versions: 1.0.x and 2.0.x</li>
	 *     <li>A fix "Fix 1" has been made in version 1.0.1-SNAPSHOT and merged to 2.0.1-SNAPSHOT</li>
	 *     <li>Version 2.0.1 has been released (but 1.0.1 still has not)</li>
	 *     <li>Fix "Fix 1" should not be repeated again in the unreleased section of ours.
	 *         The "Fixed" section in ours would be blank, and therefore should not be added.
	 *     </li>
	 *   </ul>
	 */
	@Test
	void testMerge_dontAddBlankSections() {

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
								.line("")
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
								.line("")
								.line("- Fix 1")
								.build())
						.build())
				.build();

		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		Version unreleasedVersion = mergedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getSections()).hasSize(0);
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

	/**
	 * A level 2 heading without a bracketed version name (e.g. "## Older versions") is parsed as
	 * content of the enclosing section and must survive a full parse - merge - print cycle.
	 */
	@Test
	void testMerge_nonVersionHeadingInSectionContent() throws Exception {

		String ourChangelog = """
				# Changelog

				## [Unreleased]

				### Added

				- Ours.

				## [1.0.0] - 2019-02-15

				### Added

				- Everything.

				## Older versions

				See the v0.9 tag.
				""";

		String theirChangelog = """
				# Changelog

				## [Unreleased]

				### Added

				- Theirs.
				""";

		Changelog mergedChangelog = changelogMerger.merge(parse(ourChangelog), parse(theirChangelog));

		assertThat(print(mergedChangelog)).isEqualTo("""
				# Changelog

				## [Unreleased]

				### Added

				- Ours.
				- Theirs.

				## [1.0.0] - 2019-02-15

				### Added

				- Everything.

				## Older versions

				See the v0.9 tag.
				""");
	}

	private static Changelog parse(String content) throws Exception {
		ChangelogParser parser = new ChangelogParser();
		try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
			parser.parse(reader);
		}
		return parser.getChangelog();
	}

	private static String print(Changelog changelog) throws Exception {
		StringWriter stringWriter = new StringWriter();
		try (BufferedWriter writer = new BufferedWriter(stringWriter)) {
			new ChangelogPrinter().print(changelog, writer);
		}
		return stringWriter.toString().replace(System.lineSeparator(), "\n");
	}

	@Test
	void testMerge_withoutOurUnreleasedVersion() {

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1"))
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(fixedSectionVersion("Unreleased", null, "- Fix 1", "- Fix 2"))
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1"))
				.build();

		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		assertThat(mergedChangelog.getUnreleasedVersion().getSections().get(0).getLines()).containsExactly("- Fix 2");
	}

	@Test
	void testMerge_withoutAnyUnreleasedVersion() {

		Changelog changelog = Changelog.builder()
				.name("Changelog")
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1"))
				.build();

		Changelog mergedChangelog = changelogMerger.merge(changelog, changelog);

		assertThat(mergedChangelog.getUnreleasedVersion()).isNull();
		assertThat(mergedChangelog.getReleasedVersions()).extracting(Version::getName).containsExactly("1.0.0");
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

	@Test
	void testRebase() {

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.headerLine("Header line 1")
				.headerLine("Header line 2")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Their feature 1")
								.line("- Their feature 2")
								.line("- Our feature 1")
								.line("- Our feature 2")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.0.0")
						.releaseDate("2020-01-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Feature 1")
								.line("- Feature 2")
								.build())
						.build())
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.headerLine("Header line 1")
				.headerLine("Header line 2")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.2.0")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Their feature 3")
								.line("- Their feature 4")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("2020-02-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Their feature 1")
								.line("- Their feature 2")
								.build())
						.build())
				.releasedVersion(Changelog.Version.builder()
						.name("1.0.0")
						.releaseDate("2020-01-01")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Feature 1")
								.line("- Feature 2")
								.build())
						.build())
				.build();

		// Perform rebase

		Changelog rebasedChangelog = changelogMerger.rebase(ourChangelog, theirChangelog);

		// Check result

		assertThat(rebasedChangelog.getName()).isEqualTo("Changelog");
		assertThat(rebasedChangelog.getHeaderLines()).containsExactly("Header line 1", "Header line 2");

		// Check the unreleased version

		Version unreleasedVersion = rebasedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getName()).isEqualTo("1.2.0");
		assertThat(unreleasedVersion.getReleaseDate()).isEqualTo("[SNAPSHOT]");
		assertThat(unreleasedVersion.getSections()).hasSize(1);
		assertThat(unreleasedVersion.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly(
				"- Their feature 3",
				"- Their feature 4",
				"- Our feature 1",
				"- Our feature 2");

		// Check the released versions

		assertThat(rebasedChangelog.getReleasedVersions()).extracting(Version::getName).containsExactly("1.1.0", "1.0.0");
	}

	@Test
	void testRebase_preserveSectionOrder() {

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.headerLine("Header line 1")
				.headerLine("Header line 2")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Our feature 1")
								.line("- Our feature 2")
								.build())
						.section(Changelog.Section.builder()
								.name("Changed")
								.line("- Our change 1")
								.line("- Our change 2")
								.build())
						.build())
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.headerLine("Header line 1")
				.headerLine("Header line 2")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Changed")
								.line("- Their change 1")
								.line("- Their change 2")
								.build())
						.build())
				.build();

		// Perform rebase

		Changelog rebasedChangelog = changelogMerger.rebase(ourChangelog, theirChangelog);

		// Check the unreleased version

		Version unreleasedVersion = rebasedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getSections()).hasSize(2);
		assertThat(unreleasedVersion.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly(
				"- Our feature 1",
				"- Our feature 2");
		assertThat(unreleasedVersion.getSections().get(1).getName()).isEqualTo("Changed");
		assertThat(unreleasedVersion.getSections().get(1).getLines()).containsExactly(
				"- Their change 1",
				"- Their change 2",
				"- Our change 1",
				"- Our change 2");
	}

	@Test
	void testRebase_withoutOurUnreleasedVersion() {

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1"))
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(fixedSectionVersion("Unreleased", null, "- Fix 2"))
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1"))
				.build();

		Changelog rebasedChangelog = changelogMerger.rebase(ourChangelog, theirChangelog);

		assertThat(rebasedChangelog.getUnreleasedVersion().getSections().get(0).getLines()).containsExactly("- Fix 2");
	}

	@Test
	void testMergeReleasedVersion_theirFixIsApplied() {

		Version base = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 with tpyo", "- Fix 2");
		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 with tpyo", "- Fix 2");
		Version their = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 with typo", "- Fix 2");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getName()).isEqualTo("1.0.0");
		assertThat(merged.getReleaseDate()).isEqualTo("2020-01-01");
		assertThat(merged.getSections()).hasSize(1);
		assertThat(merged.getSections().get(0).getName()).isEqualTo("Fixed");
		assertThat(merged.getSections().get(0).getLines()).containsExactly("- Fix 1 with typo", "- Fix 2");
	}

	@Test
	void testMergeReleasedVersion_ourFixIsKept() {

		Version base = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 with tpyo", "- Fix 2");
		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 with typo", "- Fix 2");
		Version their = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 with tpyo", "- Fix 2");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getSections().get(0).getLines()).containsExactly("- Fix 1 with typo", "- Fix 2");
	}

	@Test
	void testMergeReleasedVersion_identicalVersionIsKept() {

		Version base = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1");
		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1");
		Version their = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged).isSameAs(our);
	}

	@Test
	void testMergeReleasedVersion_conflictingChangeIsTakenFromTheirs() {

		Version base = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1", "- Fix 2");
		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 changed by us", "- Fix 2");
		Version their = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 changed by them", "- Fix 2");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getSections().get(0).getLines()).containsExactly("- Fix 1 changed by them", "- Fix 2");
	}

	@Test
	void testMerge_releasedVersionMissingInBaseIsKeptFromOurs() {

		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 changed by us");
		Version their = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 changed by them");

		Changelog mergedChangelog = changelogMerger.merge(
				headerOnlyChangelog("Changelog"),
				Changelog.builder().name("Changelog").releasedVersion(our).build(),
				Changelog.builder().name("Changelog").releasedVersion(their).build());

		assertThat(mergedChangelog.getReleasedVersions()).containsExactly(our);
	}

	@Test
	void testRebase_releasedVersionWithoutBaseIsKeptFromTheirs() {

		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 changed by us");
		Version their = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1 changed by them");

		Changelog rebasedChangelog = changelogMerger.rebase(
				Changelog.builder().name("Changelog").releasedVersion(our).build(),
				Changelog.builder().name("Changelog").releasedVersion(their).build());

		assertThat(rebasedChangelog.getReleasedVersions()).containsExactly(their);
	}

	@Test
	void testMergeReleasedVersion_sectionAddedByTheirs() {

		Version base = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1");
		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1");
		Version their = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Fixed")
						.line("- Fix 1")
						.build())
				.section(Changelog.Section.builder()
						.name("Security")
						.line("- Security fix 1")
						.build())
				.build();

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getSections()).hasSize(2);
		assertThat(merged.getSections().get(0).getName()).isEqualTo("Fixed");
		assertThat(merged.getSections().get(0).getLines()).containsExactly("- Fix 1");
		assertThat(merged.getSections().get(1).getName()).isEqualTo("Security");
		assertThat(merged.getSections().get(1).getLines()).containsExactly("- Security fix 1");
	}

	@Test
	void testMergeReleasedVersion_descriptionFixIsApplied() {

		Version base = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.headerLine("")
				.headerLine("Description with tpyo.")
				.build();
		Version our = base;
		Version their = Changelog.Version.builder()
				.name("1.0.0")
				.releaseDate("2020-01-01")
				.headerLine("")
				.headerLine("Description with typo.")
				.build();

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getHeaderLines()).containsExactly("", "Description with typo.");
		assertThat(merged.getSections()).isEmpty();
	}

	@Test
	void testMergeReleasedVersion_releaseDateCorrectedByTheirs() {

		Version base = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1");
		Version our = fixedSectionVersion("1.0.0", "2020-01-01", "- Fix 1");
		Version their = fixedSectionVersion("1.0.0", "2020-01-02", "- Fix 1");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getReleaseDate()).isEqualTo("2020-01-02");
	}

	@Test
	void testMerge_changesInSharedReleasedVersionsAreMerged() {

		Changelog baseChangelog = Changelog.builder()
				.name("Changelog")
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix with tpyo"))
				.build();

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(Changelog.Version.builder()
						.name("Unreleased")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Line U1")
								.build())
						.build())
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix with tpyo"))
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.releasedVersion(fixedSectionVersion("1.1.0", "2020-02-01", "- Fix 2"))
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix with typo"))
				.build();

		Changelog mergedChangelog = changelogMerger.merge(baseChangelog, ourChangelog, theirChangelog);

		assertThat(mergedChangelog.getReleasedVersions()).extracting(Version::getName).containsExactly("1.1.0", "1.0.0");
		assertThat(mergedChangelog.getReleasedVersions().get(1).getSections().get(0).getLines()).containsExactly("- Fix with typo");

		Version unreleasedVersion = mergedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getSections()).hasSize(2);
		assertThat(unreleasedVersion.getSections().get(0).getName()).isEqualTo("Added");
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly("- Line U1");
		assertThat(unreleasedVersion.getSections().get(1).getName()).isEqualTo("Fixed");
		assertThat(unreleasedVersion.getSections().get(1).getLines()).containsExactly("- [from `1.1.0`] Fix 2");
	}

	@Test
	void testRebase_ourFixInReleasedVersionIsKept() {

		Changelog baseChangelog = Changelog.builder()
				.name("Changelog")
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix with tpyo"))
				.build();

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.1.0")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Our feature")
								.build())
						.build())
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix with typo"))
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(Changelog.Version.builder()
						.name("1.2.0")
						.releaseDate("[SNAPSHOT]")
						.section(Changelog.Section.builder()
								.name("Added")
								.line("- Their feature")
								.build())
						.build())
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Fix with tpyo"))
				.build();

		Changelog rebasedChangelog = changelogMerger.rebase(baseChangelog, ourChangelog, theirChangelog);

		assertThat(rebasedChangelog.getReleasedVersions()).extracting(Version::getName).containsExactly("1.0.0");
		assertThat(rebasedChangelog.getReleasedVersions().get(0).getSections().get(0).getLines()).containsExactly("- Fix with typo");

		Version unreleasedVersion = rebasedChangelog.getUnreleasedVersion();
		assertThat(unreleasedVersion.getName()).isEqualTo("1.2.0");
		assertThat(unreleasedVersion.getSections()).hasSize(1);
		assertThat(unreleasedVersion.getSections().get(0).getLines()).containsExactly("- Their feature", "- Our feature");
	}

	@Test
	void testRebase_lineAddedToReleasedVersionByUsIsRemovedFromUnreleased() {

		Changelog baseChangelog = Changelog.builder()
				.name("Changelog")
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Old fix"))
				.build();

		Changelog ourChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(fixedSectionVersion("1.1.0", "[SNAPSHOT]", "- Fix X"))
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Old fix", "- Fix X"))
				.build();

		Changelog theirChangelog = Changelog.builder()
				.name("Changelog")
				.unreleasedVersion(fixedSectionVersion("1.1.0", "[SNAPSHOT]", "- Their fix"))
				.releasedVersion(fixedSectionVersion("1.0.0", "2020-01-01", "- Old fix"))
				.build();

		Changelog rebasedChangelog = changelogMerger.rebase(baseChangelog, ourChangelog, theirChangelog);

		assertThat(rebasedChangelog.getReleasedVersions().get(0).getSections().get(0).getLines()).containsExactly("- Old fix", "- Fix X");
		assertThat(rebasedChangelog.getUnreleasedVersion().getSections().get(0).getLines()).containsExactly("- Their fix");
	}

	@Test
	void testMerge_theirHeaderFixIsApplied() {

		Changelog baseChangelog = headerOnlyChangelog("Changelog", "", "Description with tpyo.");
		Changelog ourChangelog = headerOnlyChangelog("Changelog", "", "Description with tpyo.");
		Changelog theirChangelog = headerOnlyChangelog("Changelog", "", "Description with typo.");

		Changelog mergedChangelog = changelogMerger.merge(baseChangelog, ourChangelog, theirChangelog);

		assertThat(mergedChangelog.getName()).isEqualTo("Changelog");
		assertThat(mergedChangelog.getHeaderLines()).containsExactly("", "Description with typo.");
	}

	@Test
	void testMerge_ourHeaderFixIsKept() {

		Changelog baseChangelog = headerOnlyChangelog("Changelog", "", "Description with tpyo.");
		Changelog ourChangelog = headerOnlyChangelog("Changelog", "", "Description with typo.");
		Changelog theirChangelog = headerOnlyChangelog("Changelog", "", "Description with tpyo.");

		Changelog mergedChangelog = changelogMerger.merge(baseChangelog, ourChangelog, theirChangelog);

		assertThat(mergedChangelog.getHeaderLines()).containsExactly("", "Description with typo.");
	}

	@Test
	void testMerge_captionRenamedByTheirs() {

		Changelog baseChangelog = headerOnlyChangelog("Changelog");
		Changelog ourChangelog = headerOnlyChangelog("Changelog");
		Changelog theirChangelog = headerOnlyChangelog("My Project Changelog");

		Changelog mergedChangelog = changelogMerger.merge(baseChangelog, ourChangelog, theirChangelog);

		assertThat(mergedChangelog.getName()).isEqualTo("My Project Changelog");
	}

	@Test
	void testMerge_headerWithoutBaseIsKeptFromOurs() {

		Changelog ourChangelog = headerOnlyChangelog("Changelog", "Our description.");
		Changelog theirChangelog = headerOnlyChangelog("Their Changelog", "Their description.");

		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		assertThat(mergedChangelog.getName()).isEqualTo("Changelog");
		assertThat(mergedChangelog.getHeaderLines()).containsExactly("Our description.");
	}

	@Test
	void testRebase_ourHeaderFixIsKept() {

		Changelog baseChangelog = headerOnlyChangelog("Changelog", "", "Description with tpyo.");
		Changelog ourChangelog = headerOnlyChangelog("Changelog", "", "Description with typo.");
		Changelog theirChangelog = headerOnlyChangelog("Changelog", "", "Description with tpyo.");

		Changelog rebasedChangelog = changelogMerger.rebase(baseChangelog, ourChangelog, theirChangelog);

		assertThat(rebasedChangelog.getHeaderLines()).containsExactly("", "Description with typo.");
	}

	/**
	 * The version heading form Keep a Changelog recommends must survive all three modes: the link is
	 * part of the heading, not of the version content, so it round-trips regardless of the merge.
	 */
	@Test
	void testMerge_linkedVersionHeadingIsKept() throws Exception {

		Changelog baseChangelog = parse(LINKED_HEADING_CHANGELOG);
		Changelog ourChangelog = parse(LINKED_HEADING_CHANGELOG);
		Changelog theirChangelog = parse(LINKED_HEADING_CHANGELOG);

		assertThat(print(changelogMerger.merge(baseChangelog, ourChangelog, theirChangelog))).isEqualTo(LINKED_HEADING_CHANGELOG);
		assertThat(print(changelogMerger.rebase(baseChangelog, ourChangelog, theirChangelog))).isEqualTo(LINKED_HEADING_CHANGELOG);
		assertThat(print(changelogMerger.merge(ourChangelog, theirChangelog))).isEqualTo(LINKED_HEADING_CHANGELOG);
		assertThat(print(changelogMerger.rebase(ourChangelog, theirChangelog))).isEqualTo(LINKED_HEADING_CHANGELOG);
	}

	/**
	 * The link of an unreleased version survives the rewrites the unreleased version goes through
	 * while entries are merged into it.
	 */
	@Test
	void testMerge_linkedUnreleasedHeadingIsKeptWhileEntriesAreMerged() throws Exception {

		String ourChangelog = """
				# Changelog

				## [Unreleased](https://example.com/compare/v1.0.0...HEAD)

				### Added

				- Ours.
				""";

		String theirChangelog = """
				# Changelog

				## [Unreleased](https://example.com/compare/v1.0.0...HEAD)

				### Added

				- Theirs.
				""";

		Changelog mergedChangelog = changelogMerger.merge(parse(ourChangelog), parse(theirChangelog));

		assertThat(print(mergedChangelog)).isEqualTo("""
				# Changelog

				## [Unreleased](https://example.com/compare/v1.0.0...HEAD)

				### Added

				- Ours.
				- Theirs.
				""");
	}

	@Test
	void testMergeReleasedVersion_linkIsKeptWhileContentIsMerged() {

		Version base = linkedVersion("1.0.0", COMPARE_LINK, "- Fix 1 with tpyo");
		Version our = linkedVersion("1.0.0", COMPARE_LINK, "- Fix 1 with tpyo");
		Version their = linkedVersion("1.0.0", COMPARE_LINK, "- Fix 1 with typo");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getLink()).isEqualTo(COMPARE_LINK);
		assertThat(merged.getSections().get(0).getLines()).containsExactly("- Fix 1 with typo");
	}

	@Test
	void testMergeReleasedVersion_linkCorrectedByTheirs() {

		Version base = linkedVersion("1.0.0", "https://example.com/compare/v0.9.0...v1.0.0", "- Fix 1");
		Version our = linkedVersion("1.0.0", "https://example.com/compare/v0.9.0...v1.0.0", "- Fix 1");
		Version their = linkedVersion("1.0.0", "https://example.com/releases/v1.0.0", "- Fix 1");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getLink()).isEqualTo("https://example.com/releases/v1.0.0");
	}

	@Test
	void testMergeReleasedVersion_ourLinkIsKept() {

		Version base = linkedVersion("1.0.0", null, "- Fix 1");
		Version our = linkedVersion("1.0.0", COMPARE_LINK, "- Fix 1");
		Version their = linkedVersion("1.0.0", null, "- Fix 1");

		Version merged = changelogMerger.mergeReleasedVersion(base, our, their);

		assertThat(merged.getLink()).isEqualTo(COMPARE_LINK);
	}

	private static final String COMPARE_LINK = "https://example.com/compare/v0.9.0...v1.0.0";

	private static final String LINKED_HEADING_CHANGELOG = """
			# Changelog

			## [Unreleased](https://example.com/compare/v1.0.0...HEAD)

			### Added

			- Something.

			## [1.0.0](https://example.com/compare/v0.9.0...v1.0.0) - 2019-02-15

			### Added

			- Everything.
			""";

	private static Version linkedVersion(String name, String link, String... fixedSectionLines) {
		return Changelog.Version.builder()
				.name(name)
				.link(link)
				.releaseDate("2020-01-01")
				.section(Changelog.Section.builder()
						.name("Fixed")
						.lines(List.of(fixedSectionLines))
						.build())
				.build();
	}

	private static Changelog headerOnlyChangelog(String name, String... headerLines) {
		return Changelog.builder()
				.name(name)
				.headerLines(List.of(headerLines))
				.unreleasedVersion(Changelog.Version.builder()
						.name("Unreleased")
						.build())
				.build();
	}

	private static Version fixedSectionVersion(String name, String releaseDate, String... fixedSectionLines) {
		return Changelog.Version.builder()
				.name(name)
				.releaseDate(releaseDate)
				.section(Changelog.Section.builder()
						.name("Fixed")
						.lines(List.of(fixedSectionLines))
						.build())
				.build();
	}

}
