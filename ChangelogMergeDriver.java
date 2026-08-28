///usr/bin/env jbang "$0" "$@" ; exit $?

// Entry point for running the merge driver with jbang, without building or downloading a jar:
//
//     git config merge.changelog.driver "jbang changelog-merge-driver@maven-flow/changelog-merge-driver %A %O %B"
//
// The sources below are the same ones "mvn package" compiles, so both ways stay in sync.
// Keep the //DEPS versions aligned with pom.xml.

//JAVA 17+
//DEPS org.apache.commons:commons-lang3:3.20.0
//DEPS io.github.java-diff-utils:java-diff-utils:4.17
//DEPS org.projectlombok:lombok:1.18.46
// Listed one by one rather than as a glob: jbang fetches these over HTTP when the launcher is
// run from GitHub, and HTTP offers no directory listing to expand a wildcard against. A class
// added to the package has to be added here too.
//SOURCES src/main/java/com/jardoapps/changelog/merge/driver/Changelog.java
//SOURCES src/main/java/com/jardoapps/changelog/merge/driver/ChangelogMergeDriverApplication.java
//SOURCES src/main/java/com/jardoapps/changelog/merge/driver/ChangelogMerger.java
//SOURCES src/main/java/com/jardoapps/changelog/merge/driver/ChangelogParser.java
//SOURCES src/main/java/com/jardoapps/changelog/merge/driver/ChangelogPrinter.java
//SOURCES src/main/java/com/jardoapps/changelog/merge/driver/ThreeWayLineMerger.java

// Lombok is an annotation processor. Enabling processing explicitly keeps javac from
// warning that it found one on the class path.
//COMPILE_OPTIONS -proc:full

import java.io.IOException;

import com.jardoapps.changelog.merge.driver.ChangelogMergeDriverApplication;

public class ChangelogMergeDriver {

	public static void main(String[] args) throws IOException {
		ChangelogMergeDriverApplication.main(args);
	}
}
