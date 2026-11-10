package com.example.firstassigment.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Small "now playing" equaliser: a few bars that bounce while a track is
 * playing and settle flat when stopped. Used in the music library table.
 */
public class MiniEqualizer extends HBox {

    private static final int BARS = 4;
    private static final double BASE = 5;

    private final List<Timeline> timelines = new ArrayList<>();

    public MiniEqualizer() {
        setSpacing(2);
        setAlignment(Pos.CENTER_RIGHT);
        setMouseTransparent(true);

        double[] peaks = {11, 17, 13, 18};
        for (int i = 0; i < BARS; i++) {
            Rectangle bar = new Rectangle(3, BASE);
            bar.setArcWidth(2);
            bar.setArcHeight(2);
            bar.getStyleClass().add("viz-bar");
            getChildren().add(bar);

            Timeline t = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(bar.heightProperty(), BASE, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(220 + i * 70),
                            new KeyValue(bar.heightProperty(), peaks[i], Interpolator.EASE_BOTH))
            );
            t.setCycleCount(Timeline.INDEFINITE);
            t.setAutoReverse(true);
            timelines.add(t);
        }
    }

    public void play() {
        timelines.forEach(Timeline::play);
    }

    public void stop() {
        timelines.forEach(Timeline::stop);
        for (Node n : getChildren()) {
            if (n instanceof Rectangle r) r.setHeight(BASE);
        }
    }
}
