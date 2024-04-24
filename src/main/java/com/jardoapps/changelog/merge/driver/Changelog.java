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

		private String releaseDate;

		@Singular
		private List<Section> sections;

		public boolean isReleased() {
			return !StringUtils.equals(releaseDate, "SNAPSHOT");
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

