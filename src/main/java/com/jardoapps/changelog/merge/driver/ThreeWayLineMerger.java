package com.jardoapps.changelog.merge.driver;

import java.util.ArrayList;
import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;

/**
 * Line-based three-way merge, built on the diff implementation of
 * <a href="https://github.com/java-diff-utils/java-diff-utils">java-diff-utils</a>.
 * <p>
 * The changes of both sides relative to the common base are computed and combined:
 * <ul>
 * <li>A change made by only one side is applied.
 * <li>Identical changes made by both sides are applied once.
 * <li>Lines inserted by both sides at the same place are kept from both, "ours" first, without
 * repeating lines which both sides inserted.
 * <li>Where both sides changed the same lines differently, the lines from "theirs" are taken.
 * A conflict is never reported.
 * </ul>
 * Unlike a standard Git merge, changes which merely touch (edits to adjacent lines) do not count
 * as conflicting; only changes to overlapping line ranges do.
 * <p>
 * java-diff-utils itself offers no three-way merge (see
 * <a href="https://github.com/java-diff-utils/java-diff-utils/issues/132">java-diff-utils#132</a>).
 * The closest it has, {@code Patch.applyTo} with a conflict output, is patch application: it
 * applies deltas at their exact base line numbers, so it misplaces or false-conflicts them once
 * "ours" has shifted lines, and on a conflict it writes Git conflict markers into the result.
 * This class therefore takes only the diffs from the library and combines the delta streams of
 * the two sides itself.
 */
public class ThreeWayLineMerger {

	public List<String> merge(List<String> base, List<String> ours, List<String> theirs) {

		// getDeltas() returns deltas ordered by source position; deltas of one side never overlap
		List<AbstractDelta<String>> ourDeltas = DiffUtils.diff(base, ours).getDeltas();
		List<AbstractDelta<String>> theirDeltas = DiffUtils.diff(base, theirs).getDeltas();

		List<String> result = new ArrayList<>();
		int basePosition = 0;
		int ourIndex = 0;
		int theirIndex = 0;

		while (ourIndex < ourDeltas.size() || theirIndex < theirDeltas.size()) {

			// collect the next group of deltas whose base line ranges overlap

			List<AbstractDelta<String>> ourGroup = new ArrayList<>();
			List<AbstractDelta<String>> theirGroup = new ArrayList<>();
			int groupStart = -1;
			int groupEnd = -1;

			while (true) {

				AbstractDelta<String> ourHead = ourIndex < ourDeltas.size() ? ourDeltas.get(ourIndex) : null;
				AbstractDelta<String> theirHead = theirIndex < theirDeltas.size() ? theirDeltas.get(theirIndex) : null;
				if (ourHead == null && theirHead == null) {
					break;
				}

				boolean takeOurs = pickOurHead(ourHead, theirHead);
				AbstractDelta<String> candidate = takeOurs ? ourHead : theirHead;

				if (groupEnd >= 0 && !overlapsGroup(groupStart, groupEnd, candidate)) {
					break;
				}

				if (takeOurs) {
					ourGroup.add(candidate);
					ourIndex++;
				} else {
					theirGroup.add(candidate);
					theirIndex++;
				}

				groupStart = groupStart < 0 ? sourceStart(candidate) : Math.min(groupStart, sourceStart(candidate));
				groupEnd = Math.max(groupEnd, sourceEnd(candidate));
			}

			result.addAll(base.subList(basePosition, groupStart));
			result.addAll(mergeGroup(base, groupStart, groupEnd, ourGroup, theirGroup));
			basePosition = groupEnd;
		}

		result.addAll(base.subList(basePosition, base.size()));
		return result;
	}

	/**
	 * Both delta lists are ordered by source position, so the next delta to consider is the head
	 * with the smaller start. On a tie, an insertion goes first: it inserts before the tied
	 * position, while a change starts at it.
	 */
	private static boolean pickOurHead(AbstractDelta<String> ourHead, AbstractDelta<String> theirHead) {

		if (ourHead == null) {
			return false;
		}
		if (theirHead == null) {
			return true;
		}

		if (sourceStart(ourHead) != sourceStart(theirHead)) {
			return sourceStart(ourHead) < sourceStart(theirHead);
		}

		return isInsertion(ourHead) || !isInsertion(theirHead);
	}

	/**
	 * A delta belongs to the current group if it changes base lines the group already covers. An
	 * insertion at the very start or end of the group's range lies outside the changed lines and
	 * starts a group of its own - except when the group itself is an insertion at the same point:
	 * concurrent insertions have no defined order and must be decided together.
	 */
	private static boolean overlapsGroup(int groupStart, int groupEnd, AbstractDelta<String> delta) {

		int start = sourceStart(delta);

		if (isInsertion(delta)) {
			return (start > groupStart && start < groupEnd) || (start == groupStart && groupStart == groupEnd);
		}

		return start < groupEnd;
	}

	private static List<String> mergeGroup(List<String> base, int groupStart, int groupEnd,
			List<AbstractDelta<String>> ourGroup, List<AbstractDelta<String>> theirGroup) {

		List<String> ourLines = applyDeltas(base, groupStart, groupEnd, ourGroup);
		List<String> theirLines = applyDeltas(base, groupStart, groupEnd, theirGroup);

		if (theirGroup.isEmpty() || ourLines.equals(theirLines)) {
			return ourLines;
		}

		if (ourGroup.isEmpty()) {
			return theirLines;
		}

		if (isOnlyInsertions(ourGroup) && isOnlyInsertions(theirGroup)) {
			List<String> merged = new ArrayList<>(ourLines);
			theirLines.stream().filter(line -> !ourLines.contains(line)).forEach(merged::add);
			return merged;
		}

		// both sides changed the same lines differently: take theirs
		return theirLines;
	}

	/** The given base line range as it looks with the (non-overlapping) deltas of one side applied. */
	private static List<String> applyDeltas(List<String> base, int from, int to, List<AbstractDelta<String>> deltas) {

		List<String> result = new ArrayList<>();
		int position = from;

		for (AbstractDelta<String> delta : deltas) {
			result.addAll(base.subList(position, sourceStart(delta)));
			result.addAll(delta.getTarget().getLines());
			position = sourceEnd(delta);
		}

		result.addAll(base.subList(position, to));
		return result;
	}

	private static boolean isOnlyInsertions(List<AbstractDelta<String>> deltas) {
		return deltas.stream().allMatch(ThreeWayLineMerger::isInsertion);
	}

	private static boolean isInsertion(AbstractDelta<String> delta) {
		return delta.getSource().size() == 0;
	}

	private static int sourceStart(AbstractDelta<String> delta) {
		return delta.getSource().getPosition();
	}

	private static int sourceEnd(AbstractDelta<String> delta) {
		return delta.getSource().getPosition() + delta.getSource().size();
	}
}
