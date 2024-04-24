package com.jardoapps.changelog.merge.driver;

public class ChangelogMergeDriverApplication {

	public static void main(String[] args) {

		if (args.length < 3) {
			System.err.println("Expected 3 arguments, but found " + args.length);
			return;
		}

		String ourFile = args[0];

		String originalFile = args[1];

		String theirFile = args[2];

		System.out.println(String.format("Changelog merge. Ours: %s, original: %s, their: %s", ourFile, originalFile, theirFile));

		// TODO: parse and merge changelogs

	}

}
