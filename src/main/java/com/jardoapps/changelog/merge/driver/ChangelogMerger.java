package com.jardoapps.changelog.merge.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import com.jardoapps.changelog.merge.driver.Changelog.Section;
import com.jardoapps.changelog.merge.driver.Changelog.Section.SectionBuilder;
import com.jardoapps.changelog.merge.driver.Changelog.Version;
import com.jardoapps.changelog.merge.driver.Changelog.Version.VersionBuilder;

public class ChangelogMerger {

	private static final Pattern FROM_LABEL_PATTERN = Pattern.compile("\\[from `.*`\\] ");

	private final ThreeWayLineMerger threeWayLineMerger = new ThreeWayLineMerger();

	public Changelog merge(Changelog our, Changelog their) {
		return merge(our, null, their);
	}

	/**
	 * Merge their changelog into ours. The base changelog is the common ancestor of both (the "%O"
	 * file Git passes to a merge driver); it is consulted for the changelog header and for released
	 * versions present in both changelogs (see
	 * {@link #mergeReleasedVersion(Version, Version, Version)}) and may be null when no parsable
	 * ancestor exists.
	 */
	public Changelog merge(Changelog our, Changelog base, Changelog their) {

		Version unreleasedVersion = our.getUnreleasedVersion();

		Set<String> ourReleasedVersionNames = our.getReleasedVersions().stream().map(Version::getName).collect(Collectors.toSet());
		List<Version> mergedReleasedVersions = new LinkedList<>();

		for (Version ourReleasedVersion : our.getReleasedVersions()) {
			Optional<Version> theirReleasedVersion = findVersionByName(their.getReleasedVersions(), ourReleasedVersion.getName());
			if (theirReleasedVersion.isPresent()) {
				Version baseVersion = base == null ? null : findVersionByName(base.getReleasedVersions(), ourReleasedVersion.getName()).orElse(null);
				mergedReleasedVersions.add(mergeReleasedVersion(ourReleasedVersion, baseVersion, theirReleasedVersion.get()));
			} else {
				mergedReleasedVersions.add(ourReleasedVersion);
			}
		}

		// iterate through versions in reversed order to add newer versions first

		for (int i = their.getReleasedVersions().size() - 1; i >= 0; i--) {

			Version theirReleasedVersion = their.getReleasedVersions().get(i);

			if (!ourReleasedVersionNames.contains(theirReleasedVersion.getName())) {
				mergedReleasedVersions.add(0, theirReleasedVersion);
				if (unreleasedVersion != null) {
					unreleasedVersion = mergeVersions(unreleasedVersion, theirReleasedVersion, true);
				}
			}
		}

		unreleasedVersion = addMissingFromLabels(unreleasedVersion, mergedReleasedVersions);

		// add unreleased changes of theirs to the end of unreleased changes of ours
		unreleasedVersion = mergeVersions(unreleasedVersion, their.getUnreleasedVersion(), false);

		unreleasedVersion = removeDuplicatedUnreleasedLines(unreleasedVersion, mergedReleasedVersions);

		return Changelog.builder()
				.name(mergeChangelogName(our, base, their, our.getName()))
				.headerLines(mergeHeaderLines(our, base, their, our.getHeaderLines()))
				.unreleasedVersion(unreleasedVersion)
				.releasedVersions(mergedReleasedVersions)
				.build();
	}

	public Changelog rebase(Changelog our, Changelog their) {
		return rebase(our, null, their);
	}

