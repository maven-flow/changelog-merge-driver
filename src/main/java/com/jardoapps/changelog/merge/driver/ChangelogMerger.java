package com.jardoapps.changelog.merge.driver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.jardoapps.changelog.merge.driver.Changelog.Section;
import com.jardoapps.changelog.merge.driver.Changelog.Version;

public class ChangelogMerger {

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

		// add unreleased changes of theirs to the end of unreleased changes of ours
		unreleasedVersion = mergeVersions(unreleasedVersion, their.getUnreleasedVersion(), false);

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
						.map(l -> fromLabel + l)
						.collect(Collectors.toCollection(LinkedHashSet::new))
				)
				.build();
	}

	Section mergeSections(Section our, Section their, String fromLabel) {

		LinkedHashSet<String> resultLines = new LinkedHashSet<>();

		for (String line : our.getLines()) {
			resultLines.add(line);
		}

		for (String line : their.getLines()) {
			if (!resultLines.contains(line)) {
				resultLines.add(fromLabel + line);
			}
		}

		return Section.builder().name(our.getName()).lines(resultLines).build();
	}

	Optional<Section> findByName(List<Section> sections, String name) {
		return sections
				.stream()
				.filter(s -> s.getName().equals(name))
				.findFirst();
	}
}
