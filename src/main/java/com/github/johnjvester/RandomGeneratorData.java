package com.github.johnjvester;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomGeneratorData {
    private static final String[] FIRST_NAMES = {
            "Alex", "Avery", "Blake", "Casey", "Drew", "Elliot", "Finley", "Harper",
            "Jordan", "Kai", "Logan", "Morgan", "Parker", "Quinn", "Riley", "Skyler"
    };

    private static final String[] MIDDLE_NAMES = {
            "Lee", "Taylor", "Robin", "Sage", "Reese", "Lane", "Noel", "Shay"
    };

    private static final String[] LAST_NAMES = {
            "Anderson", "Bennett", "Carter", "Dawson", "Ellis", "Foster", "Grant", "Hayes",
            "Iverson", "Jensen", "Keller", "Morris", "Perry", "Reed", "Sullivan", "Turner"
    };

    private RandomGeneratorData() {
    }

    public static List<String> fullNames(int count, NameFormat nameFormat) {
        int safeCount = Math.max(count, 0);
        NameFormat safeFormat = nameFormat == null ? NameFormat.FIRST_LAST : nameFormat;
        List<String> values = new ArrayList<>(safeCount);

        for (int i = 0; i < safeCount; i++) {
            values.add(randomName(safeFormat));
        }

        return values;
    }

    private static String randomName(NameFormat nameFormat) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String middle = MIDDLE_NAMES[random.nextInt(MIDDLE_NAMES.length)];
        String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];

        return switch (nameFormat) {
            case FIRST_LAST -> first + " " + last;
            case LAST_FIRST -> last + ", " + first;
            case FIRST_MIDDLE_LAST -> first + " " + middle + " " + last;
        };
    }
}
