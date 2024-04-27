package com.jardoapps.changelog.merge.driver;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.jardoapps.changelog.merge.driver.Changelog.Section;
import com.jardoapps.changelog.merge.driver.Changelog.Version;

public class ChangelogPrinter {

	private boolean lastLineWasEmpty = false;

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
		lastLineWasEmpty = false;

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

		if (!lastLineWasEmpty) {
			writer.newLine();
		}

		writer.write("## [");
		writer.write(version.getName());
		writer.write(']');

		if (version.getReleaseDate() != null) {
			writer.write(" - ");
			writer.write(version.getReleaseDate());
		}

		writer.newLine();
		lastLineWasEmpty = false;

		for (String line : version.getHeaderLines()) {
			writeLine(line, writer);
		}

		for (Section section : version.getSections()) {

			if (!lastLineWasEmpty) {
				writer.newLine();
			}

			writer.write("### ");
			writer.write(section.getName());
			writer.newLine();
			lastLineWasEmpty = false;

			for (String line : section.getLines()) {
				writeLine(line, writer);
			}
		}
	}

	private void writeLine(String line, BufferedWriter writer) throws IOException {
		writer.write(line);
		writer.newLine();
		lastLineWasEmpty = StringUtils.isEmpty(line);
	}
}
