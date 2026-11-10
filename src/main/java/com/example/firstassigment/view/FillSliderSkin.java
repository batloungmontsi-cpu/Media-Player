package com.example.firstassigment.view;

import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.Region;

/**
 * Slider skin that paints the "played" portion of the track.
 *
 * <p>The stock {@link SliderSkin} only draws a uniform {@code .track} plus a
 * {@code .thumb}, so there is no styled {@code .fill} node (the matching CSS
 * rule in MediaPlayer.css was a no-op). This skin inserts a {@code .slider-fill}
 * region between the track and the thumb and sizes it to the current value.</p>
 */
public class FillSliderSkin extends SliderSkin {

    private final Region fill = new Region();

    private Region track;
    private Region thumb;

    public FillSliderSkin(Slider control) {
        super(control);

        fill.getStyleClass().add("slider-fill");
        fill.setMouseTransparent(true);
        getChildren().add(1, fill);

        control.valueProperty().addListener((o, ov, nv) -> updateFill());
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        updateFill();
    }

    private void updateFill() {
        if (track == null) track = (Region) getSkinnable().lookup(".track");
        if (thumb == null) thumb = (Region) getSkinnable().lookup(".thumb");
        if (track == null || thumb == null || track.getWidth() <= 0) {
            fill.setVisible(false);
            return;
        }

        Slider s = getSkinnable();
        double range = s.getMax() - s.getMin();
        if (range <= 0) {
            fill.setVisible(false);
            return;
        }

        double startX = track.getLayoutX();
        double endX = thumb.getLayoutX() + thumb.getWidth() / 2;
        double width = Math.max(0, Math.min(track.getWidth(), endX - startX));

        fill.setVisible(width > 0);
        fill.resizeRelocate(startX, track.getLayoutY(), width, track.getHeight());
    }
}
