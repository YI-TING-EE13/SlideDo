package com.klotski.android;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates lightweight screen transitions without coupling navigation to a
 * specific screen implementation.
 */
final class AndroidMotion {
    private static final long EXIT_DURATION_MS = 120;
    private static final long ENTER_DURATION_MS = 240;
    private static final long ENTER_STAGGER_MS = 28;
    private static final int MAX_STAGGER_INDEX = 7;

    private final Activity activity;

    AndroidMotion(Activity activity) {
        this.activity = activity;
    }

    /**
     * Fades and lifts the current screen before its view hierarchy is replaced.
     */
    void animateExit(View root, boolean reducedMotion, Runnable onFinished) {
        if (root == null || reducedMotion || !root.isLaidOut()) {
            onFinished.run();
            return;
        }

        setInteractionsEnabled(root, false);
        root.animate().cancel();
        root.animate()
                .alpha(0.08f)
                .translationY(-dp(6))
                .setDuration(EXIT_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withLayer()
                .withEndAction(onFinished)
                .start();
    }

    /**
     * Reveals the meaningful top-level groups in reading order with a short
     * stagger. The stable background remains visible, avoiding a full-screen
     * flash between destinations.
     */
    void animateEntrance(View root, boolean reducedMotion) {
        root.animate().cancel();
        setInteractionsEnabled(root, true);
        root.setAlpha(1f);
        root.setTranslationY(0f);
        List<View> elements = collectTopLevelElements(root);
        resetElements(elements);
        if (reducedMotion || elements.isEmpty()) {
            return;
        }

        float offset = dp(14);
        for (View element : elements) {
            element.animate().cancel();
            element.setAlpha(0.12f);
            element.setTranslationY(offset);
        }

        for (int index = 0; index < elements.size(); index++) {
            View element = elements.get(index);
            long delay = Math.min(index, MAX_STAGGER_INDEX) * ENTER_STAGGER_MS;
            element.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(ENTER_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .withLayer()
                    .start();
        }
    }

    /**
     * Gives the Results completion mark one restrained settle after the screen
     * entrance. Reduced motion immediately applies the final scale.
     */
    void animateCompletionMark(View mark, boolean reducedMotion) {
        mark.animate().cancel();
        if (reducedMotion) {
            mark.setScaleX(1f);
            mark.setScaleY(1f);
            return;
        }
        mark.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420)
                .setInterpolator(new OvershootInterpolator(1.15f))
                .withLayer()
                .start();
    }

    private List<View> collectTopLevelElements(View root) {
        ViewGroup container = root instanceof ViewGroup ? (ViewGroup) root : null;
        if (root instanceof ScrollView && container.getChildCount() > 0
                && container.getChildAt(0) instanceof ViewGroup) {
            container = (ViewGroup) container.getChildAt(0);
        }

        List<View> elements = new ArrayList<>();
        if (container == null) {
            elements.add(root);
            return elements;
        }
        for (int index = 0; index < container.getChildCount(); index++) {
            View child = container.getChildAt(index);
            if (child.getVisibility() == View.VISIBLE) {
                elements.add(child);
            }
        }
        return elements;
    }

    private void resetElements(List<View> elements) {
        for (View element : elements) {
            element.animate().cancel();
            element.setAlpha(1f);
            element.setTranslationY(0f);
        }
    }

    private void setInteractionsEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            setInteractionsEnabled(group.getChildAt(index), enabled);
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
