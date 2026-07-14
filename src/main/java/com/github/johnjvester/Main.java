package com.github.johnjvester;

import java.util.List;
import java.util.Locale;

public class Main {
    /**
     * <h3>CLI for RandomGenerator</h3>
     * <p>Usage: java -jar random-generator-version listString [options]</p>
     * <p>where:</p>
     * <ul>
     * <li>listString = a concatenated list using either the default delimiter (~~~) or the value of the delimiterString provided.</li>
     * </ul>
     * <p>options:</p>
     * <ul>
     * <li>-delimiter = the custom delimiter to use</li>
     * <li>-returnSize = the number of elements to return</li>
     * <li>-randomData = used to call RandomGeneratorData options</li>
     * <li>-nameFormat = one of the possible NameFormat enum options</li>
     * </ul>
     * <p>examples:</p>
     * <ul>
     * <li>java -jar random-generator-version One~~~Two~~~Three~~Four~~~Five~~~<br>
     * Randomizes a list of five elements using the default delimiter (~~~)
     * </li>
     * <li>java -jar random-generator-version One^^Two^^Three^^Four^^Five^^ -delimiter ^^<br>
     * Randomizes a list of five elements using a custom delimiter (^^)
     * </li>
     * <li>java -jar random-generator-version One~~~Two~~~Three~~~Four~~~Five~~~ -returnSize 3<br>
     * Randomizes a list of five elements using the default delimiter (~~~), returning only three elements
     * </li>
     * <li>java -jar random-generator-version One^^Two^^Three^^Four^^Five^^ -delimiter ^^ -returnSize 3<br>
     * Randomizes a list of five elements using a custom delimiter (^^), returning only three elements
     * </li>
     * <li>java -jar random-generator-version -randomData fullNames -nameFormat FIRST_LAST -returnSize 101<br>
     * Returns a list of 101 random full names in first name last name format.
     * </li>
     * </ul>
     *
     * @param args see examples for possible usage options.
     * @since 1.0
     */
    public static void main(String[] args) {
        CliOptions options = CliOptions.parse(args);
        RandomGenerator<String> generator = new RandomGenerator<>();

        if (options.randomData != null) {
            List<String> values = RandomGeneratorData.fullNames(options.returnSizeOrDefault(), options.nameFormat);
            System.out.println(String.join(options.delimiter, generator.randomize(values)));
            return;
        }

        if (options.listString == null || options.listString.isBlank()) {
            printUsage();
            return;
        }

        String output = options.returnSize == null
                ? generator.randomize(options.listString, options.delimiter)
                : generator.randomize(options.listString, options.delimiter, options.returnSize);
        System.out.println(output);
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar random-generator-version listString [options]");
    }

    private static final class CliOptions {
        private String listString;
        private String delimiter = RandomGenerator.DEFAULT_DELIMITER;
        private Integer returnSize;
        private String randomData;
        private NameFormat nameFormat = NameFormat.FIRST_LAST;

        static CliOptions parse(String[] args) {
            CliOptions options = new CliOptions();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "-delimiter" -> options.delimiter = nextValue(args, ++i, arg);
                    case "-returnSize" -> options.returnSize = Integer.valueOf(nextValue(args, ++i, arg));
                    case "-randomData" -> options.randomData = nextValue(args, ++i, arg);
                    case "-nameFormat" -> options.nameFormat = NameFormat.valueOf(nextValue(args, ++i, arg).toUpperCase(Locale.ROOT));
                    default -> {
                        if (arg.startsWith("-")) {
                            throw new IllegalArgumentException("Unknown option: " + arg);
                        }

                        if (options.listString == null) {
                            options.listString = arg;
                        } else {
                            options.listString = options.listString + " " + arg;
                        }
                    }
                }
            }

            return options;
        }

        int returnSizeOrDefault() {
            return returnSize == null ? 101 : returnSize;
        }

        private static String nextValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }

            return args[index];
        }
    }
}
