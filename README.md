# Random Generator

`random-generator` is a small Java 21 project for shuffling lists and delimiter-separated strings, with optional result limits and rating-based filtering. It also includes a CLI for generating random full names.

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
- `RATING = "rating"`
- `RATING_LEVEL_OFF = 0`
- `RATING_LEVEL_LOW = 1`
- `RATING_LEVEL_MEDIUM = 2`
- `RATING_LEVEL_HIGH = 3`

Supported methods:

- `List<T> randomize(List<T> tList)`
- `List<T> randomize(List<T> tList, Boolean useRating)`
- `List<T> randomize(List<T> tList, int ratingLevel)`
- `List<T> randomize(List<T> tList, Integer maxResults)`
- `List<T> randomize(List<T> tList, Integer maxResults, Boolean useRating)`
- `List<T> randomize(List<T> tList, Integer maxResults, int ratingLevel)`
- `String randomize(String thisString, String thisSeparator)`
- `String randomize(String thisString, String thisSeparator, Integer maxResults)`

Behavior summary:

- `null` list inputs are treated as empty lists.
- `null` string inputs are treated as empty strings.
- String inputs are split using the supplied separator, or the default delimiter when the separator is `null`.
- Empty trailing segments are ignored when splitting.
- Randomization uses a shuffled copy, so the input collection is not modified.
- `maxResults` trims the randomized result after shuffling.
- Rating-aware overloads look for embedded markers using the default format:

```text
<value>~~~rating~~~<number>
```

Values without a rating marker, or values that cannot be parsed cleanly, are treated as `RATING_LEVEL_HIGH`.

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
