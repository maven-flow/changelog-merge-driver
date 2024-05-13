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
import com.jardoapps.changelog.merge.driver.Changelog.Version;

public class ChangelogMerger {

	private static final Pattern FROM_LABEL_PATTERN = Pattern.compile("\\[from `.*`\\] ");

	public Changelog merge(Changelog our, Changelog their) {

		Version unreleasedVersion = our.getUnreleasedVersion();

		Set<String> ourReleasedVersionNames = our.getReleasedVersions().stream().map(Version::getName).collect(Collectors.toSet());
		List<Version> mergedReleasedVersions = new LinkedList<>(our.getReleasedVersions());

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
				.name(our.getName())
				.headerLines(our.getHeaderLines())
				.unreleasedVersion(unreleasedVersion)
				.releasedVersions(mergedReleasedVersions)
				.build();
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
				sectionLines.addAll(releasedSection.getLines());
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
				if (!unreleasedLines.isEmpty()) {
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
