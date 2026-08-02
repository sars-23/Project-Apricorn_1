package com.apricorn.judge.comparator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OutputComparatorTest {
    private OutputComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new OutputComparator();
    }

    @Test
    void matchesExactOutput() {
        assertThat(comparator.isEqual("42", "42")).isTrue();
    }

    @Test
    void normalizesCRLFAndLF() {
        assertThat(comparator.isEqual("Hello\r\nWorld\r\n", "Hello\nWorld\n")).isTrue();
    }

    @Test
    void ignoresTrailingWhitespacePerLineAndTrailingNewlines() {
        assertThat(comparator.isEqual("Line 1  \nLine 2   \n\n", "Line 1\nLine 2")).isTrue();
    }

    @Test
    void rejectsWrongOutput() {
        assertThat(comparator.isEqual("42", "43")).isFalse();
    }
}
