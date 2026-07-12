package dev.hellgates.retekeyime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

final class SelectionExpectationLedger {
    private final int capacity;
    private final Deque<Entry> pending = new ArrayDeque<>();
    private final Deque<RetiredFeedback> retired = new ArrayDeque<>();

    SelectionExpectationLedger(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    boolean hasCapacity() {
        return pending.size() < capacity;
    }

    void reserve(long revision, EditorExpectation expectation) {
        if (!hasCapacity()) {
            throw new IllegalStateException("selection expectation ledger is full");
        }
        pending.addLast(new Entry(revision, expectation));
    }

    void cancel(long revision) {
        Iterator<Entry> iterator = pending.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().revision == revision) {
                iterator.remove();
                return;
            }
        }
    }

    int pendingCount() {
        return pending.size();
    }

    boolean isKnown(EditorBounds bounds) {
        if (!bounds.hasSelection()) {
            return false;
        }
        for (RetiredFeedback feedback : retired) {
            if (feedback.matches(bounds)) {
                return true;
            }
        }
        return false;
    }

    SelectionReconcileResult reconcile(EditorBounds observed) {
        int matchingIndex = lastMatchingIndex(observed);
        if (matchingIndex >= 0) {
            for (int index = 0; index <= matchingIndex; index++) {
                Entry entry = pending.removeFirst();
                EditorBounds actualFinal = index == matchingIndex
                    ? observed
                    : singleAlternative(entry.expectation);
                retire(actualFinal, entry.expectation);
            }
            return matchingIndex == 0
                ? SelectionReconcileResult.MATCHED
                : SelectionReconcileResult.COALESCED;
        }
        for (Entry entry : pending) {
            if (entry.expectation.matchesIntermediate(observed)) {
                return SelectionReconcileResult.INTERMEDIATE;
            }
        }
        return isKnown(observed)
            ? SelectionReconcileResult.DUPLICATE_OR_DELAYED
            : SelectionReconcileResult.CONTRADICTION;
    }

    void clear() {
        pending.clear();
        retired.clear();
    }

    private int lastMatchingIndex(EditorBounds observed) {
        int index = 0;
        int matchingIndex = -1;
        for (Entry entry : pending) {
            if (entry.expectation.matches(observed)) {
                matchingIndex = index;
            }
            index++;
        }
        return matchingIndex;
    }

    private void retire(EditorBounds actualFinal, EditorExpectation expectation) {
        while (retired.size() >= capacity) {
            retired.removeFirst();
        }
        retired.addLast(new RetiredFeedback(actualFinal, expectation));
    }

    private static EditorBounds singleAlternative(EditorExpectation expectation) {
        return expectation.alternatives().size() == 1
            ? expectation.alternatives().get(0)
            : null;
    }

    private static final class Entry {
        private final long revision;
        private final EditorExpectation expectation;

        private Entry(long revision, EditorExpectation expectation) {
            this.revision = revision;
            this.expectation = expectation;
        }
    }

    private static final class RetiredFeedback {
        private final EditorBounds actualFinal;
        private final EditorExpectation expectation;

        private RetiredFeedback(
            EditorBounds actualFinal,
            EditorExpectation expectation
        ) {
            this.actualFinal = actualFinal;
            this.expectation = expectation;
        }

        private boolean matches(EditorBounds bounds) {
            return bounds.equals(actualFinal) || expectation.matchesIntermediate(bounds);
        }
    }
}
