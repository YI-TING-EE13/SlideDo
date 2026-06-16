package com.klotski.android;

import android.view.View;
import android.view.ViewGroup;

/**
 * Utility for moving reusable Android views between parent containers.
 */
final class ViewParentRemover {
    private ViewParentRemover() {
    }

    static void removeFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }
}
