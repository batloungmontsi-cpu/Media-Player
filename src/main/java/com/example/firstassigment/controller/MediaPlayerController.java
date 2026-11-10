package com.example.firstassigment.controller;

import com.example.firstassigment.model.PlaylistModel;
import com.example.firstassigment.util.Icons;
import com.example.firstassigment.util.MediaMetadata;
import com.example.firstassigment.view.MediaPlayerView;
import com.example.firstassigment.view.MiniEqualizer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MediaPlayerController {

    private static final double VOLUME_STEP = 0.05;

    private final Stage stage;
    private final PlaylistModel model = new PlaylistModel();
    private final MediaPlayerView view = new MediaPlayerView();

    /** The files currently shown in the playlist list (a filtered view of the playlist). */
    private final ObservableList<File> visibleTracks = FXCollections.observableArrayList();

    /** The curated playlist: a user-built subset of the library. */
    private final ObservableList<File> playlistTracks = FXCollections.observableArrayList();

    /** The files currently shown in the centre library section. */
    private final ObservableList<File> sectionTracks = FXCollections.observableArrayList();

    private MediaPlayer mediaPlayer;
    private File currentFile;
    private boolean muted = false;
    private boolean fullscreen = false;
    private int themeIndex = 0;
    private Section currentSection = Section.HOME;
    private Album selectedAlbum;

    /** Recently played files, most recent first (audio and video). */
    private final List<File> recentTracks = new ArrayList<>();
    private final Map<File, Image> artworkCache = new HashMap<>();
    private final Map<File, String> artistCache = new HashMap<>();
    private final Map<File, Image> videoThumbCache = new HashMap<>();

    /** Music library table data: ID3 tags and lazily loaded durations. */
    private final Map<File, MediaMetadata.Tags> tagCache = new HashMap<>();
    private final Map<File, Duration> durationCache = new HashMap<>();
    private final Set<File> durationUnavailable = new HashSet<>();
    private final Set<File> durationQueue = new LinkedHashSet<>();
    private boolean durationLoading = false;
    private File durationCurrent = null;
    private boolean queueReordering = false;

    /** The library section shown in the centre area. */
    private enum Section { HOME, MUSIC, VIDEO, ALBUMS, PLAYLIST, QUEUE }

    /** A folder-backed album: its tracks plus lazily loaded artist and cover art. */
    private static final class Album {
        final String name;
        final File folder;
        final List<File> tracks;
        private boolean metaLoaded;
        private String artist;
        private Image artwork;

        Album(String name, File folder, List<File> tracks) {
            this.name = name;
            this.folder = folder;
            this.tracks = tracks;
        }

        void loadMeta() {
            if (metaLoaded) return;
            metaLoaded = true;

            for (File t : tracks) {
                String a = MediaMetadata.readArtist(t);
                if (a != null) { artist = a; break; }
            }

            for (File t : tracks) {
                byte[] bytes = MediaMetadata.readArtwork(t);
                if (bytes != null) {
                    Image img = imageFromBytes(bytes);
                    if (img != null) { artwork = img; break; }
                }
            }
            if (artwork == null) {
                File cover = MediaMetadata.findFolderImage(folder);
                if (cover != null) artwork = imageFromUrl(cover.toURI().toString());
            }
        }

        private static Image imageFromBytes(byte[] bytes) {
            try {
                return new Image(new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                return null;
            }
        }

        private static Image imageFromUrl(String url) {
            try {
                Image img = new Image(url);
                return img.isError() ? null : img;
            } catch (Exception e) {
                return null;
            }
        }
    }

    public MediaPlayerController(Stage stage) {
        this.stage = stage;
        view.playlistView.setItems(visibleTracks);
        view.contentView.setItems(sectionTracks);
        view.musicTable.table.setItems(sectionTracks);
        view.albumTable.table.setItems(sectionTracks);
        view.playlistTable.table.setItems(sectionTracks);
        model.getTracks().addListener((ListChangeListener<File>) c -> {
            playlistTracks.removeIf(f -> !model.getTracks().contains(f));
            applyFilter();
            refreshSection();
            updateTrackCount();
        });
        updateTrackCount();
        installPlaylistCells();
        installSectionCells();
        installMusicTable();
        installQueueTable();
        installAlbumTable();
        installPlaylistTable();
        installPlaylistContextMenu();
        wireEvents();
        selectSection(Section.HOME);
    }

    public Scene createScene() {
        Scene scene = view.createScene();
        installDragAndDrop(scene);
        installKeyboardControls(scene);
        return scene;
    }

    public void installKeyboardControls(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getTarget() instanceof javafx.scene.control.TextInputControl) return;
            switch (e.getCode()) {
                case SPACE   -> togglePlayPause();
                case S       -> stop();
                case N       -> playNext();
                case P       -> playPrevious();
                case UP      -> bumpVolume(+VOLUME_STEP);
                case DOWN    -> bumpVolume(-VOLUME_STEP);
                case M       -> toggleMute();
                case ESCAPE  -> { if (fullscreen) toggleFullscreen(); else return; }
                default      -> { return; }
            }
            e.consume();
        });
    }

    /* ---------- event wiring ---------- */

    private void wireEvents() {
        view.playButton.setOnAction(e -> togglePlayPause());
        view.stopButton.setOnAction(e -> stop());
        view.nextButton.setOnAction(e -> playNext());
        view.prevButton.setOnAction(e -> playPrevious());
        view.addButton.setOnAction(e -> addFiles());
        view.removeButton.setOnAction(e -> removeSelected());
        view.playlistAddButton.setOnAction(e -> openAddToPlaylistDialog());
        view.playlistRemoveButton.setOnAction(e -> removeFromPlaylist());
        view.muteButton.setOnAction(e -> toggleMute());
        view.fullscreenButton.setOnAction(e -> toggleFullscreen());
        view.helpButton.setOnAction(e -> openHelpDialog());
        view.themeButton.setOnAction(e -> cycleTheme());

        view.openMenuItem.setOnAction(e -> addFiles());
        view.openFolderMenuItem.setOnAction(e -> addFolder());
        view.clearMenuItem.setOnAction(e -> clearPlaylist());
        view.exitMenuItem.setOnAction(e -> { Platform.exit(); stage.close(); });
        view.playPauseMenuItem.setOnAction(e -> togglePlayPause());
        view.stopMenuItem.setOnAction(e -> stop());
        view.nextMenuItem.setOnAction(e -> playNext());
        view.prevMenuItem.setOnAction(e -> playPrevious());
        view.volUpMenuItem.setOnAction(e -> bumpVolume(+VOLUME_STEP));
        view.volDownMenuItem.setOnAction(e -> bumpVolume(-VOLUME_STEP));
        view.muteMenuItem.setOnAction(e -> toggleMute());
        view.fullscreenMenuItem.setOnAction(e -> toggleFullscreen());
        view.queueMenuItem.setOnAction(e -> openQueueDialog());
        view.helpMenuItem.setOnAction(e -> openHelpDialog());
        view.aboutMenuItem.setOnAction(e -> showAbout());

        view.searchField.textProperty().addListener((o, ov, nv) -> {
            applyFilter();
            refreshSection();
        });

        view.homeButton.setOnAction(e -> selectSection(Section.HOME));
        view.musicButton.setOnAction(e -> selectSection(Section.MUSIC));
        view.videoButton.setOnAction(e -> selectSection(Section.VIDEO));
        view.albumsButton.setOnAction(e -> selectSection(Section.ALBUMS));
        view.playlistButton.setOnAction(e -> selectSection(Section.PLAYLIST));
        view.queueNavButton.setOnAction(e -> selectSection(Section.QUEUE));

        view.progressSlider.setOnMouseReleased(e -> seek());

        view.volumeSlider.valueProperty().addListener((o, ov, nv) -> {
            applyVolume();
            updateVolumeLabel(nv.doubleValue());
        });
        updateVolumeLabel(view.volumeSlider.getValue());

        view.playlistView.getSelectionModel().selectedItemProperty().addListener((o, oldF, newF) -> {
            if (newF != null && !newF.equals(currentFile)) {
                loadFile(newF);
                if (currentSection != Section.QUEUE) view.showStage();
            }
        });
        view.playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && view.playlistView.getSelectionModel().getSelectedItem() != null)
                play();
        });

        view.contentView.getSelectionModel().selectedItemProperty().addListener((o, oldF, newF) -> {
            if (newF != null && !newF.equals(currentFile)) {
                loadFile(newF);
                view.showStage();
            }
        });
        view.contentView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && view.contentView.getSelectionModel().getSelectedItem() != null)
                play();
        });
    }

    /* ---------- playlist cells & menu ---------- */

    private void installPlaylistCells() {
        view.playlistView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("current-track");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                boolean current = item.equals(currentFile);
                setText((current ? "\u25B6  " : "     ") + (getIndex() + 1) + ".  " + item.getName());
                if (current) getStyleClass().add("current-track");
            }
        });
    }

    private void installSectionCells() {
        view.contentView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("current-track");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.getName());
                setGraphic(Icons.icon(isVideoFile(item) ? Icons.VIDEO : Icons.NOTE, 14));
                if (item.equals(currentFile)) getStyleClass().add("current-track");
            }
        });
    }

    private void installMusicTable() {
        configureMediaColumns(view.musicTable);
        view.musicTable.table.setRowFactory(tv -> createMediaRow(false));
        installPlayOnClick(view.musicTable.table);
    }

    private void installQueueTable() {
        view.queueTable.table.setItems(playlistTracks);
        configureMediaColumns(view.queueTable);
        view.queueTable.table.setRowFactory(tv -> createMediaRow(true));
        installPlayOnClick(view.queueTable.table);
    }

    private void installAlbumTable() {
        configureMediaColumns(view.albumTable);
        view.albumTable.table.setRowFactory(tv -> createMediaRow(false));
        installPlayOnClick(view.albumTable.table);
    }

    private void installPlaylistTable() {
        configureMediaColumns(view.playlistTable);
        view.playlistTable.table.setRowFactory(tv -> createMediaRow(false));
        installPlayOnClick(view.playlistTable.table);
    }

    /** Wires the Title / Album / Duration / wave columns shared by Music and Queue. */
    private void configureMediaColumns(MediaPlayerView.MediaTable t) {
        t.titleCol.setCellValueFactory(cd ->
                new SimpleStringProperty(trackTitle(cd.getValue())));
        t.albumCol.setCellValueFactory(cd ->
                new SimpleStringProperty(trackAlbum(cd.getValue())));
        t.durationCol.setCellValueFactory(cd ->
                new SimpleStringProperty(trackDuration(cd.getValue())));
        t.durationCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        t.waveCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        t.waveCol.setCellFactory(col -> new TableCell<>() {
            private final MiniEqualizer wave = new MiniEqualizer();

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                boolean current = !empty && item != null && item.equals(currentFile) && isPlaying();
                if (current) {
                    setGraphic(wave);
                    wave.play();
                } else {
                    wave.stop();
                    setGraphic(null);
                }
            }
        });
    }

    /** Row highlighting shared by the tables; queue rows can also be reordered by dragging. */
    private TableRow<File> createMediaRow(boolean reorderable) {
        TableRow<File> row = new TableRow<>() {
            private final GaussianBlur blur = new GaussianBlur(3);

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("playing", "blurred");
                setEffect(null);
                if (empty || item == null || !isPlaying()) return;

                if (item.equals(currentFile)) {
                    getStyleClass().add("playing");
                } else {
                    getStyleClass().add("blurred");
                    setEffect(blur);
                }
            }
        };
        if (reorderable) installRowReorder(row);
        return row;
    }

    private void installPlayOnClick(TableView<File> table) {
        table.setOnMouseClicked(e -> {
            if (queueReordering) {
                queueReordering = false;
                return;
            }
            File selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (!selected.equals(currentFile)) loadFile(selected);
                play();
            }
        });
    }

    /* ---------- queue reordering (drag & drop) ---------- */

    private void installRowReorder(TableRow<File> row) {
        row.setOnDragDetected(e -> {
            if (row.isEmpty()) return;
            queueReordering = true;
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(Integer.toString(row.getIndex()));
            db.setContent(content);
            db.setDragView(row.snapshot(null, null));
            e.consume();
        });
        row.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.MOVE);
            e.consume();
        });
        row.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean moved = false;
            if (db.hasString()) {
                int from = Integer.parseInt(db.getString());
                int to = row.isEmpty() ? playlistTracks.size() - 1 : row.getIndex();
                moveTrack(from, to);
                moved = true;
            }
            e.setDropCompleted(moved);
            e.consume();
        });
        row.setOnDragDone(e -> Platform.runLater(() -> queueReordering = false));
    }

    private void moveTrack(int from, int to) {
        List<File> tracks = playlistTracks;
        if (from < 0 || from >= tracks.size()) return;
        if (to < 0) to = 0;
        if (to >= tracks.size()) to = tracks.size() - 1;
        if (from == to) return;

        File moved = tracks.remove(from);
        tracks.add(to, moved);
        if (currentFile != null) model.setCurrentIndex(model.getTracks().indexOf(currentFile));
        applyFilter();
        view.queueTable.table.getSelectionModel().select(moved);
    }

    private boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    private void refreshMediaTables() {
        view.musicTable.table.refresh();
        view.queueTable.table.refresh();
        view.albumTable.table.refresh();
        view.playlistTable.table.refresh();
    }

    private MediaMetadata.Tags tags(File file) {
        return tagCache.computeIfAbsent(file, MediaMetadata::readTags);
    }

    private String trackTitle(File file) {
        String title = tags(file).title;
        return title != null ? title : stripExtension(file.getName());
    }

    private String trackAlbum(File file) {
        String album = tags(file).album;
        if (album != null) return album;
        File dir = file.getParentFile();
        return dir != null ? dir.getName() : "Unknown Album";
    }

    private String trackDuration(File file) {
        Duration d = durationCache.get(file);
        if (d != null) return format(d);
        if (durationUnavailable.contains(file)) return "\u2014";
        enqueueDuration(file);
        return "\u2026";
    }

    private void enqueueDuration(File file) {
        if (durationCache.containsKey(file) || durationUnavailable.contains(file)
                || durationQueue.contains(file) || file.equals(durationCurrent))
            return;
        durationQueue.add(file);
        pumpDurationQueue();
    }

    /** Queues every track so its duration is resolved even before it scrolls into view. */
    private void enqueueDurations(List<File> files) {
        for (File f : files) enqueueDuration(f);
    }

    private void pumpDurationQueue() {
        if (durationLoading || durationQueue.isEmpty()) return;
        Iterator<File> it = durationQueue.iterator();
        File file = it.next();
        it.remove();

        durationLoading = true;
        durationCurrent = file;
        try {
            Media media = new Media(file.toURI().toString());
            MediaPlayer probe = new MediaPlayer(media);
            PauseTransition timeout = new PauseTransition(Duration.seconds(4));
            timeout.setOnFinished(e -> {
                if (file.equals(durationCurrent)) finishDuration(file, null, probe);
            });
            timeout.play();
            probe.setOnReady(() -> finishDuration(file, media.getDuration(), probe));
            probe.setOnError(() -> finishDuration(file, null, probe));
        } catch (Exception e) {
            finishDuration(file, null, null);
        }
    }

    private void finishDuration(File file, Duration d, MediaPlayer probe) {
        if (probe != null) probe.dispose();
        durationLoading = false;
        durationCurrent = null;
        if (d != null && !d.isUnknown() && d.toMillis() > 0) durationCache.put(file, d);
        else durationUnavailable.add(file);
        refreshMediaTables();
        pumpDurationQueue();
    }

    private void installPlaylistContextMenu() {
        MenuItem playItem = new MenuItem("Play");
        MenuItem removeItem = new MenuItem("Remove");
        playItem.setOnAction(e -> play());
        removeItem.setOnAction(e -> removeFromPlaylist());
        view.playlistView.setContextMenu(new ContextMenu(playItem, removeItem));
    }

    private void updateTrackCount() {
        int n = playlistTracks.size();
        view.trackCountLabel.setText(n + (n == 1 ? " track" : " tracks"));
    }

    /** Rebuilds the playlist list from the search query. */
    private void applyFilter() {
        String query = searchQuery();

        File keep = view.playlistView.getSelectionModel().getSelectedItem();
        if (keep == null) keep = currentFile;

        List<File> filtered = playlistTracks.stream()
                .filter(f -> matchesQuery(f, query))
                .toList();

        visibleTracks.setAll(filtered);
        view.playlistView.setPlaceholder(new Label(playlistTracks.isEmpty()
                ? "Your playlist is empty" : "No matching tracks"));
        view.playlistView.refresh();

        if (keep != null && visibleTracks.contains(keep)) {
            view.playlistView.getSelectionModel().select(keep);
        }
    }

    private String searchQuery() {
        return view.searchField.getText() == null
                ? "" : view.searchField.getText().trim().toLowerCase();
    }

    private boolean matchesQuery(File f, String query) {
        return query.isEmpty() || f.getName().toLowerCase().contains(query);
    }

    /** Switches the centre area to the given library section. */
    private void selectSection(Section section) {
        currentSection = section;
        selectedAlbum = null;
        if (section == Section.HOME) view.searchField.clear();
        refreshSection();
        view.showContent();
    }

    /** Opens a single album: shows its tracks in the centre list. */
    private void selectAlbum(Album album) {
        selectedAlbum = album;
        currentSection = Section.ALBUMS;
        refreshSection();
        view.showContent();
    }

    /** Rebuilds the centre content for the active section and search query. */
    private void refreshSection() {
        String query = searchQuery();
        view.setPlaylistButtonsVisible(currentSection == Section.PLAYLIST);

        if (currentSection == Section.ALBUMS && selectedAlbum == null) {
            view.contentTitle.setText("Albums");
            populateAlbumGrid(query);
            view.showAlbumGrid();
            return;
        }

        if (currentSection == Section.HOME) {
            view.contentTitle.setText("Recently Played");
            populateRecentGrid(query);
            view.showAlbumGrid();
            return;
        }

        if (currentSection == Section.VIDEO) {
            view.contentTitle.setText("Video Library");
            populateVideoGrid(query);
            view.showAlbumGrid();
            return;
        }

        if (currentSection == Section.QUEUE) {
            view.contentTitle.setText("Queue");
            view.queueTable.table.setPlaceholder(new Label("Your playlist is empty"));
            enqueueDurations(playlistTracks);
            view.queueTable.table.refresh();
            view.showQueueContent();
            return;
        }

        List<File> source = (currentSection == Section.ALBUMS && selectedAlbum != null)
                ? selectedAlbum.tracks
                : currentSection == Section.PLAYLIST
                        ? playlistTracks
                        : model.getTracks();

        List<File> list = source.stream()
                .filter(this::matchesSection)
                .filter(f -> matchesQuery(f, query))
                .toList();

        sectionTracks.setAll(list);
        view.contentTitle.setText(currentSection == Section.ALBUMS ? selectedAlbum.name : sectionTitle());
        view.contentView.setPlaceholder(new Label(sectionPlaceholder()));
        view.contentView.refresh();

        if (currentSection == Section.MUSIC) {
            view.musicTable.table.setPlaceholder(new Label(sectionPlaceholder()));
            enqueueDurations(sectionTracks);
            view.musicTable.table.refresh();
            view.showTableContent();
        } else if (currentSection == Section.ALBUMS && selectedAlbum != null) {
            view.albumTable.table.setPlaceholder(new Label("No tracks here"));
            enqueueDurations(sectionTracks);
            view.albumTable.table.refresh();
            view.showAlbumTableContent();
        } else if (currentSection == Section.PLAYLIST) {
            view.playlistTable.table.setPlaceholder(new Label("Your playlist is empty"));
            enqueueDurations(sectionTracks);
            view.playlistTable.table.refresh();
            view.showPlaylistContent();
        } else {
            view.showListContent();
        }
    }

    /* ---------- albums ---------- */

    private void populateAlbumGrid(String query) {
        view.albumGrid.getChildren().clear();

        List<Album> albums = buildAlbums();
        int shown = 0;
        for (Album album : albums) {
            if (!query.isEmpty() && !album.name.toLowerCase().contains(query)) continue;
            view.albumGrid.getChildren().add(createAlbumTile(album));
            shown++;
        }

        if (shown == 0) {
            Label empty = new Label(model.isEmpty()
                    ? "Your library is empty — add some media to get started"
                    : "No albums match your search");
            empty.getStyleClass().add("album-empty");
            view.albumGrid.getChildren().add(empty);
        }
    }

    private List<Album> buildAlbums() {
        Map<String, List<File>> byFolder = new LinkedHashMap<>();
        for (File f : model.getTracks()) {
            File dir = f.getParentFile();
            String key = dir != null ? dir.getAbsolutePath() : "";
            byFolder.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }

        List<Album> albums = new ArrayList<>();
        for (Map.Entry<String, List<File>> entry : byFolder.entrySet()) {
            File dir = entry.getKey().isEmpty() ? null : new File(entry.getKey());
            String name = dir != null ? dir.getName() : "Unknown Album";
            albums.add(new Album(name, dir, entry.getValue()));
        }
        return albums;
    }

    private Node createAlbumTile(Album album) {
        album.loadMeta();

        StackPane art = new StackPane();
        art.getStyleClass().add("album-art");
        art.setPrefSize(150, 150);
        art.setMinSize(150, 150);
        art.setMaxSize(150, 150);

        if (album.artwork != null) {
            ImageView iv = new ImageView(album.artwork);
            double w = album.artwork.getWidth();
            double h = album.artwork.getHeight();
            double side = Math.min(w, h);
            iv.setViewport(new Rectangle2D((w - side) / 2, (h - side) / 2, side, side));
            iv.setFitWidth(150);
            iv.setFitHeight(150);
            iv.setPreserveRatio(false);
            art.getChildren().add(iv);
        } else {
            art.getChildren().add(Icons.icon(Icons.NOTE, 60));
        }

        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        art.setClip(clip);

        Label name = new Label(album.name);
        name.getStyleClass().add("album-name");
        name.setMaxWidth(150);

        Label meta = new Label((album.artist != null ? album.artist : "Unknown artist")
                + " · " + album.tracks.size() + (album.tracks.size() == 1 ? " track" : " tracks"));
        meta.getStyleClass().add("album-meta");
        meta.setMaxWidth(150);

        VBox tile = new VBox(8, art, name, meta);
        tile.getStyleClass().add("album-tile");
        tile.setPrefWidth(150);
        tile.setAlignment(Pos.TOP_LEFT);
        tile.setOnMouseClicked(e -> selectAlbum(album));
        return tile;
    }

    /* ---------- recent tracks (Home) ---------- */

    private void recordRecent(File file) {
        recentTracks.remove(file);
        recentTracks.add(0, file);
        if (recentTracks.size() > 50) {
            recentTracks.subList(50, recentTracks.size()).clear();
        }
    }

    private void populateRecentGrid(String query) {
        view.albumGrid.getChildren().clear();

        List<File> ordered = new ArrayList<>();
        for (File f : recentTracks) if (!isVideoFile(f) && matchesQuery(f, query)) ordered.add(f);
        for (File f : recentTracks) if (isVideoFile(f) && matchesQuery(f, query)) ordered.add(f);

        if (ordered.isEmpty()) {
            Label empty = new Label(recentTracks.isEmpty()
                    ? "Nothing played yet — pick a track to see it here"
                    : "No recent items match your search");
            empty.getStyleClass().add("album-empty");
            view.albumGrid.getChildren().add(empty);
            return;
        }

        for (File f : ordered) view.albumGrid.getChildren().add(createTrackTile(f));
    }

    private void populateVideoGrid(String query) {
        view.albumGrid.getChildren().clear();

        List<File> videos = model.getTracks().stream()
                .filter(this::isVideoFile)
                .filter(f -> matchesQuery(f, query))
                .toList();

        if (videos.isEmpty()) {
            Label empty = new Label(model.isEmpty()
                    ? "Your library is empty — add some media to get started"
                    : "No videos match your search");
            empty.getStyleClass().add("album-empty");
            view.albumGrid.getChildren().add(empty);
            return;
        }

        for (File f : videos) {
            Node tile = createTrackTile(f);
            tile.setOnMouseClicked(e -> {
                loadFile(f);
                view.showStage();
                play();
            });
            view.albumGrid.getChildren().add(tile);
        }
    }

    private Node createTrackTile(File file) {
        StackPane art = new StackPane();
        art.getStyleClass().add("album-art");
        art.setPrefSize(150, 150);
        art.setMinSize(150, 150);
        art.setMaxSize(150, 150);

        Image img = trackArtwork(file);
        if (img != null) {
            art.getChildren().add(croppedSquare(img, 150));
        } else if (isVideoFile(file)) {
            MediaView mv = new MediaView();
            mv.setFitWidth(150);
            mv.setFitHeight(150);
            mv.setPreserveRatio(false);
            art.getChildren().add(mv);
            buildVideoThumbnail(file, mv, art);
        } else {
            art.getChildren().add(Icons.icon(Icons.NOTE, 56));
        }

        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        art.setClip(clip);

        Label name = new Label(stripExtension(file.getName()));
        name.getStyleClass().add("album-name");
        name.setMaxWidth(150);

        String artist = trackArtist(file);
        Label meta = new Label(artist != null ? artist : "Unknown artist");
        meta.getStyleClass().add("album-meta");
        meta.setMaxWidth(150);

        VBox tile = new VBox(8, art, name, meta);
        tile.getStyleClass().add("album-tile");
        tile.setPrefWidth(150);
        tile.setAlignment(Pos.TOP_LEFT);
        tile.setOnMouseClicked(e -> {
            loadFile(file);
            view.showStage();
            if (e.getClickCount() == 2) play();
        });
        return tile;
    }

    private Image trackArtwork(File file) {
        if (artworkCache.containsKey(file)) return artworkCache.get(file);

        Image img = null;
        byte[] bytes = MediaMetadata.readArtwork(file);
        if (bytes != null) img = imageFromBytes(bytes);
        if (img == null) {
            File cover = MediaMetadata.findFolderImage(file.getParentFile());
            if (cover != null) img = imageFromUrl(cover.toURI().toString());
        }
        artworkCache.put(file, img);
        return img;
    }

    private String trackArtist(File file) {
        if (artistCache.containsKey(file)) return artistCache.get(file);
        String artist = MediaMetadata.readArtist(file);
        artistCache.put(file, artist);
        return artist;
    }

    /** Fills the now-playing stage with the cover art and tag details of the given track. */
    private void applyNowPlaying(File file) {
        MediaMetadata.Tags t = tags(file);
        view.audioTitle.setText(t.title != null ? t.title : stripExtension(file.getName()));
        view.audioArtist.setText(t.artist != null ? t.artist : "Unknown artist");

        StringBuilder details = new StringBuilder();
        if (t.album != null) {
            details.append(t.album);
        } else if (file.getParentFile() != null) {
            details.append(file.getParentFile().getName());
        }
        if (t.year != null) details.append(details.length() > 0 ? "  \u00b7  " : "").append(t.year);
        if (t.genre != null) details.append(details.length() > 0 ? "  \u00b7  " : "").append(t.genre);
        if (t.track != null) details.append(details.length() > 0 ? "  \u00b7  " : "").append("Track ").append(t.track);
        view.audioAlbum.setText(details.toString());

        view.audioArtView.setImage(trackArtwork(file));
    }

    /**
     * Grabs a representative frame from a video and drops it into the tile.
     * The MediaView is already live in the scene, so a snapshot after a short
     * delay yields a real frame; the temporary player is then disposed.
     */
    private void buildVideoThumbnail(File file, MediaView mediaView, StackPane art) {
        if (videoThumbCache.containsKey(file)) {
            art.getChildren().setAll(croppedSquare(videoThumbCache.get(file), 150));
            return;
        }

        try {
            Media media = new Media(file.toURI().toString());
            MediaPlayer player = new MediaPlayer(media);
            player.setMute(true);
            mediaView.setMediaPlayer(player);

            final boolean[] captured = {false};

            player.setOnReady(() -> {
                double duration = media.getDuration().toSeconds();
                double at = duration > 4 ? Math.min(3, duration * 0.25) : Math.max(0, duration / 2);
                player.seek(Duration.seconds(at));
                player.play();
            });

            player.currentTimeProperty().addListener((o, oldV, now) -> {
                if (captured[0] || now.toSeconds() <= 0) return;
                captured[0] = true;

                PauseTransition settle = new PauseTransition(Duration.millis(220));
                settle.setOnFinished(e -> {
                    WritableImage snap = mediaView.snapshot(null, null);
                    if (snap != null && snap.getWidth() > 0 && snap.getHeight() > 0) {
                        videoThumbCache.put(file, snap);
                        art.getChildren().setAll(croppedSquare(snap, 150));
                    } else {
                        art.getChildren().setAll(Icons.icon(Icons.VIDEO, 56));
                    }
                    mediaView.setMediaPlayer(null);
                    player.stop();
                    player.dispose();
                });
                settle.play();
            });

            player.setOnError(() -> {
                art.getChildren().setAll(Icons.icon(Icons.VIDEO, 56));
                mediaView.setMediaPlayer(null);
                player.dispose();
            });
        } catch (Exception e) {
            art.getChildren().setAll(Icons.icon(Icons.VIDEO, 56));
        }
    }

    private static ImageView croppedSquare(Image img, double size) {
        double w = img.getWidth();
        double h = img.getHeight();
        double side = Math.min(w, h);
        ImageView iv = new ImageView(img);
        iv.setViewport(new Rectangle2D((w - side) / 2, (h - side) / 2, side, side));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(false);
        return iv;
    }

    private static Image imageFromBytes(byte[] bytes) {
        try {
            return new Image(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private static Image imageFromUrl(String url) {
        try {
            Image img = new Image(url);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private boolean matchesSection(File f) {
        return switch (currentSection) {
            case MUSIC -> !isVideoFile(f);
            case VIDEO -> isVideoFile(f);
            default    -> true;
        };
    }

    private String sectionTitle() {
        return switch (currentSection) {
            case HOME     -> "Home";
            case MUSIC    -> "Music Library";
            case VIDEO    -> "Video Library";
            case ALBUMS   -> "Albums";
            case PLAYLIST -> "Playlist";
            case QUEUE    -> "Queue";
        };
    }

    private String sectionPlaceholder() {
        if (model.isEmpty()) return "Your library is empty — add some media to get started";
        return switch (currentSection) {
            case MUSIC -> "No audio tracks here";
            case VIDEO -> "No video tracks here";
            case PLAYLIST -> "Your playlist is empty";
            default    -> "No matching tracks";
        };
    }

    /* ---------- drag & drop ---------- */

    private void installDragAndDrop(Scene scene) {
        scene.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                view.dropHint.setVisible(true);
            }
            e.consume();
        });

        scene.setOnDragExited(e -> view.dropHint.setVisible(false));

        scene.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean ok = false;
            if (db.hasFiles()) {
                List<File> mediaFiles = db.getFiles().stream()
                        .filter(this::isMediaFile)
                        .toList();
                if (!mediaFiles.isEmpty()) {
                    model.addAll(mediaFiles);
                    if (model.getCurrentIndex() < 0)
                        view.playlistView.getSelectionModel().select(0);
                    ok = true;
                }
            }
            view.dropHint.setVisible(false);
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private boolean isMediaFile(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a")
                || n.endsWith(".aac") || n.endsWith(".mp4") || n.endsWith(".m4v")
                || n.endsWith(".flv");
    }

    private boolean isVideoFile(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".mp4") || n.endsWith(".m4v") || n.endsWith(".flv");
    }

    /* ---------- playback ---------- */

    private void loadFile(File file) {
        boolean resume = mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
        disposePlayer();
        currentFile = file;
        recordRecent(file);
        model.setCurrentIndex(model.getTracks().indexOf(file));

        Media media;
        try {
            media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
        } catch (Exception ex) {
            showMessage("Unsupported media file");
            view.setStatus("ERROR", "error");
            view.playlistView.refresh();
            view.contentView.refresh();
            return;
        }

        boolean video = isVideoFile(file);
        view.mediaView.setMediaPlayer(mediaPlayer);
        view.mediaView.setVisible(video);
        view.audioBackdrop.setVisible(!video);
        applyNowPlaying(file);
        view.mediaMessage.setVisible(false);
        view.trackLabel.setText(file.getName());
        view.progressSlider.setDisable(false);
        view.setStatus("LOADED", null);
        view.playlistView.refresh();
        view.contentView.refresh();

        mediaPlayer.setVolume(muted ? 0 : view.volumeSlider.getValue());

        mediaPlayer.setOnReady(() -> {
            view.progressSlider.setMax(media.getDuration().toSeconds());
            view.setStatus("READY", null);
            if (resume) play();
        });
        mediaPlayer.setOnPlaying(() -> refreshMediaTables());

        mediaPlayer.currentTimeProperty().addListener((o, ot, nt) -> updateProgress(nt));
        mediaPlayer.setOnEndOfMedia(this::playNext);
        mediaPlayer.setOnError(() -> {
            showMessage("Unable to play this file");
            view.setStatus("ERROR", "error");
            view.setVisualizerActive(false);
        });

        refreshMediaTables();
    }

    private void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            view.setStatus("PLAYING", "playing");
            MediaPlayerView.setIcon(view.playButton, Icons.PAUSE);
            if (view.audioBackdrop.isVisible()) view.setVisualizerActive(true);
            refreshMediaTables();
            return;
        }
        List<File> list = playbackList();
        if (list.isEmpty()) return;
        File first = (currentFile != null && list.contains(currentFile)) ? currentFile : list.get(0);
        playTrack(first);
    }

    /** The list driving playback and next/previous: the playlist, else the library before one exists. */
    private List<File> playbackList() {
        return playlistTracks.isEmpty() ? model.getTracks() : playlistTracks;
    }

    /** Loads and plays a track, keeping the playlist highlight in sync when it belongs to the playlist. */
    private void playTrack(File file) {
        if (visibleTracks.contains(file)) {
            view.playlistView.getSelectionModel().select(file);
        } else if (!file.equals(currentFile)) {
            loadFile(file);
        }
        if (mediaPlayer != null) {
            mediaPlayer.play();
            view.setStatus("PLAYING", "playing");
            MediaPlayerView.setIcon(view.playButton, Icons.PAUSE);
            if (view.audioBackdrop.isVisible()) view.setVisualizerActive(true);
            refreshMediaTables();
        }
    }

    private void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            view.setStatus("PAUSED", "paused");
            MediaPlayerView.setIcon(view.playButton, Icons.PLAY);
            view.setVisualizerActive(false);
            refreshMediaTables();
        }
    }

    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            view.setStatus("STOPPED", "stopped");
            MediaPlayerView.setIcon(view.playButton, Icons.PLAY);
            view.setVisualizerActive(false);
            view.progressSlider.setValue(0);
            view.timeLabel.setText("00:00 / " + format(mediaPlayer.getTotalDuration()));
            refreshMediaTables();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) pause();
        else play();
    }

    /* ---------- N and P: next / previous ---------- */

    private void playNext() {
        List<File> list = playbackList();
        if (list.isEmpty()) return;
        int idx = currentFile == null ? -1 : list.indexOf(currentFile);
        int next = idx < 0 ? 0 : (idx + 1) % list.size();
        playTrack(list.get(next));
    }

    private void playPrevious() {
        List<File> list = playbackList();
        if (list.isEmpty()) return;
        int idx = currentFile == null ? -1 : list.indexOf(currentFile);
        int prev = idx < 0 ? list.size() - 1 : (idx - 1 + list.size()) % list.size();
        playTrack(list.get(prev));
    }

    /* ---------- volume / mute ---------- */

    private void bumpVolume(double delta) {
        double v = Math.max(0, Math.min(1, view.volumeSlider.getValue() + delta));
        view.volumeSlider.setValue(v);
    }

    private void applyVolume() {
        if (mediaPlayer != null) mediaPlayer.setVolume(muted ? 0 : view.volumeSlider.getValue());
    }

    private void updateVolumeLabel(double value) {
        view.volumeLabel.setText(muted ? "MUTE" : Math.round(value * 100) + "%");
    }

    private void toggleMute() {
        muted = !muted;
        MediaPlayerView.setIcon(view.muteButton, muted ? Icons.MUTE : Icons.VOLUME);
        applyVolume();
        updateVolumeLabel(view.volumeSlider.getValue());
    }

    /* ---------- fullscreen ---------- */

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        if (fullscreen) {
            view.mediaView.fitWidthProperty().bind(view.videoArea.widthProperty());
            view.mediaView.fitHeightProperty().bind(view.videoArea.heightProperty());
            showFullscreenHint();
        } else {
            view.mediaView.fitWidthProperty().unbind();
            view.mediaView.fitHeightProperty().unbind();
            view.mediaView.setFitWidth(520);
            view.mediaView.setFitHeight(330);
            view.fullscreenHint.setVisible(false);
        }
        MediaPlayerView.setIcon(view.fullscreenButton,
                fullscreen ? Icons.FULLSCREEN_EXIT : Icons.FULLSCREEN);
        view.fullscreenButton.toFront();
    }

    /** Briefly shows the custom "Press ESC" hint at the bottom of the stage when entering fullscreen. */
    private void showFullscreenHint() {
        view.fullscreenHint.setVisible(true);
        PauseTransition hide = new PauseTransition(Duration.seconds(4));
        hide.setOnFinished(e -> view.fullscreenHint.setVisible(false));
        hide.play();
    }

    /* ---------- progress ---------- */

    private void updateProgress(Duration current) {
        if (!view.progressSlider.isValueChanging()) {
            view.progressSlider.setValue(current.toSeconds());
        }
        view.timeLabel.setText(format(current) + " / " + format(mediaPlayer.getTotalDuration()));
    }

    private void seek() {
        if (mediaPlayer != null) mediaPlayer.seek(Duration.seconds(view.progressSlider.getValue()));
    }

    private String format(Duration d) {
        if (d == null || d.isUnknown()) return "00:00";
        int s = (int) Math.floor(d.toSeconds());
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    /* ---------- playlist ---------- */

    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose media files");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Media files", "*.mp3", "*.wav", "*.m4a", "*.aac", "*.mp4", "*.m4v", "*.flv"));
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) return;

        model.addAll(files);
        if (model.getCurrentIndex() < 0) view.playlistView.getSelectionModel().select(0);
    }

    private void addFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a folder of media");
        File dir = chooser.showDialog(stage);
        if (dir == null) return;

        List<File> found = new ArrayList<>();
        collectMedia(dir, found);
        if (found.isEmpty()) {
            showMessage("No media files found in that folder");
            return;
        }

        model.addAll(found);
        if (model.getCurrentIndex() < 0) view.playlistView.getSelectionModel().select(0);
    }

    private void collectMedia(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collectMedia(child, out);
            else if (isMediaFile(child)) out.add(child);
        }
    }

    private void removeSelected() {
        File selected = view.playlistView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int idx = model.getTracks().indexOf(selected);
        if (idx < 0) return;

        boolean removingCurrent = selected.equals(currentFile);
        if (removingCurrent) {
            disposePlayer();
            currentFile = null;
            model.setCurrentIndex(-1);
            view.trackLabel.setText("No media selected");
            view.setStatus("READY", null);
            refreshMediaTables();
        } else if (idx < model.getCurrentIndex()) {
            model.setCurrentIndex(model.getCurrentIndex() - 1);
        }

        model.remove(idx);

        if (!model.isEmpty()) {
            if (view.playlistView.getSelectionModel().getSelectedItem() == null && !visibleTracks.isEmpty()) {
                view.playlistView.getSelectionModel().select(0);
            }
        } else {
            showMessage("Add an audio or video file to begin");
        }
    }

    private void clearPlaylist() {
        disposePlayer();
        currentFile = null;
        model.setCurrentIndex(-1);
        model.getTracks().clear();
        playlistTracks.clear();
        view.searchField.clear();
        view.trackLabel.setText("No media selected");
        view.setStatus("READY", null);
        showMessage("Add an audio or video file to begin");
        applyFilter();
        updateTrackCount();
        refreshMediaTables();
    }

    /* ---------- curated playlist ---------- */

    /** Opens a checklist of Music library songs to add to the playlist. */
    private void openAddToPlaylistDialog() {
        List<File> candidates = model.getTracks().stream()
                .filter(f -> !isVideoFile(f))
                .filter(f -> !playlistTracks.contains(f))
                .toList();

        if (candidates.isEmpty()) {
            showMessage(model.isEmpty()
                    ? "Add songs to your Music library first"
                    : "Every song is already in your playlist");
            return;
        }

        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add to Playlist");

        Label title = new Label("ADD SONGS FROM MUSIC LIBRARY");
        title.getStyleClass().add("panel-title");

        Set<File> checked = new LinkedHashSet<>();
        ListView<File> list = new ListView<>();
        list.getItems().setAll(candidates);
        list.getStyleClass().add("playlist");
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox box = new CheckBox();

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                box.setText(item.getName());
                box.setSelected(checked.contains(item));
                box.setOnAction(e -> {
                    if (box.isSelected()) checked.add(item);
                    else checked.remove(item);
                });
                setText(null);
                setGraphic(box);
            }
        });

        Button selectAll = textButton("Select all");
        Button cancel = textButton("Cancel");
        Button add = textButton("Add");
        selectAll.setOnAction(e -> {
            checked.addAll(candidates);
            list.refresh();
        });
        cancel.setOnAction(e -> dialog.close());
        add.setOnAction(e -> {
            for (File f : candidates) {
                if (checked.contains(f)) playlistTracks.add(f);
            }
            applyFilter();
            updateTrackCount();
            refreshSection();
            dialog.close();
        });

        HBox row = new HBox(8, selectAll, cancel, add);
        row.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(10, title, list, row);
        layout.setPadding(new Insets(14));
        layout.getStyleClass().add("media-player-root");
        view.applyThemeClass(layout);

        Scene scene = new Scene(layout, 360, 440);
        var css = getClass().getResource("/com/example/firstassigment/MediaPlayer.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /** Removes the selected songs from the curated playlist (leaves the library intact). */
    private void removeFromPlaylist() {
        List<File> selected = new ArrayList<>(
                view.playlistTable.table.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            File single = view.playlistView.getSelectionModel().getSelectedItem();
            if (single != null) selected.add(single);
        }
        if (selected.isEmpty()) return;

        playlistTracks.removeAll(selected);
        applyFilter();
        updateTrackCount();
        refreshSection();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("About");
        alert.setHeaderText("Vintage Media Player");
        alert.setContentText("JavaFX audio & video player with keyboard controls "
                + "and playlist management.\n\nPress the ? button or open Help ▸ Keyboard Shortcuts for controls "
                + "and the palette button to change the colour theme.");
        alert.showAndWait();
    }

    /* ---------- theme ---------- */

    private void cycleTheme() {
        themeIndex = (themeIndex + 1) % MediaPlayerView.THEME_CLASSES.length;
        view.setThemeClass(MediaPlayerView.THEME_CLASSES[themeIndex]);
        view.themeButton.setTooltip(new Tooltip("Theme: " + MediaPlayerView.THEME_NAMES[themeIndex]));
    }

    private void showMessage(String text) {
        view.mediaMessage.setText(text);
        view.mediaMessage.setVisible(true);
    }

    /* ---------- queue / reorder ---------- */

    private void openQueueDialog() {
        if (playlistTracks.isEmpty()) {
            showMessage("Add songs to your playlist before arranging the queue");
            return;
        }

        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Arrange Queue");

        Label title = new Label("REORDER QUEUE");
        title.getStyleClass().add("panel-title");

        ListView<File> queueList = new ListView<>(playlistTracks);
        queueList.getStyleClass().add("playlist");
        if (currentFile != null && playlistTracks.contains(currentFile))
            queueList.getSelectionModel().select(currentFile);
        VBox.setVgrow(queueList, Priority.ALWAYS);

        Button upBtn     = miniIconButton(Icons.ARROW_UP,   "Move up");
        Button downBtn   = miniIconButton(Icons.ARROW_DOWN, "Move down");
        Button topBtn    = textButton("Top");
        Button bottomBtn = textButton("Bottom");
        Button closeBtn  = textButton("Done");

        upBtn.setOnAction(e     -> moveSelected(queueList, -1));
        downBtn.setOnAction(e   -> moveSelected(queueList, +1));
        topBtn.setOnAction(e    -> moveToEnd(queueList, true));
        bottomBtn.setOnAction(e -> moveToEnd(queueList, false));
        closeBtn.setOnAction(e  -> dialog.close());

        HBox row1 = new HBox(8, upBtn, downBtn, topBtn, bottomBtn);
        row1.setAlignment(Pos.CENTER);
        HBox row2 = new HBox(closeBtn);
        row2.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(10, title, queueList, row1, row2);
        layout.setPadding(new Insets(14));
        layout.getStyleClass().add("media-player-root");
        view.applyThemeClass(layout);

        Scene scene = new Scene(layout, 340, 420);
        var css = getClass().getResource("/com/example/firstassigment/MediaPlayer.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void moveSelected(ListView<File> list, int delta) {
        int idx = list.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        int target = idx + delta;
        if (target < 0 || target >= list.getItems().size()) return;

        ObservableList<File> items = list.getItems();
        File moved = items.remove(idx);
        items.add(target, moved);

        list.getSelectionModel().select(target);
        syncCurrentIndexAfterReorder();
    }

    private void moveToEnd(ListView<File> list, boolean toTop) {
        int idx = list.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        int target = toTop ? 0 : list.getItems().size() - 1;
        if (target == idx) return;

        ObservableList<File> items = list.getItems();
        File moved = items.remove(idx);
        items.add(target, moved);

        list.getSelectionModel().select(target);
        syncCurrentIndexAfterReorder();
    }

    /**
     * After a reorder, the currently playing file's library index may have changed.
     * Re-resolve it and refresh the playlist panel order.
     */
    private void syncCurrentIndexAfterReorder() {
        if (currentFile != null) model.setCurrentIndex(model.getTracks().indexOf(currentFile));
        applyFilter();
        view.playlistView.refresh();
    }

    private Button miniIconButton(String svg, String tooltip) {
        Button b = new Button();
        b.setGraphic(Icons.icon(svg, 16));
        b.setTooltip(new Tooltip(tooltip));
        b.getStyleClass().add("icon-button");
        b.setFocusTraversable(false);
        return b;
    }

    private Button textButton(String label) {
        Button b = new Button(label);
        b.getStyleClass().add("pill-button");
        b.setFocusTraversable(false);
        return b;
    }

    /* ---------- keyboard help ---------- */

    private void openHelpDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Keyboard Shortcuts");

        Label title = new Label("KEYBOARD SHORTCUTS");
        title.getStyleClass().add("panel-title");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(9);
        ColumnConstraints keys = new ColumnConstraints();
        keys.setMinWidth(96);
        grid.getColumnConstraints().add(keys);

        String[][] rows = {
                {"SPACE", "Play / Pause"},
                {"S", "Stop playback"},
                {"N", "Play next track"},
                {"P", "Play previous track"},
                {"↑ / ↓", "Volume up / down"},
                {"M", "Mute / unmute"}
        };
        for (int i = 0; i < rows.length; i++) {
            Label key = new Label(rows[i][0]);
            key.getStyleClass().add("key-cap");
            Label action = new Label(rows[i][1]);
            action.getStyleClass().add("key-action");
            grid.add(key, 0, i);
            grid.add(action, 1, i);
        }

        Button close = textButton("Close");
        close.setOnAction(e -> dialog.close());
        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12, title, grid, footer);
        layout.getStyleClass().add("media-player-root");
        view.applyThemeClass(layout);
        layout.setPadding(new Insets(18));

        Scene scene = new Scene(layout, 380, 470);
        var css = getClass().getResource("/com/example/firstassigment/MediaPlayer.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /* ---------- cleanup ---------- */

    private void disposePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        view.mediaView.setMediaPlayer(null);
        view.setVisualizerActive(false);
        view.mediaView.setVisible(true);
        view.audioBackdrop.setVisible(false);
        view.progressSlider.setValue(0);
        view.progressSlider.setDisable(true);
        view.timeLabel.setText("00:00 / 00:00");
        MediaPlayerView.setIcon(view.playButton, Icons.PLAY);
    }
}
