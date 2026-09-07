package com.klotski.ui;

import java.awt.Color;

/**
 * Small, desktop-native palette catalog.  Theme ids are persisted by
 * {@link com.klotski.core.SaveManager}; the palette never changes puzzle
 * rules, saved records, or solver behavior.
 */
public enum DesktopTheme {
    /** The original dark board with a light home surface. */
    MIDNIGHT("midnight", new Color(40, 40, 40), new Color(60, 60, 60),
            new Color(32, 120, 70), Color.WHITE, new Color(245, 247, 250),
            new Color(32, 40, 48), new Color(86, 96, 108), new Color(255, 193, 7)),

    /** A blue-green palette suitable for a brighter desktop session. */
    OCEAN("ocean", new Color(16, 47, 64), new Color(24, 83, 105),
            new Color(20, 105, 120), Color.WHITE, new Color(231, 245, 249),
            new Color(22, 57, 72), new Color(55, 91, 103), new Color(255, 235, 59));

    private final String id;
    private final Color boardBackground;
    private final Color boardSurface;
    private final Color tile;
    private final Color tileText;
    private final Color homeBackground;
    private final Color homeTitle;
    private final Color homeSecondary;
    private final Color focusIndicator;

    DesktopTheme(String id, Color boardBackground, Color boardSurface, Color tile,
            Color tileText, Color homeBackground, Color homeTitle, Color homeSecondary) {
        this(id, boardBackground, boardSurface, tile, tileText, homeBackground,
                homeTitle, homeSecondary, new Color(255, 193, 7));
    }

    DesktopTheme(String id, Color boardBackground, Color boardSurface, Color tile,
            Color tileText, Color homeBackground, Color homeTitle, Color homeSecondary,
            Color focusIndicator) {
        this.id = id;
        this.boardBackground = boardBackground;
        this.boardSurface = boardSurface;
        this.tile = tile;
        this.tileText = tileText;
        this.homeBackground = homeBackground;
        this.homeTitle = homeTitle;
        this.homeSecondary = homeSecondary;
        this.focusIndicator = focusIndicator;
    }

    /**
     * Returns the stable persistence id.
     *
     * @return stable theme id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the board component background.
     *
     * @return board background
     */
    public Color getBoardBackground() {
        return boardBackground;
    }

    /**
     * Returns the rounded board surface color.
     *
     * @return board surface color
     */
    public Color getBoardSurface() {
        return boardSurface;
    }

    /**
     * Returns the numbered tile fill.
     *
     * @return tile color
     */
    public Color getTile() {
        return tile;
    }

    /**
     * Returns the numbered tile text color.
     *
     * @return tile text color
     */
    public Color getTileText() {
        return tileText;
    }

    /**
     * Returns the Home card background.
     *
     * @return Home background color
     */
    public Color getHomeBackground() {
        return homeBackground;
    }

    /**
     * Returns the Home title color.
     *
     * @return Home title color
     */
    public Color getHomeTitle() {
        return homeTitle;
    }

    /**
     * Returns the Home secondary text color.
     *
     * @return Home secondary color
     */
    public Color getHomeSecondary() {
        return homeSecondary;
    }

    /**
     * Returns the visible keyboard-focus indicator color.
     *
     * @return focus indicator color
     */
    public Color getFocusIndicator() {
        return focusIndicator;
    }

    /**
     * Resolves a persisted id, falling back to the conservative original
     * palette when a future or corrupt value is encountered.
     *
     * @param id persisted id
     * @return matching theme or {@link #MIDNIGHT}
     */
    public static DesktopTheme fromId(String id) {
        if (id != null) {
            for (DesktopTheme theme : values()) {
                if (theme.id.equalsIgnoreCase(id.trim())) {
                    return theme;
                }
            }
        }
        return MIDNIGHT;
    }
}
