package com.klotski.ui;

/** Coalesces navigation requested while the Swing board is moving. */
final class DesktopDeferredNavigation {
    private Runnable pending;

    void request(boolean boardBusy, Runnable navigation) {
        if (!boardBusy) {
            pending = null;
            navigation.run();
            return;
        }
        pending = navigation;
    }

    boolean runIfIdle(boolean boardBusy) {
        if (boardBusy || pending == null) {
            return false;
        }
        Runnable navigation = pending;
        pending = null;
        navigation.run();
        return true;
    }

    void clear() {
        pending = null;
    }
}
