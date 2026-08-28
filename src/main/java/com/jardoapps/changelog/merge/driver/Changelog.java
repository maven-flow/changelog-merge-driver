package com.jardoapps.changelog.merge.driver;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class Changelog {

	private String name;

	@Singular
	private final List<String> headerLines;

	private Version unreleasedVersion;

	@Singular
	private final List<Version> releasedVersions;

	@Value
	@Builder
	public static class Version {

		private String name;

		/**
		 * The target of the version name when the heading writes it as a link, as Keep a Changelog
		 * recommends ("## [1.0.0](https://.../compare/v0.9.0...v1.0.0) - 2019-02-15"), without the
		 * enclosing parentheses. Null for a plain "## [1.0.0]" heading.
		 */
		private String link;

		private String releaseDate;

		@Singular
		private final List<String> headerLines;

		@Singular
		private List<Section> sections;

		public boolean isReleased() {

			if (StringUtils.containsAnyIgnoreCase(releaseDate, "SNAPSHOT", "UNRELEASED")) {
				return false;
			}

			if (StringUtils.containsAnyIgnoreCase(name, "SNAPSHOT", "UNRELEASED")) {
				return false;
			}

			return true;
		}
	}

	@Value
	@Builder
	public static class Section {

		private String name;

		@Singular
		private List<String> lines;

	}

}

