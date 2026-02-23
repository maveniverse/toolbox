/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

/**
 * A utility class to manage strings.
 * <p>
 * Copied from venerable <a href="https://github.com/apache/commons-lang">ASF commons-lang</a>.
 *
 * @since 0.12.1
 */
public final class StringUtils {
    private StringUtils() {}

    /**
     * Compares all Strings in an array and returns the initial sequence of characters that is common to all of them.
     *
     * <p>
     * For example, {@code getCommonPrefix("i am a machine", "i am a robot") -&gt; "i am a "}
     * </p>
     *
     * <pre>
     * StringUtils.getCommonPrefix(null)                             = ""
     * StringUtils.getCommonPrefix(new String[] {})                  = ""
     * StringUtils.getCommonPrefix(new String[] {"abc"})             = "abc"
     * StringUtils.getCommonPrefix(new String[] {null, null})        = ""
     * StringUtils.getCommonPrefix(new String[] {"", ""})            = ""
     * StringUtils.getCommonPrefix(new String[] {"", null})          = ""
     * StringUtils.getCommonPrefix(new String[] {"abc", null, null}) = ""
     * StringUtils.getCommonPrefix(new String[] {null, null, "abc"}) = ""
     * StringUtils.getCommonPrefix(new String[] {"", "abc"})         = ""
     * StringUtils.getCommonPrefix(new String[] {"abc", ""})         = ""
     * StringUtils.getCommonPrefix(new String[] {"abc", "abc"})      = "abc"
     * StringUtils.getCommonPrefix(new String[] {"abc", "a"})        = "a"
     * StringUtils.getCommonPrefix(new String[] {"ab", "abxyz"})     = "ab"
     * StringUtils.getCommonPrefix(new String[] {"abcde", "abxyz"})  = "ab"
     * StringUtils.getCommonPrefix(new String[] {"abcde", "xyz"})    = ""
     * StringUtils.getCommonPrefix(new String[] {"xyz", "abcde"})    = ""
     * StringUtils.getCommonPrefix(new String[] {"i am a machine", "i am a robot"}) = "i am a "
     * </pre>
     *
     * @param strs array of String objects, entries may be null.
     * @return the initial sequence of characters that are common to all Strings in the array; empty String if the array is null, the elements are all null or
     *         if there is no common prefix.
     * @since 2.4 (commons-lang)
     */
    public static String getCommonPrefix(final String... strs) {
        if (strs.length == 0) {
            return "";
        }
        int smallestIndexOfDiff = indexOfDifference(strs);
        if (smallestIndexOfDiff == -1) {
            // all strings were identical
            if (strs[0] == null) {
                return "";
            }
            return strs[0];
        }
        if (smallestIndexOfDiff == 0) {
            // there were no common initial characters
            return "";
        }
        // we found a common initial character sequence
        return strs[0].substring(0, smallestIndexOfDiff);
    }

    /**
     * Compares all CharSequences in an array and returns the index at which the CharSequences begin to differ.
     *
     * <p>
     * For example, {@code indexOfDifference(new String[] {"i am a machine", "i am a robot"}) -> 7}
     * </p>
     *
     * <pre>
     * StringUtils.indexOfDifference(null)                             = -1
     * StringUtils.indexOfDifference(new String[] {})                  = -1
     * StringUtils.indexOfDifference(new String[] {"abc"})             = -1
     * StringUtils.indexOfDifference(new String[] {null, null})        = -1
     * StringUtils.indexOfDifference(new String[] {"", ""})            = -1
     * StringUtils.indexOfDifference(new String[] {"", null})          = 0
     * StringUtils.indexOfDifference(new String[] {"abc", null, null}) = 0
     * StringUtils.indexOfDifference(new String[] {null, null, "abc"}) = 0
     * StringUtils.indexOfDifference(new String[] {"", "abc"})         = 0
     * StringUtils.indexOfDifference(new String[] {"abc", ""})         = 0
     * StringUtils.indexOfDifference(new String[] {"abc", "abc"})      = -1
     * StringUtils.indexOfDifference(new String[] {"abc", "a"})        = 1
     * StringUtils.indexOfDifference(new String[] {"ab", "abxyz"})     = 2
     * StringUtils.indexOfDifference(new String[] {"abcde", "abxyz"})  = 2
     * StringUtils.indexOfDifference(new String[] {"abcde", "xyz"})    = 0
     * StringUtils.indexOfDifference(new String[] {"xyz", "abcde"})    = 0
     * StringUtils.indexOfDifference(new String[] {"i am a machine", "i am a robot"}) = 7
     * </pre>
     *
     * @param css array of CharSequences, entries may be null.
     * @return the index where the strings begin to differ; -1 if they are all equal.
     * @since 2.4
     * @since 3.0 Changed signature from indexOfDifference(String...) to indexOfDifference(CharSequence...)
     */
    public static int indexOfDifference(final CharSequence... css) {
        if (css.length <= 1) {
            return -1;
        }
        boolean anyStringNull = false;
        boolean allStringsNull = true;
        final int arrayLen = css.length;
        int shortestStrLen = Integer.MAX_VALUE;
        int longestStrLen = 0;
        // find the min and max string lengths; this avoids checking to make
        // sure we are not exceeding the length of the string each time through
        // the bottom loop.
        for (final CharSequence cs : css) {
            if (cs == null) {
                anyStringNull = true;
                shortestStrLen = 0;
            } else {
                allStringsNull = false;
                shortestStrLen = Math.min(cs.length(), shortestStrLen);
                longestStrLen = Math.max(cs.length(), longestStrLen);
            }
        }
        // handle lists containing all nulls or all empty strings
        if (allStringsNull || longestStrLen == 0 && !anyStringNull) {
            return -1;
        }
        // handle lists containing some nulls or some empty strings
        if (shortestStrLen == 0) {
            return 0;
        }
        // find the position with the first difference across all strings
        int firstDiff = -1;
        for (int stringPos = 0; stringPos < shortestStrLen; stringPos++) {
            final char comparisonChar = css[0].charAt(stringPos);
            for (int arrayPos = 1; arrayPos < arrayLen; arrayPos++) {
                if (css[arrayPos].charAt(stringPos) != comparisonChar) {
                    firstDiff = stringPos;
                    break;
                }
            }
            if (firstDiff != -1) {
                break;
            }
        }
        if (firstDiff == -1 && shortestStrLen != longestStrLen) {
            // we compared all of the characters up to the length of the
            // shortest string and didn't find a match, but the string lengths
            // vary, so return the length of the shortest string.
            return shortestStrLen;
        }
        return firstDiff;
    }
}
