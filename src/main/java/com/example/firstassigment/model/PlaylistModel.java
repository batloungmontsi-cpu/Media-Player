package com.example.firstassigment.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.List;

public class PlaylistModel {

    private final ObservableList<File> tracks = FXCollections.observableArrayList();
    private int currentIndex = -1;

    public ObservableList<File> getTracks() { return tracks; }

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int index) { this.currentIndex = index; }

    public boolean isEmpty() { return tracks.isEmpty(); }
    public int size() { return tracks.size(); }

    public void addAll(List<File> files) { tracks.addAll(files); }
    public void remove(int index) { tracks.remove(index); }

    public File current() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) return null;
        return tracks.get(currentIndex);
    }

    /**
     * Next track. If nothing has been selected yet, start at index 0.
     * Otherwise wrap around at the end.
     */
    public int nextIndex() {
        if (tracks.isEmpty()) return -1;
        if (currentIndex < 0) return 0;                       // first play
        return (currentIndex + 1) % tracks.size();
    }

    /**
     * Previous track. If nothing has been selected yet, jump to the LAST item.
     * Otherwise wrap around at the start.
     */
    public int previousIndex() {
        if (tracks.isEmpty()) return -1;
        if (currentIndex < 0) return tracks.size() - 1;       // last item
        return (currentIndex - 1 + tracks.size()) % tracks.size();
    }
}