	/**
	 * Rebase our changelog on top of their changelog.
	 * <p>
	 * Take their changelog as base, and only add the changes from our unreleased
	 * version into unreleased version from base.
	 * <p>
	 * Released versions present in both changelogs are merged (see
	 * {@link #mergeReleasedVersion(Version, Version, Version)}), so changes made to them on our
	 * side are preserved. The base changelog may be null when no parsable ancestor exists.
	 */
	public Changelog rebase(Changelog our, Changelog base, Changelog their) {

		Version unreleasedVersion = their.getUnreleasedVersion();
		if (unreleasedVersion == null) {
			unreleasedVersion = our.getUnreleasedVersion();
		} else {
			unreleasedVersion = rebaseVersions(our.getUnreleasedVersion(), unreleasedVersion);
			unreleasedVersion = removeDuplicatedUnreleasedLines(unreleasedVersion, their.getReleasedVersions());
		}

		List<Version> rebasedReleasedVersions = new ArrayList<>(their.getReleasedVersions().size());

		for (Version theirReleasedVersion : their.getReleasedVersions()) {
			Optional<Version> ourReleasedVersion = findVersionByName(our.getReleasedVersions(), theirReleasedVersion.getName());
			if (ourReleasedVersion.isPresent()) {
				Version baseVersion = base == null ? null : findVersionByName(base.getReleasedVersions(), theirReleasedVersion.getName()).orElse(null);
				rebasedReleasedVersions.add(mergeReleasedVersion(ourReleasedVersion.get(), baseVersion, theirReleasedVersion));
			} else {
				rebasedReleasedVersions.add(theirReleasedVersion);
			}
		}

		return Changelog.builder()
				.name(mergeChangelogName(our, base, their, their.getName()))
				.headerLines(mergeHeaderLines(our, base, their, their.getHeaderLines()))
				.unreleasedVersion(unreleasedVersion)
				.releasedVersions(rebasedReleasedVersions)
				.build();
	}

	/**
	 * Three-way merge of the changelog name (the caption in the first line of the file): the name
	 * changed by one side wins; changed by both, "theirs" wins, consistently with
	 * {@link #mergeReleasedVersion(Version, Version, Version)}. Without a base to compare against,
	 * the name of the changelog serving as the result's foundation is kept: "ours" when merging,
	 * "theirs" when rebasing.
	 */
	private String mergeChangelogName(Changelog our, Changelog base, Changelog their, String nameWithoutBase) {

		if (base == null) {
			return nameWithoutBase;
		}

		return StringUtils.equals(their.getName(), base.getName()) ? our.getName() : their.getName();
	}

	/**
	 * Three-way merge of the changelog header (the description lines between the caption and the
	 * first version), with the same rules as {@link #mergeReleasedVersion(Version, Version,
	 * Version)}. Without a base to compare against, the header of the changelog serving as the
	 * result's foundation is kept: "ours" when merging, "theirs" when rebasing.
	 */
	private List<String> mergeHeaderLines(Changelog our, Changelog base, Changelog their, List<String> headerLinesWithoutBase) {

		if (base == null) {
			return headerLinesWithoutBase;
		}

		return threeWayLineMerger.merge(base.getHeaderLines(), our.getHeaderLines(), their.getHeaderLines());
	}

	/**
	 * Merge a released version which is present in both changelogs. The version content
	 * (description lines and sections) is merged line-based three-way against the version from the
	 * merge base, so a change made on either side alone (e.g. a typo fix) is preserved. Where both
	 * sides changed the same lines differently, "theirs" wins: for released history, the changelog
	 * being merged in is considered authoritative. Without a base version to compare against, a
	 * content difference is likewise resolved by taking "theirs" completely.
	 */
	Version mergeReleasedVersion(Version our, Version base, Version their) {

		List<String> ourLines = versionContentLines(our);
		List<String> theirLines = versionContentLines(their);

		if (ourLines.equals(theirLines) && StringUtils.equals(our.getReleaseDate(), their.getReleaseDate())) {
			return our;
		}

		if (base == null) {
			return their;
		}

		List<String> mergedLines = threeWayLineMerger.merge(versionContentLines(base), ourLines, theirLines);

		String releaseDate = StringUtils.equals(their.getReleaseDate(), base.getReleaseDate())
				? our.getReleaseDate()
				: their.getReleaseDate();

		return parseVersionContent(our.getName(), releaseDate, mergedLines);
	}

	/**
	 * The version content as the lines the parser read it from: description lines first, then each
	 * section as its heading line followed by its lines. Blank lines are kept by the parser as part
	 * of the surrounding section, so this reconstructs the original file lines of the version,
	 * minus the version heading itself.
	 */
	static List<String> versionContentLines(Version version) {

		List<String> lines = new ArrayList<>(version.getHeaderLines());

		for (Section section : version.getSections()) {
			lines.add(ChangelogParser.SECTION_MARKER + section.getName());
			lines.addAll(section.getLines());
		}

		return lines;
	}

