package com.github.johnjvester;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGeneratorTest {
    private final RandomGenerator<String> generator = new RandomGenerator<>(new Random(1234L));

    @Test
    void randomizeListKeepsAllElements() {
        List<String> input = List.of("a", "b", "c", "d");

        List<String> randomized = generator.randomize(input);

        assertEquals(input.size(), randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeNullListReturnsEmptyList() {
        List<String> randomized = generator.randomize((List<String>) null);

        assertTrue(randomized.isEmpty());
    }

    @Test
    void randomizeListHonorsMaxResults() {
        List<String> input = List.of("a", "b", "c", "d");

        List<String> randomized = generator.randomize(input, Integer.valueOf(2));

        assertEquals(2, randomized.size());
        assertTrue(input.containsAll(randomized));
    }

    @Test
    void randomizeListWithBooleanRatingPrefersHigherRatedValues() {
        List<RatedItem> input = List.of(
                new RatedItem("low", 1),
                new RatedItem("high", 10)
        );

        int highFirst = countFirstSelections(input, seed -> {
            RandomGenerator<RatedItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, Boolean.TRUE);
        }, "high");

        assertTrue(highFirst > 350);
    }

    @Test
    void randomizeListWithNumberRatingsPrefersHigherValues() {
        List<Integer> input = List.of(1, 10);

        int highFirst = countFirstSelections(input, seed -> {
            RandomGenerator<Integer> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, Boolean.TRUE);
        }, "10");

        assertTrue(highFirst > 350);
    }

    @Test
    void randomizeListWithRatingLevelAmplifiesPreference() {
        List<RatedItem> input = List.of(
                new RatedItem("low", 1),
                new RatedItem("high", 10)
        );

        int lowLevelHighFirst = countFirstSelections(input, seed -> {
            RandomGenerator<RatedItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, RandomGenerator.RATING_LEVEL_LOW);
        }, "high");

        int highLevelHighFirst = countFirstSelections(input, seed -> {
            RandomGenerator<RatedItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, RandomGenerator.RATING_LEVEL_HIGH);
        }, "high");

        assertTrue(highLevelHighFirst > lowLevelHighFirst);
    }

    @Test
    void randomizeListWithRatingLevelDefaultBranchUsesWeightedPath() {
        List<RatedItem> input = List.of(
                new RatedItem("low", 1),
                new RatedItem("high", 10)
        );

        int highPicked = countFirstSelections(input, seed -> {
            RandomGenerator<RatedItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, 99);
        }, "high");

        assertTrue(highPicked > 350);
    }

    @Test
    void randomizeListWithMaxResultsAndBooleanRatingUsesWeightedPath() {
        List<RatedItem> input = List.of(new RatedItem("low", 1), new RatedItem("high", 10));

        int highPicked = countFirstSelections(input, seed -> {
            RandomGenerator<RatedItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, Integer.valueOf(1), Boolean.TRUE);
        }, "high");

        assertTrue(highPicked > 350);
    }

    @Test
    void randomizeListWithMaxResultsAndRatingLevelUsesWeightedPath() {
        List<RatedItem> input = List.of(new RatedItem("low", 1), new RatedItem("high", 10));

        int highPicked = countFirstSelections(input, seed -> {
            RandomGenerator<RatedItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, Integer.valueOf(1), RandomGenerator.RATING_LEVEL_MEDIUM);
        }, "high");

        assertTrue(highPicked > 350);
    }

    @Test
    void randomizeListWithFieldRatingUsesReflection() {
        List<FieldRatedItem> input = List.of(
                new FieldRatedItem("low", "1"),
                new FieldRatedItem("high", "10")
        );

        int highFirst = countFirstSelections(input, seed -> {
            RandomGenerator<FieldRatedItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, Boolean.TRUE);
        }, "high");

        assertTrue(highFirst > 350);
    }

    @Test
    void randomizeListWithInvalidStringRatingFallsBackToDefaultWeight() {
        List<FieldRatedItem> input = List.of(
                new FieldRatedItem("valid", "10"),
                new FieldRatedItem("invalid", "bad")
        );

        List<FieldRatedItem> randomized = new RandomGenerator<FieldRatedItem>(new Random(11L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithThrowingGetterFallsBackToDefaultWeight() {
        List<ThrowingGetterItem> input = List.of(
                new ThrowingGetterItem("a", 1),
                new ThrowingGetterItem("b", 10)
        );

        List<ThrowingGetterItem> randomized = new RandomGenerator<ThrowingGetterItem>(new Random(10L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithLegacyStringRatingMarkerIsSupported() {
        List<String> input = List.of(
                "low~~~rating~~~1",
                "high~~~rating~~~10"
        );

        int highFirst = countFirstSelections(input, seed -> {
            RandomGenerator<String> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, Boolean.TRUE);
        }, "high~~~rating~~~10");

        assertTrue(highFirst > 350);
    }

    @Test
    void randomizeListWithMalformedLegacyStringRatingFallsBackToDefaultWeight() {
        List<String> input = List.of(
                "low~~~rating~~~1",
                "bad~~~rating~~~"
        );

        List<String> randomized = new RandomGenerator<String>(new Random(11L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithSignedLegacyStringRatingIsSupported() {
        List<String> input = List.of(
                "positive~~~rating~~~+7",
                "negative~~~rating~~~-7"
        );

        List<String> randomized = new RandomGenerator<String>(new Random(17L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithLegacyStringRatingBreaksOnUnexpectedCharacter() {
        List<String> input = List.of(
                "ok~~~rating~~~1",
                "broken~~~rating~~~+x"
        );

        List<String> randomized = new RandomGenerator<String>(new Random(18L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithMissingRatingFallsBackToEqualWeight() {
        List<PlainItem> input = List.of(
                new PlainItem("a"),
                new PlainItem("b")
        );

        int firstItemCount = countFirstSelections(input, seed -> {
            RandomGenerator<PlainItem> seededGenerator = new RandomGenerator<>(new Random(seed));
            return seededGenerator.randomize(input, Boolean.TRUE);
        }, "a");

        assertTrue(firstItemCount > 300);
        assertTrue(firstItemCount < 450);
    }

    @Test
    void randomizeListWithStringWithoutRatingUsesDefaultWeight() {
        List<String> input = List.of("alpha", "beta");

        List<String> randomized = new RandomGenerator<String>(new Random(12L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithNullValueUsesDefaultWeight() {
        List<String> input = new java.util.ArrayList<>(java.util.Arrays.asList("alpha", null));

        List<String> randomized = new RandomGenerator<String>(new Random(13L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithBooleanFalseUsesNormalPath() {
        List<RatedItem> input = List.of(
                new RatedItem("low", 1),
                new RatedItem("high", 10)
        );

        List<RatedItem> randomized = new RandomGenerator<RatedItem>(new Random(15L)).randomize(input, Boolean.FALSE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithRatingLevelOffUsesNormalPath() {
        List<RatedItem> input = List.of(
                new RatedItem("low", 1),
                new RatedItem("high", 10)
        );

        List<RatedItem> randomized = new RandomGenerator<RatedItem>(new Random(16L)).randomize(input, RandomGenerator.RATING_LEVEL_OFF);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithZeroRatingUsesMinimumWeight() {
        List<Integer> input = List.of(0, 10);

        List<Integer> randomized = new RandomGenerator<Integer>(new Random(14L)).randomize(input, Boolean.TRUE);

        assertEquals(2, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithNullAndNegativeMaxResultsReturnsOriginalShuffle() {
        List<String> input = List.of("a", "b", "c");

        List<String> withNull = generator.randomize(input, (Integer) null);
        List<String> withNegative = generator.randomize(input, Integer.valueOf(-1));

        assertEquals(3, withNull.size());
        assertEquals(3, withNegative.size());
    }

    @Test
    void randomizeListWithMaxResultsGreaterThanSizeReturnsAllElements() {
        List<String> input = List.of("a", "b", "c");

        List<String> randomized = generator.randomize(input, Integer.valueOf(10));

        assertEquals(3, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithMaxResultsAndBooleanFalseUsesNormalLimitPath() {
        List<String> input = List.of("a", "b", "c", "d");

        List<String> randomized = generator.randomize(input, Integer.valueOf(2), Boolean.FALSE);

        assertEquals(2, randomized.size());
        assertTrue(input.containsAll(randomized));
    }

    @Test
    void randomizeStringSplitsAndRejoins() {
        String randomized = generator.randomize("one~~~two~~~three", "~~~");

        assertTrue(List.of("one", "two", "three").containsAll(List.of(randomized.split("~~~"))));
    }

    @Test
    void randomizeStringHonorsMaxResults() {
        String randomized = generator.randomize("one~~~two~~~three", "~~~", 2);

        assertEquals(2, randomized.split("~~~", -1).length);
    }

    @Test
    void randomizeStringSupportsNullSeparatorAndEmptyInput() {
        String randomizedWithNullSeparator = generator.randomize("one~~~two~~~three", null);
        String randomizedEmpty = generator.randomize("", "~~~");
        String randomizedNull = generator.randomize(null, "~~~");

        assertTrue(List.of("one", "two", "three").containsAll(List.of(randomizedWithNullSeparator.split("~~~"))));
        assertEquals("", randomizedEmpty);
        assertEquals("", randomizedNull);
    }

    @Test
    void coerceToDoubleSupportsNullValues() throws Exception {
        Method method = RandomGenerator.class.getDeclaredMethod("coerceToDouble", Object.class);
        method.setAccessible(true);

        Object result = method.invoke(generator, new Object[]{null});

        assertEquals(1.0d, ((Number) result).doubleValue());
    }

    @Test
    void readHandlesIllegalAccess() throws Exception {
        Field field = String.class.getDeclaredField("value");
        Method method = RandomGenerator.class.getDeclaredMethod("read", Object.class, Field.class);
        method.setAccessible(true);

        Object result = method.invoke(generator, "abc", field);

        assertTrue(result instanceof java.util.Optional<?> optional && optional.isEmpty());
    }

    private <T> int countFirstSelections(List<T> input,
                                         java.util.function.Function<Long, List<T>> randomizer,
                                         String expectedFirst) {
        int matches = 0;

        for (long seed = 0; seed < 500; seed++) {
            List<T> randomized = randomizer.apply(seed);
            if (identify(randomized.get(0)).equals(expectedFirst)) {
                matches++;
            }
        }

        return matches;
    }

    private String identify(Object value) {
        if (value instanceof RatedItem item) {
            return item.label();
        }

        if (value instanceof FieldRatedItem item) {
            return item.label();
        }

        if (value instanceof PlainItem item) {
            return item.label();
        }

        return value == null ? "null" : value.toString();
    }

    private record RatedItem(String label, int rating) {
    }

    private record ThrowingGetterItem(String label, int rating) {
        private Integer getRating() {
            throw new IllegalStateException("boom");
        }
    }

    private static final class FieldRatedItem {
        private final String label;
        private final String rating;

        private FieldRatedItem(String label, String rating) {
            this.label = label;
            this.rating = rating;
        }

        private String label() {
            return label;
        }
    }

    private record PlainItem(String label) {
    }
}
