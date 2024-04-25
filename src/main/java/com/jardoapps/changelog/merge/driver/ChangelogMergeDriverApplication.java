package com.jardoapps.changelog.merge.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChangelogMergeDriverApplication {

	public static void main(String[] args) throws IOException {

		if (args.length < 3) {
			System.err.println("Expected 3 arguments, but found " + args.length);
			return;
		}

		String ourFile = args[0];

		String originalFile = args[1];

		String theirFile = args[2];

		System.out.println(String.format("Changelog merge. Ours: %s, original: %s, their: %s", ourFile, originalFile, theirFile));

		Changelog ourChangelog = loadChangelog(ourFile);
		Changelog theirChangelog = loadChangelog(theirFile);

		// TODO: merge changelogs

	}

	private static Changelog loadChangelog(String path) throws IOException {

		try (BufferedReader reader = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
			ChangelogParser parser = new ChangelogParser();
			parser.parse(reader);
			return parser.getChangelog();
		}
	}
}
