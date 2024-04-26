package com.jardoapps.changelog.merge.driver;

import java.io.BufferedWriter;
import java.io.IOException;

import com.jardoapps.changelog.merge.driver.Changelog.Section;
import com.jardoapps.changelog.merge.driver.Changelog.Version;

public class ChangelogPrinter {

	public void print(Changelog changelog, BufferedWriter writer) throws IOException {

		printHeader(changelog, writer);

		if (changelog.getUnreleasedVersion() != null) {
			printVersion(changelog.getUnreleasedVersion(), writer);
		}

		printReleasedVersions(changelog, writer);
	}

	private void printHeader(Changelog changelog, BufferedWriter writer) throws IOException {

		writer.write("# ");
		writer.write(changelog.getName());
		writer.newLine();

		for (String line : changelog.getHeaderLines()) {
			writeLine(line, writer);
		}
	}

	private void printReleasedVersions(Changelog changelog, BufferedWriter writer) throws IOException {
		for (Version version : changelog.getReleasedVersions()) {
			printVersion(version, writer);
		}
	}

	private void printVersion(Version version, BufferedWriter writer) throws IOException {

		writer.write("## [");
		writer.write(version.getName());
		writer.write(']');

		if (version.getReleaseDate() != null) {
			writer.write(" - ");
			writer.write(version.getReleaseDate());
		}

		writer.newLine();
		writer.newLine();

		for (Section section : version.getSections()) {
			writer.write("### ");
			writer.write(section.getName());
			writer.newLine();

			for (String line : section.getLines()) {
				writeLine(line, writer);
			}
		}
	}

	private void writeLine(String line, BufferedWriter writer) throws IOException {
		writer.write(line);
		writer.newLine();
	}
}
