package com.apricorn.judge.comparator;

import org.springframework.stereotype.Component;

@Component
public class OutputComparator {
    /**
     * Compares actual output with expected output using normalized comparison.
     * 1. Replaces CRLF (\r\n) with LF (\n)
     * 2. Trims trailing whitespace from each line
     * 3. Trims final trailing newlines
     */
    public boolean isEqual(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;

        String normExpected = normalize(expected);
        String normActual = normalize(actual);

        return normExpected.equals(normActual);
    }

    public String normalize(String input) {
        if (input == null) return "";
        String text = input.replace("\r\n", "\n").replace('\r', '\n');

        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(trimTrailingWhitespace(lines[i]));
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        String result = sb.toString();
        while (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String trimTrailingWhitespace(String line) {
        int len = line.length();
        while (len > 0 && Character.isWhitespace(line.charAt(len - 1))) {
            len--;
        }
        return line.substring(0, len);
    }
}
