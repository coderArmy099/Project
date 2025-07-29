package com.example.project;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CourseTopic {
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty(false);

    public CourseTopic(String name, boolean completed) {
        this.name.set(name);
        this.completed.set(completed);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public BooleanProperty completedProperty() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }
}