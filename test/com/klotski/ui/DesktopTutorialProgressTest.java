package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.klotski.core.Direction;
import com.klotski.core.GameModel;

import org.junit.jupiter.api.Test;

class DesktopTutorialProgressTest {
    @Test
    void lessonAdvancesOnlyAfterAdjacentThenWholeLineActions() {
        GameModel model = new GameModel(3);
        model.loadState(new int[][] {{1, 2, 3}, {4, 0, 6}, {7, 5, 8}}, 0);
        DesktopTutorialProgress progress = new DesktopTutorialProgress();

        assertEquals(DesktopTutorialProgress.Step.FIRST_MOVE, progress.getStep());
        model.move(Direction.DOWN);
        progress.observe(model);
        assertEquals(DesktopTutorialProgress.Step.WHOLE_LINE, progress.getStep());

        model.slideLineTo(0, 1);
        progress.observe(model);
        assertEquals(DesktopTutorialProgress.Step.COMPLETE, progress.getStep());
    }
}
