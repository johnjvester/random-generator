package com.github.johnjvester;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGeneratorDataTest {
    @Test
    void fullNamesProducesRequestedCount() {
        List<String> names = RandomGeneratorData.fullNames(5, NameFormat.FIRST_LAST);

        assertEquals(5, names.size());
    }

    @Test
    void fullNamesRespectsNameFormat() {
        List<String> names = RandomGeneratorData.fullNames(3, NameFormat.LAST_FIRST);

        assertEquals(3, names.size());
        assertTrue(names.stream().allMatch(name -> name.contains(", ")));
    }

    @Test
    void fullNamesSupportsMiddleNameFormatAndNullFallback() {
        List<String> withMiddle = RandomGeneratorData.fullNames(2, NameFormat.FIRST_MIDDLE_LAST);
        List<String> withFallback = RandomGeneratorData.fullNames(1, null);

        assertEquals(2, withMiddle.size());
        assertTrue(withMiddle.stream().allMatch(name -> name.split(" ").length == 3));
        assertEquals(1, withFallback.size());
        assertTrue(withFallback.getFirst().contains(" "));
    }
}