	/** Inverse of {@link #versionContentLines(Version)}: rebuild a version from merged content lines. */
	static Version parseVersionContent(String name, String releaseDate, List<String> lines) {

		VersionBuilder version = Version.builder().name(name).releaseDate(releaseDate);
		SectionBuilder section = null;

		for (String line : lines) {
			if (StringUtils.startsWith(line, ChangelogParser.SECTION_MARKER)) {
				if (section != null) {
					version.section(section.build());
				}
				section = Section.builder().name(line.substring(ChangelogParser.SECTION_MARKER.length()));
			} else if (section != null) {
				section.line(line);
			} else {
				version.headerLine(line);
			}
		}

		if (section != null) {
			version.section(section.build());
		}

		return version.build();
	}

	Optional<Version> findVersionByName(List<Version> versions, String name) {
		return versions
				.stream()
				.filter(v -> v.getName().equals(name))
				.findFirst();
	}

	Version mergeVersions(Version our, Version their, boolean addFromLabel) {

		if (our == null) {
			return their;
		}

		if (their == null) {
			return our;
		}

		String fromLabel = addFromLabel ? "[from `" + their.getName() + "`] " : StringUtils.EMPTY;

		List<Section> mergedSections = new ArrayList<>();

		for (Section ourSection: our.getSections()) {

			Optional<Section> theirSection = findByName(their.getSections(), ourSection.getName());

			if (theirSection.isPresent()) {
				mergedSections.add(mergeSections(ourSection, theirSection.get(), fromLabel));
			} else {
				mergedSections.add(ourSection);
			}
		}

		for (Section theirSection : their.getSections()) {

			Optional<Section> ourSection = findByName(our.getSections(), theirSection.getName());

			if (!ourSection.isPresent()) {
				mergedSections.add(addFromLabel(theirSection, fromLabel));
			}
		}

		return Version.builder()
				.name(our.getName())
				.releaseDate(our.getReleaseDate())
				.sections(mergedSections)
				.build();
	}

	/**
	 * Similar to {@link #mergeVersions(Version, Version, boolean)}, but:
	 * <ul>
	 * <li>Uses version name and release date from "theirs"
	 * <li>When merging sections, uses items from "theirs" first and items from "ours" second.
	 * <li>Does not use "from label".
	 */
	Version rebaseVersions(Version our, Version their) {

		List<Section> mergedSections = new ArrayList<>();

		for (Section ourSection : our.getSections()) {

			Optional<Section> theirSection = findByName(their.getSections(), ourSection.getName());

			if (theirSection.isPresent()) {
				mergedSections.add(mergeSections(theirSection.get(), ourSection, StringUtils.EMPTY));
			} else {
				mergedSections.add(ourSection);
			}
		}

		for (Section theirSection : their.getSections()) {

			Optional<Section> ourSection = findByName(our.getSections(), theirSection.getName());

			if (!ourSection.isPresent()) {
				mergedSections.add(theirSection);
			}
		}

		return Version.builder()
				.name(their.getName())
				.releaseDate(their.getReleaseDate())
				.sections(mergedSections)
				.build();
	}

	Section addFromLabel(Section section, String fromLabel) {

		if (StringUtils.isBlank(fromLabel)) {
			return section;
		}

		return Section.builder()
				.name(section.getName())
				.lines(section.getLines()
						.stream()
						.map(l -> addFromLabel(l, fromLabel))
						.collect(Collectors.toCollection(LinkedHashSet::new))
				)
				.build();
	}

	Section mergeSections(Section our, Section their, String fromLabel) {

		LinkedHashMap<String, String> resultLines = new LinkedHashMap<>();

		for (String line : our.getLines()) {
			resultLines.put(line, line);
		}

		for (String line : their.getLines()) {
			String lineWithoutFromLabel = RegExUtils.removeFirst(line, FROM_LABEL_PATTERN);
			boolean lineHasFromLabel = !StringUtils.equals(line, lineWithoutFromLabel);
			if (lineHasFromLabel) {
				// add from label to original line
				resultLines.put(lineWithoutFromLabel, addFromLabel(line, fromLabel));
			} else {
				if (!resultLines.containsKey(line)) {
					resultLines.put(line, addFromLabel(line, fromLabel));
				}
			}
		}

		return Section.builder()
				.name(our.getName())
				.lines(resultLines.values())
				.build();
	}

