package com.example.is413project;

import java.util.Objects;

public class Bird {
    private String name;
    private int imageResId;

    public Bird(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bird)) return false;
        Bird bird = (Bird) o;
        return name.equals(bird.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
