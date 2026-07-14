package com.github.johnjvester;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGeneratorTest {
    private final RandomGenerator<String> generator = new RandomGenerator<>();

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
    void randomizeListWithBooleanRatingUsesRatingPath() {
        List<String> input = new java.util.ArrayList<>(List.of(
                "alpha~~~rating~~~1",
                "beta~~~rating~~~3",
                "plain",
                "broken~~~rating~~~",
                "trailing~~~rating~~~12x",
                "gamma~~~rating~~~5"
        ));
        input.add(null);

        List<String> randomized = generator.randomize(input, Boolean.TRUE);

        assertEquals(5, randomized.size());
        assertTrue(randomized.contains("alpha~~~rating~~~1"));
        assertTrue(randomized.contains("beta~~~rating~~~3"));
        assertTrue(randomized.contains("plain"));
        assertTrue(randomized.contains("broken~~~rating~~~"));
        assertTrue(randomized.contains(null));
    }

    @Test
    void randomizeListWithBooleanFalseUsesNormalPath() {
        List<String> input = List.of("a", "b", "c");

        List<String> randomized = generator.randomize(input, Boolean.FALSE);

        assertEquals(3, randomized.size());
        assertTrue(randomized.containsAll(input));
    }

    @Test
    void randomizeListWithRatingLevelIncludesRatedAndUnratedValues() {
        List<String> input = new java.util.ArrayList<>(List.of(
                "alpha~~~rating~~~1",
                "beta~~~rating~~~2",
                "gamma~~~rating~~~4",
                "plain"
        ));
        input.add(null);

        List<String> randomized = generator.randomize(input, RandomGenerator.RATING_LEVEL_MEDIUM);

        assertEquals(2, randomized.size());
        assertTrue(randomized.contains("alpha~~~rating~~~1"));
        assertTrue(randomized.contains("beta~~~rating~~~2"));
        assertTrue(randomized.stream().noneMatch(value -> "gamma~~~rating~~~4".equals(value)));
    }

    @Test
    void randomizeListWithRatingLevelOffUsesNormalShuffle() {
        List<String> input = List.of("a", "b", "c");

        List<String> randomized = generator.randomize(input, RandomGenerator.RATING_LEVEL_OFF);

        assertEquals(3, randomized.size());
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
    void randomizeListWithMaxResultsAndBooleanRatingUsesRatingLimitPath() {
        List<String> input = List.of("alpha~~~rating~~~1", "beta~~~rating~~~3", "gamma~~~rating~~~5");

        List<String> randomized = generator.randomize(input, Integer.valueOf(1), Boolean.TRUE);

        assertEquals(1, randomized.size());
        assertTrue(List.of("alpha~~~rating~~~1", "beta~~~rating~~~3").containsAll(randomized));
    }

    @Test
    void randomizeListWithMaxResultsAndBooleanFalseUsesNormalLimitPath() {
        List<String> input = List.of("a", "b", "c", "d");

        List<String> randomized = generator.randomize(input, Integer.valueOf(2), Boolean.FALSE);

        assertEquals(2, randomized.size());
        assertTrue(input.containsAll(randomized));
    }

    @Test
    void randomizeListWithMaxResultsAndRatingLevelUsesCombinedPath() {
        List<String> input = List.of("alpha~~~rating~~~1", "beta~~~rating~~~4", "gamma~~~rating~~~5");

        List<String> randomized = generator.randomize(input, Integer.valueOf(1), RandomGenerator.RATING_LEVEL_MEDIUM);

        assertEquals(1, randomized.size());
        assertTrue(randomized.contains("alpha~~~rating~~~1"));
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
}
