package com.jardoapps.changelog.merge.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

public class ChangelogMergeDriverApplication {

	public static void main(String[] args) throws IOException {

		// The version comes from the jar manifest, which jbang runs do not have.
		String version = ChangelogMergeDriverApplication.class.getPackage().getImplementationVersion();
		System.out.println("Running Changelog Merge Driver " + Objects.requireNonNullElse(version, "(from source)"));

		if (args.length < 3) {
			System.err.println("Expected at least 3 arguments, but found " + args.length);
			return;
		}

		String ourFile = args[0];
		String baseFile = args[1];
		String theirFile = args[2];

		Changelog ourChangelog = loadChangelog(ourFile);
		Changelog theirChangelog = loadChangelog(theirFile);
		Changelog baseChangelog = loadBaseChangelog(baseFile);

		ChangelogMerger changelogMerger = new ChangelogMerger();
		Changelog mergedChangelog;

		boolean rebase = Arrays.asList(args).contains("--rebase");
		if (rebase) {
			System.out.println("Performing changelog rebase");
			mergedChangelog = changelogMerger.rebase(baseChangelog, ourChangelog, theirChangelog);
		} else {
			System.out.println("Performing changelog merge");
			mergedChangelog = changelogMerger.merge(baseChangelog, ourChangelog, theirChangelog);
		}

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

	/**
	 * The base file is the common ancestor of "ours" and "theirs" (the "%O" file Git passes to a
	 * merge driver). When the changelog was created independently on both branches there is no
	 * ancestor, and Git passes an empty file, which is not a parsable changelog. The merge then
	 * runs without a base: released versions present on both sides with different content are
	 * taken from "theirs".
	 */
	private static Changelog loadBaseChangelog(String path) {

		try {
			return loadChangelog(path);
		} catch (IOException | RuntimeException ex) {
			System.out.println("Could not parse base changelog (" + ex.getMessage() + "). Merging without a base.");
			return null;
		}
	}
}
