package com.example.firstassigment.util;

import javafx.scene.Group;
import javafx.scene.shape.SVGPath;

public final class Icons {

    private Icons() {}

    public static final String PLAY   = "M8 5v14l11-7z";
    public static final String PAUSE  = "M6 5h4v14H6zM14 5h4v14h-4z";
    public static final String STOP   = "M6 6h12v12H6z";
    public static final String NEXT   = "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z";
    public static final String PREV   = "M18 6l-8.5 6L18 18V6zM8 6H6v12h2V6z";
    public static final String VOLUME = "M3 10v4h3l4 4V6L6 10H3zM14 8.5v7c1.5-.6 2.5-2 2.5-3.5S15.5 9.1 14 8.5z";
    public static final String MUTE   = "M3 10v4h3l4 4V6L6 10H3zM15.5 10.5l2 2m0-2l-2 2";
    public static final String ADD    = "M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5z";
    public static final String REMOVE = "M5 11h14v2H5z";
    public static final String QUEUE  = "M3 6h13v2H3zM3 10h13v2H3zM3 14h9v2H3zM17 9v8l5-4z";
    public static final String FULLSCREEN      = "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z";
    public static final String FULLSCREEN_EXIT = "M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z";
    public static final String ARROW_UP   = "M12 4l-8 8h5v8h6v-8h5z";
    public static final String ARROW_DOWN = "M12 20l8-8h-5V4h-6v8H4z";
    public static final String HELP       = "M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5 0-2.21-1.79-4-4-4z";
    public static final String SEARCH     = "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
    public static final String HOME       = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";
    public static final String VIDEO      = "M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z";
    public static final String PLAYLIST   = "M4 6h16v2H4zM4 11h16v2H4zM4 16h10v2H4zM19 14v6l4-3z";
    public static final String ALBUM      = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z";
    public static final String NOTE       = "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-8z";
    public static final String PALETTE    = "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z";

    /** Icon tinted by the active theme through the ".app-icon" CSS rule. */
    public static Group icon(String svg, double size) {
        return icon(svg, size, null);
    }

    /** Icon tinted by the theme, with an optional extra style class (e.g. "note-icon"). */
    public static Group icon(String svg, double size, String extraStyleClass) {
        SVGPath path = new SVGPath();
        path.setContent(svg);
        path.getStyleClass().add("app-icon");
        if (extraStyleClass != null) path.getStyleClass().add(extraStyleClass);

        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);

        double scaledWidth  = path.getBoundsInLocal().getWidth()  * scale;
        double scaledHeight = path.getBoundsInLocal().getHeight() * scale;
        path.setTranslateX(-scaledWidth  / 2 + size / 2);
        path.setTranslateY(-scaledHeight / 2 + size / 2);

        Group group = new Group(path);
        group.setManaged(true);
        group.setMouseTransparent(true);
        return group;
    }
}