	Optional<Section> findByName(List<Section> sections, String name) {
		return sections
				.stream()
				.filter(s -> s.getName().equals(name))
				.findFirst();
	}

	Version addMissingFromLabels(Version unreleasedVersion, List<Version> releasedVersions) {

		Map<String, LinkedHashMap<String, String>> unreleasedLinesBySectionName = new HashMap<>(unreleasedVersion.getSections().size());
		for (Section unreleasedSection : unreleasedVersion.getSections()) {

			LinkedHashMap<String, String> sectionLines = new LinkedHashMap<>(unreleasedSection.getLines().size());
			unreleasedSection.getLines().forEach(l -> sectionLines.put(l, l));

			unreleasedLinesBySectionName.put(unreleasedSection.getName(), sectionLines);
		}

		for (Version releasedVersion : releasedVersions) {
			String fromLabel = "[from `" + releasedVersion.getName() + "`] ";
			for (Section releasedSection : releasedVersion.getSections()) {
				LinkedHashMap<String, String> unreleasedSectionLines = unreleasedLinesBySectionName.get(releasedSection.getName());
				if (unreleasedSectionLines != null) {
					addMissingFromLabels(unreleasedSectionLines, releasedSection, fromLabel);
				}
			}
		}

		ArrayList<Section> newSections = new ArrayList<>(unreleasedVersion.getSections().size());
		for (Section originalUnreleasedSection : unreleasedVersion.getSections()) {
			Collection<String> lines = unreleasedLinesBySectionName.get(originalUnreleasedSection.getName()).values();
			Section newSection = Section.builder()
					.name(originalUnreleasedSection.getName())
					.lines(lines)
					.build();
			newSections.add(newSection);
		}

		return Version.builder()
				.name(unreleasedVersion.getName())
				.releaseDate(unreleasedVersion.getReleaseDate())
				.sections(newSections)
				.build();
	}

	void addMissingFromLabels(LinkedHashMap<String, String> unreleasedSectionLines, Section releasedSection, String fromLabel) {
		for (String releasedLine : releasedSection.getLines()) {
			unreleasedSectionLines.replace(releasedLine, addFromLabel(releasedLine, fromLabel));
		}
	}

	String addFromLabel(String line, String fromLabel) {
		if (StringUtils.startsWith(line, "- ")) {
			return "- " + fromLabel + StringUtils.substring(line, 2);
		} else {
			// assuming this is a new line of the same item as previous line, therefore not adding the label
			return line;
		}
	}

	Version removeDuplicatedUnreleasedLines(Version unreleasedVersion, List<Version> releasedVersions) {

		Map<String, Set<String>> allReleasedLinesBySectionName = new HashMap<>();
		for (Version releasedVersion : releasedVersions) {
			for (Section releasedSection : releasedVersion.getSections()) {
				Set<String> sectionLines = allReleasedLinesBySectionName.computeIfAbsent(releasedSection.getName(), k -> new HashSet<>());
				releasedSection.getLines().stream().filter(StringUtils::isNotBlank).forEach(sectionLines::add);
			}
		}

		List<Section> newSections = new ArrayList<>(unreleasedVersion.getSections().size());

		for (Section unreleasedSection : unreleasedVersion.getSections()) {
			Set<String> allReleasedLines = allReleasedLinesBySectionName.get(unreleasedSection.getName());
			if (allReleasedLines == null) {
				newSections.add(unreleasedSection);
			} else {
				Set<String> unreleasedLines = new LinkedHashSet<>(unreleasedSection.getLines());
				unreleasedLines.removeAll(allReleasedLines);
				if (unreleasedLines.stream().anyMatch(StringUtils::isNotBlank)) {
					newSections.add(Section.builder()
							.name(unreleasedSection.getName())
							.lines(unreleasedLines)
							.build());
				}
			}
		}

		return Version.builder()
				.name(unreleasedVersion.getName())
				.releaseDate(unreleasedVersion.getReleaseDate())
				.sections(newSections)
				.build();
	}

}
