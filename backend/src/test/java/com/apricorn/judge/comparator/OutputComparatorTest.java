package com.apricorn.judge.comparator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputComparatorTest {

    private final OutputComparator comparator = new OutputComparator();

    @Test
    void matchesIdenticalJson() {
        OutputComparator.ComparisonResult result = comparator.compare("[1, 2, 3]", "[1, 2, 3]", "int[]");
        assertThat(result.isMatch()).isTrue();
    }

    @Test
    void matchesEquivalentJsonWithSpacing() {
        OutputComparator.ComparisonResult result = comparator.compare("[1, 2, 3]", "[\n  1,\n  2,\n  3\n]", "int[]");
        assertThat(result.isMatch()).isTrue();
    }

    @Test
    void failsOnDifferentJson() {
        OutputComparator.ComparisonResult result = comparator.compare("[1, 2, 3]", "[1, 3, 2]", "int[]");
        assertThat(result.isMatch()).isFalse();
        assertThat(result.errorReason()).contains("Return value did not match");
    }

    @Test
    void handlesMalformedActualJson() {
        OutputComparator.ComparisonResult result = comparator.compare("[1, 2, 3]", "[1, 2, ", "int[]");
        assertThat(result.isMatch()).isFalse();
        assertThat(result.errorReason()).contains("Malformed result structure.");
    }
}
