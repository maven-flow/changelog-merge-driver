package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChangelogTest {

	@ParameterizedTest
	@CsvSource(nullValues = "NULL", value = {
			"         1.0.0,   2021-01-01,  true",
			"         1.0.0,     SNAPSHOT, false",
			"         1.0.0,   [SNAPSHOT], false",
			"         1.0.0,   UNRELEASED, false",
			"         1.0.0,   Unreleased, false",
			"         1.0.0, [Unreleased], false",
			"      SNAPSHOT,         NULL, false",
			"    UNRELEASED,         NULL, false",
			"    Unreleased,         NULL, false",
			"    UNRELEASED,         NULL, false",
			"1.0.0-SNAPSHOT,         NULL, false",
	})
	void testVersionIsReleased(String name, String releaseDate, boolean expected) {
		Changelog.Version version = Changelog.Version.builder().name(name).releaseDate(releaseDate).build();
		assertThat(version.isReleased()).isEqualTo(expected);
	}

}
