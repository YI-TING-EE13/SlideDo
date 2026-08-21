package com.klotski.core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Platform-independent specification for one offline daily puzzle.
 * <p>
 * Version 1 uses the local calendar date as the puzzle identity and derives a
 * stable seed from that date. The date source remains a platform concern so
 * tests and front ends can select the intended time zone explicitly.
 * </p>
 */
public final class DailyChallenge {
    private static final long SEED_NAMESPACE_V1 = 0x534C494445444F00L;
    private static final int SIZE = 4;
    private static final PuzzleDifficulty DIFFICULTY = PuzzleDifficulty.CLASSIC;

    private final LocalDate date;
    private final long seed;

    private DailyChallenge(LocalDate date) {
        this.date = date;
        this.seed = SEED_NAMESPACE_V1 ^ date.toEpochDay();
    }

    /**
     * Creates the version-1 challenge specification for a calendar date.
     *
     * @param date local calendar date selected by the caller
     * @return immutable daily challenge specification
     * @throws NullPointerException when {@code date} is {@code null}
     */
    public static DailyChallenge forDate(LocalDate date) {
        return new DailyChallenge(Objects.requireNonNull(date, "date"));
    }

    /**
     * Restores a challenge from its persisted ISO-8601 identity.
     *
     * @param dateId date in {@code YYYY-MM-DD} form
     * @return immutable daily challenge specification
     * @throws NullPointerException when {@code dateId} is {@code null}
     * @throws java.time.format.DateTimeParseException when the ID is malformed
     */
    public static DailyChallenge fromDateId(String dateId) {
        return forDate(LocalDate.parse(
                Objects.requireNonNull(dateId, "dateId"), DateTimeFormatter.ISO_LOCAL_DATE));
    }

    /**
     * Returns the stable ISO-8601 puzzle identity.
     *
     * @return date in {@code YYYY-MM-DD} form
     */
    public String getDateId() {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    /**
     * Returns the daily board width and height.
     *
     * @return {@code 4}
     */
    public int getSize() {
        return SIZE;
    }

    /**
     * Returns the daily scramble preset.
     *
     * @return Classic difficulty
     */
    public PuzzleDifficulty getDifficulty() {
        return DIFFICULTY;
    }

    /**
     * Returns the versioned deterministic scramble seed.
     *
     * @return stable seed for the challenge date
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Creates a fresh model at the daily starting grid.
     *
     * @return new solvable daily puzzle with zero moves and elapsed time
     */
    public GameModel createGame() {
        GameModel model = new GameModel(SIZE);
        model.scramble(DIFFICULTY, seed);
        return model;
    }
}
