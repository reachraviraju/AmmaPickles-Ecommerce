package com.ammapickles.backend.entity;

public enum Size {
    SMALL("1/2 kg"),
    MEDIUM("1 kg"),
    LARGE("2 kg");

    private final String label;

    Size(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

