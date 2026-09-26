package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.Direction;
import com.klotski.core.GameModel;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class DesktopDeferredNavigationTest {
    @Test
    void navigationWaitsForIdleAndRunsOnlyOnceUsingLatestRequest() {
        DesktopDeferredNavigation navigation = new DesktopDeferredNavigation();
        int[] destinations = {0, 0};

        navigation.request(true, () -> destinations[0]++);
        navigation.request(true, () -> destinations[1]++);

        assertFalse(navigation.runIfIdle(true));
        assertEquals(0, destinations[0]);
        assertEquals(0, destinations[1]);
        assertTrue(navigation.runIfIdle(false));
        assertFalse(navigation.runIfIdle(false));
        assertEquals(0, destinations[0]);
        assertEquals(1, destinations[1]);
    }

    @Test
    void navigationRunsImmediatelyWhenBoardIsStable() {
        DesktopDeferredNavigation navigation = new DesktopDeferredNavigation();
        int[] destinations = {0};

        navigation.request(false, () -> destinations[0]++);

        assertEquals(1, destinations[0]);
        assertFalse(navigation.runIfIdle(false));
    }

    @Test
    void boardSignalsIdleAfterTheMoveAnimationSettles() throws Exception {
        AtomicInteger idleNotifications = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            GameModel model = new GameModel(3);
            model.scramble(5);
            BoardPanel board = new BoardPanel(model);
            board.setReducedMotion(true);
            board.setWinDialogHandler((parent, moves, timeMs) -> { });
            board.setIdleListener(idleNotifications::incrementAndGet);

            Direction direction = model.getEmptyRow() > 0 ? Direction.UP
                    : model.getEmptyCol() > 0 ? Direction.LEFT : Direction.DOWN;
            assertTrue(model.move(direction));
            assertFalse(board.isBusy());
        });

        assertEquals(1, idleNotifications.get());
    }
}
