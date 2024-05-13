package com.jardoapps.changelog.merge.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ChangelogMergeDriverApplication {

	public static void main(String[] args) throws IOException {

		System.out.println("Running Changelog Merge Driver");

		if (args.length < 3) {
			System.err.println("Expected 3 arguments, but found " + args.length);
			return;
		}

		String ourFile = args[0];
		String theirFile = args[2];

		Changelog ourChangelog = loadChangelog(ourFile);
		Changelog theirChangelog = loadChangelog(theirFile);

		ChangelogMerger changelogMerger = new ChangelogMerger();
		Changelog mergedChangelog = changelogMerger.merge(ourChangelog, theirChangelog);

		ChangelogPrinter changelogPrinter = new ChangelogPrinter();
		try (BufferedWriter writer = Files.newBufferedWriter(Path.of(ourFile), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
			changelogPrinter.print(mergedChangelog, writer);
		}

	}

	private static Changelog loadChangelog(String path) throws IOException {

		try (BufferedReader reader = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
			ChangelogParser parser = new ChangelogParser();
			parser.parse(reader);
			return parser.getChangelog();
		}
	}
}
