package com.jardoapps.changelog.merge.driver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.jardoapps.changelog.merge.driver.Changelog.Section;
import com.jardoapps.changelog.merge.driver.Changelog.Version;

public class ChangelogMerger {

	public Changelog merge(Changelog our, Changelog their) {

		Version unreleasedVersion = mergeVersions(our.getUnreleasedVersion(), their.getUnreleasedVersion());

		Set<String> ourReleasedVersionNames = our.getReleasedVersions().stream().map(Version::getName).collect(Collectors.toSet());
		List<Version> mergedReleasedVersions = new LinkedList<>(our.getReleasedVersions());

		// iterate through versions in reversed order to add newer versions first

		for (int i = their.getReleasedVersions().size() - 1; i >= 0; i--) {

			Version theirReleasedVersion = their.getReleasedVersions().get(i);

			if (!ourReleasedVersionNames.contains(theirReleasedVersion.getName())) {
				mergedReleasedVersions.add(0, theirReleasedVersion);
				if (unreleasedVersion != null) {
					unreleasedVersion = mergeVersions(unreleasedVersion, theirReleasedVersion);
				}
			}
		}

		return Changelog.builder()
				.name(our.getName())
				.headerLines(our.getHeaderLines())
				.unreleasedVersion(unreleasedVersion)
				.releasedVersions(mergedReleasedVersions)
				.build();
	}

	Version mergeVersions(Version our, Version their) {

		if (our == null) {
			return their;
		}

		if (their == null) {
			return our;
		}

		List<Section> mergedSections = new ArrayList<>();

		for (Section ourSection: our.getSections()) {

			Optional<Section> theirSection = findByName(their.getSections(), ourSection.getName());

			if (theirSection.isPresent()) {
				mergedSections.add(mergeSections(ourSection, theirSection.get()));
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
				.name(our.getName())
				.releaseDate(our.getReleaseDate())
				.sections(mergedSections)
				.build();
	}

	Section mergeSections(Section our, Section their) {

		LinkedHashSet<String> resultLines = new LinkedHashSet<>();

		for (String line : our.getLines()) {
			resultLines.add(line);
		}

		for (String line : their.getLines()) {
			resultLines.add(line);
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
