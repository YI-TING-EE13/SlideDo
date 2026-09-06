package com.klotski.ui;

import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;

import java.util.Objects;

/**
 * Creates Desktop sessions through the shared difficulty and scramble contract.
 * <p>
 * The seeded overload is intentionally package-private so deterministic tests
 * and future Desktop entry points can use the same core behavior without
 * introducing a second puzzle-generation algorithm.
 * </p>
 */
final class DesktopGameFactory {
    private DesktopGameFactory() {
    }

    /**
     * Creates a normally randomized Desktop puzzle for a selected difficulty.
     *
     * @param size supported square board size
     * @param difficulty selected scramble preset
     * @return a running model with a fresh active-play timer
     */
    static GameModel create(int size, PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = Objects.requireNonNull(difficulty, "difficulty");
        GameModel model = new GameModel(size);
        model.scramble(selected);
        return model;
    }

    /**
     * Creates a reproducible Desktop puzzle using the shared seeded overload.
     *
     * @param size supported square board size
     * @param difficulty selected scramble preset
     * @param seed deterministic scramble seed
     * @return a running model with the reproducible starting grid
     */
    static GameModel create(int size, PuzzleDifficulty difficulty, long seed) {
        PuzzleDifficulty selected = Objects.requireNonNull(difficulty, "difficulty");
        GameModel model = new GameModel(size);
        model.scramble(selected, seed);
        return model;
    }
}
