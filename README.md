# Random Generator

`random-generator` is a small Java 21 project for shuffling lists and delimiter-separated strings, with optional result limits and weighted rating-based selection. It also includes a CLI for generating random full names.

## Requirements

- Java 21
- Gradle wrapper (`./gradlew`)

## Build

```bash
./gradlew build
```

## Test

```bash
./gradlew test
```

Jacoco coverage is enabled and enforced at 100% line coverage.

```bash
./gradlew test jacocoTestCoverageVerification jacocoTestReport
```

HTML coverage output is written to:

```text
build/reports/jacoco/test/html/index.html
```

## Public API

### `RandomGenerator<T>`

Package: `com.github.johnjvester`

Supported constants:

- `DEFAULT_DELIMITER = "~~~"`
- `RATING = "rating"` — the property/field name used for weighted selection, and the legacy string marker name
- `RATING_LEVEL_OFF = 0` — disable rating weighting
- `RATING_LEVEL_LOW = 1` — low weighting strength
- `RATING_LEVEL_MEDIUM = 2` — medium weighting strength
- `RATING_LEVEL_HIGH = 3` — high weighting strength

Supported methods:

- `List<T> randomize(List<T> tList)`
- `List<T> randomize(List<T> tList, Boolean useRating)`
- `List<T> randomize(List<T> tList, int ratingLevel)`
- `List<T> randomize(List<T> tList, Integer maxResults)`
- `List<T> randomize(List<T> tList, Integer maxResults, Boolean useRating)`
- `List<T> randomize(List<T> tList, Integer maxResults, int ratingLevel)`
- `String randomize(String thisString, String thisSeparator)`
- `String randomize(String thisString, String thisSeparator, Integer maxResults)`

Examples:

```java
import com.github.johnjvester.RandomGenerator;

import java.util.List;

class Team {
    private final String name;
    private final double rating;

    Team(String name, double rating) {
        this.name = name;
        this.rating = rating;
    }

    String name() {
        return name;
    }

    double rating() {
        return rating;
    }
}

RandomGenerator<Team> generator = new RandomGenerator<>();
List<Team> teams = List.of(
    new Team("alpha", 1.0),
    new Team("beta", 3.5),
    new Team("gamma", 9.0),
    new Team("delta", 2.25)
);

List<Team> randomized = generator.randomize(teams);
List<Team> limited = generator.randomize(teams, 2);
```

Using `ratingLevel` to increase how strongly the `rating` value influences the result:

```java
import com.github.johnjvester.RandomGenerator;

import java.util.List;

class Team {
    private final String name;
    private final double rating;

    Team(String name, double rating) {
        this.name = name;
        this.rating = rating;
    }

    String name() {
        return name;
    }

    double rating() {
        return rating;
    }
}

RandomGenerator<Team> generator = new RandomGenerator<>();
List<Team> teams = List.of(
    new Team("alpha", 1.0),
    new Team("beta", 3.5),
    new Team("gamma", 9.0),
    new Team("delta", 2.25)
);

List<Team> lightlyWeighted = generator.randomize(teams, RandomGenerator.RATING_LEVEL_LOW);
List<Team> stronglyWeighted = generator.randomize(teams, RandomGenerator.RATING_LEVEL_HIGH);
```

Behavior summary:

- `null` list inputs are treated as empty lists.
- `null` string inputs are treated as empty strings.
- String inputs are split using the supplied separator, or the default delimiter when the separator is `null`.
- Empty trailing segments are ignored when splitting.
- Randomization uses a shuffled copy, so the input collection is not modified.
- `maxResults` trims the randomized result after shuffling.
- Rating-aware overloads read a `rating` property, getter, or field when present.
- The `rating` value can be any numeric type, including floating-point values.
- Higher ratings increase the chance that an item appears earlier in the randomized result.
- `RATING_LEVEL_LOW`, `RATING_LEVEL_MEDIUM`, and `RATING_LEVEL_HIGH` progressively increase how much the rating influences selection.
- Legacy strings in the form `<value>~~~rating~~~<number>` are still recognized as a fallback.

## CLI

The CLI is implemented in `Main.main(String[] args)`.

### Usage

```bash
java -jar random-generator-version listString [options]
```

### Options

- `-delimiter <value>`: custom delimiter for parsing and output
- `-returnSize <n>`: maximum number of elements to return
- `-randomData <value>`: use built-in random data generation
- `-nameFormat <enum>`: format used with `-randomData fullNames`

### Examples

Randomize a list using the default delimiter:

```bash
java -jar random-generator-version One~~~Two~~~Three~~~Four~~~Five~~~
```

Randomize a list using a custom delimiter:

```bash
java -jar random-generator-version One^^Two^^Three^^Four^^Five^^ -delimiter ^^
```

Randomize and limit the result size:

```bash
java -jar random-generator-version One~~~Two~~~Three~~~Four~~~Five~~~ -returnSize 3
```

Randomize with a custom delimiter and size limit:

```bash
java -jar random-generator-version One^^Two^^Three^^Four^^Five^^ -delimiter ^^ -returnSize 3
```

Generate 101 random full names:

```bash
java -jar random-generator-version -randomData fullNames -nameFormat FIRST_LAST -returnSize 101
```

## Random Data

`RandomGeneratorData` currently provides one data source:

- `fullNames(int count, NameFormat nameFormat)`

Supported `NameFormat` values:

- `FIRST_LAST`
- `LAST_FIRST`
- `FIRST_MIDDLE_LAST`

If `nameFormat` is `null`, the default is `FIRST_LAST`.

## Project Layout

```text
src/main/java/com/github/johnjvester/
  Main.java
  RandomGenerator.java
  RandomGeneratorData.java
  NameFormat.java

src/test/java/com/github/johnjvester/
  MainTest.java
  RandomGeneratorTest.java
  RandomGeneratorDataTest.java
```

## Notes

- The project targets Java 21 through Gradle toolchains.
- `Main` prints a simple usage line when no list input is provided.
- Unknown options and missing option values cause `IllegalArgumentException`.
- Coverage is intentionally strict; the test suite is expected to exercise all lines.
