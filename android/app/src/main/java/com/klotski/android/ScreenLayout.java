package com.klotski.android;

import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * Standard scroll-backed screen shell for Android menu-style screens.
 */
final class ScreenLayout {
    final ScrollView root;
    final LinearLayout content;

    ScreenLayout(ScrollView root, LinearLayout content) {
        this.root = root;
        this.content = content;
    }
}
