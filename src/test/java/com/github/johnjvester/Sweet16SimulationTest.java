package com.github.johnjvester;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sweet16SimulationTest {
    private static final Path INPUT = Path.of("ncaa-mens-basketball-sweet-16.txt");
    private static final int MAX_ITERATIONS = 1_000_000;
    private static final Pattern SWEET16_LINE = Pattern.compile(
            "^(.*?) \\(([-+]?\\d+(?:\\.\\d+)?)\\) vs (.*?) \\(([-+]?\\d+(?:\\.\\d+)?)\\)$"
    );
    private static final Pattern PAIR_LINE = Pattern.compile("^(.*?) vs (.*?)$");

    @Test
    void simulateSweet16TournamentAcrossRatingLevels() throws IOException {
        TournamentSpec spec = parse(INPUT);
        List<Integer> ratingLevels = List.of(
                RandomGenerator.RATING_LEVEL_OFF,
                RandomGenerator.RATING_LEVEL_LOW,
                RandomGenerator.RATING_LEVEL_MEDIUM,
                RandomGenerator.RATING_LEVEL_HIGH
        );

        for (int ratingLevel : ratingLevels) {
            CumulativeHitReport report = findCumulativeRoundHits(spec, ratingLevel, MAX_ITERATIONS);

            System.out.printf(Locale.ROOT,
                    "ratingLevel=%d Elite 8 attempts=%s, Final 4 attempts=%s, Title Game attempts=%s, Champion attempts=%s (Total Iterations Required=%s)%n",
                    ratingLevel,
                    formatStage(report.sweet16()),
                    formatStage(report.elite8()),
                    formatStage(report.final4()),
                    formatStage(report.champion()),
                    formatIteration(report.championCumulative()));
        }
    }

    @Test
    void explicitRatingAccessorSupportsNullAndThrowingFallbacks() {
        RandomGenerator<String> generator = new RandomGenerator<>(new Random(7L));

        List<String> nullValueInput = new ArrayList<>(java.util.Arrays.asList("alpha", null));
        List<String> nullValueResult = generator.randomize(nullValueInput, String::length, RandomGenerator.RATING_LEVEL_HIGH);
        assertEquals(2, nullValueResult.size());
        assertTrue(nullValueResult.containsAll(nullValueInput));

        List<ThrowingTeam> throwingInput = List.of(
                new ThrowingTeam("a", 1.0d),
                new ThrowingTeam("b", 10.0d)
        );
        List<ThrowingTeam> throwingResult = new RandomGenerator<ThrowingTeam>(new Random(11L))
                .randomize(throwingInput, Integer.valueOf(1), team -> {
                    throw new IllegalStateException("boom");
                }, RandomGenerator.RATING_LEVEL_HIGH);

        assertEquals(1, throwingResult.size());
        assertTrue(throwingInput.contains(throwingResult.get(0)));
    }

    private CumulativeHitReport findCumulativeRoundHits(TournamentSpec spec, int ratingLevel, long maxIterations) {
        StageHit sweet16 = findStageHit(spec, ratingLevel, maxIterations, 1L, outcome -> outcome.matchesSweet16(spec));
        StageHit elite8 = sweet16.found()
                ? findStageHit(spec, ratingLevel, maxIterations, sweet16.cumulativeIteration() + 1, outcome -> outcome.matchesElite8(spec))
                : StageHit.notFound();
        StageHit final4 = elite8.found()
                ? findStageHit(spec, ratingLevel, maxIterations, elite8.cumulativeIteration() + 1, outcome -> outcome.matchesFinal4(spec))
                : StageHit.notFound();
        StageHit final2 = final4.found()
                ? findStageHit(spec, ratingLevel, maxIterations, final4.cumulativeIteration() + 1, outcome -> outcome.matchesFinal2(spec))
                : StageHit.notFound();
        StageHit champion = final2.found()
                ? findStageHit(spec, ratingLevel, maxIterations, final2.cumulativeIteration() + 1, outcome -> outcome.matchesChampion(spec))
                : StageHit.notFound();

        return new CumulativeHitReport(sweet16, elite8, final4, final2, champion);
    }

    private StageHit findStageHit(TournamentSpec spec,
                                  int ratingLevel,
                                  long maxIterations,
                                  long startIteration,
                                  java.util.function.Predicate<TournamentOutcome> predicate) {
        if (startIteration > maxIterations) {
            return StageHit.notFound();
        }

        long attempts = 0;
        for (long iteration = startIteration; iteration <= maxIterations; iteration++) {
            attempts++;
            TournamentOutcome outcome = simulate(spec, ratingLevel, iteration);
            if (predicate.test(outcome)) {
                return new StageHit(attempts, iteration);
            }
        }

        return StageHit.notFound();
    }

    private TournamentOutcome simulate(TournamentSpec spec, int ratingLevel, long seed) {
        RandomGenerator<Team> generator = new RandomGenerator<>(new Random(seed));

        List<Team> sweet16Winners = playSweet16Round(generator, spec.sweet16Games(), ratingLevel);
        List<Team> elite8Winners = playBracketRound(generator, sweet16Winners, ratingLevel);
        List<Team> final4Winners = playBracketRound(generator, elite8Winners, ratingLevel);
        Team champion = playGame(generator, final4Winners.get(0), final4Winners.get(1), ratingLevel);

        return new TournamentOutcome(
                names(sweet16Winners),
                names(elite8Winners),
                names(final4Winners),
                champion.name()
        );
    }

    private List<Team> playSweet16Round(RandomGenerator<Team> generator, List<Game> games, int ratingLevel) {
        List<Team> winners = new ArrayList<>(games.size());
        for (Game game : games) {
            winners.add(playGame(generator, game.left(), game.right(), ratingLevel));
        }

        return winners;
    }

    private List<Team> playBracketRound(RandomGenerator<Team> generator, List<Team> teams, int ratingLevel) {
        List<Team> winners = new ArrayList<>(teams.size() / 2);
        for (int index = 0; index < teams.size(); index += 2) {
            winners.add(playGame(generator, teams.get(index), teams.get(index + 1), ratingLevel));
        }

        return winners;
    }

    private Team playGame(RandomGenerator<Team> generator, Team left, Team right, int ratingLevel) {
        return generator.randomize(List.of(left, right), Integer.valueOf(1), Team::rating, ratingLevel).get(0);
    }

    private List<String> names(List<Team> teams) {
        List<String> names = new ArrayList<>(teams.size());
        for (Team team : teams) {
            names.add(team.name());
        }
        return names;
    }

    private TournamentSpec parse(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<String> sweet16Lines = new ArrayList<>();
        List<String> elite8Lines = new ArrayList<>();
        List<String> final4Lines = new ArrayList<>();
        List<String> championshipLines = new ArrayList<>();
        List<String> championLines = new ArrayList<>();

        String section = "";
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.equals("- - - - -")) {
                continue;
            }

            if (line.equals("Sweet 16") || line.equals("Elite 8") || line.equals("Final 4")
                    || line.equals("Championship") || line.equals("Champion")) {
                section = line;
                continue;
            }

            switch (section) {
                case "Sweet 16" -> sweet16Lines.add(line);
                case "Elite 8" -> elite8Lines.add(line);
                case "Final 4" -> final4Lines.add(line);
                case "Championship" -> championshipLines.add(line);
                case "Champion" -> championLines.add(line);
                default -> {
                }
            }
        }

        List<Game> sweet16Games = new ArrayList<>(sweet16Lines.size());
        for (String line : sweet16Lines) {
            Matcher matcher = SWEET16_LINE.matcher(line);
            assertTrue(matcher.matches(), () -> "Unparseable Sweet 16 line: " + line);

            Team left = new Team(matcher.group(1), Double.parseDouble(matcher.group(2)));
            Team right = new Team(matcher.group(3), Double.parseDouble(matcher.group(4)));
            sweet16Games.add(new Game(left, right));
        }

        List<String> sweet16ExpectedWinners = new ArrayList<>(sweet16Games.size());
        for (String line : elite8Lines) {
            List<String> pair = parsePair(line);
            sweet16ExpectedWinners.add(pair.get(0));
            sweet16ExpectedWinners.add(pair.get(1));
        }

        List<String> elite8ExpectedWinners = new ArrayList<>(final4Lines.size() * 2);
        for (String line : final4Lines) {
            List<String> pair = parsePair(line);
            elite8ExpectedWinners.add(pair.get(0));
            elite8ExpectedWinners.add(pair.get(1));
        }

        List<String> final2ExpectedWinners = new ArrayList<>(2);
        for (String line : championshipLines) {
            List<String> pair = parsePair(line);
            final2ExpectedWinners.add(pair.get(0));
            final2ExpectedWinners.add(pair.get(1));
        }

        assertEquals(1, championLines.size(), "Champion section should contain one winner");

        return new TournamentSpec(
                sweet16Games,
                sweet16ExpectedWinners,
                elite8ExpectedWinners,
                final2ExpectedWinners,
                championLines.get(0)
        );
    }

    private List<String> parsePair(String line) {
        Matcher matcher = PAIR_LINE.matcher(line);
        assertTrue(matcher.matches(), () -> "Unparseable pairing line: " + line);
        return List.of(matcher.group(1), matcher.group(2));
    }

    private String formatStage(StageHit stageHit) {
        return stageHit.found()
                ? String.format(Locale.ROOT, "%,d", stageHit.attempts())
                : "not found";
    }

    private String formatIteration(Long iteration) {
        return iteration == null ? "not found" : String.format(Locale.ROOT, "%,d", iteration);
    }

    private record Team(String name, double rating) {
    }

    private record Game(Team left, Team right) {
    }

    private record TournamentSpec(
            List<Game> sweet16Games,
            List<String> sweet16ExpectedWinners,
            List<String> elite8ExpectedWinners,
            List<String> final2ExpectedWinners,
            String championExpected) {
    }

    private record TournamentOutcome(
            List<String> sweet16Winners,
            List<String> elite8Winners,
            List<String> final4Winners,
            String champion) {

        private boolean matchesSweet16(TournamentSpec spec) {
            return sweet16Winners.equals(spec.sweet16ExpectedWinners());
        }

        private boolean matchesElite8(TournamentSpec spec) {
            return elite8Winners.equals(spec.elite8ExpectedWinners());
        }

        private boolean matchesFinal4(TournamentSpec spec) {
            return final4Winners.equals(spec.final2ExpectedWinners());
        }

        private boolean matchesFinal2(TournamentSpec spec) {
            return final4Winners.equals(spec.final2ExpectedWinners());
        }

        private boolean matchesChampion(TournamentSpec spec) {
            return champion.equals(spec.championExpected());
        }
    }

    private record CumulativeHitReport(
            StageHit sweet16,
            StageHit elite8,
            StageHit final4,
            StageHit final2,
            StageHit champion) {

        private Long sweet16Cumulative() {
            return sweet16.found() ? sweet16.cumulativeIteration() : null;
        }

        private Long elite8Cumulative() {
            return elite8.found() ? elite8.cumulativeIteration() : null;
        }

        private Long final4Cumulative() {
            return final4.found() ? final4.cumulativeIteration() : null;
        }

        private Long final2Cumulative() {
            return final2.found() ? final2.cumulativeIteration() : null;
        }

        private Long championCumulative() {
            return champion.found() ? champion.cumulativeIteration() : null;
        }
    }

    private record StageHit(long attempts, long cumulativeIteration) {
        private static StageHit notFound() {
            return new StageHit(-1L, -1L);
        }

        private boolean found() {
            return attempts >= 0;
        }
    }

    private record ThrowingTeam(String name, double rating) {
    }
}
