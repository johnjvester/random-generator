package com.github.johnjvester;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @Test
    void mainPrintsUsageWhenNoArgsAreProvided() {
        new Main();
        String output = runMain();

        assertTrue(output.startsWith("Usage: java -jar random-generator-version listString [options]"));
    }

    @Test
    void mainPrintsUsageWhenListStringIsBlank() {
        String output = runMain("   ");

        assertTrue(output.startsWith("Usage: java -jar random-generator-version listString [options]"));
    }

    @Test
    void mainRandomizesListStringWithCustomDelimiter() {
        String output = runMain("One^^Two^^Three^^Four^^Five^^", "-delimiter", "^^", "-returnSize", "3");
        List<String> values = List.of(output.split("\\^\\^"));

        assertEquals(3, values.size());
        assertTrue(List.of("One", "Two", "Three", "Four", "Five").containsAll(values));
    }

    @Test
    void mainRandomizesListStringWithDefaultDelimiterAndNoReturnSize() {
        String output = runMain("One~~~Two~~~Three");
        List<String> values = List.of(output.split("~~~"));

        assertEquals(3, values.size());
        assertTrue(List.of("One", "Two", "Three").containsAll(values));
    }

    @Test
    void mainConcatenatesMultipleListArguments() {
        String output = runMain("One~~~Two", "Three~~~Four", "-returnSize", "2");
        List<String> values = List.of(output.split("~~~"));

        assertEquals(2, values.size());
        assertTrue(List.of("One", "Two Three", "Four").containsAll(values));
    }

    @Test
    void mainGeneratesRandomNames() {
        String output = runMain("-randomData", "fullNames", "-nameFormat", "FIRST_LAST", "-returnSize", "4");
        List<String> values = List.of(output.split("~~~"));

        assertEquals(4, values.size());
        assertTrue(values.stream().allMatch(name -> name.matches("[A-Z][a-z]+ [A-Z][a-z]+")));
    }

    @Test
    void mainGeneratesDefaultRandomNamesWhenReturnSizeIsOmitted() {
        String output = runMain("-randomData", "fullNames");
        List<String> values = List.of(output.split("~~~"));

        assertEquals(101, values.size());
    }

    @Test
    void mainRejectsUnknownOption() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Main.main(new String[]{"-bogus"})
        );

        assertTrue(exception.getMessage().contains("Unknown option"));
    }

    @Test
    void mainRejectsMissingOptionValue() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Main.main(new String[]{"-delimiter"})
        );

        assertTrue(exception.getMessage().contains("Missing value"));
    }

    private String runMain(String... args) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            Main.main(args);
        } finally {
            System.setOut(originalOut);
        }

        return buffer.toString(StandardCharsets.UTF_8).trim();
    }
}
