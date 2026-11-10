package com.example.firstassigment.view;

import com.example.firstassigment.util.Icons;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerView {

    /** Theme style classes understood by the stylesheet. */
    public static final String[] THEME_CLASSES = {
            "theme-rose", "theme-day"
    };
    public static final String[] THEME_NAMES = {
            "Rose", "Daylight"
    };

    private static final int BAR_COUNT = 5;

    private final BorderPane root = new BorderPane();

    // Menu bar
    public final MenuBar menuBar = new MenuBar();
    public final MenuItem openMenuItem         = new MenuItem("Open Files…");
    public final MenuItem openFolderMenuItem   = new MenuItem("Add Folder…");
    public final MenuItem clearMenuItem        = new MenuItem("Clear Playlist");
    public final MenuItem exitMenuItem       = new MenuItem("Exit");
    public final MenuItem playPauseMenuItem  = new MenuItem("Play / Pause");
    public final MenuItem stopMenuItem       = new MenuItem("Stop");
    public final MenuItem nextMenuItem       = new MenuItem("Next Track");
    public final MenuItem prevMenuItem       = new MenuItem("Previous Track");
    public final MenuItem volUpMenuItem      = new MenuItem("Volume Up");
    public final MenuItem volDownMenuItem    = new MenuItem("Volume Down");
    public final MenuItem muteMenuItem       = new MenuItem("Mute / Unmute");
    public final MenuItem fullscreenMenuItem = new MenuItem("Toggle Fullscreen");
    public final MenuItem queueMenuItem      = new MenuItem("Arrange Queue…");
    public final MenuItem helpMenuItem       = new MenuItem("Keyboard Shortcuts…");
    public final MenuItem aboutMenuItem      = new MenuItem("About");

    // Left navigation sidebar
    public final ToggleButton homeButton     = navToggle(Icons.HOME,     "Home");
    public final ToggleButton musicButton    = navToggle(Icons.NOTE,     "Music Library");
    public final ToggleButton videoButton    = navToggle(Icons.VIDEO,    "Video Library");
    public final ToggleButton albumsButton   = navToggle(Icons.ALBUM,    "Albums");
    public final ToggleButton playlistButton = navToggle(Icons.PLAYLIST, "Playlist");
    public final ToggleButton queueNavButton = navToggle(Icons.QUEUE,    "Queue");

    // Playlist
    public final ListView<File> playlistView = new ListView<>();
    public final TextField searchField = new TextField();
    public final Button addButton    = new Button("Add Files");
    public final Button removeButton = new Button("Remove");
    public final Label  trackCountLabel = new Label("0 tracks");

    // Media area
    public final MediaView mediaView = new MediaView();
    public final StackPane mediaStage = new StackPane();
    public final StackPane videoArea = new StackPane();
    public final Label mediaMessage  = new Label("Add an audio or video file to begin");
    public final Label dropHint      = new Label("⤓  Drop media files here to add them");
    public final Button fullscreenButton = iconButton(Icons.FULLSCREEN, "Fullscreen");
    public final Label fullscreenHint = new Label("Press ESC to exit full screen");
    public final StackPane audioBackdrop = new StackPane();
    public final ImageView audioArtView = new ImageView();
    public final StackPane audioArt = new StackPane();
    public final Label audioTitle  = new Label();
    public final Label audioArtist = new Label();
    public final Label audioAlbum  = new Label();

    // Center library content (Home / Music / Video / Playlist / Queue)
    public final ListView<File> contentView = new ListView<>();
    public final MediaTable musicTable = new MediaTable();
    public final MediaTable queueTable = new MediaTable();
    public final MediaTable albumTable = new MediaTable();
    public final MediaTable playlistTable = new MediaTable();
    public final FlowPane albumGrid = new FlowPane();
    public final Label contentTitle = new Label("Home");
    public final Button playlistAddButton = new Button("+ Add");
    public final Button playlistRemoveButton = new Button("Remove");
    private ScrollPane albumScroll;
    private StackPane searchBox;
    private Node stageNode;
    private Node contentNode;

    // Transport
    public final Button prevButton = iconButton(Icons.PREV, "Previous (P)");
    public final Button playButton = iconButton(Icons.PLAY, "Play / Pause (Space)");
    public final Button stopButton = iconButton(Icons.STOP, "Stop (S)");
    public final Button nextButton = iconButton(Icons.NEXT, "Next (N)");

    // Progress + volume
    public final Slider progressSlider = new Slider();
    public final Slider volumeSlider   = new Slider(0, 1, 0.7);
    public final Label  timeLabel      = new Label("00:00 / 00:00");
    public final Label  volumeLabel    = new Label("70%");
    public final Button muteButton     = iconButton(Icons.VOLUME, "Mute (M)");

    public final Label trackLabel = new Label("No media selected");
    public final Label statusChip = new Label("READY");
    public final Button helpButton = iconButton(Icons.HELP, "Keyboard shortcuts");
    public final Button themeButton = iconButton(Icons.PALETTE, "Change theme");

    private String currentThemeClass = THEME_CLASSES[0];

    // Animated visualizer (shown behind audio-only playback)
    private final HBox visualizerRow = new HBox(4);
    private final List<Timeline> barTimelines = new ArrayList<>();
    private final List<Rectangle> bars = new ArrayList<>();

    public MediaPlayerView() {
        root.getStyleClass().addAll("media-player-root", currentThemeClass);
        buildMenuBar();
        root.setTop(new VBox(menuBar, buildHeader()));
        root.setLeft(buildSidebar());
        root.setCenter(buildCenter());
        root.setRight(buildPlaylistPanel());
        root.setBottom(buildControls());

        buildVisualizer();
        makeNonFocusable(
                playlistView, addButton, removeButton,
                playlistAddButton, playlistRemoveButton,
                prevButton, playButton, stopButton, nextButton,
                muteButton, fullscreenButton, helpButton, themeButton,
                homeButton, musicButton, videoButton, albumsButton, playlistButton, queueNavButton,
                progressSlider, volumeSlider
        );
    }

    /** Switches the active colour theme by swapping the theme style class. */
    public void setThemeClass(String themeClass) {
        root.getStyleClass().removeAll(THEME_CLASSES);
        root.getStyleClass().add(themeClass);
        currentThemeClass = themeClass;
    }

    public String getThemeClass() {
        return currentThemeClass;
    }

    /** Adds the current theme class to a dialog root so it matches the app. */
    public void applyThemeClass(Node dialogRoot) {
        dialogRoot.getStyleClass().removeAll(THEME_CLASSES);
        dialogRoot.getStyleClass().add(currentThemeClass);
    }

    public Scene createScene() {
        Scene scene = new Scene(root, 1060, 640);
        var css = getClass().getResource("/com/example/firstassigment/MediaPlayer.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        // Clicking anywhere outside the search box releases the field so the keyboard
        // shortcuts (Play/Pause, Next, …) work again instead of typing into the search.
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (searchField.isFocused() && !isWithinSearchBox(e.getTarget())) {
                root.requestFocus();
            }
        });
        return scene;
    }

    private boolean isWithinSearchBox(Object target) {
        if (!(target instanceof Node node)) return false;
        for (Node n = node; n != null; n = n.getParent()) {
            if (n == searchBox) return true;
        }
        return false;
    }

    /** Updates the status chip text and switches its colour state. */
    public void setStatus(String text, String state) {
        statusChip.setText(text);
        statusChip.getStyleClass().removeAll("playing", "paused", "stopped", "error");
        if (state != null) statusChip.getStyleClass().add(state);
    }

    /** Starts or stops the equaliser animation. */
    public void setVisualizerActive(boolean active) {
        if (active) {
            barTimelines.forEach(Timeline::play);
        } else {
            barTimelines.forEach(Timeline::stop);
            bars.forEach(b -> b.setHeight(8));
        }
    }

    private void buildMenuBar() {
        Menu file = new Menu("File");
        file.getItems().addAll(openMenuItem, openFolderMenuItem, clearMenuItem, new SeparatorMenuItem(), exitMenuItem);

        Menu playback = new Menu("Playback");
        playback.getItems().addAll(playPauseMenuItem, stopMenuItem, nextMenuItem, prevMenuItem,
                new SeparatorMenuItem(), volUpMenuItem, volDownMenuItem, muteMenuItem);

        Menu view = new Menu("View");
        view.getItems().addAll(fullscreenMenuItem, queueMenuItem, new SeparatorMenuItem(), helpMenuItem);

        Menu help = new Menu("Help");
        help.getItems().add(aboutMenuItem);

        menuBar.getMenus().addAll(file, playback, view, help);
        menuBar.getStyleClass().add("menu-bar");
    }

    private Node buildSidebar() {
        Node searchBox = buildSearchBox();

        ToggleGroup group = new ToggleGroup();
        homeButton.setToggleGroup(group);
        musicButton.setToggleGroup(group);
        videoButton.setToggleGroup(group);
        albumsButton.setToggleGroup(group);
        playlistButton.setToggleGroup(group);
        queueNavButton.setToggleGroup(group);
        homeButton.setSelected(true);

        // Keep exactly one section selected at all times.
        group.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) group.selectToggle(oldT);
        });

        homeButton.getStyleClass().add("nav-button");
        musicButton.getStyleClass().add("nav-button");
        videoButton.getStyleClass().add("nav-button");
        albumsButton.getStyleClass().add("nav-button");
        playlistButton.getStyleClass().add("nav-button");
        queueNavButton.getStyleClass().add("nav-button");

        VBox nav = new VBox(4, homeButton, musicButton, videoButton, albumsButton, playlistButton, queueNavButton);
        nav.getStyleClass().add("sidebar-nav");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        addButton.getStyleClass().add("text-button");
        removeButton.getStyleClass().add("text-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setMaxWidth(Double.MAX_VALUE);

        VBox footer = new VBox(6, addButton, removeButton);
        footer.getStyleClass().add("sidebar-footer-box");

        VBox sidebar = new VBox(10, searchBox, nav, spacer, footer);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(184);
        sidebar.setMinWidth(160);
        sidebar.setPadding(new Insets(14));
        return sidebar;
    }

    private Node buildSearchBox() {
        searchField.setPromptText("Search");
        searchField.getStyleClass().add("search-field");
        searchField.setMaxWidth(Double.MAX_VALUE);
        // Not part of the focus traversal cycle: the field only takes focus when clicked,
        // so it no longer swallows keystrokes meant for the global shortcuts on startup.
        searchField.setFocusTraversable(false);

        Group icon = Icons.icon(Icons.SEARCH, 14);
        StackPane.setAlignment(icon, Pos.CENTER_LEFT);
        StackPane.setMargin(icon, new Insets(0, 0, 0, 12));

        searchBox = new StackPane(searchField, icon);
        searchBox.getStyleClass().add("search-box");
        searchBox.setOnMousePressed(e -> searchField.requestFocus());
        return searchBox;
    }

    private static ToggleButton navToggle(String svg, String text) {
        ToggleButton b = new ToggleButton(text);
        b.setGraphic(Icons.icon(svg, 18));
        b.setContentDisplay(ContentDisplay.LEFT);
        b.setGraphicTextGap(12);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        return b;
    }

    private Node buildHeader() {
        Label title = new Label("VINTAGE MEDIA PLAYER");
        title.getStyleClass().add("app-title");

        VBox text = new VBox(0, title);
        text.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        helpButton.getStyleClass().add("ghost-button");
        themeButton.getStyleClass().add("ghost-button");
        statusChip.getStyleClass().add("status-chip");

        HBox header = new HBox(8, text, spacer, themeButton, helpButton, statusChip);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");
        return header;
    }

    private Node buildCenter() {
        stageNode = buildStage();
        contentNode = buildContentPane();
        stageNode.setVisible(false);

        StackPane center = new StackPane(contentNode, stageNode);
        center.getStyleClass().add("center-stack");
        return center;
    }

    private Node buildContentPane() {
        contentTitle.getStyleClass().add("content-title");

        playlistAddButton.getStyleClass().add("playlist-action-button");
        playlistRemoveButton.getStyleClass().add("playlist-action-button");
        playlistAddButton.setVisible(false);
        playlistRemoveButton.setVisible(false);
        playlistAddButton.setManaged(false);
        playlistRemoveButton.setManaged(false);
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox titleRow = new HBox(8, contentTitle, titleSpacer, playlistAddButton, playlistRemoveButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getStyleClass().add("content-title-row");

        contentView.getStyleClass().addAll("playlist", "content-list");
        contentView.setPlaceholder(new Label("Nothing here yet"));
        VBox.setVgrow(contentView, Priority.ALWAYS);

        albumGrid.getStyleClass().add("album-grid");
        albumGrid.setHgap(20);
        albumGrid.setVgap(20);
        albumGrid.setPadding(new Insets(6));
        albumGrid.setAlignment(Pos.TOP_LEFT);

        albumScroll = new ScrollPane(albumGrid);
        albumScroll.getStyleClass().add("album-scroll");
        albumScroll.setFitToWidth(true);
        albumScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        albumScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        albumScroll.setVisible(false);

        StackPane contentStack = new StackPane(contentView, musicTable.table, queueTable.table, albumTable.table, playlistTable.table, albumScroll);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        VBox pane = new VBox(12, titleRow, contentStack);
        pane.getStyleClass().add("content-pane");
        pane.setPadding(new Insets(16, 18, 16, 18));
        return pane;
    }

    /** Shows or hides the playlist Add/Remove buttons in the content title row. */
    public void setPlaylistButtonsVisible(boolean visible) {
        playlistAddButton.setVisible(visible);
        playlistRemoveButton.setVisible(visible);
        playlistAddButton.setManaged(visible);
        playlistRemoveButton.setManaged(visible);
    }

    /** Shows the library content for the active section (Home, Music, …). */
    public void showContent() {
        stageNode.setVisible(false);
        contentNode.setVisible(true);
    }

    /** Shows the media player stage. */
    public void showStage() {
        contentNode.setVisible(false);
        stageNode.setVisible(true);
    }

    /** Shows the track list (Home / Music / Video / Playlist / album detail). */
    public void showListContent() {
        contentView.setVisible(true);
        musicTable.table.setVisible(false);
        queueTable.table.setVisible(false);
        albumTable.table.setVisible(false);
        playlistTable.table.setVisible(false);
        albumScroll.setVisible(false);
    }

    /** Shows the music library table (Title / Album / Duration). */
    public void showTableContent() {
        contentView.setVisible(false);
        musicTable.table.setVisible(true);
        queueTable.table.setVisible(false);
        albumTable.table.setVisible(false);
        playlistTable.table.setVisible(false);
        albumScroll.setVisible(false);
    }

    /** Shows the queue table (Title / Album / Duration, reorderable). */
    public void showQueueContent() {
        contentView.setVisible(false);
        musicTable.table.setVisible(false);
        queueTable.table.setVisible(true);
        albumTable.table.setVisible(false);
        playlistTable.table.setVisible(false);
        albumScroll.setVisible(false);
    }

    /** Shows an album's tracks in the same table layout as the library. */
    public void showAlbumTableContent() {
        contentView.setVisible(false);
        musicTable.table.setVisible(false);
        queueTable.table.setVisible(false);
        albumTable.table.setVisible(true);
        playlistTable.table.setVisible(false);
        albumScroll.setVisible(false);
    }

    /** Shows the curated playlist table. */
    public void showPlaylistContent() {
        contentView.setVisible(false);
        musicTable.table.setVisible(false);
        queueTable.table.setVisible(false);
        albumTable.table.setVisible(false);
        playlistTable.table.setVisible(true);
        albumScroll.setVisible(false);
    }

    /** Shows the album tile grid (Albums section). */
    public void showAlbumGrid() {
        contentView.setVisible(false);
        musicTable.table.setVisible(false);
        queueTable.table.setVisible(false);
        albumTable.table.setVisible(false);
        playlistTable.table.setVisible(false);
        albumScroll.setVisible(true);
    }

    /** A themed Title / Album / Duration / wave table used by both Music and Queue. */
    public static final class MediaTable {
        public final TableView<File> table = new TableView<>();
        public final TableColumn<File, String> titleCol = new TableColumn<>("Title");
        public final TableColumn<File, String> albumCol = new TableColumn<>("Album");
        public final TableColumn<File, String> durationCol = new TableColumn<>("Duration");
        public final TableColumn<File, File> waveCol = new TableColumn<>("");

        MediaTable() {
            titleCol.setPrefWidth(300);
            titleCol.setMinWidth(140);
            albumCol.setPrefWidth(190);
            albumCol.setMinWidth(90);
            durationCol.setPrefWidth(80);
            durationCol.setMinWidth(64);
            durationCol.setMaxWidth(120);
            waveCol.setPrefWidth(44);
            waveCol.setMinWidth(44);
            waveCol.setMaxWidth(44);
            titleCol.setSortable(false);
            albumCol.setSortable(false);
            durationCol.setSortable(false);
            waveCol.setSortable(false);
            table.getColumns().addAll(titleCol, albumCol, durationCol, waveCol);

            table.getStyleClass().add("music-table");
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            table.setPlaceholder(new Label("No tracks here"));
            table.setFixedCellSize(34);
            table.setVisible(false);

            table.widthProperty().addListener((o, ov, nv) -> fitTitle());
            albumCol.widthProperty().addListener((o, ov, nv) -> fitTitle());
            durationCol.widthProperty().addListener((o, ov, nv) -> fitTitle());
            waveCol.widthProperty().addListener((o, ov, nv) -> fitTitle());
        }

        /** Keeps the Title column filling whatever space is left after the fixed columns. */
        private void fitTitle() {
            double used = albumCol.getWidth() + durationCol.getWidth() + waveCol.getWidth();
            double available = table.getWidth() - used - 4;
            if (available > titleCol.getMinWidth()) titleCol.setPrefWidth(available);
        }
    }

    private Node buildStage() {
        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(520);
        mediaView.setFitHeight(330);

        mediaMessage.getStyleClass().add("media-message");
        mediaMessage.setWrapText(true);

        buildAudioBackdrop();

        dropHint.getStyleClass().add("drop-hint");
        dropHint.setVisible(false);
        dropHint.setMouseTransparent(true);

        fullscreenButton.getStyleClass().add("overlay-button");
        StackPane.setAlignment(fullscreenButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fullscreenButton, new Insets(12));

        fullscreenHint.getStyleClass().add("fullscreen-hint");
        fullscreenHint.setMouseTransparent(true);
        fullscreenHint.setVisible(false);
        StackPane.setAlignment(fullscreenHint, Pos.BOTTOM_CENTER);
        StackPane.setMargin(fullscreenHint, new Insets(0, 0, 16, 0));

        videoArea.getChildren().setAll(mediaView);
        videoArea.setMinSize(0, 0);
        videoArea.setPrefSize(0, 0);

        mediaStage.getChildren().setAll(videoArea, audioBackdrop, mediaMessage, dropHint, fullscreenHint, fullscreenButton);
        mediaStage.getStyleClass().add("media-stage");
        mediaStage.setPadding(new Insets(12));
        return mediaStage;
    }

    private void buildAudioBackdrop() {
        Group note = Icons.icon(Icons.NOTE, 72, "note-icon");
        note.setMouseTransparent(true);

        audioArtView.setFitWidth(176);
        audioArtView.setFitHeight(176);
        audioArtView.setPreserveRatio(true);
        audioArtView.setSmooth(true);

        audioArt.getStyleClass().add("audio-art");
        audioArt.setPrefSize(176, 176);
        audioArt.setMinSize(176, 176);
        audioArt.setMaxSize(176, 176);
        Rectangle artClip = new Rectangle(176, 176);
        artClip.setArcWidth(14);
        artClip.setArcHeight(14);
        audioArt.setClip(artClip);
        audioArt.getChildren().addAll(note, audioArtView);

        audioTitle.getStyleClass().add("audio-title");
        audioTitle.setWrapText(true);
        audioTitle.setMaxWidth(340);
        audioTitle.setAlignment(Pos.CENTER);
        audioTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        audioArtist.getStyleClass().add("audio-artist");
        audioArtist.setMaxWidth(340);

        audioAlbum.getStyleClass().add("audio-album");
        audioAlbum.setWrapText(true);
        audioAlbum.setMaxWidth(340);
        audioAlbum.setAlignment(Pos.CENTER);
        audioAlbum.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox info = new VBox(8, audioArt, audioTitle, audioArtist, audioAlbum);
        info.setAlignment(Pos.CENTER);
        info.setMouseTransparent(true);

        visualizerRow.getStyleClass().add("visualizer");
        visualizerRow.setAlignment(Pos.BOTTOM_CENTER);
        visualizerRow.setMouseTransparent(true);
        visualizerRow.setMaxHeight(52);
        StackPane.setAlignment(visualizerRow, Pos.BOTTOM_CENTER);
        StackPane.setMargin(visualizerRow, new Insets(0, 0, 22, 0));

        audioBackdrop.getChildren().addAll(info, visualizerRow);
        audioBackdrop.getStyleClass().add("audio-backdrop");
        audioBackdrop.setVisible(false);
    }

    private void buildVisualizer() {
        for (int i = 0; i < BAR_COUNT; i++) {
            Rectangle bar = new Rectangle(6, 8);
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            bar.getStyleClass().add("viz-bar");
            bars.add(bar);
            visualizerRow.getChildren().add(bar);

            double peak = 46 - i * 4;
            Timeline t = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(bar.heightProperty(), 8, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(240 + i * 65),
                            new KeyValue(bar.heightProperty(), peak, Interpolator.EASE_BOTH))
            );
            t.setCycleCount(Timeline.INDEFINITE);
            t.setAutoReverse(true);
            barTimelines.add(t);
        }
    }

    private Node buildPlaylistPanel() {
        Label title = new Label("PLAYLIST");
        title.getStyleClass().add("panel-title");

        trackCountLabel.getStyleClass().add("track-count");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(6, title, spacer, trackCountLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        playlistView.getStyleClass().add("playlist");
        playlistView.setPlaceholder(new Label("Your playlist is empty"));
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        VBox panel = new VBox(10, titleRow, playlistView);
        panel.getStyleClass().add("playlist-panel");
        panel.setPrefWidth(226);
        panel.setMinWidth(196);
        panel.setPadding(new Insets(14));
        return panel;
    }

    private Node buildControls() {
        progressSlider.getStyleClass().add("progress-slider");
        progressSlider.setSkin(new FillSliderSkin(progressSlider));
        progressSlider.setDisable(true);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        timeLabel.getStyleClass().add("time-label");

        HBox progressRow = new HBox(10, progressSlider, timeLabel);
        progressRow.setAlignment(Pos.CENTER);
        progressRow.getStyleClass().add("progress-row");

        prevButton.getStyleClass().add("transport-button");
        playButton.getStyleClass().addAll("transport-button", "primary");
        stopButton.getStyleClass().add("transport-button");
        nextButton.getStyleClass().add("transport-button");

        HBox transport = new HBox(12, prevButton, playButton, stopButton, nextButton);
        transport.setAlignment(Pos.CENTER);
        transport.getStyleClass().add("transport-row");

        trackLabel.getStyleClass().add("track-label");
        trackLabel.setMaxWidth(240);

        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setSkin(new FillSliderSkin(volumeSlider));
        volumeSlider.setPrefWidth(110);
        volumeLabel.getStyleClass().add("volume-label");
        muteButton.getStyleClass().add("icon-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusRow = new HBox(10, trackLabel, spacer, muteButton, volumeSlider, volumeLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getStyleClass().add("status-row");

        VBox controls = new VBox(12, progressRow, transport, statusRow);
        controls.getStyleClass().add("controls-panel");
        controls.setPadding(new Insets(14, 18, 16, 18));
        return controls;
    }

    private static Button iconButton(String svg, String tooltip) {
        Button b = new Button();
        b.setGraphic(Icons.icon(svg, 20));
        b.setTooltip(new Tooltip(tooltip));
        b.getStyleClass().add("icon-button");
        return b;
    }

    public static void setIcon(Button button, String svg) {
        button.setGraphic(Icons.icon(svg, 20));
    }

    private static void makeNonFocusable(Node... nodes) {
        for (Node n : nodes) n.setFocusTraversable(false);
    }
}
