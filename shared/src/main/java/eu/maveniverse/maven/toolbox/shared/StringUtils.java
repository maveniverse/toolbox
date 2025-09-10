/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

/**
 * A utility class to manage strings.
 */
public final class StringUtils {
    private StringUtils() {}

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
