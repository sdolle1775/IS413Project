package com.example.is413project;

import java.util.Objects;

public class Bird {
    private String name;
    private int imageResourceId;

    public Bird(String name, int imageResourceId) {
        this.name = name;
        this.imageResourceId = imageResourceId;
    }

    public String getName() {
        return name;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Bird other = (Bird) obj;
        return name != null && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name); // More natural to hash the name
    }
}
