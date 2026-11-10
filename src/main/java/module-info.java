module com.example.firstassigment {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;  // ← ADD THIS LINE
    requires javafx.base;

    opens com.example.firstassigment to javafx.fxml;
    exports com.example.firstassigment;
    exports com.example.firstassigment.view;
    opens com.example.firstassigment.view to javafx.fxml;
    exports com.example.firstassigment.controller;
    opens com.example.firstassigment.controller to javafx.fxml;
    exports com.example.firstassigment.model;
    opens com.example.firstassigment.model to javafx.fxml;
    exports com.example.firstassigment.util;
    opens com.example.firstassigment.util to javafx.fxml;
}