package com.github.johnjvester;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Randomizes list and delimiter-separated string inputs, with optional size limiting
 * and weighted rating-based selection.
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

    private final Random random;

    public RandomGenerator() {
        this(new Random(ThreadLocalRandom.current().nextLong()));
    }

    RandomGenerator(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

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
     * Randomizes the supplied list, optionally applying rating-based weighting first.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param useRating when {@code true}, applies weighted rating-based selection
     * @return a randomized list
     */
    public List<T> randomize(List<T> tList, Boolean useRating) {
        if (Boolean.TRUE.equals(useRating)) {
            return randomize(tList, RATING_LEVEL_HIGH);
        }

        return randomize(tList);
    }

    /**
     * Randomizes the supplied list using weighted rating-based selection.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param ratingLevel weighting strength; higher values increase the influence of the rating
     * @return a randomized list
     */
    public List<T> randomize(List<T> tList, int ratingLevel) {
        if (ratingLevel <= RATING_LEVEL_OFF) {
            return randomize(tList);
        }

        return weightedShuffle(copyOf(tList), ratingLevel);
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
     * Randomizes the supplied list, optionally applying rating-based weighting, and
     * returns at most {@code maxResults} elements.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param maxResults maximum number of elements to return
     * @param useRating when {@code true}, applies weighted rating-based selection
     * @return a randomized list trimmed to the requested size when applicable
     */
    public List<T> randomize(List<T> tList, Integer maxResults, Boolean useRating) {
        if (Boolean.TRUE.equals(useRating)) {
            return limit(randomize(tList, RATING_LEVEL_HIGH), maxResults);
        }

        return limit(randomize(tList), maxResults);
    }

    /**
     * Randomizes the supplied list, applying rating-based weighting first, and returns
     * at most {@code maxResults} elements.
     *
     * @param tList list to shuffle; {@code null} is treated as empty
     * @param maxResults maximum number of elements to return
     * @param ratingLevel weighting strength; higher values increase the influence of the rating
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
        Collections.shuffle(values, random);
        return values;
    }

    private List<T> limit(List<T> values, Integer maxResults) {
        if (maxResults == null || maxResults < 0 || maxResults >= values.size()) {
            return values;
        }

        return new ArrayList<>(values.subList(0, maxResults));
    }

    private List<T> weightedShuffle(List<T> values, int ratingLevel) {
        List<WeightedValue<T>> weightedValues = new ArrayList<>(values.size());
        double exponent = ratingExponent(ratingLevel);

        for (T value : values) {
            weightedValues.add(new WeightedValue<>(value, priorityFor(value, exponent)));
        }

        weightedValues.sort(Comparator.comparingDouble(WeightedValue::priority));

        List<T> result = new ArrayList<>(weightedValues.size());
        for (WeightedValue<T> weightedValue : weightedValues) {
            result.add(weightedValue.value());
        }

        return result;
    }

    private double priorityFor(Object value, double ratingExponent) {
        double rating = resolveRating(value);
        double weight = Math.pow(rating, ratingExponent);
        double randomValue = Math.max(random.nextDouble(), Double.MIN_VALUE);
        return -Math.log(randomValue) / weight;
    }

    private double resolveRating(Object value) {
        if (value == null) {
            return 1.0d;
        }

        if (value instanceof Number number) {
            return positiveRating(number.doubleValue());
        }

        Optional<Object> ratingProperty = readRatingProperty(value);
        if (ratingProperty.isPresent()) {
            return positiveRating(coerceToDouble(ratingProperty.get()));
        }

        if (value instanceof CharSequence text) {
            double legacyRating = legacyRating(text.toString());
            if (!Double.isNaN(legacyRating)) {
                return positiveRating(legacyRating);
            }
        }

        return 1.0d;
    }

    private double positiveRating(double rating) {
        return rating > 0 ? rating : 1.0d;
    }

    private double coerceToDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value == null) {
            return 1.0d;
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (RuntimeException ex) {
            return 1.0d;
        }
    }

    private Optional<Object> readRatingProperty(Object value) {
        Class<?> type = value.getClass();

        Method method = findNoArgMethod(type, "getRating");
        if (method == null) {
            method = findNoArgMethod(type, RATING);
        }
        if (method != null) {
            return invoke(value, method);
        }

        Field field = findField(type, RATING);
        if (field != null) {
            return read(value, field);
        }

        return Optional.empty();
    }

    private Method findNoArgMethod(Class<?> type, String methodName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException | SecurityException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException | SecurityException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private Optional<Object> invoke(Object value, Method method) {
        try {
            return Optional.ofNullable(method.invoke(value));
        } catch (ReflectiveOperationException | SecurityException ex) {
            return Optional.empty();
        }
    }

    private Optional<Object> read(Object value, Field field) {
        try {
            return Optional.ofNullable(field.get(value));
        } catch (IllegalAccessException | SecurityException ex) {
            return Optional.empty();
        }
    }

    private double legacyRating(String text) {
        String marker = DEFAULT_DELIMITER + RATING + DEFAULT_DELIMITER;
        int markerIndex = text.indexOf(marker);
        if (markerIndex < 0) {
            return Double.NaN;
        }

        int start = markerIndex + marker.length();
        int end = start;
        while (end < text.length()) {
            char current = text.charAt(end);
            if (!Character.isDigit(current) && current != '.' && current != '-' && current != '+') {
                break;
            }
            end++;
        }

        try {
            return Double.parseDouble(text.substring(start, end));
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private double ratingExponent(int ratingLevel) {
        return switch (ratingLevel) {
            case RATING_LEVEL_LOW -> 1.0d;
            case RATING_LEVEL_MEDIUM -> 2.0d;
            case RATING_LEVEL_HIGH -> 3.0d;
            default -> 3.0d;
        };
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

    private record WeightedValue<T>(T value, double priority) {
    }
}
