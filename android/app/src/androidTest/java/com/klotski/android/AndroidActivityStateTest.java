package com.klotski.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import org.junit.Test;

/**
 * Regression coverage for Activity state and navigation helpers extracted from MainActivity.
 */
public class AndroidActivityStateTest {
    @Test
    public void backNavigationRoutesInfoScreensBackToGameWhenGameIsActive() {
        assertEquals(AndroidNavigation.BackTarget.GAME,
                AndroidNavigation.backTarget(Screen.HOW_TO_PLAY, Screen.GAME, true));
        assertEquals(AndroidNavigation.BackTarget.GAME,
                AndroidNavigation.backTarget(Screen.RECORDS, Screen.GAME, true));
        assertEquals(AndroidNavigation.BackTarget.GAME,
                AndroidNavigation.backTarget(Screen.SETTINGS, Screen.GAME, true));
    }

    @Test
    public void backNavigationUsesHomeForFlowScreensAndFinishForHome() {
        assertEquals(AndroidNavigation.BackTarget.HOME,
                AndroidNavigation.backTarget(Screen.GAME, Screen.HOME, true));
        assertEquals(AndroidNavigation.BackTarget.HOME,
                AndroidNavigation.backTarget(Screen.RESULTS, Screen.HOME, true));
        assertEquals(AndroidNavigation.BackTarget.HOME,
                AndroidNavigation.backTarget(Screen.TUTORIAL, Screen.HOME, false));
        assertEquals(AndroidNavigation.BackTarget.FINISH,
                AndroidNavigation.backTarget(Screen.HOME, Screen.HOME, false));
    }

    @Test
    public void backNavigationSavesOnlyGameAndResultsScreens() {
        assertTrue(AndroidNavigation.shouldSaveBeforeBack(Screen.GAME));
        assertTrue(AndroidNavigation.shouldSaveBeforeBack(Screen.RESULTS));
        assertFalse(AndroidNavigation.shouldSaveBeforeBack(Screen.HOME));
        assertFalse(AndroidNavigation.shouldSaveBeforeBack(Screen.SETTINGS));
    }

    @Test
    public void activityStateRoundTripPreservesNavigationAndResult() {
        Bundle bundle = new Bundle();
        AndroidGameStore.Best previousBest = new AndroidGameStore.Best(20, 40_000);
        GameResult result = new GameResult(4, 18, 35_000, false, true, previousBest);

        AndroidActivityState.save(bundle, Screen.RESULTS, Screen.GAME, true, 2, 1, result);
        AndroidActivityState.Snapshot snapshot = AndroidActivityState.restore(bundle, 0);

        assertEquals(Screen.RESULTS, snapshot.screen);
        assertEquals(Screen.GAME, snapshot.infoReturnScreen);
        assertTrue(snapshot.gameStarted);
        assertEquals(2, snapshot.onboardingPage);
        assertEquals(1, snapshot.tutorialStep);
        assertNotNull(snapshot.result);
        assertEquals(4, snapshot.result.size);
        assertEquals(18, snapshot.result.moves);
        assertEquals(35_000, snapshot.result.timeMs);
        assertFalse(snapshot.result.assisted);
        assertTrue(snapshot.result.newBest);
        assertNotNull(snapshot.result.previousBest);
        assertEquals(20, snapshot.result.previousBest.moves);
        assertEquals(40_000, snapshot.result.previousBest.timeMs);
    }

    @Test
    public void activityStateRoundTripHandlesMissingResult() {
        Bundle bundle = new Bundle();

        AndroidActivityState.save(bundle, Screen.HOME, Screen.HOME, false, 0, 0, null);
        AndroidActivityState.Snapshot snapshot = AndroidActivityState.restore(bundle, 0);

        assertEquals(Screen.HOME, snapshot.screen);
        assertNull(snapshot.result);
    }
}
