package com.example.alira.albumexporter.models;

import android.graphics.Bitmap;


public class Photo {

    private String id;
    private Bitmap source;

    public Photo(String id)
    {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Bitmap getSource() {
        return source;
    }

    public void setSource(Bitmap source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "Photo{" +
                "id='" + id + '\'' +
                ", source=" + source +
                '}';
    }
}
