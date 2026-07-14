package com.github.johnjvester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Randomizes list and delimiter-separated string inputs, with optional size limiting
 * and simple rating-based filtering.
 *
 * @param <T> element type for list operations
 */
public class RandomGenerator<T> {
    public static String DEFAULT_DELIMITER = "~~~";
    public static String RATING = "rating";
    public static final int RATING_LEVEL_OFF = 0;
    public static final int RATING_LEVEL_LOW = 1;
    public static final int RATING_LEVEL_MEDIUM = 2;
    public static final int RATING_LEVEL_HIGH = 3;

    /**
     * Randomizes the supplied list.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @return a shuffled copy of the input list
     */
    public List<T> randomize(List<T> tList) {
        return shuffle(copyOf(tList));
    }

    /**
     * Randomizes the supplied list, optionally applying rating-based filtering first.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param useRating when {@code true}, applies the highest rating filter path
     * @return a randomized list
     */
    public List<T> randomize(List<T> tList, Boolean useRating) {
        if (Boolean.TRUE.equals(useRating)) {
            return randomize(tList, RATING_LEVEL_HIGH);
        }

        return randomize(tList);
    }

    /**
     * Randomizes the supplied list, keeping values whose embedded rating is at or below
     * the requested level.
     *
     * @param tList list to filter and shuffle; {@code null} is treated as empty
     * @param ratingLevel maximum allowed rating value
     * @return a randomized, filtered list
     */
    public List<T> randomize(List<T> tList, int ratingLevel) {
        if (ratingLevel <= RATING_LEVEL_OFF) {
            return randomize(tList);
        }

        return shuffle(filterByRating(copyOf(tList), ratingLevel));
    }

    /**
     * Randomizes the supplied list and returns at most {@code maxResults} elements.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param maxResults maximum number of elements to return
     * @return a randomized list trimmed to the requested size when applicable
     */
    public List<T> randomize(List<T> tList, Integer maxResults) {
        return limit(randomize(tList), maxResults);
    }

    /**
     * Randomizes the supplied list, optionally applying rating-based filtering, and
     * returns at most {@code maxResults} elements.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param maxResults maximum number of elements to return
     * @param useRating when {@code true}, applies the highest rating filter path
     * @return a randomized list trimmed to the requested size when applicable
     */
    public List<T> randomize(List<T> tList, Integer maxResults, Boolean useRating) {
        if (Boolean.TRUE.equals(useRating)) {
            return limit(randomize(tList, RATING_LEVEL_HIGH), maxResults);
        }

        return limit(randomize(tList), maxResults);
    }

    /**
     * Randomizes the supplied list, applying rating-based filtering first, and returns
     * at most {@code maxResults} elements.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param maxResults maximum number of elements to return
     * @param ratingLevel maximum allowed rating value
     * @return a randomized list trimmed to the requested size when applicable
     */
    public List<T> randomize(List<T> tList, Integer maxResults, int ratingLevel) {
        return limit(randomize(tList, ratingLevel), maxResults);
    }

    /**
     * Randomizes a separator-delimited string.
     *
     * @param thisString string containing delimiter-separated values
     * @param thisSeparator delimiter to use; {@code null} falls back to {@link #DEFAULT_DELIMITER}
     * @return a randomized delimiter-separated string
     */
    public String randomize(String thisString, String thisSeparator) {
        return join(randomize(split(thisString, thisSeparator)), separatorOrDefault(thisSeparator));
    }

    /**
     * Randomizes a separator-delimited string and returns at most {@code maxResults}
     * elements.
     *
     * @param thisString string containing delimiter-separated values
     * @param thisSeparator delimiter to use; {@code null} falls back to {@link #DEFAULT_DELIMITER}
     * @param maxResults maximum number of elements to return
     * @return a randomized delimiter-separated string
     */
    public String randomize(String thisString, String thisSeparator, Integer maxResults) {
        return join(limit(randomize(split(thisString, thisSeparator)), maxResults), separatorOrDefault(thisSeparator));
    }

    private List<T> copyOf(List<T> tList) {
        return tList == null ? List.of() : new ArrayList<>(tList);
    }

    private List<T> shuffle(List<T> values) {
        Collections.shuffle(values, ThreadLocalRandom.current());
        return values;
    }

    private List<T> limit(List<T> values, Integer maxResults) {
        if (maxResults == null || maxResults < 0 || maxResults >= values.size()) {
            return values;
        }

        return new ArrayList<>(values.subList(0, maxResults));
    }

    private List<T> filterByRating(List<T> values, int ratingLevel) {
        List<T> filtered = new ArrayList<>();

        for (T value : values) {
            if (extractRatingLevel(value) <= ratingLevel) {
                filtered.add(value);
            }
        }

        return filtered;
    }

    private int extractRatingLevel(Object value) {
        if (value == null) {
            return RATING_LEVEL_HIGH;
        }

        String text = value.toString();
        String marker = DEFAULT_DELIMITER + RATING + DEFAULT_DELIMITER;
        int markerIndex = text.indexOf(marker);
        if (markerIndex < 0) {
            return RATING_LEVEL_HIGH;
        }

        int start = markerIndex + marker.length();
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }

        try {
            return Integer.parseInt(text.substring(start, end));
        } catch (RuntimeException ex) {
            return RATING_LEVEL_HIGH;
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> split(String thisString, String thisSeparator) {
        if (thisString == null || thisString.isEmpty()) {
            return List.of();
        }

        String separator = separatorOrDefault(thisSeparator);
        String[] parts = thisString.split(java.util.regex.Pattern.quote(separator), -1);
        List<T> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                values.add((T) part);
            }
        }

        return values;
    }

    private String join(List<T> values, String separator) {
        return values.stream()
                .map(Objects::toString)
                .reduce((left, right) -> left + separator + right)
                .orElse("");
    }

    private String separatorOrDefault(String separator) {
        return separator == null ? DEFAULT_DELIMITER : separator;
    }
}
