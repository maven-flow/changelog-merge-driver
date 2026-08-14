package com.jardoapps.changelog.merge.driver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ThreeWayLineMergerTest {

	private final ThreeWayLineMerger merger = new ThreeWayLineMerger();

	@Test
	void testMerge_noChanges() {

		List<String> base = List.of("- Line 1", "- Line 2");

		List<String> result = merger.merge(base, base, base);

		assertThat(result).containsExactly("- Line 1", "- Line 2");
	}

	@Test
	void testMerge_theirChangeIsApplied() {

		List<String> base = List.of("- Line 1", "- Line 2 with tpyo", "- Line 3");
		List<String> ours = base;
		List<String> theirs = List.of("- Line 1", "- Line 2 with typo", "- Line 3");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 2 with typo", "- Line 3");
	}

	@Test
	void testMerge_ourChangeIsKept() {

		List<String> base = List.of("- Line 1", "- Line 2 with tpyo", "- Line 3");
		List<String> ours = List.of("- Line 1", "- Line 2 with typo", "- Line 3");
		List<String> theirs = base;

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 2 with typo", "- Line 3");
	}

	@Test
	void testMerge_changesOfBothSidesAreCombined() {

		List<String> base = List.of("- Line 1", "- Line 2", "- Line 3", "- Line 4");
		List<String> ours = List.of("- Line 1 fixed by us", "- Line 2", "- Line 3", "- Line 4");
		List<String> theirs = List.of("- Line 1", "- Line 2", "- Line 3", "- Line 4 fixed by them");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1 fixed by us", "- Line 2", "- Line 3", "- Line 4 fixed by them");
	}

	@Test
	void testMerge_changesToAdjacentLinesDoNotConflict() {

		List<String> base = List.of("- Line 1", "- Line 2", "- Line 3");
		List<String> ours = List.of("- Line 1", "- Line 2 fixed by us", "- Line 3");
		List<String> theirs = List.of("- Line 1", "- Line 2", "- Line 3 fixed by them");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 2 fixed by us", "- Line 3 fixed by them");
	}

	@Test
	void testMerge_identicalChangesAreAppliedOnce() {

		List<String> base = List.of("- Line 1", "- Line 2");
		List<String> ours = List.of("- Line 1", "- Line 2", "- Line 3");
		List<String> theirs = List.of("- Line 1", "- Line 2", "- Line 3");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 2", "- Line 3");
	}

	@Test
	void testMerge_conflictingChangeIsTakenFromTheirs() {

		List<String> base = List.of("- Line 1", "- Line 2", "- Line 3");
		List<String> ours = List.of("- Line 1", "- Line 2 changed by us", "- Line 3");
		List<String> theirs = List.of("- Line 1", "- Line 2 changed by them", "- Line 3");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 2 changed by them", "- Line 3");
	}

	@Test
	void testMerge_insertionsAtTheSamePlaceKeepBothSides() {

		List<String> base = List.of("- Line 1", "- Line 4");
		List<String> ours = List.of("- Line 1", "- Our line", "- Line 4");
		List<String> theirs = List.of("- Line 1", "- Their line", "- Line 4");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Our line", "- Their line", "- Line 4");
	}

	@Test
	void testMerge_insertionsAtTheSamePlaceAreNotDuplicated() {

		List<String> base = List.of("- Line 1", "- Line 4");
		List<String> ours = List.of("- Line 1", "- Line 2", "- Line 3", "- Line 4");
		List<String> theirs = List.of("- Line 1", "- Line 2", "- Line 3", "- Their line", "- Line 4");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 2", "- Line 3", "- Their line", "- Line 4");
	}

	@Test
	void testMerge_appendsAtTheEndKeepBothSides() {

		List<String> base = List.of("- Line 1");
		List<String> ours = List.of("- Line 1", "- Our line");
		List<String> theirs = List.of("- Line 1", "- Their line");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Our line", "- Their line");
	}

	@Test
	void testMerge_theirDeletionIsApplied() {

		List<String> base = List.of("- Line 1", "- Line 2", "- Line 3");
		List<String> ours = base;
		List<String> theirs = List.of("- Line 1", "- Line 3");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 3");
	}

	@Test
	void testMerge_ourDeletionVersusTheirChangeIsTakenFromTheirs() {

		List<String> base = List.of("- Line 1", "- Line 2", "- Line 3");
		List<String> ours = List.of("- Line 1", "- Line 3");
		List<String> theirs = List.of("- Line 1", "- Line 2 changed by them", "- Line 3");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 2 changed by them", "- Line 3");
	}

	@Test
	void testMerge_theirDeletionVersusOurChangeIsTakenFromTheirs() {

		List<String> base = List.of("- Line 1", "- Line 2", "- Line 3");
		List<String> ours = List.of("- Line 1", "- Line 2 changed by us", "- Line 3");
		List<String> theirs = List.of("- Line 1", "- Line 3");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Line 1", "- Line 3");
	}

	@Test
	void testMerge_emptyBase() {

		List<String> base = List.of();
		List<String> ours = List.of("- Our line");
		List<String> theirs = List.of("- Their line");

		List<String> result = merger.merge(base, ours, theirs);

		assertThat(result).containsExactly("- Our line", "- Their line");
	}

	@Test
	void testMerge_bothSidesEmpty() {

		List<String> base = List.of("- Line 1");

		List<String> result = merger.merge(base, List.of(), List.of());

		assertThat(result).isEmpty();
	}
}
